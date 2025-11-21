# タスク9: スケジュール作成機能

## 概要
ミーティング提案から日程調整、Googleカレンダーイベント作成までの一連のフローを実装します。

## 目的
- 対話形式での日程調整
- 日程投票機能
- Googleカレンダーへのイベント作成と参加者招待

## 実装内容

### 1. Schedule Creator Lambda

#### 1.1 schedule_creator/handler.py

```python
import os
import json
from datetime import datetime, timedelta
from typing import Dict, List, Optional
import sys
sys.path.append('/opt/python')

from slack_client import SlackClient
from calendar_client import CalendarClient
from block_builder import BlockBuilder
from calendar_utils import parse_japanese_datetime, format_datetime_japanese, calculate_end_time
from database import EventsDB

EVENTS_TABLE = os.environ['EVENTS_TABLE']
CALENDAR_ID = os.environ.get('CALENDAR_ID', 'primary')

def lambda_handler(event, context):
    """Schedule Creator Lambdaのエントリーポイント
    
    複数のトリガーから呼び出される:
    1. 手動実行（/schedule createコマンド）
    2. ミーティング提案後の自動フロー
    """
    print(f"Schedule creator invoked: {json.dumps(event)}")
    
    try:
        # Slack commandからの呼び出し
        if 'command' in event:
            return handle_slash_command(event)
        
        # Interactive componentからの呼び出し
        elif 'action' in event:
            return handle_interactive_action(event)
        
        # 直接呼び出し（テスト用）
        else:
            return {
                'statusCode': 200,
                'body': json.dumps({'message': 'Ready'})
            }
    
    except Exception as e:
        print(f"Error: {str(e)}")
        return {
            'statusCode': 500,
            'body': json.dumps({'error': str(e)})
        }

def handle_slash_command(event: Dict) -> Dict:
    """/schedule create コマンドを処理
    
    Args:
        event: コマンドイベント
    
    Returns:
        レスポンス
    """
    command_text = event.get('text', '')
    channel_id = event.get('channel_id')
    user_id = event.get('user_id')
    
    slack = SlackClient()
    
    # 対話開始
    message = (
        "📅 スケジュールを作成します！\n"
        "以下の情報を順番に教えてください。\n\n"
        "1️⃣ イベント名は何ですか？"
    )
    
    slack.post_message(
        channel=channel_id,
        text=message
    )
    
    return {
        'statusCode': 200,
        'body': json.dumps({'message': 'Started schedule creation'})
    }

def create_schedule_from_event(
    slack: SlackClient,
    calendar: CalendarClient,
    events_db: EventsDB,
    event_data: Dict
) -> Dict:
    """イベントデータからスケジュールを作成
    
    Args:
        slack: SlackClientインスタンス
        calendar: CalendarClientインスタンス
        events_db: EventsDBインスタンス
        event_data: イベント情報
    
    Returns:
        作成結果
    """
    # リアクションしたユーザーの情報を取得
    reactions = event_data.get('reactions', [])
    attendee_emails = [r['user_email'] for r in reactions if r.get('user_email')]
    
    # スケジュール詳細
    schedule_details = event_data.get('schedule_details', {})
    
    # 日時をパース
    date_time_str = schedule_details.get('date_time')
    start_time = parse_japanese_datetime(date_time_str)
    
    if not start_time:
        raise ValueError(f"Invalid date format: {date_time_str}")
    
    # 終了時刻を計算
    duration = schedule_details.get('duration_minutes', 60)
    end_time = calculate_end_time(start_time, duration)
    
    # Googleカレンダーにイベント作成
    calendar_event = calendar.create_event(
        summary=event_data.get('event_title', 'チームミーティング'),
        start_time=start_time,
        end_time=end_time,
        description=schedule_details.get('description', ''),
        location=schedule_details.get('location', ''),
        attendees=attendee_emails
    )
    
    # イベントIDを保存
    events_db.update_event(
        event_data['event_tracking_id'],
        {
            'calendar_event_id': calendar_event['id'],
            'status': 'completed'
        }
    )
    
    # Slackに通知
    participant_names = [r.get('user_id', 'User') for r in reactions]
    
    blocks = BlockBuilder.calendar_created(
        event_title=event_data.get('event_title'),
        date_time=format_datetime_japanese(start_time),
        location=schedule_details.get('location', ''),
        participants=participant_names,
        calendar_url=calendar_event['html_link']
    )
    
    slack.post_message(
        channel=event_data.get('channel_id'),
        text='✅ Googleカレンダーにイベントを作成しました！',
        blocks=blocks,
        thread_ts=event_data.get('slack_message_ts')
    )
    
    return calendar_event
```

### 2. 日程投票機能

#### 2.1 poll_handler.py（src/lambdas/schedule_creator/poll_handler.py）

```python
from typing import Dict, List
from datetime import datetime

class PollHandler:
    """日程投票の処理クラス"""
    
    @staticmethod
    def create_poll(
        slack,
        channel: str,
        thread_ts: str,
        date_options: List[str]
    ) -> None:
        """日程投票を作成
        
        Args:
            slack: SlackClientインスタンス
            channel: チャンネルID
            thread_ts: スレッドタイムスタンプ
            date_options: 日程候補のリスト
        """
        from block_builder import BlockBuilder
        
        # 絵文字リスト
        emojis = ['1️⃣', '2️⃣', '3️⃣', '4️⃣', '5️⃣']
        
        # オプションを整形
        formatted_options = []
        for i, date_str in enumerate(date_options[:5]):  # 最大5件
            formatted_options.append({
                'emoji': emojis[i],
                'date': date_str
            })
        
        blocks = BlockBuilder.schedule_poll(formatted_options)
        
        response = slack.post_message(
            channel=channel,
            text='📊 日程投票',
            blocks=blocks,
            thread_ts=thread_ts
        )
        
        # 各オプションに対応するリアクションを追加
        for i in range(len(formatted_options)):
            # 数字の絵文字をリアクションとして追加
            slack.add_reaction(
                channel=channel,
                timestamp=response['ts'],
                emoji=f"one" if i == 0 else f"two" if i == 1 else f"three" if i == 2 else f"four" if i == 3 else "five"
            )
    
    @staticmethod
    def count_votes(
        slack,
        channel: str,
        message_ts: str
    ) -> Dict[str, int]:
        """投票結果を集計
        
        Args:
            slack: SlackClientインスタンス
            channel: チャンネルID
            message_ts: 投票メッセージのタイムスタンプ
        
        Returns:
            {emoji: vote_count} の辞書
        """
        reactions = slack.get_reactions(channel, message_ts)
        
        vote_counts = {}
        for reaction in reactions:
            emoji = reaction.get('reaction')
            if emoji in ['one', 'two', 'three', 'four', 'five']:
                vote_counts[emoji] = vote_counts.get(emoji, 0) + 1
        
        return vote_counts
    
    @staticmethod
    def determine_winner(vote_counts: Dict[str, int]) -> str:
        """最多得票の選択肢を決定
        
        Args:
            vote_counts: 投票結果
        
        Returns:
            勝利した選択肢の絵文字
        """
        if not vote_counts:
            return None
        
        max_votes = max(vote_counts.values())
        winners = [emoji for emoji, count in vote_counts.items() if count == max_votes]
        
        # 同票の場合は最初のものを選択
        return winners[0]
```

### 3. 会話ステート管理

#### 3.1 conversation_state.py（src/shared/conversation_state.py）

```python
import boto3
from typing import Dict, Optional
import json

class ConversationState:
    """会話の状態を管理するクラス"""
    
    def __init__(self, table_name: str):
        dynamodb = boto3.resource('dynamodb')
        self.table = dynamodb.Table(table_name)
    
    def get_state(self, user_id: str, channel_id: str) -> Optional[Dict]:
        """会話の状態を取得
        
        Args:
            user_id: ユーザーID
            channel_id: チャンネルID
        
        Returns:
            状態辞書
        """
        key = f"{user_id}#{channel_id}"
        
        response = self.table.get_item(Key={'state_key': key})
        
        if 'Item' in response:
            return response['Item']
        return None
    
    def set_state(
        self,
        user_id: str,
        channel_id: str,
        step: str,
        data: Dict
    ) -> None:
        """会話の状態を保存
        
        Args:
            user_id: ユーザーID
            channel_id: チャンネルID
            step: 現在のステップ
            data: 収集したデータ
        """
        key = f"{user_id}#{channel_id}"
        
        self.table.put_item(
            Item={
                'state_key': key,
                'user_id': user_id,
                'channel_id': channel_id,
                'current_step': step,
                'collected_data': data,
                'updated_at': datetime.now().isoformat()
            }
        )
    
    def clear_state(self, user_id: str, channel_id: str) -> None:
        """会話の状態をクリア
        
        Args:
            user_id: ユーザーID
            channel_id: チャンネルID
        """
        key = f"{user_id}#{channel_id}"
        self.table.delete_item(Key={'state_key': key})
```

### 4. Lambda関数定義

**Terraform (lambda_schedule_creator.tf):**
```hcl
resource "aws_lambda_function" "schedule_creator" {
  filename      = "lambda_packages/schedule_creator.zip"
  function_name = "${var.project_name}-schedule-creator"
  role          = aws_iam_role.lambda_execution_role.arn
  handler       = "handler.lambda_handler"
  runtime       = "python3.11"
  timeout       = 60
  memory_size   = 512
  
  layers = [aws_lambda_layer_version.shared_libs.arn]
  
  environment {
    variables = {
      SLACK_SECRET_NAME  = aws_secretsmanager_secret.slack_credentials.name
      GOOGLE_SECRET_NAME = aws_secretsmanager_secret.google_credentials.name
      EVENTS_TABLE       = aws_dynamodb_table.events.name
      CALENDAR_ID        = var.google_calendar_id
      AWS_REGION         = var.aws_region
      LOG_LEVEL          = "INFO"
    }
  }
  
  tags = local.common_tags
}
```

## 成果物
- [ ] schedule_creator Lambda実装完了
- [ ] poll_handler.py 実装完了
- [ ] conversation_state.py 実装完了
- [ ] 日程投票機能実装完了
- [ ] Googleカレンダー連携完了

## 検証方法

```python
# スケジュール作成のエンドツーエンドテスト
# 1. botが話題を投稿
# 2. 3人以上がリアクション
# 3. botがミーティング提案
# 4. 日程候補を入力
# 5. 投票
# 6. Googleカレンダーに作成されるか確認

# Googleカレンダーで確認
# - イベントが作成されているか
# - 参加者に招待メールが送信されているか
# - イベント詳細が正しいか
```

## 次のタスク
[タスク10: 統合テスト](./task-10-integration-testing.md)

## 参考資料
- [Slack Block Kit](https://api.slack.com/block-kit)
- [Google Calendar Events](https://developers.google.com/calendar/api/v3/reference/events)
