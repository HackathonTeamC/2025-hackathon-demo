"""
DynamoDB Database Module

DynamoDBへのアクセスを抽象化するモジュール。
各テーブルへのCRUD操作を提供します。
"""

import os
import boto3
from datetime import datetime
from typing import Dict, List, Optional, Any
from decimal import Decimal
import json


class DynamoDBClient:
    """DynamoDB操作クライアント"""
    
    def __init__(self):
        """初期化"""
        region = os.environ.get('AWS_REGION', 'ap-northeast-1')
        self.dynamodb = boto3.resource('dynamodb', region_name=region)
        
        # テーブル名を環境変数から取得
        self.topics_table_name = os.environ.get('TOPICS_TABLE', 'SlackBotTopics')
        self.conversations_table_name = os.environ.get('CONVERSATIONS_TABLE', 'SlackBotConversations')
        self.events_table_name = os.environ.get('EVENTS_TABLE', 'SlackBotEvents')
        self.questions_table_name = os.environ.get('QUESTIONS_TABLE', 'SlackBotQuestions')
        
        # テーブルオブジェクト
        self.topics_table = self.dynamodb.Table(self.topics_table_name)
        self.conversations_table = self.dynamodb.Table(self.conversations_table_name)
        self.events_table = self.dynamodb.Table(self.events_table_name)
        self.questions_table = self.dynamodb.Table(self.questions_table_name)
    
    # === Topics Table ===
    
    def get_topic(self, topic_id: str) -> Optional[Dict]:
        """話題を取得
        
        Args:
            topic_id: 話題ID
            
        Returns:
            Optional[Dict]: 話題データ（存在しない場合はNone）
        """
        try:
            response = self.topics_table.get_item(Key={'topic_id': topic_id})
            return response.get('Item')
        except Exception as e:
            print(f"Error getting topic: {e}")
            return None
    
    def put_topic(self, topic: Dict) -> bool:
        """話題を保存
        
        Args:
            topic: 話題データ
            
        Returns:
            bool: 成功したらTrue
        """
        try:
            self.topics_table.put_item(Item=topic)
            return True
        except Exception as e:
            print(f"Error putting topic: {e}")
            return False
    
    def scan_topics(
        self,
        category: Optional[str] = None,
        limit: Optional[int] = None
    ) -> List[Dict]:
        """話題をスキャン
        
        Args:
            category: カテゴリでフィルタ（casual, technical）
            limit: 取得件数上限
            
        Returns:
            List[Dict]: 話題リスト
        """
        try:
            scan_kwargs = {}
            
            if category:
                scan_kwargs['FilterExpression'] = 'category = :cat'
                scan_kwargs['ExpressionAttributeValues'] = {':cat': category}
            
            if limit:
                scan_kwargs['Limit'] = limit
            
            response = self.topics_table.scan(**scan_kwargs)
            return response.get('Items', [])
        except Exception as e:
            print(f"Error scanning topics: {e}")
            return []
    
    def update_topic_usage(self, topic_id: str, reaction_count: int) -> bool:
        """話題の使用情報を更新
        
        Args:
            topic_id: 話題ID
            reaction_count: 今回のリアクション数
            
        Returns:
            bool: 成功したらTrue
        """
        try:
            now = datetime.utcnow().isoformat()
            
            self.topics_table.update_item(
                Key={'topic_id': topic_id},
                UpdateExpression='SET last_used_at = :now, usage_count = usage_count + :inc, '
                                'total_reactions = total_reactions + :reactions',
                ExpressionAttributeValues={
                    ':now': now,
                    ':inc': 1,
                    ':reactions': reaction_count
                }
            )
            return True
        except Exception as e:
            print(f"Error updating topic usage: {e}")
            return False
    
    # === Conversations Table ===
    
    def put_conversation(self, conversation: Dict) -> bool:
        """会話を保存
        
        Args:
            conversation: 会話データ
            
        Returns:
            bool: 成功したらTrue
        """
        try:
            self.conversations_table.put_item(Item=conversation)
            return True
        except Exception as e:
            print(f"Error putting conversation: {e}")
            return False
    
    def get_conversation(self, conversation_id: str) -> Optional[Dict]:
        """会話を取得
        
        Args:
            conversation_id: 会話ID
            
        Returns:
            Optional[Dict]: 会話データ
        """
        try:
            response = self.conversations_table.get_item(Key={'conversation_id': conversation_id})
            return response.get('Item')
        except Exception as e:
            print(f"Error getting conversation: {e}")
            return None
    
    def query_conversations_by_channel(
        self,
        channel_id: str,
        limit: int = 50
    ) -> List[Dict]:
        """チャンネルIDで会話を検索
        
        Args:
            channel_id: チャンネルID
            limit: 取得件数上限
            
        Returns:
            List[Dict]: 会話リスト
        """
        try:
            response = self.conversations_table.query(
                IndexName='channel_id-index',
                KeyConditionExpression='channel_id = :channel',
                ExpressionAttributeValues={':channel': channel_id},
                Limit=limit,
                ScanIndexForward=False  # 降順（新しい順）
            )
            return response.get('Items', [])
        except Exception as e:
            print(f"Error querying conversations: {e}")
            return []
    
    # === Events Table ===
    
    def put_event(self, event: Dict) -> bool:
        """イベントを保存
        
        Args:
            event: イベントデータ
            
        Returns:
            bool: 成功したらTrue
        """
        try:
            self.events_table.put_item(Item=event)
            return True
        except Exception as e:
            print(f"Error putting event: {e}")
            return False
    
    def get_event(self, event_tracking_id: str) -> Optional[Dict]:
        """イベントを取得
        
        Args:
            event_tracking_id: イベントトラッキングID
            
        Returns:
            Optional[Dict]: イベントデータ
        """
        try:
            response = self.events_table.get_item(Key={'event_tracking_id': event_tracking_id})
            return response.get('Item')
        except Exception as e:
            print(f"Error getting event: {e}")
            return None
    
    def get_event_by_message(self, slack_message_ts: str, channel_id: str) -> Optional[Dict]:
        """Slackメッセージからイベントを取得
        
        Args:
            slack_message_ts: SlackメッセージのTimestamp
            channel_id: チャンネルID
            
        Returns:
            Optional[Dict]: イベントデータ
        """
        try:
            response = self.events_table.query(
                IndexName='slack_message_ts-index',
                KeyConditionExpression='slack_message_ts = :ts AND channel_id = :channel',
                ExpressionAttributeValues={
                    ':ts': slack_message_ts,
                    ':channel': channel_id
                },
                Limit=1
            )
            items = response.get('Items', [])
            return items[0] if items else None
        except Exception as e:
            print(f"Error getting event by message: {e}")
            return None
    
    def update_event_status(
        self,
        event_tracking_id: str,
        status: str,
        **kwargs
    ) -> bool:
        """イベントステータスを更新
        
        Args:
            event_tracking_id: イベントトラッキングID
            status: 新しいステータス
            **kwargs: 追加で更新するフィールド
            
        Returns:
            bool: 成功したらTrue
        """
        try:
            now = datetime.utcnow().isoformat()
            
            update_expr = 'SET #status = :status, updated_at = :now'
            expr_attr_names = {'#status': 'status'}
            expr_attr_values = {':status': status, ':now': now}
            
            for key, value in kwargs.items():
                update_expr += f', {key} = :{key}'
                expr_attr_values[f':{key}'] = value
            
            self.events_table.update_item(
                Key={'event_tracking_id': event_tracking_id},
                UpdateExpression=update_expr,
                ExpressionAttributeNames=expr_attr_names,
                ExpressionAttributeValues=expr_attr_values
            )
            return True
        except Exception as e:
            print(f"Error updating event status: {e}")
            return False
    
    def add_reaction_to_event(
        self,
        event_tracking_id: str,
        user_id: str,
        user_email: str,
        reaction: str
    ) -> bool:
        """イベントにリアクションを追加
        
        Args:
            event_tracking_id: イベントトラッキングID
            user_id: ユーザーID
            user_email: ユーザーメールアドレス
            reaction: リアクション（絵文字）
            
        Returns:
            bool: 成功したらTrue
        """
        try:
            now = datetime.utcnow().isoformat()
            
            reaction_data = {
                'user_id': user_id,
                'user_email': user_email,
                'reaction': reaction,
                'timestamp': now
            }
            
            self.events_table.update_item(
                Key={'event_tracking_id': event_tracking_id},
                UpdateExpression='SET reactions = list_append(if_not_exists(reactions, :empty_list), :reaction)',
                ExpressionAttributeValues={
                    ':reaction': [reaction_data],
                    ':empty_list': []
                }
            )
            return True
        except Exception as e:
            print(f"Error adding reaction to event: {e}")
            return False
    
    # === Questions Table ===
    
    def put_question(self, question: Dict) -> bool:
        """質問を保存
        
        Args:
            question: 質問データ
            
        Returns:
            bool: 成功したらTrue
        """
        try:
            self.questions_table.put_item(Item=question)
            return True
        except Exception as e:
            print(f"Error putting question: {e}")
            return False
    
    def get_recent_questions_for_user(
        self,
        user_id: str,
        days: int = 7
    ) -> List[Dict]:
        """ユーザーへの最近の質問を取得
        
        Args:
            user_id: ユーザーID
            days: 何日以内の質問を取得するか
            
        Returns:
            List[Dict]: 質問リスト
        """
        try:
            from datetime import timedelta
            cutoff_date = (datetime.utcnow() - timedelta(days=days)).isoformat()
            
            response = self.questions_table.query(
                IndexName='user_id-index',
                KeyConditionExpression='user_id = :user',
                FilterExpression='asked_at > :cutoff',
                ExpressionAttributeValues={
                    ':user': user_id,
                    ':cutoff': cutoff_date
                }
            )
            return response.get('Items', [])
        except Exception as e:
            print(f"Error getting recent questions: {e}")
            return []


def decimal_to_python(obj):
    """DynamoDB DecimalをPython型に変換
    
    Args:
        obj: 変換対象のオブジェクト
        
    Returns:
        変換後のオブジェクト
    """
    if isinstance(obj, list):
        return [decimal_to_python(i) for i in obj]
    elif isinstance(obj, dict):
        return {k: decimal_to_python(v) for k, v in obj.items()}
    elif isinstance(obj, Decimal):
        if obj % 1 == 0:
            return int(obj)
        else:
            return float(obj)
    else:
        return obj
