# タスク7: 会話履歴分析

## 概要
過去のSlack会話を分析し、興味深い話題を抽出して定期投稿に活用します。

## 目的
- チャンネルメッセージ履歴の取得
- キーワード抽出と話題分析
- 人気のあった話題の再提起

## 実装内容

### 1. 会話履歴取得Lambda

#### 1.1 conversation_analyzer/handler.py

```python
import os
import json
from datetime import datetime, timedelta
from typing import Dict, List
import boto3
import sys
sys.path.append('/opt/python')

from slack_client import SlackClient
from database import ConversationsDB
from text_analyzer import TextAnalyzer

CONVERSATIONS_TABLE = os.environ['CONVERSATIONS_TABLE']
TARGET_CHANNELS = os.environ.get('TARGET_CHANNELS', '').split(',')  # カンマ区切り
ANALYSIS_DAYS = int(os.environ.get('ANALYSIS_DAYS', '7'))  # 過去7日分

def lambda_handler(event, context):
    """会話履歴分析Lambdaのエントリーポイント
    
    定期的に実行（例：毎日深夜）して会話を分析
    """
    print(f"Conversation analysis started at {datetime.now()}")
    
    try:
        slack = SlackClient()
        conversations_db = ConversationsDB(CONVERSATIONS_TABLE)
        analyzer = TextAnalyzer()
        
        # 分析対象期間
        since = datetime.now() - timedelta(days=ANALYSIS_DAYS)
        oldest_ts = str(since.timestamp())
        
        analyzed_count = 0
        
        for channel_id in TARGET_CHANNELS:
            if not channel_id:
                continue
            
            print(f"Analyzing channel: {channel_id}")
            
            # チャンネル履歴を取得
            messages = slack.get_channel_history(
                channel=channel_id,
                limit=1000,
                oldest=oldest_ts
            )
            
            # 各メッセージを分析
            for message in messages:
                # botメッセージはスキップ
                if message.get('subtype') == 'bot_message':
                    continue
                
                # リアクションが一定数以上のものを抽出
                reactions = message.get('reactions', [])
                reaction_count = sum([r['count'] for r in reactions])
                
                if reaction_count < 3:  # 閾値
                    continue
                
                # スレッドの返信数を取得
                reply_count = message.get('reply_count', 0)
                
                # 会話が盛り上がっているか判定
                if reaction_count >= 5 or reply_count >= 3:
                    # テキスト分析
                    text = message.get('text', '')
                    keywords = analyzer.extract_keywords(text)
                    sentiment = analyzer.analyze_sentiment(text)
                    
                    # 参加者を取得
                    participants = []
                    if 'user' in message:
                        participants.append(message['user'])
                    
                    # データベースに保存
                    conversation_data = {
                        'channel_id': channel_id,
                        'message_ts': message['ts'],
                        'keywords': keywords,
                        'participants': participants,
                        'reaction_count': reaction_count,
                        'comment_count': reply_count,
                        'sentiment': sentiment,
                        'is_used_for_topic': False,
                        'created_at': datetime.fromtimestamp(float(message['ts'])).isoformat(),
                        'analyzed_at': datetime.now().isoformat()
                    }
                    
                    conversations_db.save_conversation(conversation_data)
                    analyzed_count += 1
        
        print(f"Analysis completed: {analyzed_count} conversations saved")
        
        return {
            'statusCode': 200,
            'body': json.dumps({
                'message': 'Analysis completed',
                'analyzed_count': analyzed_count
            })
        }
    
    except Exception as e:
        print(f"Error: {str(e)}")
        return {
            'statusCode': 500,
            'body': json.dumps({'error': str(e)})
        }
```

#### 1.2 text_analyzer.py（src/shared/text_analyzer.py）

```python
import re
from typing import List
from collections import Counter

class TextAnalyzer:
    """テキスト分析ユーティリティ"""
    
    # 日本語・英語の一般的なストップワード
    STOP_WORDS = {
        'です', 'ます', 'した', 'ある', 'いる', 'この', 'その',
        'the', 'is', 'are', 'was', 'were', 'a', 'an', 'to'
    }
    
    def extract_keywords(self, text: str, top_n: int = 5) -> List[str]:
        """キーワードを抽出
        
        Args:
            text: 分析対象テキスト
            top_n: 抽出するキーワード数
        
        Returns:
            キーワードのリスト
        """
        # URLを除去
        text = re.sub(r'http\S+', '', text)
        
        # 記号を除去
        text = re.sub(r'[^\w\s]', ' ', text)
        
        # 単語に分割（日本語も対応するためスペース分割）
        words = text.lower().split()
        
        # ストップワードを除去
        words = [w for w in words if w not in self.STOP_WORDS and len(w) > 2]
        
        # 出現頻度をカウント
        word_counts = Counter(words)
        
        # 上位N件を返す
        top_words = [word for word, count in word_counts.most_common(top_n)]
        
        return top_words
    
    def analyze_sentiment(self, text: str) -> str:
        """感情分析（簡易版）
        
        Args:
            text: 分析対象テキスト
        
        Returns:
            'positive' | 'neutral' | 'negative'
        """
        # ポジティブ・ネガティブな単語のリスト（簡易版）
        positive_words = [
            '良い', 'いい', '最高', '素晴らしい', '楽しい', '嬉しい',
            'good', 'great', 'awesome', 'excellent', 'happy', '👍', '❤️', '🎉'
        ]
        
        negative_words = [
            '悪い', 'ダメ', '難しい', '困る', '失敗',
            'bad', 'difficult', 'problem', 'issue', '😢', '😞'
        ]
        
        text_lower = text.lower()
        
        positive_count = sum([1 for word in positive_words if word in text_lower])
        negative_count = sum([1 for word in negative_words if word in text_lower])
        
        if positive_count > negative_count:
            return 'positive'
        elif negative_count > positive_count:
            return 'negative'
        else:
            return 'neutral'
```

#### 1.3 database.py（ConversationsDB追加）

```python
class ConversationsDB:
    """会話履歴テーブルのアクセスクラス"""
    
    def __init__(self, table_name: str):
        dynamodb = boto3.resource('dynamodb')
        self.table = dynamodb.Table(table_name)
    
    def save_conversation(self, conversation_data: Dict) -> str:
        """会話データを保存
        
        Args:
            conversation_data: 会話データ
        
        Returns:
            conversation_id
        """
        import uuid
        conversation_id = str(uuid.uuid4())
        item = {
            'conversation_id': conversation_id,
            **conversation_data
        }
        self.table.put_item(Item=item)
        return conversation_id
    
    def get_popular_conversations(
        self,
        channel_id: str,
        days: int = 7,
        limit: int = 10
    ) -> List[Dict]:
        """人気のあった会話を取得
        
        Args:
            channel_id: チャンネルID
            days: 過去何日分
            limit: 取得数
        
        Returns:
            会話のリスト
        """
        since = (datetime.now() - timedelta(days=days)).isoformat()
        
        response = self.table.query(
            IndexName='PopularityIndex',
            KeyConditionExpression=Key('channel_id').eq(channel_id),
            ScanIndexForward=False,  # 降順
            Limit=limit
        )
        
        return response.get('Items', [])
    
    def get_unused_conversations(self, channel_id: str) -> List[Dict]:
        """まだ話題として使っていない会話を取得
        
        Args:
            channel_id: チャンネルID
        
        Returns:
            会話のリスト
        """
        response = self.table.query(
            IndexName='ChannelTimeIndex',
            KeyConditionExpression=Key('channel_id').eq(channel_id),
            FilterExpression='is_used_for_topic = :false',
            ExpressionAttributeValues={':false': False},
            ScanIndexForward=False,
            Limit=20
        )
        
        return response.get('Items', [])
    
    def mark_as_used(self, conversation_id: str) -> None:
        """会話を使用済みとしてマーク
        
        Args:
            conversation_id: 会話ID
        """
        self.table.update_item(
            Key={'conversation_id': conversation_id},
            UpdateExpression='SET is_used_for_topic = :true',
            ExpressionAttributeValues={':true': True}
        )
```

### 2. 過去の会話ベース話題生成

#### 2.1 scheduled_posterの拡張

```python
def select_conversation_based_topic(
    conversations_db: ConversationsDB,
    slack: SlackClient,
    channel_id: str
) -> Optional[Dict]:
    """過去の会話から話題を生成
    
    Args:
        conversations_db: ConversationsDBインスタンス
        slack: SlackClientインスタンス
        channel_id: チャンネルID
    
    Returns:
        生成された話題情報
    """
    # 未使用の人気会話を取得
    conversations = conversations_db.get_unused_conversations(channel_id)
    
    if not conversations:
        return None
    
    # リアクション数でソートして上位を選択
    conversations.sort(key=lambda x: x['reaction_count'], reverse=True)
    selected = conversations[0]
    
    # キーワードからトピック文を生成
    keywords = selected.get('keywords', [])
    keyword_text = '、'.join(keywords[:3]) if keywords else 'あのトピック'
    
    # 元の発言者を取得
    participants = selected.get('participants', [])
    mention = f"<@{participants[0]}>" if participants else 'メンバー'
    
    # メッセージリンクを生成
    message_ts = selected['message_ts'].replace('.', '')
    message_link = f"https://slack.com/archives/{channel_id}/p{message_ts}"
    
    topic_text = (
        f"💡 先週{mention}さんが話していた「{keyword_text}」について、\n"
        f"もっと詳しく聞きたい方はいますか？\n"
        f"<{message_link}|元の会話はこちら>"
    )
    
    # 使用済みとしてマーク
    conversations_db.mark_as_used(selected['conversation_id'])
    
    return {
        'content': topic_text,
        'conversation_id': selected['conversation_id'],
        'original_message_ts': selected['message_ts']
    }
```

### 3. EventBridge設定（会話分析）

**Terraform追加:**
```hcl
# 毎日深夜2:00に会話分析を実行
resource "aws_cloudwatch_event_rule" "conversation_analysis" {
  name                = "${var.project_name}-conversation-analysis"
  description         = "Analyze conversations daily"
  schedule_expression = "cron(0 17 * * ? *)"  # UTC 17:00 = JST 2:00
  
  tags = local.common_tags
}

resource "aws_cloudwatch_event_target" "conversation_analysis_target" {
  rule      = aws_cloudwatch_event_rule.conversation_analysis.name
  target_id = "ConversationAnalyzerLambda"
  arn       = aws_lambda_function.conversation_analyzer.arn
}

resource "aws_lambda_permission" "allow_eventbridge_analyzer" {
  statement_id  = "AllowExecutionFromEventBridge"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.conversation_analyzer.function_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.conversation_analysis.arn
}
```

## 成果物
- [ ] conversation_analyzer Lambda実装完了
- [ ] text_analyzer.py 実装完了
- [ ] ConversationsDB実装完了
- [ ] 過去会話ベース話題生成ロジック実装完了
- [ ] EventBridge設定完了

## 検証方法

```python
# テキスト分析のテスト
from text_analyzer import TextAnalyzer

analyzer = TextAnalyzer()

text = "最近Dockerを勉強していて、コンテナの仕組みが面白いです！"
keywords = analyzer.extract_keywords(text)
print(f"Keywords: {keywords}")

sentiment = analyzer.analyze_sentiment(text)
print(f"Sentiment: {sentiment}")
```

## 次のタスク
[タスク8: リアクション検知・処理](./task-08-reaction-handler.md)

## 参考資料
- [自然言語処理入門](https://www.nltk.org/)
- [形態素解析（日本語）](https://github.com/mocobeta/janome)
