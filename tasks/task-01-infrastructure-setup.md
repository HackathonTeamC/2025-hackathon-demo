# タスク1: インフラセットアップ

## 概要
AWS環境の初期構築と基盤リソースのセットアップを行います。

## 目的
- AWS Lambda、DynamoDB、EventBridge等の基盤リソースを構築
- IAMロール・ポリシーの設定
- Secrets Managerでの認証情報管理

## 前提条件
- AWSアカウント作成済み
- AWS CLI インストール・設定済み
- 適切な権限を持つIAMユーザー

## 実装内容

### 1. AWSリージョン選択
- **推奨リージョン**: `ap-northeast-1` (東京)
- 理由: 日本からのレイテンシが最小

### 2. IAMロールの作成

#### 2.1 Lambda実行ロール
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "lambda.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
```

**必要なポリシー:**
- `AWSLambdaBasicExecutionRole` (CloudWatch Logs書き込み)
- DynamoDB アクセス
- Secrets Manager 読み取り
- EventBridge 実行

#### 2.2 カスタムポリシー作成

**DynamoDBアクセスポリシー:**
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "dynamodb:GetItem",
        "dynamodb:PutItem",
        "dynamodb:UpdateItem",
        "dynamodb:DeleteItem",
        "dynamodb:Query",
        "dynamodb:Scan"
      ],
      "Resource": [
        "arn:aws:dynamodb:ap-northeast-1:*:table/SlackBotTopics",
        "arn:aws:dynamodb:ap-northeast-1:*:table/SlackBotConversations",
        "arn:aws:dynamodb:ap-northeast-1:*:table/SlackBotEvents",
        "arn:aws:dynamodb:ap-northeast-1:*:table/SlackBotQuestions"
      ]
    }
  ]
}
```

**Secrets Managerアクセスポリシー:**
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "secretsmanager:GetSecretValue"
      ],
      "Resource": [
        "arn:aws:secretsmanager:ap-northeast-1:*:secret:slack-bot/*",
        "arn:aws:secretsmanager:ap-northeast-1:*:secret:google-calendar/*"
      ]
    }
  ]
}
```

### 3. Secrets Managerでの認証情報管理

#### 3.1 Slack認証情報
```bash
aws secretsmanager create-secret \
  --name slack-bot/credentials \
  --description "Slack Bot OAuth tokens and signing secret" \
  --secret-string '{
    "bot_token": "xoxb-your-bot-token",
    "signing_secret": "your-signing-secret",
    "client_id": "your-client-id",
    "client_secret": "your-client-secret"
  }'
```

#### 3.2 Google Calendar認証情報
```bash
aws secretsmanager create-secret \
  --name google-calendar/credentials \
  --description "Google Calendar API credentials" \
  --secret-string '{
    "type": "service_account",
    "project_id": "your-project-id",
    "private_key_id": "key-id",
    "private_key": "-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----\n",
    "client_email": "service-account@project.iam.gserviceaccount.com",
    "client_id": "client-id",
    "auth_uri": "https://accounts.google.com/o/oauth2/auth",
    "token_uri": "https://oauth2.googleapis.com/token"
  }'
```

### 4. VPC設定（オプション）

本システムは外部APIとの通信のみのため、VPC内に配置する必要はありませんが、セキュリティ要件に応じて設定可能。

**VPCを使用しない場合:**
- Lambda関数はパブリックインターネット経由でSlack/Google APIにアクセス
- NAT Gateway費用が不要
- シンプルな構成

**VPCを使用する場合:**
- プライベートサブネット + NAT Gateway
- VPCエンドポイント（DynamoDB, Secrets Manager）で費用削減

### 5. CloudWatch Logs設定

#### 5.1 ロググループ作成
各Lambda関数用のロググループを事前作成：
```bash
aws logs create-log-group --log-group-name /aws/lambda/scheduled-poster
aws logs create-log-group --log-group-name /aws/lambda/reaction-handler
aws logs create-log-group --log-group-name /aws/lambda/schedule-creator
aws logs create-log-group --log-group-name /aws/lambda/conversation-analyzer
```

#### 5.2 ログ保持期間設定
```bash
aws logs put-retention-policy \
  --log-group-name /aws/lambda/scheduled-poster \
  --retention-in-days 30
```

### 6. 環境変数の定義

各Lambda関数で使用する共通環境変数：
```python
ENVIRONMENT_VARIABLES = {
    'SLACK_SECRET_NAME': 'slack-bot/credentials',
    'GOOGLE_SECRET_NAME': 'google-calendar/credentials',
    'TOPICS_TABLE': 'SlackBotTopics',
    'CONVERSATIONS_TABLE': 'SlackBotConversations',
    'EVENTS_TABLE': 'SlackBotEvents',
    'QUESTIONS_TABLE': 'SlackBotQuestions',
    'AWS_REGION': 'ap-northeast-1',
    'LOG_LEVEL': 'INFO'
}
```

## Terraform/CloudFormation実装例

### Terraform版（推奨）

**main.tf:**
```hcl
terraform {
  required_version = ">= 1.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = "ap-northeast-1"
}

# Variables
variable "project_name" {
  default = "slack-bot-calendar"
}

variable "environment" {
  default = "production"
}

# Locals
locals {
  common_tags = {
    Project     = var.project_name
    Environment = var.environment
    ManagedBy   = "Terraform"
  }
}
```

**iam.tf:**
```hcl
# Lambda実行ロール
resource "aws_iam_role" "lambda_execution_role" {
  name = "${var.project_name}-lambda-execution-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "lambda.amazonaws.com"
      }
    }]
  })

  tags = local.common_tags
}

# 基本ポリシー（CloudWatch Logs）
resource "aws_iam_role_policy_attachment" "lambda_basic" {
  role       = aws_iam_role.lambda_execution_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

# DynamoDBアクセスポリシー
resource "aws_iam_role_policy" "dynamodb_access" {
  name = "${var.project_name}-dynamodb-access"
  role = aws_iam_role.lambda_execution_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Action = [
        "dynamodb:GetItem",
        "dynamodb:PutItem",
        "dynamodb:UpdateItem",
        "dynamodb:DeleteItem",
        "dynamodb:Query",
        "dynamodb:Scan"
      ]
      Resource = [
        aws_dynamodb_table.topics.arn,
        aws_dynamodb_table.conversations.arn,
        aws_dynamodb_table.events.arn,
        aws_dynamodb_table.questions.arn
      ]
    }]
  })
}

# Secrets Managerアクセスポリシー
resource "aws_iam_role_policy" "secrets_manager_access" {
  name = "${var.project_name}-secrets-access"
  role = aws_iam_role.lambda_execution_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Action = [
        "secretsmanager:GetSecretValue"
      ]
      Resource = [
        aws_secretsmanager_secret.slack_credentials.arn,
        aws_secretsmanager_secret.google_credentials.arn
      ]
    }]
  })
}
```

**secrets.tf:**
```hcl
# Slack認証情報（値は手動で設定）
resource "aws_secretsmanager_secret" "slack_credentials" {
  name        = "slack-bot/credentials"
  description = "Slack Bot OAuth tokens and signing secret"
  tags        = local.common_tags
}

# Google Calendar認証情報（値は手動で設定）
resource "aws_secretsmanager_secret" "google_credentials" {
  name        = "google-calendar/credentials"
  description = "Google Calendar API service account credentials"
  tags        = local.common_tags
}
```

## 成果物
- [ ] IAMロール作成完了
- [ ] Secrets Managerに認証情報登録完了
- [ ] CloudWatch Logsグループ作成完了
- [ ] Terraform/CloudFormationコード作成完了
- [ ] 環境変数定義ドキュメント作成完了

## 検証方法
```bash
# IAMロールの確認
aws iam get-role --role-name slack-bot-calendar-lambda-execution-role

# Secrets Managerの確認
aws secretsmanager list-secrets

# CloudWatch Logsグループの確認
aws logs describe-log-groups --log-group-name-prefix /aws/lambda/
```

## 次のタスク
[タスク2: データベース設計・構築](./task-02-database-design.md)

## 参考資料
- [AWS Lambda IAM ベストプラクティス](https://docs.aws.amazon.com/lambda/latest/dg/lambda-intro-execution-role.html)
- [AWS Secrets Manager](https://docs.aws.amazon.com/secretsmanager/)
- [Terraform AWS Provider](https://registry.terraform.io/providers/hashicorp/aws/latest/docs)
