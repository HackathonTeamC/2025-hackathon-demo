"""
Scheduled Poster Lambda Function

定期的に話題を投稿するLambda関数。
EventBridgeからトリガーされ、ランダムな話題または過去の会話から生成した話題をSlackに投稿します。
"""

import json
import os
import sys
import random
from datetime import datetime, timedelta
import uuid

# 共通モジュールをインポート
from shared.slack_client import SlackClient
from shared.block_builder import BlockBuilder
from shared.database import DynamoDBClient
from shared.models import Topic, EventTracking


def load_topics() -> dict:
    """話題マスターデータをロード"""
    topics_file = os.path.join(os.path.dirname(__file__), '..', '..', 'data', 'topics.json')
    with open(topics_file, 'r', encoding='utf-8') as f:
        return json.load(f)


def select_random_topic(db: DynamoDBClient) -> tuple:
    """ランダムに話題を選択
    
    Returns:
        tuple: (category, topic_content, reaction_emoji, topic_id)
    """
    # マスターデータをロード
    topics_data = load_topics()
    
    # カテゴリをランダムに選択（雑談40%, 技術40%, 過去の会話20%）
    # この関数ではランダム話題のみ扱う（雑談 or 技術）
    category = random.choice(['casual', 'technical'])
    
    if category == 'casual':
        topic_list = topics_data['casual_topics']
    else:
        topic_list = topics_data['technical_topics']
    
    # ランダムに話題を選択
    topic = random.choice(topic_list)
    
    # 最近使用していない話題を優先する（今回は簡易実装）
    # TODO: DynamoDBで使用履歴を確認し、最近使っていない話題を選ぶ
    
    # 新しいTopic IDを生成
    topic_id = Topic.generate_id()
    
    return (category, topic['content'], topic['reaction_emoji'], topic_id)


def select_question_target(slack: SlackClient, db: DynamoDBClient) -> tuple:
    """質問対象のユーザーを選択
    
    Returns:
        tuple: (user_id, user_name) or (None, None)
    """
    try:
        # ワークスペースの全ユーザーを取得
        users = slack.list_users()
        
        if not users:
            return (None, None)
        
        # Bot以外のアクティブユーザーをフィルタ
        active_users = [
            u for u in users
            if not u.get('is_bot', False)
            and not u.get('deleted', False)
            and u.get('id') != 'USLACKBOT'
        ]
        
        if not active_users:
            return (None, None)
        
        # 最近質問されていないユーザーを優先
        # 各ユーザーの最近の質問回数をチェック
        user_scores = []
        for user in active_users:
            recent_questions = db.get_recent_questions_for_user(user['id'], days=7)
            score = len(recent_questions)  # 少ないほど優先
            user_scores.append((user, score))
        
        # スコアでソート（質問回数が少ない順）
        user_scores.sort(key=lambda x: x[1])
        
        # 上位3名からランダムに選択（バリエーションのため）
        top_candidates = user_scores[:min(3, len(user_scores))]
        selected_user, _ = random.choice(top_candidates)
        
        return (selected_user['id'], selected_user.get('real_name', selected_user.get('name', 'メンバー')))
    
    except Exception as e:
        print(f"Error selecting question target: {e}")
        return (None, None)


def post_random_topic(slack: SlackClient, db: DynamoDBClient, channel_id: str) -> dict:
    """ランダムな話題を投稿
    
    Returns:
        dict: 投稿結果 {'success': bool, 'message_ts': str, 'topic_id': str}
    """
    try:
        # 話題を選択
        category, content, reaction_emoji, topic_id = select_random_topic(db)
        
        # Block Kitでメッセージを作成
        blocks = BlockBuilder.topic_message(
            topic_text=content,
            emoji='📢' if category == 'casual' else '💻',
            reaction_emojis=['thumbsup', 'heart', 'tada']
        )
        
        # Slackに投稿
        response = slack.post_message(
            channel=channel_id,
            text=content,  # フォールバック用
            blocks=blocks
        )
        
        if not response.get('ok'):
            return {'success': False, 'error': 'Failed to post message'}
        
        message_ts = response['ts']
        
        # Topicデータを保存
        topic = Topic(
            topic_id=topic_id,
            category=category,
            content=content,
            reaction_emoji=reaction_emoji,
            last_used_at=datetime.utcnow().isoformat()
        )
        db.put_topic(topic.to_dict())
        
        # EventTrackingを作成
        event_tracking = EventTracking(
            event_tracking_id=EventTracking.generate_id(),
            slack_message_ts=message_ts,
            channel_id=channel_id,
            topic_id=topic_id,
            event_title=None,
            status='collecting_reactions'
        )
        db.put_event(event_tracking.to_dict())
        
        print(f"Posted topic: {topic_id} ({category})")
        return {
            'success': True,
            'message_ts': message_ts,
            'topic_id': topic_id,
            'category': category
        }
    
    except Exception as e:
        print(f"Error posting random topic: {e}")
        import traceback
        traceback.print_exc()
        return {'success': False, 'error': str(e)}


def post_question_to_member(slack: SlackClient, db: DynamoDBClient, channel_id: str) -> dict:
    """メンバーへの質問を投稿
    
    Returns:
        dict: 投稿結果
    """
    try:
        # 質問対象を選択
        user_id, user_name = select_question_target(slack, db)
        
        if not user_id:
            print("No suitable user found for question")
            return {'success': False, 'error': 'No user found'}
        
        # 質問文を生成
        questions = [
            f"<@{user_id}>さん、最近取り組んでいるプロジェクトで面白いことはありますか？🤔",
            f"<@{user_id}>さん、最近学んだ技術や知識で、チームにシェアしたいことはありますか？📖",
            f"<@{user_id}>さん、最近の開発で工夫したポイントや、うまくいったことを教えてください！💡",
            f"<@{user_id}>さん、今取り組んでいる課題や、アドバイスが欲しいことはありますか？🤝",
            f"<@{user_id}>さん、最近読んだ技術記事や本でおすすめはありますか？📚"
        ]
        
        question_content = random.choice(questions)
        
        # メッセージを作成
        blocks = [
            {
                "type": "section",
                "text": {
                    "type": "mrkdwn",
                    "text": question_content
                }
            },
            {
                "type": "context",
                "elements": [
                    {
                        "type": "mrkdwn",
                        "text": "他の方も興味があれば 👀 のリアクションをください！"
                    }
                ]
            }
        ]
        
        # Slackに投稿
        response = slack.post_message(
            channel=channel_id,
            text=question_content,
            blocks=blocks
        )
        
        if not response.get('ok'):
            return {'success': False, 'error': 'Failed to post message'}
        
        message_ts = response['ts']
        
        # Questionデータを保存
        from shared.models import Question
        question = Question(
            question_id=Question.generate_id(),
            user_id=user_id,
            asked_at=datetime.utcnow().isoformat(),
            question_content=question_content,
            channel_id=channel_id,
            message_ts=message_ts
        )
        db.put_question(question.to_dict())
        
        # EventTrackingも作成
        event_tracking = EventTracking(
            event_tracking_id=EventTracking.generate_id(),
            slack_message_ts=message_ts,
            channel_id=channel_id,
            topic_id=None,
            event_title=None,
            status='collecting_reactions'
        )
        db.put_event(event_tracking.to_dict())
        
        print(f"Posted question to user: {user_id}")
        return {
            'success': True,
            'message_ts': message_ts,
            'user_id': user_id,
            'user_name': user_name
        }
    
    except Exception as e:
        print(f"Error posting question: {e}")
        import traceback
        traceback.print_exc()
        return {'success': False, 'error': str(e)}


def lambda_handler(event, context):
    """Lambda ハンドラー
    
    Args:
        event: EventBridgeイベント
        context: Lambda コンテキスト
        
    Returns:
        dict: 実行結果
    """
    print(f"Scheduled poster triggered: {json.dumps(event)}")
    
    try:
        # クライアント初期化
        slack = SlackClient()
        db = DynamoDBClient()
        
        # 投稿先チャンネル（環境変数から取得、デフォルトは#random）
        channel_id = os.environ.get('SLACK_CHANNEL_ID', 'C01234567')  # 実際のチャンネルIDに変更
        
        # 投稿タイプを決定（80%: ランダム話題, 20%: メンバーへの質問）
        post_type = random.choices(
            ['random_topic', 'member_question'],
            weights=[0.8, 0.2]
        )[0]
        
        if post_type == 'random_topic':
            result = post_random_topic(slack, db, channel_id)
        else:
            result = post_question_to_member(slack, db, channel_id)
        
        return {
            'statusCode': 200 if result['success'] else 500,
            'body': json.dumps(result)
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
