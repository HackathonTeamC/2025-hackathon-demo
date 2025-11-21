# Examples

このディレクトリには、共通モジュールの使用例とテストスクリプトが含まれています。

## テストスクリプト

### test_calendar_client.py

Google Calendar APIクライアントの動作をテストします。

#### 前提条件

1. Google Cloud Projectの作成とCalendar API有効化
2. サービスアカウントの作成とキーのダウンロード
3. AWS Secrets Managerへの認証情報登録

#### セットアップ

```bash
# サービスアカウントキーをSecrets Managerに登録
aws secretsmanager create-secret \
  --name google-calendar/credentials \
  --description "Google Calendar API service account" \
  --secret-string file://path/to/service-account-key.json

# 環境変数を設定
export GOOGLE_SECRET_NAME=google-calendar/credentials
export AWS_REGION=ap-northeast-1
```

#### 実行

```bash
# 依存パッケージのインストール
pip install -r requirements.txt

# テストスクリプトの実行
python examples/test_calendar_client.py
```

#### テスト内容

1. **日時ユーティリティのテスト**
   - 日本語日時文字列のパース
   - 所要時間文字列のパース
   - 日時フォーマット

2. **Calendar APIのテスト**（オプション）
   - イベント作成
   - イベント取得
   - イベント更新
   - イベント削除

## 使用例

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

### Calendar Client

```python
from src.shared.calendar_client import CalendarClient
from datetime import datetime, timedelta

# クライアントの初期化
client = CalendarClient()

# イベント作成
start = datetime.now() + timedelta(days=7)
end = start + timedelta(hours=2)

event = client.create_event(
    summary='チームミーティング',
    start_time=start,
    end_time=end,
    description='週次定例会議',
    location='https://meet.google.com/xxx-yyyy-zzz',
    attendees=['user1@example.com', 'user2@example.com']
)

print(f"Event created: {event['html_link']}")
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

# ミーティング提案
blocks = BlockBuilder.meeting_proposal(participant_count=5)

# カレンダー作成通知
blocks = BlockBuilder.calendar_created(
    event_title='Docker勉強会',
    date_time='2025年12月5日(木) 14:00',
    location='https://meet.google.com/xxx',
    participants=['user1', 'user2', 'user3'],
    calendar_url='https://calendar.google.com/event?eid=xxx'
)
```

### Calendar Utils

```python
from src.shared.calendar_utils import (
    parse_japanese_datetime,
    format_datetime_japanese,
    calculate_end_time,
    parse_duration
)

# 日本語日時のパース
dt = parse_japanese_datetime("12/5 14:00")
print(format_datetime_japanese(dt))  # 2025年12月5日(木) 14:00

# 所要時間のパース
duration = parse_duration("2時間30分")  # 150分
end_time = calculate_end_time(dt, duration)
```

## トラブルシューティング

### AWS Secrets Managerにアクセスできない

- IAMロールに適切な権限があるか確認
- 環境変数 `AWS_REGION` が正しく設定されているか確認
- Secrets Managerのシークレット名が正しいか確認

### Google Calendar APIでエラーが発生する

- サービスアカウントキーが正しくSecrets Managerに登録されているか確認
- Calendar APIが有効化されているか確認
- カレンダーがサービスアカウントと共有されているか確認（個人カレンダーの場合）
