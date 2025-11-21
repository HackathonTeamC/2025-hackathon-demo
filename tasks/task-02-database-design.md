# タスク2: データベース設計・構築

## 概要
DynamoDBを使用したデータベース設計と構築を行います。

## 目的
- 話題マスターデータの管理
- 会話履歴の保存と分析
- イベントトラッキング
- メンバー質問履歴の管理

## 前提条件
- タスク1（インフラセットアップ）完了
- DynamoDBの基本理解

## DynamoDBテーブル設計

### 1. SlackBotTopics（話題マスターテーブル）

**用途**: 定期投稿用の話題テンプレートを管理

**テーブル設定:**
```python
{
    'TableName': 'SlackBotTopics',
    'KeySchema': [
        {'AttributeName': 'topic_id', 'KeyType': 'HASH'}  # Partition Key
    ],
    'AttributeDefinitions': [
        {'AttributeName': 'topic_id', 'AttributeType': 'S'},
        {'AttributeName': 'category', 'AttributeType': 'S'},
        {'AttributeName': 'last_used_at', 'AttributeType': 'S'}
    ],
    'GlobalSecondaryIndexes': [
        {
            'IndexName': 'CategoryIndex',
            'KeySchema': [
                {'AttributeName': 'category', 'KeyType': 'HASH'},
                {'AttributeName': 'last_used_at', 'KeyType': 'RANGE'}
            ],
            'Projection': {'ProjectionType': 'ALL'},
            'ProvisionedThroughput': {
                'ReadCapacityUnits': 5,
                'WriteCapacityUnits': 5
            }
        }
    ],
    'BillingMode': 'PAY_PER_REQUEST'
}
```

**データ構造:**
```python
{
    'topic_id': 'uuid-string',  # UUID
    'category': 'casual|technical',  # 話題カテゴリ
    'content': '最近ハマっているゲームや趣味はありますか？',  # 話題本文
    'reaction_emoji': '🎮',  # 推奨リアクション
    'last_used_at': '2025-11-21T10:00:00Z',  # ISO 8601形式
    'usage_count': 5,  # 使用回数
    'average_reactions': 8.5,  # 平均リアクション数
    'created_at': '2025-11-01T00:00:00Z',
    'updated_at': '2025-11-21T10:00:00Z'
}
```

**GSI（Global Secondary Index）:**
- `CategoryIndex`: カテゴリと最終使用日時でクエリ（重複防止）

### 2. SlackBotConversations（会話履歴テーブル）

**用途**: 過去の会話から話題を抽出するための分析データ

**テーブル設定:**
```python
{
    'TableName': 'SlackBotConversations',
    'KeySchema': [
        {'AttributeName': 'conversation_id', 'KeyType': 'HASH'}
    ],
    'AttributeDefinitions': [
        {'AttributeName': 'conversation_id', 'AttributeType': 'S'},
        {'AttributeName': 'channel_id', 'AttributeType': 'S'},
        {'AttributeName': 'created_at', 'AttributeType': 'S'},
        {'AttributeName': 'reaction_count', 'AttributeType': 'N'}
    ],
    'GlobalSecondaryIndexes': [
        {
            'IndexName': 'ChannelTimeIndex',
            'KeySchema': [
                {'AttributeName': 'channel_id', 'KeyType': 'HASH'},
                {'AttributeName': 'created_at', 'KeyType': 'RANGE'}
            ],
            'Projection': {'ProjectionType': 'ALL'}
        },
        {
            'IndexName': 'PopularityIndex',
            'KeySchema': [
                {'AttributeName': 'channel_id', 'KeyType': 'HASH'},
                {'AttributeName': 'reaction_count', 'KeyType': 'RANGE'}
            ],
            'Projection': {'ProjectionType': 'ALL'}
        }
    ],
    'BillingMode': 'PAY_PER_REQUEST'
}
```

**データ構造:**
```python
{
    'conversation_id': 'uuid-string',
    'channel_id': 'C01234567',
    'message_ts': '1234567890.123456',  # Slackメッセージタイムスタンプ
    'keywords': ['GraphQL', 'REST', 'API'],  # 抽出されたキーワード
    'participants': ['U01234567', 'U01234568'],  # 参加者のユーザーID
    'reaction_count': 12,
    'comment_count': 5,
    'sentiment': 'positive',  # positive|neutral|negative
    'is_used_for_topic': False,  # 話題として使用済みか
    'created_at': '2025-11-15T14:30:00Z',
    'analyzed_at': '2025-11-16T10:00:00Z'
}
```

**GSI:**
- `ChannelTimeIndex`: チャンネル別の時系列クエリ
- `PopularityIndex`: 人気度でソート（話題選定に使用）

### 3. SlackBotEvents（イベントトラッキングテーブル）

**用途**: スケジュール作成の進行状況を管理

**テーブル設定:**
```python
{
    'TableName': 'SlackBotEvents',
    'KeySchema': [
        {'AttributeName': 'event_tracking_id', 'KeyType': 'HASH'}
    ],
    'AttributeDefinitions': [
        {'AttributeName': 'event_tracking_id', 'AttributeType': 'S'},
        {'AttributeName': 'slack_message_ts', 'AttributeType': 'S'},
        {'AttributeName': 'status', 'AttributeType': 'S'},
        {'AttributeName': 'created_at', 'AttributeType': 'S'}
    ],
    'GlobalSecondaryIndexes': [
        {
            'IndexName': 'MessageIndex',
            'KeySchema': [
                {'AttributeName': 'slack_message_ts', 'KeyType': 'HASH'}
            ],
            'Projection': {'ProjectionType': 'ALL'}
        },
        {
            'IndexName': 'StatusIndex',
            'KeySchema': [
                {'AttributeName': 'status', 'KeyType': 'HASH'},
                {'AttributeName': 'created_at', 'KeyType': 'RANGE'}
            ],
            'Projection': {'ProjectionType': 'ALL'}
        }
    ],
    'BillingMode': 'PAY_PER_REQUEST'
}
```

**データ構造:**
```python
{
    'event_tracking_id': 'uuid-string',
    'slack_message_ts': '1234567890.123456',
    'channel_id': 'C01234567',
    'topic_id': 'uuid-string',  # 元になった話題ID（オプション）
    'event_title': 'Docker勉強会',
    'status': 'collecting_reactions',  # collecting_reactions|scheduling|completed|cancelled
    'reactions': [
        {
            'user_id': 'U01234567',
            'user_email': 'user@example.com',
            'reaction': '👍',
            'timestamp': '2025-11-21T10:00:00Z'
        }
    ],
    'schedule_details': {
        'date_time': '2025-12-05T14:00:00+09:00',
        'duration_minutes': 120,
        'location': 'https://meet.google.com/xxx-yyyy-zzz',
        'description': 'Dockerの基礎から実践まで'
    },
    'calendar_event_id': 'abc123xyz',  # Googleカレンダーイベントid
    'created_at': '2025-11-21T09:00:00Z',
    'updated_at': '2025-11-21T10:30:00Z'
}
```

**GSI:**
- `MessageIndex`: Slackメッセージから逆引き
- `StatusIndex`: ステータス別の一覧取得

### 4. SlackBotQuestions（メンバー質問履歴テーブル）

**用途**: ランダム質問の履歴管理（同じ人に短期間で質問しないため）

**テーブル設定:**
```python
{
    'TableName': 'SlackBotQuestions',
    'KeySchema': [
        {'AttributeName': 'question_id', 'KeyType': 'HASH'}
    ],
    'AttributeDefinitions': [
        {'AttributeName': 'question_id', 'AttributeType': 'S'},
        {'AttributeName': 'user_id', 'AttributeType': 'S'},
        {'AttributeName': 'asked_at', 'AttributeType': 'S'}
    ],
    'GlobalSecondaryIndexes': [
        {
            'IndexName': 'UserTimeIndex',
            'KeySchema': [
                {'AttributeName': 'user_id', 'KeyType': 'HASH'},
                {'AttributeName': 'asked_at', 'KeyType': 'RANGE'}
            ],
            'Projection': {'ProjectionType': 'ALL'}
        }
    ],
    'BillingMode': 'PAY_PER_REQUEST'
}
```

**データ構造:**
```python
{
    'question_id': 'uuid-string',
    'user_id': 'U01234567',
    'channel_id': 'C01234567',
    'message_ts': '1234567890.123456',
    'asked_at': '2025-11-21T10:00:00Z',
    'question_content': '最近取り組んでいるプロジェクトで面白いことはありますか？',
    'response_count': 5,
    'reaction_count': 8,
    'created_meeting': False  # ミーティング作成に至ったか
}
```

**GSI:**
- `UserTimeIndex`: ユーザーごとの質問履歴を時系列で取得

## Terraform実装

**dynamodb.tf:**
```hcl
# 1. Topics Table
resource "aws_dynamodb_table" "topics" {
  name           = "SlackBotTopics"
  billing_mode   = "PAY_PER_REQUEST"
  hash_key       = "topic_id"

  attribute {
    name = "topic_id"
    type = "S"
  }

  attribute {
    name = "category"
    type = "S"
  }

  attribute {
    name = "last_used_at"
    type = "S"
  }

  global_secondary_index {
    name            = "CategoryIndex"
    hash_key        = "category"
    range_key       = "last_used_at"
    projection_type = "ALL"
  }

  ttl {
    enabled        = false
  }

  tags = local.common_tags
}

# 2. Conversations Table
resource "aws_dynamodb_table" "conversations" {
  name           = "SlackBotConversations"
  billing_mode   = "PAY_PER_REQUEST"
  hash_key       = "conversation_id"

  attribute {
    name = "conversation_id"
    type = "S"
  }

  attribute {
    name = "channel_id"
    type = "S"
  }

  attribute {
    name = "created_at"
    type = "S"
  }

  attribute {
    name = "reaction_count"
    type = "N"
  }

  global_secondary_index {
    name            = "ChannelTimeIndex"
    hash_key        = "channel_id"
    range_key       = "created_at"
    projection_type = "ALL"
  }

  global_secondary_index {
    name            = "PopularityIndex"
    hash_key        = "channel_id"
    range_key       = "reaction_count"
    projection_type = "ALL"
  }

  tags = local.common_tags
}

# 3. Events Table
resource "aws_dynamodb_table" "events" {
  name           = "SlackBotEvents"
  billing_mode   = "PAY_PER_REQUEST"
  hash_key       = "event_tracking_id"

  attribute {
    name = "event_tracking_id"
    type = "S"
  }

  attribute {
    name = "slack_message_ts"
    type = "S"
  }

  attribute {
    name = "status"
    type = "S"
  }

  attribute {
    name = "created_at"
    type = "S"
  }

  global_secondary_index {
    name            = "MessageIndex"
    hash_key        = "slack_message_ts"
    projection_type = "ALL"
  }

  global_secondary_index {
    name            = "StatusIndex"
    hash_key        = "status"
    range_key       = "created_at"
    projection_type = "ALL"
  }

  tags = local.common_tags
}

# 4. Questions Table
resource "aws_dynamodb_table" "questions" {
  name           = "SlackBotQuestions"
  billing_mode   = "PAY_PER_REQUEST"
  hash_key       = "question_id"

  attribute {
    name = "question_id"
    type = "S"
  }

  attribute {
    name = "user_id"
    type = "S"
  }

  attribute {
    name = "asked_at"
    type = "S"
  }

  global_secondary_index {
    name            = "UserTimeIndex"
    hash_key        = "user_id"
    range_key       = "asked_at"
    projection_type = "ALL"
  }

  tags = local.common_tags
}

# Outputs
output "topics_table_name" {
  value = aws_dynamodb_table.topics.name
}

output "conversations_table_name" {
  value = aws_dynamodb_table.conversations.name
}

output "events_table_name" {
  value = aws_dynamodb_table.events.name
}

output "questions_table_name" {
  value = aws_dynamodb_table.questions.name
}
```

## 初期データ投入

**topics_seed_data.json:**
```json
[
  {
    "topic_id": "casual-001",
    "category": "casual",
    "content": "☕ 今日のランチは何を食べましたか？おすすめがあればシェアしてください！",
    "reaction_emoji": "👍",
    "last_used_at": "2025-01-01T00:00:00Z",
    "usage_count": 0,
    "average_reactions": 0,
    "created_at": "2025-11-21T00:00:00Z"
  },
  {
    "topic_id": "technical-001",
    "category": "technical",
    "content": "💻 最近使ってみて良かった開発ツールやライブラリはありますか？",
    "reaction_emoji": "🚀",
    "last_used_at": "2025-01-01T00:00:00Z",
    "usage_count": 0,
    "average_reactions": 0,
    "created_at": "2025-11-21T00:00:00Z"
  }
]
```

**投入スクリプト (Python):**
```python
import boto3
import json
from datetime import datetime

dynamodb = boto3.resource('dynamodb', region_name='ap-northeast-1')
table = dynamodb.Table('SlackBotTopics')

with open('topics_seed_data.json', 'r', encoding='utf-8') as f:
    topics = json.load(f)

for topic in topics:
    table.put_item(Item=topic)
    print(f"Inserted: {topic['topic_id']}")
```

## 成果物
- [ ] 4つのDynamoDBテーブル作成完了
- [ ] GSI設定完了
- [ ] Terraformコード作成完了
- [ ] 初期データ投入完了
- [ ] データモデルドキュメント作成完了

## 検証方法
```bash
# テーブル一覧確認
aws dynamodb list-tables

# テーブル詳細確認
aws dynamodb describe-table --table-name SlackBotTopics

# データ確認
aws dynamodb scan --table-name SlackBotTopics --max-items 5
```

## 次のタスク
[タスク3: Slack App構築](./task-03-slack-app-setup.md)

## 参考資料
- [DynamoDB ベストプラクティス](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/best-practices.html)
- [DynamoDB GSI設計](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/GSI.html)
