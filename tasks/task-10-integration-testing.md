# タスク10: 統合テスト

## 概要
システム全体のエンドツーエンドテストを実施し、各機能が正しく連携していることを確認します。

## 目的
- 各Lambda関数の単体テスト
- エンドツーエンドの統合テスト
- パフォーマンステスト
- 本番デプロイ前の最終検証

## 実装内容

### 1. 単体テストの実装

#### 1.1 テストフレームワークのセットアップ

**requirements-dev.txt:**
```txt
pytest==7.4.3
pytest-mock==3.12.0
moto==4.2.9  # AWS サービスのモック
responses==0.24.1  # HTTPリクエストのモック
freezegun==1.4.0  # 時刻のモック
```

#### 1.2 Slack Client テスト（tests/unit/test_slack_client.py）

```python
import pytest
from moto import mock_secretsmanager
import boto3
import json
from unittest.mock import Mock, patch

from slack_client import SlackClient

@mock_secretsmanager
def test_slack_client_initialization():
    """SlackClientの初期化テスト"""
    # Secrets Managerのモック作成
    secrets_client = boto3.client('secretsmanager', region_name='ap-northeast-1')
    secrets_client.create_secret(
        Name='slack-bot/credentials',
        SecretString=json.dumps({
            'bot_token': 'xoxb-test-token',
            'signing_secret': 'test-secret'
        })
    )
    
    # 環境変数を設定
    import os
    os.environ['SLACK_SECRET_NAME'] = 'slack-bot/credentials'
    os.environ['AWS_REGION'] = 'ap-northeast-1'
    
    # SlackClient作成
    with patch('slack_bolt.App'):
        client = SlackClient()
        assert client.secrets['bot_token'] == 'xoxb-test-token'

@patch('slack_client.SlackClient.client')
def test_post_message(mock_client):
    """メッセージ投稿のテスト"""
    mock_response = Mock()
    mock_response.data = {'ts': '1234567890.123456', 'ok': True}
    mock_client.chat_postMessage.return_value = mock_response
    
    with patch('slack_client.SlackClient._get_secrets'):
        client = SlackClient()
        client.client = mock_client
        
        result = client.post_message(
            channel='C01234567',
            text='Test message'
        )
        
        assert result['ts'] == '1234567890.123456'
        mock_client.chat_postMessage.assert_called_once()
```

#### 1.3 Calendar Client テスト（tests/unit/test_calendar_client.py）

```python
import pytest
from datetime import datetime
from unittest.mock import Mock, patch, MagicMock

from calendar_client import CalendarClient

@patch('calendar_client.CalendarClient._get_credentials')
@patch('googleapiclient.discovery.build')
def test_create_event(mock_build, mock_get_creds):
    """イベント作成のテスト"""
    # モックサービス
    mock_service = MagicMock()
    mock_build.return_value = mock_service
    
    # モックレスポンス
    mock_event = {
        'id': 'event123',
        'htmlLink': 'https://calendar.google.com/event?eid=xxx',
        'summary': 'Test Event',
        'start': {'dateTime': '2025-12-05T14:00:00+09:00'},
        'end': {'dateTime': '2025-12-05T16:00:00+09:00'}
    }
    mock_service.events().insert().execute.return_value = mock_event
    
    # CalendarClient作成
    client = CalendarClient()
    client.service = mock_service
    
    # イベント作成
    start = datetime(2025, 12, 5, 14, 0)
    end = datetime(2025, 12, 5, 16, 0)
    
    result = client.create_event(
        summary='Test Event',
        start_time=start,
        end_time=end,
        attendees=['test@example.com']
    )
    
    assert result['id'] == 'event123'
    assert result['html_link'] == 'https://calendar.google.com/event?eid=xxx'
```

#### 1.4 Topic Selector テスト（tests/unit/test_topic_selector.py）

```python
import pytest
from datetime import datetime, timedelta
from unittest.mock import Mock

from topic_selector import TopicSelector

def test_select_with_diversity():
    """話題選択の多様性テスト"""
    # モックDB
    mock_topics_db = Mock()
    mock_analytics = Mock()
    
    # テストデータ
    topics = [
        {
            'topic_id': 'casual-001',
            'category': 'casual',
            'content': 'Test topic 1',
            'last_used_at': '2025-01-01T00:00:00Z',
            'usage_count': 2,
            'average_reactions': 5.0
        },
        {
            'topic_id': 'casual-002',
            'category': 'casual',
            'content': 'Test topic 2',
            'last_used_at': '2025-01-05T00:00:00Z',
            'usage_count': 1,
            'average_reactions': 8.0
        }
    ]
    
    mock_topics_db.get_unused_topics.return_value = topics
    
    selector = TopicSelector(mock_topics_db, mock_analytics)
    
    # 選択を実行
    selected = selector.select_with_diversity('casual')
    
    assert selected is not None
    assert selected['topic_id'] in ['casual-001', 'casual-002']
```

### 2. 統合テストシナリオ

#### 2.1 エンドツーエンドテスト（tests/integration/test_e2e_flow.py）

```python
import pytest
import time
from datetime import datetime, timedelta

class TestEndToEndFlow:
    """エンドツーエンドの統合テスト"""
    
    def test_full_schedule_creation_flow(self):
        """
        完全なスケジュール作成フローのテスト
        
        シナリオ:
        1. 定期投稿が実行される
        2. ユーザーがリアクションする
        3. 3人以上のリアクションでミーティング提案
        4. 日程調整
        5. Googleカレンダーにイベント作成
        """
        # 1. 定期投稿をトリガー
        # （Lambda関数を直接呼び出す）
        
        # 2. モックユーザーからリアクション
        # （Slack Events APIにPOST）
        
        # 3. ミーティング提案の確認
        # （Slackメッセージを確認）
        
        # 4. 日程入力
        # （インタラクティブコンポーネント）
        
        # 5. カレンダー作成確認
        # （Google Calendar APIで検証）
        
        pass  # 実装
    
    def test_conversation_analysis_and_repost(self):
        """
        会話分析から話題再提起までのフロー
        
        シナリオ:
        1. 会話履歴分析が実行される
        2. 人気のあった話題が抽出される
        3. その話題が再投稿される
        4. リアクションが集まる
        """
        pass  # 実装
```

#### 2.2 テストヘルパー（tests/helpers/test_helpers.py）

```python
import boto3
from typing import Dict, List

class TestHelpers:
    """テスト用のヘルパー関数"""
    
    @staticmethod
    def create_test_topic(dynamodb_table: str, topic_data: Dict) -> str:
        """テスト用の話題を作成
        
        Args:
            dynamodb_table: テーブル名
            topic_data: 話題データ
        
        Returns:
            topic_id
        """
        dynamodb = boto3.resource('dynamodb')
        table = dynamodb.Table(dynamodb_table)
        
        table.put_item(Item=topic_data)
        return topic_data['topic_id']
    
    @staticmethod
    def cleanup_test_data(dynamodb_table: str, keys: List[Dict]) -> None:
        """テストデータをクリーンアップ
        
        Args:
            dynamodb_table: テーブル名
            keys: 削除するキーのリスト
        """
        dynamodb = boto3.resource('dynamodb')
        table = dynamodb.Table(dynamodb_table)
        
        for key in keys:
            table.delete_item(Key=key)
    
    @staticmethod
    def mock_slack_event(event_type: str, **kwargs) -> Dict:
        """Slack イベントのモックを生成
        
        Args:
            event_type: イベントタイプ
            **kwargs: 追加パラメータ
        
        Returns:
            イベント辞書
        """
        base_event = {
            'type': 'event_callback',
            'event': {
                'type': event_type,
                **kwargs
            }
        }
        return base_event
```

### 3. パフォーマンステスト

#### 3.1 負荷テスト（tests/performance/test_load.py）

```python
import concurrent.futures
import time

class TestPerformance:
    """パフォーマンステスト"""
    
    def test_concurrent_topic_posting(self):
        """同時話題投稿のテスト"""
        # 10個のLambda関数を同時実行
        with concurrent.futures.ThreadPoolExecutor(max_workers=10) as executor:
            futures = []
            for i in range(10):
                future = executor.submit(self._invoke_scheduled_poster)
                futures.append(future)
            
            # 全ての実行が成功することを確認
            results = [f.result() for f in futures]
            assert all(r['statusCode'] == 200 for r in results)
    
    def test_reaction_processing_speed(self):
        """リアクション処理速度のテスト"""
        start = time.time()
        
        # リアクション処理を実行
        # （実装）
        
        duration = time.time() - start
        
        # 5秒以内に処理完了することを確認
        assert duration < 5.0
    
    def _invoke_scheduled_poster(self):
        """Scheduled Poster Lambdaを呼び出し"""
        import boto3
        lambda_client = boto3.client('lambda')
        
        response = lambda_client.invoke(
            FunctionName='slack-bot-calendar-scheduled-poster',
            InvocationType='RequestResponse',
            Payload='{}'
        )
        
        return response
```

### 4. テスト実行スクリプト

#### 4.1 run_tests.sh

```bash
#!/bin/bash

echo "🧪 Running unit tests..."
pytest tests/unit/ -v

echo ""
echo "🔗 Running integration tests..."
pytest tests/integration/ -v

echo ""
echo "⚡ Running performance tests..."
pytest tests/performance/ -v

echo ""
echo "📊 Generating coverage report..."
pytest --cov=src --cov-report=html --cov-report=term

echo ""
echo "✅ All tests completed!"
```

### 5. 本番デプロイチェックリスト

#### 5.1 deployment_checklist.md

```markdown
# 本番デプロイチェックリスト

## インフラ
- [ ] DynamoDBテーブル作成確認
- [ ] Lambda関数デプロイ確認
- [ ] API Gateway設定確認
- [ ] EventBridge ルール設定確認
- [ ] IAMロール・ポリシー確認
- [ ] Secrets Manager登録確認

## Slack設定
- [ ] Slack App作成
- [ ] OAuth権限設定
- [ ] Event Subscriptions設定
- [ ] Request URL設定とVerification
- [ ] Bot Token取得

## Google設定
- [ ] Google Cloud Project作成
- [ ] Calendar API有効化
- [ ] サービスアカウント作成
- [ ] カレンダー共有設定

## データ
- [ ] 話題マスターデータ投入
- [ ] 初期設定完了

## テスト
- [ ] 単体テスト全てパス
- [ ] 統合テスト全てパス
- [ ] 実環境でのスモークテスト
- [ ] エラーハンドリング確認

## モニタリング
- [ ] CloudWatch Logs確認
- [ ] CloudWatch Alarms設定
- [ ] エラー通知設定

## ドキュメント
- [ ] README更新
- [ ] 運用手順書作成
- [ ] トラブルシューティングガイド作成
```

### 6. CI/CDパイプライン（GitHub Actions）

#### 6.1 .github/workflows/test.yml

```yaml
name: Test

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up Python
      uses: actions/setup-python@v4
      with:
        python-version: '3.11'
    
    - name: Install dependencies
      run: |
        pip install -r requirements.txt
        pip install -r requirements-dev.txt
    
    - name: Run unit tests
      run: pytest tests/unit/ -v
    
    - name: Run integration tests
      run: pytest tests/integration/ -v
      env:
        AWS_ACCESS_KEY_ID: ${{ secrets.AWS_ACCESS_KEY_ID }}
        AWS_SECRET_ACCESS_KEY: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
    
    - name: Generate coverage report
      run: pytest --cov=src --cov-report=xml
    
    - name: Upload coverage to Codecov
      uses: codecov/codecov-action@v3
```

## 成果物
- [ ] 単体テスト実装完了（カバレッジ80%以上）
- [ ] 統合テスト実装完了
- [ ] パフォーマンステスト実装完了
- [ ] CI/CDパイプライン構築完了
- [ ] デプロイチェックリスト作成完了
- [ ] 運用ドキュメント作成完了

## 検証方法

```bash
# 全テストを実行
./run_tests.sh

# カバレッジレポートを確認
open htmlcov/index.html

# 統合テストのみ実行
pytest tests/integration/ -v -s

# パフォーマンステストのみ実行
pytest tests/performance/ -v
```

## 完了基準
- [ ] 全テストがパス
- [ ] カバレッジ80%以上
- [ ] 本番環境でスモークテスト成功
- [ ] ドキュメント完成
- [ ] チームレビュー完了

## 参考資料
- [pytest Documentation](https://docs.pytest.org/)
- [moto - Mock AWS Services](https://github.com/spulec/moto)
- [AWS Lambda Testing Best Practices](https://docs.aws.amazon.com/lambda/latest/dg/testing-functions.html)
