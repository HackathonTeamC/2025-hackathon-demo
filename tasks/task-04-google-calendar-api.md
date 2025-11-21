# タスク4: Google Calendar API連携

## 概要
Google Calendar APIを使ってイベント作成・管理機能を実装します。

## 目的
- サービスアカウントの作成と認証
- カレンダーイベントの作成・更新・削除
- 参加者招待機能の実装

## 前提条件
- Googleアカウント（Workspaceまたは個人アカウント）
- タスク1（インフラセットアップ）完了

## 実装内容

### 1. Google Cloud Projectセットアップ

#### 1.1 プロジェクト作成
1. https://console.cloud.google.com/ にアクセス
2. 新しいプロジェクトを作成: `slack-bot-calendar`

#### 1.2 Google Calendar API有効化
1. 「APIとサービス」→「ライブラリ」
2. 「Google Calendar API」を検索して有効化

#### 1.3 サービスアカウント作成
1. 「IAMと管理」→「サービスアカウント」
2. 「サービスアカウントを作成」
   - 名前: `slack-bot-calendar-sa`
   - 説明: `Slack Bot用のカレンダーAPI認証`
3. 「キーを作成」→「JSON」形式でダウンロード

#### 1.4 カレンダー共有設定
個人のGoogleカレンダーでサービスアカウントを使用する場合：
1. Googleカレンダーを開く
2. 「設定」→「マイカレンダーの設定」
3. サービスアカウントのメールアドレスを追加（「予定の変更」権限）

### 2. Calendar Clientモジュール実装

#### 2.1 calendar_client.py

```python
import os
import json
import boto3
from datetime import datetime, timedelta
from typing import Dict, List, Optional
from google.oauth2 import service_account
from googleapiclient.discovery import build
from googleapiclient.errors import HttpError

class CalendarClient:
    """Google Calendar APIクライアント"""
    
    SCOPES = ['https://www.googleapis.com/auth/calendar']
    
    def __init__(self, calendar_id: Optional[str] = None):
        """初期化
        
        Args:
            calendar_id: カレンダーID（Noneの場合はprimary）
        """
        self.credentials = self._get_credentials()
        self.service = build('calendar', 'v3', credentials=self.credentials)
        self.calendar_id = calendar_id or 'primary'
    
    def _get_credentials(self) -> service_account.Credentials:
        """Secrets Managerから認証情報を取得"""
        secret_name = os.environ['GOOGLE_SECRET_NAME']
        region = os.environ['AWS_REGION']
        
        session = boto3.session.Session()
        client = session.client('secretsmanager', region_name=region)
        
        try:
            response = client.get_secret_value(SecretId=secret_name)
            service_account_info = json.loads(response['SecretString'])
            
            credentials = service_account.Credentials.from_service_account_info(
                service_account_info,
                scopes=self.SCOPES
            )
            return credentials
        except Exception as e:
            raise Exception(f"Failed to get Google credentials: {str(e)}")
    
    def create_event(
        self,
        summary: str,
        start_time: datetime,
        end_time: datetime,
        description: str = "",
        location: str = "",
        attendees: List[str] = None,
        timezone: str = "Asia/Tokyo"
    ) -> Dict:
        """カレンダーイベントを作成
        
        Args:
            summary: イベント名
            start_time: 開始時刻
            end_time: 終了時刻
            description: 説明
            location: 場所/URL
            attendees: 参加者のメールアドレスリスト
            timezone: タイムゾーン
        
        Returns:
            作成されたイベント情報
        """
        event = {
            'summary': summary,
            'location': location,
            'description': description,
            'start': {
                'dateTime': start_time.isoformat(),
                'timeZone': timezone,
            },
            'end': {
                'dateTime': end_time.isoformat(),
                'timeZone': timezone,
            },
            'reminders': {
                'useDefault': False,
                'overrides': [
                    {'method': 'email', 'minutes': 24 * 60},  # 1日前
                    {'method': 'popup', 'minutes': 30},  # 30分前
                ],
            },
        }
        
        # 参加者追加
        if attendees:
            event['attendees'] = [{'email': email} for email in attendees]
            event['guestsCanModify'] = False
            event['guestsCanInviteOthers'] = False
            event['guestsCanSeeOtherGuests'] = True
        
        try:
            created_event = self.service.events().insert(
                calendarId=self.calendar_id,
                body=event,
                sendUpdates='all'  # 招待メールを送信
            ).execute()
            
            return {
                'id': created_event['id'],
                'html_link': created_event.get('htmlLink', ''),
                'summary': created_event.get('summary', ''),
                'start': created_event['start'].get('dateTime', ''),
                'end': created_event['end'].get('dateTime', '')
            }
        except HttpError as error:
            raise Exception(f"Failed to create event: {error}")
    
    def update_event(
        self,
        event_id: str,
        summary: Optional[str] = None,
        start_time: Optional[datetime] = None,
        end_time: Optional[datetime] = None,
        description: Optional[str] = None,
        location: Optional[str] = None,
        add_attendees: Optional[List[str]] = None,
        remove_attendees: Optional[List[str]] = None
    ) -> Dict:
        """イベントを更新
        
        Args:
            event_id: イベントID
            summary: 新しいイベント名
            start_time: 新しい開始時刻
            end_time: 新しい終了時刻
            description: 新しい説明
            location: 新しい場所
            add_attendees: 追加する参加者
            remove_attendees: 削除する参加者
        
        Returns:
            更新されたイベント情報
        """
        try:
            # 既存イベントを取得
            event = self.service.events().get(
                calendarId=self.calendar_id,
                eventId=event_id
            ).execute()
            
            # 更新するフィールドを適用
            if summary:
                event['summary'] = summary
            if start_time:
                event['start']['dateTime'] = start_time.isoformat()
            if end_time:
                event['end']['dateTime'] = end_time.isoformat()
            if description is not None:
                event['description'] = description
            if location is not None:
                event['location'] = location
            
            # 参加者の追加・削除
            if add_attendees or remove_attendees:
                attendees = event.get('attendees', [])
                attendee_emails = {a['email'] for a in attendees}
                
                if add_attendees:
                    for email in add_attendees:
                        if email not in attendee_emails:
                            attendees.append({'email': email})
                
                if remove_attendees:
                    attendees = [a for a in attendees if a['email'] not in remove_attendees]
                
                event['attendees'] = attendees
            
            # イベントを更新
            updated_event = self.service.events().update(
                calendarId=self.calendar_id,
                eventId=event_id,
                body=event,
                sendUpdates='all'
            ).execute()
            
            return {
                'id': updated_event['id'],
                'html_link': updated_event.get('htmlLink', ''),
                'summary': updated_event.get('summary', '')
            }
        except HttpError as error:
            raise Exception(f"Failed to update event: {error}")
    
    def delete_event(self, event_id: str) -> None:
        """イベントを削除
        
        Args:
            event_id: イベントID
        """
        try:
            self.service.events().delete(
                calendarId=self.calendar_id,
                eventId=event_id,
                sendUpdates='all'
            ).execute()
        except HttpError as error:
            raise Exception(f"Failed to delete event: {error}")
    
    def get_event(self, event_id: str) -> Dict:
        """イベント情報を取得
        
        Args:
            event_id: イベントID
        
        Returns:
            イベント情報
        """
        try:
            event = self.service.events().get(
                calendarId=self.calendar_id,
                eventId=event_id
            ).execute()
            
            return {
                'id': event['id'],
                'summary': event.get('summary', ''),
                'start': event['start'].get('dateTime', ''),
                'end': event['end'].get('dateTime', ''),
                'location': event.get('location', ''),
                'description': event.get('description', ''),
                'attendees': [a['email'] for a in event.get('attendees', [])],
                'html_link': event.get('htmlLink', '')
            }
        except HttpError as error:
            raise Exception(f"Failed to get event: {error}")
    
    def check_availability(
        self,
        emails: List[str],
        start_time: datetime,
        end_time: datetime
    ) -> Dict[str, bool]:
        """参加者の空き時間をチェック（フェーズ2機能）
        
        Args:
            emails: チェックする参加者のメールアドレス
            start_time: 開始時刻
            end_time: 終了時刻
        
        Returns:
            {email: is_available} の辞書
        """
        try:
            body = {
                "timeMin": start_time.isoformat() + 'Z',
                "timeMax": end_time.isoformat() + 'Z',
                "items": [{"id": email} for email in emails]
            }
            
            result = self.service.freebusy().query(body=body).execute()
            
            availability = {}
            for email in emails:
                calendar = result['calendars'].get(email, {})
                busy_slots = calendar.get('busy', [])
                # busyスロットが空ならavailable
                availability[email] = len(busy_slots) == 0
            
            return availability
        except HttpError as error:
            raise Exception(f"Failed to check availability: {error}")
```

#### 2.2 ヘルパー関数（src/shared/calendar_utils.py）

```python
from datetime import datetime, timedelta
from typing import Dict, Optional
import pytz

def parse_japanese_datetime(date_str: str) -> Optional[datetime]:
    """日本語の日時文字列をパース
    
    Args:
        date_str: "12/5 14:00", "2025/12/05 14:00" など
    
    Returns:
        datetime オブジェクト
    """
    jst = pytz.timezone('Asia/Tokyo')
    
    # パターン1: "12/5 14:00"
    try:
        dt = datetime.strptime(date_str, "%m/%d %H:%M")
        dt = dt.replace(year=datetime.now().year)
        return jst.localize(dt)
    except ValueError:
        pass
    
    # パターン2: "2025/12/05 14:00"
    try:
        dt = datetime.strptime(date_str, "%Y/%m/%d %H:%M")
        return jst.localize(dt)
    except ValueError:
        pass
    
    # パターン3: "12月5日 14時"
    # 必要に応じて追加実装
    
    return None

def format_datetime_japanese(dt: datetime) -> str:
    """datetimeを日本語形式でフォーマット
    
    Args:
        dt: datetime オブジェクト
    
    Returns:
        "2025年12月5日(木) 14:00" 形式の文字列
    """
    jst = pytz.timezone('Asia/Tokyo')
    dt_jst = dt.astimezone(jst)
    
    weekdays = ['月', '火', '水', '木', '金', '土', '日']
    weekday = weekdays[dt_jst.weekday()]
    
    return dt_jst.strftime(f"%Y年%m月%d日({weekday}) %H:%M")

def calculate_end_time(start_time: datetime, duration_minutes: int) -> datetime:
    """終了時刻を計算
    
    Args:
        start_time: 開始時刻
        duration_minutes: 所要時間（分）
    
    Returns:
        終了時刻
    """
    return start_time + timedelta(minutes=duration_minutes)
```

### 3. 環境変数とシークレット設定

```bash
# Secrets Managerにサービスアカウントキーを登録
aws secretsmanager create-secret \
  --name google-calendar/credentials \
  --description "Google Calendar API service account" \
  --secret-string file://service-account-key.json
```

### 4. requirements.txt更新

```txt
slack-bolt==1.18.0
slack-sdk==3.26.0
google-auth==2.25.2
google-auth-oauthlib==1.2.0
google-api-python-client==2.111.0
boto3==1.34.0
pytz==2023.3
```

## 成果物
- [ ] Google Cloud Project作成完了
- [ ] Google Calendar API有効化完了
- [ ] サービスアカウント作成完了
- [ ] calendar_client.py 実装完了
- [ ] calendar_utils.py 実装完了
- [ ] Secrets Manager登録完了

## 検証方法

```python
# calendar_client.py のテスト
from calendar_client import CalendarClient
from datetime import datetime, timedelta

client = CalendarClient()

# イベント作成テスト
start = datetime.now() + timedelta(days=7)
end = start + timedelta(hours=2)

event = client.create_event(
    summary='テストイベント',
    start_time=start,
    end_time=end,
    description='テスト用のイベントです',
    location='https://meet.google.com/xxx',
    attendees=['test@example.com']
)

print(f"Event created: {event['html_link']}")

# イベント取得テスト
retrieved = client.get_event(event['id'])
print(f"Event: {retrieved['summary']}")

# イベント削除テスト
client.delete_event(event['id'])
print("Event deleted")
```

## 次のタスク
[タスク5: 定期実行システム](./task-05-scheduled-posting.md)

## 参考資料
- [Google Calendar API Python Quickstart](https://developers.google.com/calendar/api/quickstart/python)
- [Google Calendar API Reference](https://developers.google.com/calendar/api/v3/reference)
- [サービスアカウント認証](https://cloud.google.com/iam/docs/service-accounts)
