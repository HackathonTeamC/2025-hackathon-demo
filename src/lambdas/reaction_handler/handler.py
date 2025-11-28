"""
Reaction Handler Lambda Function

Slackのリアクション追加イベントとメッセージイベントを処理するLambda関数。
API GatewayまたはSlack EventsAPIからトリガーされます。
"""

import json
import os
import sys
from datetime import datetime, timedelta

# 共通モジュールをインポート
from shared.slack_client import SlackClient
from shared.block_builder import BlockBuilder
from shared.calendar_client import CalendarClient
from shared.calendar_utils import (
    parse_japanese_datetime,
    format_datetime_japanese,
    calculate_end_time,
    parse_duration
)
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


def extract_datetime_from_message(text: str):
    """メッセージから日時情報を抽出
    
    Args:
        text: メッセージテキスト
        
    Returns:
        tuple: (parsed_datetime, confidence)
    """
    # 複数の日時パターンを試す
    lines = text.split('\n')
    for line in lines:
        dt = parse_japanese_datetime(line.strip())
        if dt:
            return (dt, 'high')
    
    # 全体でも試す
    dt = parse_japanese_datetime(text.strip())
    if dt:
        return (dt, 'medium')
    
    return (None, 'low')


def extract_duration_from_message(text: str) -> int:
    """メッセージから所要時間を抽出
    
    Args:
        text: メッセージテキスト
        
    Returns:
        int: 所要時間（分）、デフォルトは60分
    """
    duration = parse_duration(text)
    return duration if duration else 60


def handle_schedule_request(
    event_tracking_id: str,
    channel_id: str,
    thread_ts: str,
    user_id: str,
    message_text: str,
    slack: SlackClient,
    db: DynamoDBClient,
    calendar: CalendarClient
) -> dict:
    """スケジュール作成リクエストを処理
    
    Args:
        event_tracking_id: イベントトラッキングID
        channel_id: チャンネルID
        thread_ts: スレッドTS
        user_id: リクエストユーザーID
        message_text: メッセージテキスト
        slack: SlackClient
        db: DynamoDBClient
        calendar: CalendarClient
        
    Returns:
        dict: 処理結果
    """
    try:
        # EventTrackingを取得
        event_tracking = db.get_event(event_tracking_id)
        
        if not event_tracking:
            return {'success': False, 'error': 'Event not found'}
        
        # 状態によって処理を分岐
        status = event_tracking.get('status')
        
        if status == 'scheduling':
            # 日程情報を抽出
            parsed_dt, confidence = extract_datetime_from_message(message_text)
            
            if not parsed_dt:
                # 日時が解析できない場合は再度尋ねる
                slack.post_message(
                    channel=channel_id,
                    text="日時が認識できませんでした。「12/5 14:00」や「12月5日 14時」のような形式で教えてください。",
                    thread_ts=thread_ts
                )
                return {'success': True, 'action': 'asked_datetime_again'}
            
            # 所要時間を抽出（デフォルト60分）
            duration = extract_duration_from_message(message_text)
            end_dt = calculate_end_time(parsed_dt, duration)
            
            # イベントタイトルを決定
            topic_id = event_tracking.get('topic_id')
            if topic_id:
                topic = db.get_topic(topic_id)
                event_title = topic.get('content', 'ディスカッション')[:50] + ' - ミーティング'
            else:
                event_title = 'チームミーティング'
            
            # 参加者を取得
            reactions = event_tracking.get('reactions', [])
            attendee_emails = list(set(r['user_email'] for r in reactions if r.get('user_email')))
            
            if not attendee_emails:
                slack.post_message(
                    channel=channel_id,
                    text="参加者のメールアドレスが取得できませんでした。",
                    thread_ts=thread_ts
                )
                return {'success': False, 'error': 'No attendees'}
            
            # Googleカレンダーにイベントを作成
            description = f"Slackの話題から作成されたミーティングです。\n\n元のメッセージ: https://slack.com/archives/{channel_id}/p{thread_ts.replace('.', '')}"
            
            calendar_event = calendar.create_event(
                summary=event_title,
                start_time=parsed_dt,
                end_time=end_dt,
                description=description,
                location='',  # オンラインミーティングURLは別途設定
                attendees=attendee_emails
            )
            
            # EventTrackingを更新
            db.update_event_status(
                event_tracking_id=event_tracking_id,
                status='completed',
                calendar_event_id=calendar_event['id'],
                event_title=event_title
            )
            
            # 完了通知を投稿
            participant_names = [f"<@{r['user_id']}>" for r in reactions]
            
            blocks = BlockBuilder.calendar_created(
                event_title=event_title,
                date_time=format_datetime_japanese(parsed_dt),
                location='Google Meet（カレンダーをご確認ください）',
                participants=participant_names,
                calendar_url=calendar_event['html_link']
            )
            
            slack.post_message(
                channel=channel_id,
                text='カレンダーにイベントを作成しました！',
                blocks=blocks,
                thread_ts=thread_ts
            )
            
            return {
                'success': True,
                'action': 'event_created',
                'calendar_event_id': calendar_event['id']
            }
        
        return {'success': True, 'action': 'no_action'}
    
    except Exception as e:
        print(f"Error handling schedule request: {e}")
        import traceback
        traceback.print_exc()
        
        # エラーをSlackに通知
        error_blocks = BlockBuilder.error_message(
            error_text="スケジュール作成中にエラーが発生しました。",
            details=str(e)
        )
        slack.post_message(
            channel=channel_id,
            text="エラーが発生しました",
            blocks=error_blocks,
            thread_ts=thread_ts
        )
        
        return {'success': False, 'error': str(e)}


def handle_message_event(event_data: dict, slack: SlackClient, db: DynamoDBClient, calendar: CalendarClient) -> dict:
    """メッセージイベントを処理
    
    Args:
        event_data: Slackイベントデータ
        slack: SlackClient
        db: DynamoDBClient
        calendar: CalendarClient
        
    Returns:
        dict: 処理結果
    """
    try:
        # スレッドメッセージのみ処理
        thread_ts = event_data.get('thread_ts')
        if not thread_ts:
            return {'success': True, 'action': 'ignored', 'reason': 'not_thread_message'}
        
        channel_id = event_data.get('channel')
        user_id = event_data.get('user')
        message_text = event_data.get('text', '')
        
        # Bot自身のメッセージは無視
        if event_data.get('bot_id'):
            return {'success': True, 'action': 'ignored', 'reason': 'bot_message'}
        
        # スレッドの元メッセージに対応するEventTrackingを取得
        event_tracking = db.get_event_by_message(thread_ts, channel_id)
        
        if event_tracking and event_tracking.get('status') == 'scheduling':
            # スケジュール作成処理
            result = handle_schedule_request(
                event_tracking_id=event_tracking['event_tracking_id'],
                channel_id=channel_id,
                thread_ts=thread_ts,
                user_id=user_id,
                message_text=message_text,
                slack=slack,
                db=db,
                calendar=calendar
            )
            return result
        
        return {'success': True, 'action': 'ignored', 'reason': 'not_scheduling'}
    
    except Exception as e:
        print(f"Error handling message event: {e}")
        import traceback
        traceback.print_exc()
        return {'success': False, 'error': str(e)}


def lambda_handler(event, context):
    """Lambda ハンドラー
    
    Slack Events APIから受け取ったイベントを処理します。
    reaction_added と message イベントを処理します。
    
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
            
            elif event_type == 'message':
                # クライアント初期化
                slack = SlackClient()
                db = DynamoDBClient()
                calendar = CalendarClient(calendar_id=os.environ.get('CALENDAR_ID', 'primary'))

                
                # メッセージ処理
                result = handle_message_event(event_data, slack, db, calendar)
                
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