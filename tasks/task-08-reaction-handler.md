# タスク8: リアクション検知・処理

## 概要
Slackのリアクションイベントを検知し、ミーティング提案やスケジュール作成のトリガーとして処理します。

## 目的
- reaction_addedイベントの処理
- リアクション数の閾値判定
- 自動ミーティング提案

## 実装内容

### 1. Reaction Handler Lambda

#### 1.1 reaction_handler/handler.py

```python
import os
import json
from datetime import datetime
from typing import Dict
import boto3
import sys
sys.path.append('/opt/python')

from slack_client import SlackClient
from block_builder import BlockBuilder
from database import EventsDB

EVENTS_TABLE = os.environ['EVENTS_TABLE']
MEETING_PROPOSAL_THRESHOLD = int(os.environ.get('MEETING_PROPOSAL_THRESHOLD', '3'))

def lambda_handler(event, context):
    """Reaction Handler Lambdaのエントリーポイント
    
    Slack Events APIからのリアクションイベントを処理
    """
    print(f"Received event: {json.dumps(event)}")
    
    # Slack URL verification
    if 'challenge' in event:
        return {
            'statusCode': 200,
            'body': event['challenge']
        }
    
    try:
        body = json.loads(event.get('body', '{}'))
        
        # イベントタイプの確認
        if body.get('type') == 'event_callback':
            slack_event = body.get('event', {})
            
            if slack_event.get('type') == 'reaction_added':
                handle_reaction_added(slack_event)
        
        return {
            'statusCode': 200,
            'body': json.dumps({'message': 'OK'})
        }
    
    except Exception as e:
        print(f"Error: {str(e)}")
        return {
            'statusCode': 500,
            'body': json.dumps({'error': str(e)})
        }

def handle_reaction_added(event: Dict) -> None:
    """reaction_addedイベントを処理
    
    Args:
        event: Slackイベントデータ
    """
    user_id = event.get('user')
    reaction = event.get('reaction')
    item = event.get('item', {})
    channel = item.get('channel')
    message_ts = item.get('ts')
    
    print(f"Reaction: {reaction} by {user_id} on {message_ts}")
    
    slack = SlackClient()
    events_db = EventsDB(EVENTS_TABLE)
    
    # このメッセージがbotの投稿かチェック
    tracked_event = events_db.get_event_by_message(message_ts)
    
    if not tracked_event:
        print("Not a tracked message, ignoring")
        return
    
    # ユーザー情報を取得
    user_info = slack.get_user_info(user_id)
    
    # リアクション情報を追加
    reaction_data = {
        'user_id': user_id,
        'user_email': user_info['email'],
        'reaction': reaction,
        'timestamp': datetime.now().isoformat()
    }
    
    reactions = tracked_event.get('reactions', [])
    
    # 既存のリアクションか確認（重複防止）
    existing = next((r for r in reactions if r['user_id'] == user_id), None)
    
    if not existing:
        reactions.append(reaction_data)
        
        # イベントを更新
        events_db.update_event(
            tracked_event['event_tracking_id'],
            {'reactions': reactions}
        )
        
        print(f"Reaction count: {len(reactions)}")
        
        # 閾値チェック
        if len(reactions) >= MEETING_PROPOSAL_THRESHOLD:
            status = tracked_event.get('status')
            
            # まだ提案していない場合のみ提案
            if status == 'collecting_reactions':
                propose_meeting(slack, channel, message_ts, len(reactions), tracked_event)
                
                # ステータスを更新
                events_db.update_event(
                    tracked_event['event_tracking_id'],
                    {'status': 'scheduling'}
                )

def propose_meeting(
    slack: SlackClient,
    channel: str,
    thread_ts: str,
    participant_count: int,
    tracked_event: Dict
) -> None:
    """ミーティングを提案
    
    Args:
        slack: SlackClientインスタンス
        channel: チャンネルID
        thread_ts: スレッドタイムスタンプ
        participant_count: 参加者数
        tracked_event: トラッキング中のイベント
    """
    blocks = BlockBuilder.meeting_proposal(participant_count)
    
    slack.post_message(
        channel=channel,
        text=f'この話題、盛り上がってますね！ミーティングを設定しませんか？',
        blocks=blocks,
        thread_ts=thread_ts
    )
    
    print(f"Meeting proposal posted to thread {thread_ts}")
```

### 2. インタラクティブイベント処理

#### 2.1 interactive_handler/handler.py

```python
import os
import json
from datetime import datetime
from typing import Dict
import sys
sys.path.append('/opt/python')

from slack_client import SlackClient
from database import EventsDB

EVENTS_TABLE = os.environ['EVENTS_TABLE']

def lambda_handler(event, context):
    """Interactive Handler Lambdaのエントリーポイント
    
    Slack Interactive Components (ボタンクリックなど) を処理
    """
    print(f"Received interactive event: {json.dumps(event)}")
    
    try:
        # SlackからのPOSTデータをパース
        body = event.get('body', '')
        
        # URL-encodedなのでデコード
        import urllib.parse
        parsed = urllib.parse.parse_qs(body)
        payload = json.loads(parsed['payload'][0])
        
        action_type = payload.get('type')
        
        if action_type == 'block_actions':
            handle_block_actions(payload)
        
        return {
            'statusCode': 200,
            'body': json.dumps({'message': 'OK'})
        }
    
    except Exception as e:
        print(f"Error: {str(e)}")
        return {
            'statusCode': 500,
            'body': json.dumps({'error': str(e)})
        }

def handle_block_actions(payload: Dict) -> None:
    """Block Actionsを処理
    
    Args:
        payload: Slackからのペイロード
    """
    actions = payload.get('actions', [])
    
    for action in actions:
        action_id = action.get('action_id')
        value = action.get('value')
        
        print(f"Action: {action_id}, Value: {value}")
        
        # アクションに応じた処理
        # 例: 日程投票、ミーティング確認など
```

### 3. API Gateway設定

**Terraform (api_gateway.tf 追加):**
```hcl
# /slack/interactive リソース
resource "aws_api_gateway_resource" "interactive" {
  rest_api_id = aws_api_gateway_rest_api.slack_bot.id
  parent_id   = aws_api_gateway_resource.slack.id
  path_part   = "interactive"
}

resource "aws_api_gateway_method" "interactive_post" {
  rest_api_id   = aws_api_gateway_rest_api.slack_bot.id
  resource_id   = aws_api_gateway_resource.interactive.id
  http_method   = "POST"
  authorization = "NONE"
}

resource "aws_api_gateway_integration" "interactive_lambda" {
  rest_api_id = aws_api_gateway_rest_api.slack_bot.id
  resource_id = aws_api_gateway_resource.interactive.id
  http_method = aws_api_gateway_method.interactive_post.http_method

  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_function.interactive_handler.invoke_arn
}

resource "aws_lambda_permission" "apigw_interactive" {
  statement_id  = "AllowAPIGatewayInvokeInteractive"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.interactive_handler.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.slack_bot.execution_arn}/*/*"
}
```

### 4. Lambda関数定義

**Terraform (lambda_handlers.tf):**
```hcl
# Reaction Handler Lambda
resource "aws_lambda_function" "reaction_handler" {
  filename      = "lambda_packages/reaction_handler.zip"
  function_name = "${var.project_name}-reaction-handler"
  role          = aws_iam_role.lambda_execution_role.arn
  handler       = "handler.lambda_handler"
  runtime       = "python3.11"
  timeout       = 30
  memory_size   = 256
  
  layers = [aws_lambda_layer_version.shared_libs.arn]
  
  environment {
    variables = {
      SLACK_SECRET_NAME           = aws_secretsmanager_secret.slack_credentials.name
      EVENTS_TABLE                = aws_dynamodb_table.events.name
      MEETING_PROPOSAL_THRESHOLD  = "3"
      AWS_REGION                  = var.aws_region
      LOG_LEVEL                   = "INFO"
    }
  }
  
  tags = local.common_tags
}

# Interactive Handler Lambda
resource "aws_lambda_function" "interactive_handler" {
  filename      = "lambda_packages/interactive_handler.zip"
  function_name = "${var.project_name}-interactive-handler"
  role          = aws_iam_role.lambda_execution_role.arn
  handler       = "handler.lambda_handler"
  runtime       = "python3.11"
  timeout       = 30
  memory_size   = 256
  
  layers = [aws_lambda_layer_version.shared_libs.arn]
  
  environment {
    variables = {
      SLACK_SECRET_NAME = aws_secretsmanager_secret.slack_credentials.name
      EVENTS_TABLE      = aws_dynamodb_table.events.name
      AWS_REGION        = var.aws_region
      LOG_LEVEL         = "INFO"
    }
  }
  
  tags = local.common_tags
}
```

### 5. Slack App設定更新

Event SubscriptionsのRequest URLを設定：
```
https://your-api-id.execute-api.ap-northeast-1.amazonaws.com/prod/slack/events
```

Interactive ComponentsのRequest URLを設定：
```
https://your-api-id.execute-api.ap-northeast-1.amazonaws.com/prod/slack/interactive
```

## 成果物
- [ ] reaction_handler Lambda実装完了
- [ ] interactive_handler Lambda実装完了
- [ ] API Gateway設定完了
- [ ] Slack App Event Subscriptions設定完了
- [ ] リアクション閾値判定ロジック実装完了

## 検証方法

```bash
# Slackで実際にリアクションを追加してテスト
# 1. botが投稿した話題にリアクション
# 2. 3人以上がリアクション
# 3. botがスレッドでミーティング提案を投稿するか確認

# CloudWatch Logsで確認
aws logs tail /aws/lambda/slack-bot-calendar-reaction-handler --follow

# DynamoDBでイベント確認
aws dynamodb get-item \
  --table-name SlackBotEvents \
  --key '{"event_tracking_id": {"S": "your-event-id"}}'
```

## 次のタスク
[タスク9: スケジュール作成機能](./task-09-schedule-creator.md)

## 参考資料
- [Slack Events API](https://api.slack.com/apis/connections/events-api)
- [Slack Interactive Components](https://api.slack.com/interactivity)
