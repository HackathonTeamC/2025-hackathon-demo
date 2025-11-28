"""
Conversation Analyzer Lambda Function

過去の会話を分析して興味深い話題を抽出するLambda関数。
定期的に実行され、リアクションが多い会話やキーワードを抽出してデータベースに保存します。
"""

import json
import os
import sys
from datetime import datetime, timedelta
from collections import Counter
import re

# 共通モジュールをインポート
from shared.slack_client import SlackClient
from shared.database import DynamoDBClient
from shared.models import Conversation


def extract_keywords(text: str, min_length: int = 2) -> list:
    """テキストからキーワードを抽出
    
    Args:
        text: テキスト
        min_length: 最小文字数
        
    Returns:
        list: キーワードリスト
    """
    # 技術用語パターン
    tech_patterns = [
        r'\b[A-Z][a-z]+(?:[A-Z][a-z]+)+\b',  # CamelCase (e.g., JavaScript, TypeScript)
        r'\b[A-Z]{2,}\b',  # 大文字略語 (e.g., API, AWS, CI/CD)
        r'\w+(?:\.js|\.py|\.go|\.rb)\b',  # ファイル拡張子付き
    ]
    
    keywords = []
    
    # 技術用語を抽出
    for pattern in tech_patterns:
        matches = re.findall(pattern, text)
        keywords.extend(matches)
    
    # 日本語のキーワード抽出（簡易版）
    # TODO: 形態素解析ライブラリ（MeCab等）を使用して精度向上
    japanese_words = re.findall(r'[ぁ-んァ-ヶー一-龠]+', text)
    keywords.extend([w for w in japanese_words if len(w) >= min_length])
    
    # 英単語の抽出
    english_words = re.findall(r'\b[a-zA-Z]{3,}\b', text)
    keywords.extend(english_words)
    
    # 重複を除去して返す
    return list(set(keywords))


def analyze_sentiment(text: str, reaction_count: int) -> str:
    """感情分析（簡易版）
    
    Args:
        text: テキスト
        reaction_count: リアクション数
        
    Returns:
        str: 'positive', 'neutral', 'negative'
    """
    # ポジティブなキーワード
    positive_keywords = ['良い', '素晴らしい', '最高', 'いいね', '面白い', 'すごい', 'ありがとう', '成功', '解決']
    
    # ネガティブなキーワード
    negative_keywords = ['悪い', '問題', 'エラー', 'バグ', '失敗', '困った', '難しい']
    
    text_lower = text.lower()
    
    positive_count = sum(1 for kw in positive_keywords if kw in text_lower)
    negative_count = sum(1 for kw in negative_keywords if kw in text_lower)
    
    # リアクション数も考慮
    if reaction_count >= 5:
        positive_count += 2
    elif reaction_count >= 3:
        positive_count += 1
    
    if positive_count > negative_count:
        return 'positive'
    elif negative_count > positive_count:
        return 'negative'
    else:
        return 'neutral'


def analyze_channel_history(
    slack: SlackClient,
    db: DynamoDBClient,
    channel_id: str,
    days: int = 7
) -> dict:
    """チャンネルの会話履歴を分析
    
    Args:
        slack: SlackClient
        db: DynamoDBClient
        channel_id: チャンネルID
        days: 分析対象期間（日数）
        
    Returns:
        dict: 分析結果
    """
    try:
        # 対象期間を計算
        oldest_ts = (datetime.now() - timedelta(days=days)).timestamp()
        
        # チャンネル履歴を取得
        messages = slack.get_channel_history(
            channel=channel_id,  # ✅ 'channel_id' → 'channel' に変更
            oldest=str(oldest_ts),
            limit=1000
        )
        
        print(f"Retrieved {len(messages)} messages from channel {channel_id}")
        
        analyzed_conversations = []
        all_keywords = []
        
        for message in messages:
            # Bot メッセージは除外
            if message.get('bot_id') or message.get('subtype'):
                continue
            
            text = message.get('text', '')
            if not text or len(text) < 10:
                continue
            
            message_ts = message.get('ts')
            
            # リアクションを取得
            reactions = slack.get_reactions(channel_id, message_ts)
            reaction_count = sum(r.get('count', 0) for r in reactions)
            
            # リアクションが2件以上のメッセージのみ分析
            if reaction_count < 2:
                continue
            
            # キーワード抽出
            keywords = extract_keywords(text)
            all_keywords.extend(keywords)
            
            # 参加者（リアクションしたユーザー）を取得
            participants = []
            for reaction in reactions:
                participants.extend(reaction.get('users', []))
            participants = list(set(participants))
            
            # 感情分析
            sentiment = analyze_sentiment(text, reaction_count)
            
            # Conversationデータを作成
            conversation = Conversation(
                conversation_id=Conversation.generate_id(),
                channel_id=channel_id,
                message_ts=message_ts,
                keywords=keywords,
                participants=participants,
                reaction_count=reaction_count,
                sentiment=sentiment,
                is_used_for_topic=False
            )
            
            # データベースに保存
            db.put_conversation(conversation.to_dict())
            
            analyzed_conversations.append({
                'conversation_id': conversation.conversation_id,
                'keywords': keywords,
                'reaction_count': reaction_count,
                'sentiment': sentiment
            })
        
        # キーワードの頻度を集計
        keyword_counter = Counter(all_keywords)
        top_keywords = keyword_counter.most_common(20)
        
        print(f"Analyzed {len(analyzed_conversations)} conversations")
        print(f"Top keywords: {top_keywords[:10]}")
        
        return {
            'success': True,
            'analyzed_count': len(analyzed_conversations),
            'top_keywords': top_keywords,
            'conversations': analyzed_conversations
        }
    
    except Exception as e:
        print(f"Error analyzing channel history: {e}")
        import traceback
        traceback.print_exc()
        return {'success': False, 'error': str(e)}


def lambda_handler(event, context):
    """Lambda ハンドラー
    
    定期的に実行され、チャンネルの会話履歴を分析します。
    
    Args:
        event: EventBridgeイベント
        context: Lambda コンテキスト
        
    Returns:
        dict: 実行結果
    """
    print(f"Conversation analyzer triggered: {json.dumps(event)}")
    
    try:
        # クライアント初期化
        slack = SlackClient()
        db = DynamoDBClient()
        
        # 分析対象チャンネル（環境変数から取得）
        channel_ids = os.environ.get('SLACK_CHANNEL_IDS', 'C01234567').split(',')
        
        # 分析期間（日数）
        analysis_days = int(os.environ.get('ANALYSIS_DAYS', '7'))
        
        results = []
        
        for channel_id in channel_ids:
            channel_id = channel_id.strip()
            print(f"Analyzing channel: {channel_id}")
            
            result = analyze_channel_history(
                slack=slack,
                db=db,
                channel_id=channel_id,
                days=analysis_days
            )
            
            results.append({
                'channel_id': channel_id,
                'result': result
            })
        
        return {
            'statusCode': 200,
            'body': json.dumps({
                'success': True,
                'results': results
            })
        }
    
    except Exception as e:
        print(f"Error in lambda_handler: {e}")
        import traceback
        traceback.print_exc()
        
        return {
            'statusCode': 500,
            'body': json.dumps({
                'success': False,
                'error': str(e)
            })
        }
