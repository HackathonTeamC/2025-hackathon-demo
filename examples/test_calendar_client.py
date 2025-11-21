"""
Google Calendar Client のテストスクリプト

使用方法:
1. AWS Secrets Managerにサービスアカウントキーを登録
2. 環境変数を設定
   export GOOGLE_SECRET_NAME=google-calendar/credentials
   export AWS_REGION=ap-northeast-1
3. このスクリプトを実行
   python examples/test_calendar_client.py
"""

import sys
import os
from datetime import datetime, timedelta

# src/sharedをパスに追加
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'src'))

from shared.calendar_client import CalendarClient
from shared.calendar_utils import (
    format_datetime_japanese,
    calculate_end_time,
    parse_japanese_datetime,
    parse_duration
)


def test_event_lifecycle():
    """イベントのライフサイクルをテスト"""
    print("=" * 60)
    print("Google Calendar Client Test")
    print("=" * 60)
    
    # クライアント初期化
    print("\n1. Initializing Calendar Client...")
    client = CalendarClient()
    print("✓ Calendar client initialized")
    
    # イベント作成
    print("\n2. Creating test event...")
    start = datetime.now() + timedelta(days=7)
    end = calculate_end_time(start, 120)  # 2時間後
    
    event = client.create_event(
        summary='テストイベント - Slack Bot Calendar',
        start_time=start,
        end_time=end,
        description='これはテスト用のイベントです。\n自動的に作成されました。',
        location='https://meet.google.com/test-meeting',
        attendees=['test@example.com']  # 実際のメールアドレスに変更してください
    )
    
    print(f"✓ Event created: {event['id']}")
    print(f"  Summary: {event['summary']}")
    print(f"  Link: {event['html_link']}")
    print(f"  Start: {format_datetime_japanese(datetime.fromisoformat(event['start']))}")
    
    event_id = event['id']
    
    # イベント取得
    print(f"\n3. Retrieving event {event_id}...")
    retrieved = client.get_event(event_id)
    print(f"✓ Event retrieved")
    print(f"  Summary: {retrieved['summary']}")
    print(f"  Location: {retrieved['location']}")
    print(f"  Attendees: {', '.join(retrieved['attendees'])}")
    
    # イベント更新
    print(f"\n4. Updating event...")
    updated = client.update_event(
        event_id=event_id,
        summary='【更新】テストイベント - Slack Bot Calendar',
        description='イベントが更新されました。'
    )
    print(f"✓ Event updated")
    print(f"  New summary: {updated['summary']}")
    
    # イベント削除
    print(f"\n5. Deleting event...")
    client.delete_event(event_id)
    print(f"✓ Event deleted")
    
    print("\n" + "=" * 60)
    print("All tests passed! ✓")
    print("=" * 60)


def test_datetime_utils():
    """日時ユーティリティのテスト"""
    print("\n" + "=" * 60)
    print("DateTime Utils Test")
    print("=" * 60)
    
    test_cases = [
        "12/5 14:00",
        "2025/12/05 14:00",
        "12月5日 14時",
        "12月5日 14:00",
        "2025年12月5日 14:00",
    ]
    
    print("\nParsing Japanese datetime strings:")
    for test_str in test_cases:
        parsed = parse_japanese_datetime(test_str)
        if parsed:
            formatted = format_datetime_japanese(parsed)
            print(f"  '{test_str}' → {formatted}")
        else:
            print(f"  '{test_str}' → Failed to parse")
    
    duration_cases = [
        "1時間",
        "2時間30分",
        "90分",
        "1.5時間",
    ]
    
    print("\nParsing duration strings:")
    for duration_str in duration_cases:
        minutes = parse_duration(duration_str)
        if minutes:
            print(f"  '{duration_str}' → {minutes}分")
        else:
            print(f"  '{duration_str}' → Failed to parse")


if __name__ == '__main__':
    try:
        # 日時ユーティリティのテスト
        test_datetime_utils()
        
        # カレンダークライアントのテスト
        # 注意: 実際のGoogleカレンダーにイベントが作成されます
        response = input("\nRun Calendar API tests? (This will create/delete a real event) [y/N]: ")
        if response.lower() == 'y':
            test_event_lifecycle()
        else:
            print("\nCalendar API tests skipped.")
    
    except Exception as e:
        print(f"\n❌ Error: {str(e)}")
        import traceback
        traceback.print_exc()
        sys.exit(1)
