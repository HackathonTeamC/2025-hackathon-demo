# GitHub Actions Workflows

このディレクトリには、CI/CDパイプラインのワークフロー定義が含まれています。

## ワークフロー一覧

### 1. CI Workflow (`ci.yml`)

**トリガー:**
- `push`: develop-*, feature/*, main ブランチへのプッシュ
- `pull_request`: main, develop ブランチへのPR

**ジョブ:**
- `lint`: コード品質チェック（flake8, black, isort）
- `type-check`: 型チェック（mypy）
- `test`: ユニットテスト + カバレッジ
- `security`: セキュリティスキャン（bandit, safety）
- `build`: Lambda デプロイパッケージのビルド
- `terraform-check`: Terraform フォーマット＆バリデーション

### 2. Deploy Workflow (`deploy.yml`)

**トリガー:**
- `push`: main ブランチへのプッシュ
- `workflow_dispatch`: 手動実行（環境選択可能）

**ジョブ:**
- `deploy-infrastructure`: Terraform でインフラをデプロイ
- `deploy-lambdas`: Lambda 関数をデプロイ（4つの関数を並列実行）
- `integration-test`: 統合テスト（staging環境のみ）
- `notify`: Slack通知

## 必要なシークレット

GitHub リポジトリの Settings > Secrets and variables > Actions で以下のシークレットを設定してください：

### AWS関連
- `AWS_ACCESS_KEY_ID`: AWS アクセスキーID
- `AWS_SECRET_ACCESS_KEY`: AWS シークレットアクセスキー

### Slack関連（通知用）
- `SLACK_WEBHOOK_URL`: デプロイ通知用のWebhook URL
- `SLACK_TEST_CHANNEL`: 統合テスト用のチャンネルID（オプション）

## 環境変数

### CI Workflow
- `AWS_DEFAULT_REGION`: ap-northeast-1（デフォルト）
- `AWS_REGION`: ap-northeast-1（デフォルト）

### Deploy Workflow
- `AWS_REGION`: ap-northeast-1
- `TERRAFORM_VERSION`: 1.6.0

## ローカルでの実行

### Linting
```bash
# Flake8
flake8 src/

# Black
black --check src/

# isort
isort --check-only src/
```

### Type Checking
```bash
mypy src/ --ignore-missing-imports
```

### Unit Tests
```bash
pytest tests/unit/ -v --cov=src
```

### Integration Tests
```bash
# AWS credentials required
pytest tests/integration/ -v
```

### Lambda Build
```bash
# Single function
./scripts/build_lambda.sh scheduled_poster

# All functions
./scripts/build_lambdas.sh
```

## トラブルシューティング

### Linting エラー
コードスタイルを自動修正:
```bash
black src/
isort src/
```

### Type Check エラー
型ヒントを追加するか、`# type: ignore` コメントで無視:
```python
result = some_function()  # type: ignore
```

### Test エラー
詳細なログを確認:
```bash
pytest tests/unit/ -v -s
```

### Lambda Build エラー
依存関係を確認:
```bash
pip install -r requirements.txt
```

## デプロイフロー

### Staging環境
1. Feature ブランチで開発
2. PR作成（自動でCIが実行される）
3. レビュー・承認
4. develop ブランチにマージ
5. 手動で Deploy ワークフローを実行（staging選択）

### Production環境
1. develop から main へPR作成
2. レビュー・承認
3. main にマージ（自動でデプロイが実行される）

## ベストプラクティス

1. **必ずCIが通ってからマージ**: すべてのチェックが成功していることを確認
2. **テストカバレッジ**: 80%以上を目標
3. **セキュリティスキャン**: 定期的に依存関係を更新
4. **Terraform Plan確認**: デプロイ前に変更内容を確認
5. **Staging環境でテスト**: 本番デプロイ前に必ずテスト

## 参考リンク

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [AWS Lambda Deployment](https://docs.aws.amazon.com/lambda/latest/dg/python-package.html)
- [Terraform in CI/CD](https://www.terraform.io/docs/cloud/run/index.html)
