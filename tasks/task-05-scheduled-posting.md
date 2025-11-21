# タスク5: 定期実行システム

## 概要
EventBridge（CloudWatch Events）とLambdaを使って、定期的に話題を投稿する機能を実装します。

## 目的
- 平日10:00、15:00に自動実行
- 話題をランダムに選択して投稿
- 投稿履歴の管理

## 前提条件
- タスク1-4完了
- DynamoDBテーブル作成済み
- Slack App設定済み

## 実装内容

### 1. Lambda関数実装

#### 1.1 scheduled_poster/handler.py

```python
import os
import json
import random
from datetime import datetime, timedelta
from typing import Dict, List, Optional
import boto3
from boto3.dynamodb.conditions import Key
import sys
sys.path.append('/opt/python')  # Lambdaレイヤーのパス

from slack_client import SlackClient
from block_builder import BlockBuilder
from database import TopicsDB, EventsDB

# 環境変数
TOPICS_TABLE = os.environ['TOPICS_TABLE']
EVENTS_TABLE = os.environ['EVENTS_TABLE']
TARGET_CHANNEL = os.environ.get('TARGET_CHANNEL', 'C01234567')  # デフォルトチャンネル
CASUAL_RATIO = float(os.environ.get('CASUAL_RATIO', '0.4'))  # 雑談の割合
TECHNICAL_RATIO = float(os.environ.get('TECHNICAL_RATIO', '0.4'))  # 技術の割合

def lambda_handler(event, context):
    """定期投稿Lambda のエントリーポイント
    
    Args:
        event: EventBridgeからのイベント
        context: Lambda実行コンテキスト
    
    Returns:
        レスポンス辞書
    """
    print(f"Scheduled posting triggered at {datetime.now()}")
    
    try:
        # クライアント初期化
        slack = SlackClient()
        topics_db = TopicsDB(TOPICS_TABLE)
        events_db = EventsDB(EVENTS_TABLE)
        
        # 話題を選択
        topic = select_topic(topics_db)
        
        if not topic:
            print("No suitable topic found")
            return {
                'statusCode': 200,
                'body': json.dumps({'message': 'No topic available'})
            }
        
        # Block Kit形式でメッセージ作成
        blocks = BlockBuilder.topic_message(
            topic_text=topic['content'],
            emoji="📢",
            reaction_emojis=['👍', '❤️', '🎉']
        )
        
        # Slackに投稿
        response = slack.post_message(
            channel=TARGET_CHANNEL,
            text=topic['content'],  # フォールバック用
            blocks=blocks
        )
        
        message_ts = response['ts']
        print(f"Posted message: {message_ts}")
        
        # イベントトラッキングレコード作成
        event_data = {
            'slack_message_ts': message_ts,
            'channel_id': TARGET_CHANNEL,
            'topic_id': topic['topic_id'],
            'event_title': '',  # まだ未定
            'status': 'collecting_reactions',
            'reactions': [],
            'schedule_details': {},
            'calendar_event_id': '',
            'created_at': datetime.now().isoformat(),
            'updated_at': datetime.now().isoformat()
        }
        events_db.create_event(event_data)
        
        # 話題の使用情報を更新
        topics_db.update_usage(topic['topic_id'])
        
        return {
            'statusCode': 200,
            'body': json.dumps({
                'message': 'Topic posted successfully',
                'topic_id': topic['topic_id'],
                'message_ts': message_ts
            })
        }
    
    except Exception as e:
        print(f"Error: {str(e)}")
        return {
            'statusCode': 500,
            'body': json.dumps({'error': str(e)})
        }

def select_topic(topics_db: 'TopicsDB') -> Optional[Dict]:
    """話題を選択
    
    Args:
        topics_db: TopicsDBインスタンス
    
    Returns:
        選択された話題、またはNone
    """
    # カテゴリをランダムに決定
    rand = random.random()
    
    if rand < CASUAL_RATIO:
        category = 'casual'
    elif rand < CASUAL_RATIO + TECHNICAL_RATIO:
        category = 'technical'
    else:
        # 過去の会話ベースの話題（タスク7で実装）
        # 今は代わりにランダム選択
        category = random.choice(['casual', 'technical'])
    
    # 最近使っていない話題を取得
    two_weeks_ago = (datetime.now() - timedelta(days=14)).isoformat()
    candidates = topics_db.get_unused_topics(category, two_weeks_ago)
    
    if not candidates:
        print(f"No unused topics in category: {category}")
        # フォールバック: 全話題から選択
        candidates = topics_db.get_all_topics(category)
    
    if not candidates:
        return None
    
    # 平均リアクション数が多い話題を優先（重み付け）
    weights = [topic.get('average_reactions', 1) + 1 for topic in candidates]
    selected = random.choices(candidates, weights=weights, k=1)[0]
    
    return selected
```

#### 1.2 database.py（共通モジュール）

```python
import boto3
from boto3.dynamodb.conditions import Key, Attr
from datetime import datetime
from typing import Dict, List, Optional
import uuid

class TopicsDB:
    """話題マスターテーブルのアクセスクラス"""
    
    def __init__(self, table_name: str):
        dynamodb = boto3.resource('dynamodb')
        self.table = dynamodb.Table(table_name)
    
    def get_unused_topics(self, category: str, since: str) -> List[Dict]:
        """指定期間使用されていない話題を取得
        
        Args:
            category: 'casual' or 'technical'
            since: この日時以降使用されていないもの
        
        Returns:
            話題のリスト
        """
        response = self.table.query(
            IndexName='CategoryIndex',
            KeyConditionExpression=Key('category').eq(category) & Key('last_used_at').lt(since)
        )
        return response.get('Items', [])
    
    def get_all_topics(self, category: str) -> List[Dict]:
        """カテゴリの全話題を取得
        
        Args:
            category: 'casual' or 'technical'
        
        Returns:
            話題のリスト
        """
        response = self.table.query(
            IndexName='CategoryIndex',
            KeyConditionExpression=Key('category').eq(category)
        )
        return response.get('Items', [])
    
    def update_usage(self, topic_id: str) -> None:
        """話題の使用情報を更新
        
        Args:
            topic_id: 話題ID
        """
        now = datetime.now().isoformat()
        self.table.update_item(
            Key={'topic_id': topic_id},
            UpdateExpression='SET last_used_at = :now, usage_count = usage_count + :inc, updated_at = :now',
            ExpressionAttributeValues={
                ':now': now,
                ':inc': 1
            }
        )

class EventsDB:
    """イベントトラッキングテーブルのアクセスクラス"""
    
    def __init__(self, table_name: str):
        dynamodb = boto3.resource('dynamodb')
        self.table = dynamodb.Table(table_name)
    
    def create_event(self, event_data: Dict) -> str:
        """イベントトラッキングレコードを作成
        
        Args:
            event_data: イベントデータ
        
        Returns:
            event_tracking_id
        """
        event_id = str(uuid.uuid4())
        item = {
            'event_tracking_id': event_id,
            **event_data
        }
        self.table.put_item(Item=item)
        return event_id
    
    def get_event_by_message(self, message_ts: str) -> Optional[Dict]:
        """メッセージタイムスタンプからイベントを取得
        
        Args:
            message_ts: Slackメッセージタイムスタンプ
        
        Returns:
            イベントデータ、またはNone
        """
        response = self.table.query(
            IndexName='MessageIndex',
            KeyConditionExpression=Key('slack_message_ts').eq(message_ts)
        )
        items = response.get('Items', [])
        return items[0] if items else None
    
    def update_event(self, event_id: str, updates: Dict) -> None:
        """イベント情報を更新
        
        Args:
            event_id: event_tracking_id
            updates: 更新する項目の辞書
        """
        # UpdateExpressionを動的に生成
        update_expr_parts = []
        expr_attr_values = {}
        
        for key, value in updates.items():
            update_expr_parts.append(f"{key} = :{key}")
            expr_attr_values[f":{key}"] = value
        
        update_expr_parts.append("updated_at = :updated_at")
        expr_attr_values[":updated_at"] = datetime.now().isoformat()
        
        self.table.update_item(
            Key={'event_tracking_id': event_id},
            UpdateExpression='SET ' + ', '.join(update_expr_parts),
            ExpressionAttributeValues=expr_attr_values
        )
```

### 2. EventBridge設定

**Terraform（eventbridge.tf）:**
```hcl
# EventBridge ルール: 平日10:00
resource "aws_cloudwatch_event_rule" "scheduled_post_morning" {
  name                = "${var.project_name}-scheduled-post-morning"
  description         = "Post topic at 10:00 on weekdays"
  schedule_expression = "cron(0 1 ? * MON-FRI *)"  # UTC 1:00 = JST 10:00
  
  tags = local.common_tags
}

resource "aws_cloudwatch_event_target" "scheduled_post_morning_target" {
  rule      = aws_cloudwatch_event_rule.scheduled_post_morning.name
  target_id = "ScheduledPosterLambda"
  arn       = aws_lambda_function.scheduled_poster.arn
}

# EventBridge ルール: 平日15:00
resource "aws_cloudwatch_event_rule" "scheduled_post_afternoon" {
  name                = "${var.project_name}-scheduled-post-afternoon"
  description         = "Post topic at 15:00 on weekdays"
  schedule_expression = "cron(0 6 ? * MON-FRI *)"  # UTC 6:00 = JST 15:00
  
  tags = local.common_tags
}

resource "aws_cloudwatch_event_target" "scheduled_post_afternoon_target" {
  rule      = aws_cloudwatch_event_rule.scheduled_post_afternoon.name
  target_id = "ScheduledPosterLambda"
  arn       = aws_lambda_function.scheduled_poster.arn
}

# Lambda実行権限
resource "aws_lambda_permission" "allow_eventbridge_morning" {
  statement_id  = "AllowExecutionFromEventBridgeMorning"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.scheduled_poster.function_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.scheduled_post_morning.arn
}

resource "aws_lambda_permission" "allow_eventbridge_afternoon" {
  statement_id  = "AllowExecutionFromEventBridgeAfternoon"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.scheduled_poster.function_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.scheduled_post_afternoon.arn
}
```

### 3. Lambda関数設定

**Terraform（lambda_scheduled_poster.tf）:**
```hcl
# Lambda関数: Scheduled Poster
resource "aws_lambda_function" "scheduled_poster" {
  filename      = "lambda_packages/scheduled_poster.zip"
  function_name = "${var.project_name}-scheduled-poster"
  role          = aws_iam_role.lambda_execution_role.arn
  handler       = "handler.lambda_handler"
  runtime       = "python3.11"
  timeout       = 60
  memory_size   = 256
  
  layers = [aws_lambda_layer_version.shared_libs.arn]
  
  environment {
    variables = {
      SLACK_SECRET_NAME = aws_secretsmanager_secret.slack_credentials.name
      TOPICS_TABLE      = aws_dynamodb_table.topics.name
      EVENTS_TABLE      = aws_dynamodb_table.events.name
      TARGET_CHANNEL    = var.slack_target_channel
      CASUAL_RATIO      = "0.4"
      TECHNICAL_RATIO   = "0.4"
      AWS_REGION        = var.aws_region
      LOG_LEVEL         = "INFO"
    }
  }
  
  tags = local.common_tags
}

# Lambda Layer（共通ライブラリ）
resource "aws_lambda_layer_version" "shared_libs" {
  filename   = "lambda_packages/shared_layer.zip"
  layer_name = "${var.project_name}-shared-libs"
  
  compatible_runtimes = ["python3.11"]
  
  description = "Shared libraries for Slack Bot"
}
```

### 4. デプロイパッケージ作成

**build_lambda.sh:**
```bash
#!/bin/bash

# Scheduled Poster パッケージ
cd src/lambdas/scheduled_poster
zip -r ../../../lambda_packages/scheduled_poster.zip handler.py
cd ../../..

# Shared Layer パッケージ
mkdir -p lambda_packages/python
pip install -r requirements.txt -t lambda_packages/python/
cp -r src/shared/* lambda_packages/python/
cd lambda_packages
zip -r shared_layer.zip python/
rm -rf python/
cd ..

echo "Lambda packages created successfully"
```

## 成果物
- [ ] scheduled_poster Lambda関数実装完了
- [ ] database.py 共通モジュール実装完了
- [ ] EventBridgeルール設定完了
- [ ] Lambda Layer作成完了
- [ ] デプロイスクリプト作成完了

## 検証方法

```bash
# Lambda関数の手動実行テスト
aws lambda invoke \
  --function-name slack-bot-calendar-scheduled-poster \
  --payload '{}' \
  response.json

cat response.json

# CloudWatch Logsで確認
aws logs tail /aws/lambda/slack-bot-calendar-scheduled-poster --follow

# DynamoDBで投稿履歴確認
aws dynamodb scan --table-name SlackBotEvents --max-items 5
```

## 次のタスク
[タスク6: 話題生成エンジン](./task-06-topic-generator.md)

## 参考資料
- [EventBridge Schedule Expressions](https://docs.aws.amazon.com/eventbridge/latest/userguide/eb-create-rule-schedule.html)
- [Lambda Layers](https://docs.aws.amazon.com/lambda/latest/dg/configuration-layers.html)
