# 実装タスク一覧

Slack Bot × Googleカレンダー連携システムの実装を以下のタスクに分割しています。

## タスク実施順序

### フェーズ1: 基盤構築（1-3週目）
1. [タスク1: インフラセットアップ](./task-01-infrastructure-setup.md) - AWS環境構築
2. [タスク2: データベース設計・構築](./task-02-database-design.md) - DynamoDB設計
3. [タスク3: Slack App構築](./task-03-slack-app-setup.md) - Slack連携基盤
4. [タスク4: Google Calendar API連携](./task-04-google-calendar-api.md) - カレンダー連携

### フェーズ2: コア機能実装（4-6週目）
5. [タスク5: 定期実行システム](./task-05-scheduled-posting.md) - EventBridge + Lambda
6. [タスク6: 話題生成エンジン](./task-06-topic-generator.md) - ランダム話題生成
7. [タスク7: 会話履歴分析](./task-07-conversation-analyzer.md) - 過去の会話分析
8. [タスク8: リアクション検知・処理](./task-08-reaction-handler.md) - リアクションイベント処理

### フェーズ3: スケジュール機能（7-8週目）
9. [タスク9: スケジュール作成機能](./task-09-schedule-creator.md) - カレンダーイベント作成
10. [タスク10: 統合テスト](./task-10-integration-testing.md) - エンドツーエンドテスト

## 技術スタック

- **インフラ**: AWS (Lambda, EventBridge, DynamoDB, API Gateway, Secrets Manager)
- **言語**: Python 3.11
- **Slack SDK**: slack-bolt (Python)
- **Google API**: google-auth, google-api-python-client
- **その他**: boto3 (AWS SDK)

## 前提条件

- AWSアカウント
- Slackワークスペースの管理者権限
- Google Workspace管理者権限（または個人のGoogleアカウント）
- Python 3.11以上
- AWS CLI設定済み

## ディレクトリ構造（実装後）

```
2025-hackathon-demo/
├── slack-bot-calendar-specification.md  # 仕様書
├── tasks/                               # タスク定義
│   ├── README.md
│   ├── task-01-infrastructure-setup.md
│   ├── task-02-database-design.md
│   └── ...
├── src/                                 # ソースコード
│   ├── lambdas/                         # Lambda関数
│   │   ├── scheduled_poster/            # 定期投稿
│   │   ├── reaction_handler/            # リアクション処理
│   │   ├── schedule_creator/            # スケジュール作成
│   │   └── conversation_analyzer/       # 会話分析
│   ├── shared/                          # 共通ライブラリ
│   │   ├── slack_client.py
│   │   ├── calendar_client.py
│   │   ├── database.py
│   │   └── models.py
│   └── data/                            # マスターデータ
│       └── topics.json                  # 話題テンプレート
├── infrastructure/                      # IaC（Terraform or CloudFormation）
│   ├── main.tf
│   ├── lambda.tf
│   ├── dynamodb.tf
│   └── eventbridge.tf
├── tests/                               # テストコード
│   ├── unit/
│   └── integration/
└── requirements.txt                     # Python依存関係
```

## 見積もり工数

| タスク | 工数（人日） | 担当者例 |
|--------|------------|---------|
| タスク1 | 2日 | インフラエンジニア |
| タスク2 | 3日 | バックエンドエンジニア |
| タスク3 | 3日 | バックエンドエンジニア |
| タスク4 | 2日 | バックエンドエンジニア |
| タスク5 | 3日 | バックエンドエンジニア |
| タスク6 | 4日 | バックエンドエンジニア |
| タスク7 | 5日 | バックエンドエンジニア |
| タスク8 | 4日 | バックエンドエンジニア |
| タスク9 | 5日 | バックエンドエンジニア |
| タスク10 | 3日 | QAエンジニア |
| **合計** | **34日** | 2-3名体制で約3-4週間 |

## 注意事項

- 各タスクの詳細設計は対応するマークダウンファイルを参照
- タスク間に依存関係があるため、順序を守って実装
- API制限やレート制限に注意してテスト実施
- 本番デプロイ前に必ず統合テストを実施
