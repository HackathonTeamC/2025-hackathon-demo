"""
Reaction Handler Lambda Function

Slackのリアクション追加イベントを処理するLambda関数。
API GatewayまたはSlack EventsAPIからトリガーされます。
"""

import json
import os
import sys

# 共通モジュールをインポート
from shared.slack_client import SlackClient
from shared.block_builder import BlockBuilder
from shared.database import DynamoDBClient
from shared.models import EventTracking


def handle_reaction_added(event_data: dict, slack: SlackClient, db: DynamoDBClient) -> dict:
    """リアクション追加イベントを処理
    
    Args:
        event_data: Slackイベントデータ
        slack: SlackClient
        db: DynamoDBClient
        
    Returns:
        dict: 処理結果
    """
    try:
        # イベント情報を抽出
        reaction = event_data.get('reaction')
        user_id = event_data.get('user')
        item = event_data.get('item', {})
        channel_id = item.get('channel')
        message_ts = item.get('ts')
        
        if not all([reaction, user_id, channel_id, message_ts]):
            return {'success': False, 'error': 'Missing required fields'}
        
        print(f"Reaction added: {reaction} by {user_id} on {channel_id}/{message_ts}")
        
        # 対象メッセージに対応するEventTrackingを取得
        event_tracking = db.get_event_by_message(message_ts, channel_id)
        
        if not event_tracking:
            print("No event tracking found for this message")
            return {'success': True, 'action': 'ignored', 'reason': 'not tracked'}
        
        # ユーザー情報を取得
        user_info = slack.get_user_info(user_id)
        user_email = user_info.get('email', '')
        
        # リアクションを追加
        db.add_reaction_to_event(
            event_tracking_id=event_tracking['event_tracking_id'],
            user_id=user_id,
            user_email=user_email,
            reaction=reaction
        )
        
        # 更新後のevent_trackingを取得
        event_tracking = db.get_event(event_tracking['event_tracking_id'])
        
        # リアクション数をカウント
        reactions = event_tracking.get('reactions', [])
        unique_users = set(r['user_id'] for r in reactions)
        reaction_count = len(unique_users)
        
        print(f"Total unique reactions: {reaction_count}")
        
        # 閾値チェック（3人以上のリアクション）
        threshold = int(os.environ.get('MEETING_PROPOSAL_THRESHOLD', '3'))
        
        if reaction_count >= threshold and event_tracking.get('status') == 'collecting_reactions':
            # ミーティング提案を投稿
            print(f"Threshold reached ({reaction_count} >= {threshold}), posting meeting proposal")
            
            blocks = BlockBuilder.meeting_proposal(participant_count=reaction_count)
            
            response = slack.post_message(
                channel=channel_id,
                text=f"この話題、盛り上がってますね！（{reaction_count}名が興味あり）",
                blocks=blocks,
                thread_ts=message_ts  # スレッドに投稿
            )
            
            if response.get('ok'):
                # ステータスを更新
                db.update_event_status(
                    event_tracking_id=event_tracking['event_tracking_id'],
                    status='scheduling'
                )
                
                return {
                    'success': True,
                    'action': 'meeting_proposed',
                    'reaction_count': reaction_count
                }
        
        return {
            'success': True,
            'action': 'reaction_recorded',
            'reaction_count': reaction_count
        }
    
    except Exception as e:
        print(f"Error handling reaction: {e}")
        import traceback
        traceback.print_exc()
        return {'success': False, 'error': str(e)}


def lambda_handler(event, context):
    """Lambda ハンドラー
    
    Args:
        event: API Gateway / Slack Events API イベント
        context: Lambda コンテキスト
        
    Returns:
        dict: レスポンス
    """
    print(f"Received event: {json.dumps(event)}")
    
    try:
        # API Gateway経由の場合、bodyをパース
        if 'body' in event:
            body = json.loads(event['body']) if isinstance(event['body'], str) else event['body']
        else:
            body = event
        
        # Slack URL Verification（初回セットアップ時）
        if body.get('type') == 'url_verification':
            return {
                'statusCode': 200,
                'body': json.dumps({'challenge': body['challenge']})
            }
        
        # Event Callback
        if body.get('type') == 'event_callback':
            event_data = body.get('event', {})
            event_type = event_data.get('type')
            
            if event_type == 'reaction_added':
                # クライアント初期化
                slack = SlackClient()
                db = DynamoDBClient()
                
                # リアクション処理
                result = handle_reaction_added(event_data, slack, db)
                
                return {
                    'statusCode': 200 if result['success'] else 500,
                    'body': json.dumps(result)
                }
            else:
                print(f"Unhandled event type: {event_type}")
                return {
                    'statusCode': 200,
                    'body': json.dumps({'message': 'Event type not handled'})
                }
        
        return {
            'statusCode': 200,
            'body': json.dumps({'message': 'OK'})
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
