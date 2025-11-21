# Slack Bot × Google Calendar Integration System

Slackワークスペース内でbotを通じて会話を促進し、その反応を基に複数人のスケジュールをGoogleカレンダーに自動作成するシステムです。

## 概要

このシステムは以下の機能を提供します：
- 定期的な話題提供（平日 10:00, 15:00）
- リアクションベースの参加者管理
- 自動ミーティング提案
- Googleカレンダーへのイベント作成

## ドキュメント

- [仕様書](./slack-bot-calendar-specification.md) - システムの完全な仕様
- [実装タスク](./tasks/README.md) - タスク分割と実装手順

## 技術スタック

- **インフラ**: AWS (Lambda, EventBridge, DynamoDB, API Gateway, Secrets Manager)
- **言語**: Python 3.11
- **Slack SDK**: slack-bolt
- **Google API**: google-api-python-client
- **IaC**: Terraform

## ディレクトリ構造

```
2025-hackathon-demo/
├── slack-bot-calendar-specification.md  # 仕様書
├── tasks/                               # 実装タスク
│   ├── README.md
│   ├── task-01-infrastructure-setup.md
│   └── ...
├── src/                                 # ソースコード
│   ├── lambdas/                         # Lambda関数
│   │   ├── scheduled_poster/            # 定期投稿
│   │   ├── reaction_handler/            # リアクション処理
│   │   ├── schedule_creator/            # スケジュール作成
│   │   └── conversation_analyzer/       # 会話分析
│   ├── shared/                          # 共通ライブラリ
│   │   ├── slack_client.py              # Slack APIクライアント
│   │   ├── block_builder.py             # Block Kitヘルパー
│   │   ├── calendar_client.py           # Google Calendar API
│   │   └── database.py                  # DynamoDB操作
│   └── data/                            # マスターデータ
│       └── topics.json                  # 話題テンプレート
├── infrastructure/                      # IaC (Terraform)
│   ├── main.tf
│   ├── lambda.tf
│   ├── dynamodb.tf
│   └── eventbridge.tf
├── tests/                               # テストコード
│   ├── unit/
│   └── integration/
└── requirements.txt                     # Python依存関係
```

## セットアップ

### 前提条件

- AWSアカウント
- Slackワークスペースの管理者権限
- Google Workspace管理者権限（または個人のGoogleアカウント）
- Python 3.11以上
- AWS CLI設定済み

### インストール

```bash
# リポジトリのクローン
git clone https://github.com/HackathonTeamC/2025-hackathon-demo.git
cd 2025-hackathon-demo

# Python依存パッケージのインストール
pip install -r requirements.txt
```

## 使い方

### Slack Client

```python
from src.shared.slack_client import SlackClient

# クライアントの初期化
client = SlackClient()

# メッセージ投稿
response = client.post_message(
    channel='C01234567',
    text='Hello, Slack!'
)

# ユーザー情報取得
user_info = client.get_user_info('U01234567')
print(f"User: {user_info['name']} <{user_info['email']}>")
```

### Block Builder

```python
from src.shared.block_builder import BlockBuilder

# 話題メッセージの作成
blocks = BlockBuilder.topic_message(
    topic_text='最近読んだ本で面白かったものを教えてください！',
    emoji='📚',
    reaction_emojis=['thumbsup', 'heart', 'tada']
)

# Slackに投稿
client.post_message(
    channel='C01234567',
    text='話題投稿',
    blocks=blocks
)
```

## 実装タスクの実施

実装は以下の順序で進めてください：

1. [タスク1: インフラセットアップ](./tasks/task-01-infrastructure-setup.md)
2. [タスク2: データベース設計](./tasks/task-02-database-design.md)
3. [タスク3: Slack App構築](./tasks/task-03-slack-app-setup.md)
4. [タスク4: Google Calendar API連携](./tasks/task-04-google-calendar-api.md)
5. [タスク5: 定期実行システム](./tasks/task-05-scheduled-posting.md)
6. [タスク6: 話題生成エンジン](./tasks/task-06-topic-generator.md)
7. [タスク7: 会話履歴分析](./tasks/task-07-conversation-analyzer.md)
8. [タスク8: リアクション検知・処理](./tasks/task-08-reaction-handler.md)
9. [タスク9: スケジュール作成機能](./tasks/task-09-schedule-creator.md)
10. [タスク10: 統合テスト](./tasks/task-10-integration-testing.md)

## 開発状況

### 完了済み
- ✅ 仕様書作成
- ✅ タスク分割と詳細設計
- ✅ Slack Client 共通モジュール実装
- ✅ Block Builder 共通モジュール実装

### 進行中
- 🔄 Lambda関数の実装

### 予定
- ⏳ インフラ構築（Terraform）
- ⏳ DynamoDBテーブル作成
- ⏳ 統合テスト

## テスト

```bash
# 依存パッケージのインストール
pip install -r requirements.txt
pip install -r requirements-dev.txt

# 単体テスト実行
pytest tests/unit/ -v

# 統合テスト実行
pytest tests/integration/ -v

# カバレッジレポート生成
pytest --cov=src --cov-report=html
```

## ライセンス

MIT License

## 貢献

プルリクエストを歓迎します。大きな変更の場合は、まずissueを開いて変更内容を議論してください。

## 連絡先

- Repository: https://github.com/HackathonTeamC/2025-hackathon-demo
- Issues: https://github.com/HackathonTeamC/2025-hackathon-demo/issues