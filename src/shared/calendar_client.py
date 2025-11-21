"""
Google Calendar API Client Module

Google Calendar APIとの通信を行うクライアントクラスを提供します。
サービスアカウント認証を使用して、カレンダーイベントの作成・更新・削除を実行します。
"""

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
        
        Secrets Managerからサービスアカウント認証情報を取得し、
        Google Calendar APIサービスを構築します。
        
        Args:
            calendar_id: カレンダーID（Noneの場合はprimary）
        """
        self.credentials = self._get_credentials()
        self.service = build('calendar', 'v3', credentials=self.credentials)
        self.calendar_id = calendar_id or 'primary'
    
    def _get_credentials(self) -> service_account.Credentials:
        """Secrets Managerから認証情報を取得
        
        Returns:
            service_account.Credentials: サービスアカウント認証情報
            
        Raises:
            Exception: 認証情報取得失敗時
        """
        secret_name = os.environ.get('GOOGLE_SECRET_NAME', 'google-calendar/credentials')
        region = os.environ.get('AWS_REGION', 'ap-northeast-1')
        
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
        attendees: Optional[List[str]] = None,
        timezone: str = "Asia/Tokyo"
    ) -> Dict:
        """カレンダーイベントを作成
        
        Args:
            summary: イベント名
            start_time: 開始時刻（timezone-aware datetime推奨）
            end_time: 終了時刻（timezone-aware datetime推奨）
            description: 説明
            location: 場所/URL
            attendees: 参加者のメールアドレスリスト
            timezone: タイムゾーン（デフォルト: Asia/Tokyo）
        
        Returns:
            Dict: 作成されたイベント情報
                {
                    'id': イベントID,
                    'html_link': カレンダーURL,
                    'summary': イベント名,
                    'start': 開始時刻,
                    'end': 終了時刻
                }
                
        Raises:
            Exception: イベント作成失敗時
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
            event['attendees'] = [{'email': email} for email in attendees if email]
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
            add_attendees: 追加する参加者のメールアドレスリスト
            remove_attendees: 削除する参加者のメールアドレスリスト
        
        Returns:
            Dict: 更新されたイベント情報
                {
                    'id': イベントID,
                    'html_link': カレンダーURL,
                    'summary': イベント名
                }
                
        Raises:
            Exception: イベント更新失敗時
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
                        if email and email not in attendee_emails:
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
            
        Raises:
            Exception: イベント削除失敗時
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
            Dict: イベント情報
                {
                    'id': イベントID,
                    'summary': イベント名,
                    'start': 開始時刻,
                    'end': 終了時刻,
                    'location': 場所,
                    'description': 説明,
                    'attendees': 参加者メールアドレスリスト,
                    'html_link': カレンダーURL
                }
                
        Raises:
            Exception: イベント取得失敗時
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
        
        指定された時間帯に各参加者が空いているかをチェックします。
        
        Args:
            emails: チェックする参加者のメールアドレスリスト
            start_time: 開始時刻
            end_time: 終了時刻
        
        Returns:
            Dict[str, bool]: {email: is_available} の辞書
                Trueの場合は空いている、Falseの場合は予定あり
                
        Raises:
            Exception: 空き時間チェック失敗時
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
