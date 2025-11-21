"""
Slack API Client Wrapper Module

Slack APIとの通信を行うクライアントクラスを提供します。
slack-boltライブラリを使用して、メッセージ投稿、リアクション取得、
ユーザー情報取得などの機能を実装しています。
"""

import os
import json
import boto3
from slack_bolt import App
from slack_sdk.errors import SlackApiError
from typing import Dict, List, Optional


class SlackClient:
    """Slack APIクライアントのラッパークラス"""
    
    def __init__(self):
        """初期化: Secrets Managerから認証情報を取得"""
        self.secrets = self._get_secrets()
        self.app = App(
            token=self.secrets['bot_token'],
            signing_secret=self.secrets['signing_secret']
        )
        self.client = self.app.client
    
    def _get_secrets(self) -> Dict[str, str]:
        """Secrets Managerから認証情報を取得
        
        Returns:
            Dict[str, str]: Slack認証情報
            
        Raises:
            Exception: Secrets Managerからの取得失敗時
        """
        secret_name = os.environ.get('SLACK_SECRET_NAME', 'slack-bot/credentials')
        region = os.environ.get('AWS_REGION', 'ap-northeast-1')
        
        session = boto3.session.Session()
        client = session.client('secretsmanager', region_name=region)
        
        try:
            response = client.get_secret_value(SecretId=secret_name)
            return json.loads(response['SecretString'])
        except Exception as e:
            raise Exception(f"Failed to get Slack secrets: {str(e)}")
    
    def post_message(
        self,
        channel: str,
        text: str,
        blocks: Optional[List[Dict]] = None,
        thread_ts: Optional[str] = None
    ) -> Dict:
        """メッセージを投稿
        
        Args:
            channel: チャンネルID（例: C01234567）
            text: メッセージテキスト（Block使用時はfallback用）
            blocks: Block Kit形式のブロック
            thread_ts: スレッドに返信する場合のタイムスタンプ
        
        Returns:
            Dict: レスポンス辞書（tsを含む）
            
        Raises:
            Exception: メッセージ投稿失敗時
        """
        try:
            kwargs = {
                'channel': channel,
                'text': text
            }
            
            if blocks:
                kwargs['blocks'] = blocks
            
            if thread_ts:
                kwargs['thread_ts'] = thread_ts
            
            response = self.client.chat_postMessage(**kwargs)
            return response.data
        except SlackApiError as e:
            raise Exception(f"Failed to post message: {e.response['error']}")
    
    def get_reactions(self, channel: str, timestamp: str) -> List[Dict]:
        """メッセージのリアクションを取得
        
        Args:
            channel: チャンネルID
            timestamp: メッセージタイムスタンプ
        
        Returns:
            List[Dict]: リアクション情報のリスト
                [{'user_id': 'U123', 'reaction': 'thumbsup'}, ...]
                
        Raises:
            Exception: リアクション取得失敗時
        """
        try:
            response = self.client.reactions_get(
                channel=channel,
                timestamp=timestamp
            )
            
            reactions = []
            if 'message' in response and 'reactions' in response['message']:
                for reaction in response['message']['reactions']:
                    for user in reaction['users']:
                        reactions.append({
                            'user_id': user,
                            'reaction': reaction['name']
                        })
            
            return reactions
        except SlackApiError as e:
            raise Exception(f"Failed to get reactions: {e.response['error']}")
    
    def get_user_info(self, user_id: str) -> Dict:
        """ユーザー情報を取得
        
        Args:
            user_id: ユーザーID（例: U01234567）
        
        Returns:
            Dict: ユーザー情報（id, name, email, display_name）
            
        Raises:
            Exception: ユーザー情報取得失敗時
        """
        try:
            response = self.client.users_info(user=user_id)
            user = response['user']
            return {
                'id': user['id'],
                'name': user.get('real_name', user['name']),
                'email': user['profile'].get('email', ''),
                'display_name': user['profile'].get('display_name', '')
            }
        except SlackApiError as e:
            raise Exception(f"Failed to get user info: {e.response['error']}")
    
    def get_channel_history(
        self,
        channel: str,
        limit: int = 100,
        oldest: Optional[str] = None
    ) -> List[Dict]:
        """チャンネル履歴を取得
        
        Args:
            channel: チャンネルID
            limit: 取得件数（最大1000）
            oldest: この時刻（Unix timestamp）以降のメッセージを取得
        
        Returns:
            List[Dict]: メッセージリスト
            
        Raises:
            Exception: チャンネル履歴取得失敗時
        """
        try:
            kwargs = {
                'channel': channel,
                'limit': min(limit, 1000)  # API制限
            }
            
            if oldest:
                kwargs['oldest'] = oldest
            
            response = self.client.conversations_history(**kwargs)
            return response['messages']
        except SlackApiError as e:
            raise Exception(f"Failed to get channel history: {e.response['error']}")
    
    def list_users(self) -> List[Dict]:
        """ワークスペース内の全ユーザーを取得
        
        bot、削除済みユーザーは除外されます。
        
        Returns:
            List[Dict]: ユーザーリスト（id, name, email）
            
        Raises:
            Exception: ユーザーリスト取得失敗時
        """
        try:
            response = self.client.users_list()
            users = []
            for user in response['members']:
                # botやdeleted userを除外
                if not user.get('is_bot') and not user.get('deleted'):
                    users.append({
                        'id': user['id'],
                        'name': user.get('real_name', user['name']),
                        'email': user['profile'].get('email', '')
                    })
            return users
        except SlackApiError as e:
            raise Exception(f"Failed to list users: {e.response['error']}")
    
    def add_reaction(self, channel: str, timestamp: str, emoji: str) -> None:
        """メッセージにリアクションを追加
        
        Args:
            channel: チャンネルID
            timestamp: メッセージタイムスタンプ
            emoji: 絵文字名（コロンなし、例: 'thumbsup'）
            
        Raises:
            Exception: リアクション追加失敗時（already_reactedは無視）
        """
        try:
            self.client.reactions_add(
                channel=channel,
                timestamp=timestamp,
                name=emoji
            )
        except SlackApiError as e:
            # already_reactedエラーは無視
            if e.response['error'] != 'already_reacted':
                raise Exception(f"Failed to add reaction: {e.response['error']}")
    
    def update_message(
        self,
        channel: str,
        timestamp: str,
        text: str,
        blocks: Optional[List[Dict]] = None
    ) -> Dict:
        """既存メッセージを更新
        
        Args:
            channel: チャンネルID
            timestamp: メッセージタイムスタンプ
            text: 新しいメッセージテキスト
            blocks: 新しいBlock Kit形式のブロック
        
        Returns:
            Dict: レスポンス辞書
            
        Raises:
            Exception: メッセージ更新失敗時
        """
        try:
            kwargs = {
                'channel': channel,
                'ts': timestamp,
                'text': text
            }
            
            if blocks:
                kwargs['blocks'] = blocks
            
            response = self.client.chat_update(**kwargs)
            return response.data
        except SlackApiError as e:
            raise Exception(f"Failed to update message: {e.response['error']}")
