"""
Data Models

DynamoDBテーブルのデータモデルを定義します。
"""

from datetime import datetime
from typing import List, Optional
from dataclasses import dataclass, field, asdict
import uuid


@dataclass
class Topic:
    """話題マスターデータ"""
    topic_id: str
    category: str  # 'casual' or 'technical'
    content: str
    reaction_emoji: str
    last_used_at: Optional[str] = None
    usage_count: int = 0
    total_reactions: int = 0
    average_reactions: float = 0.0
    created_at: str = field(default_factory=lambda: datetime.utcnow().isoformat())
    
    @classmethod
    def from_dict(cls, data: dict) -> 'Topic':
        """辞書からインスタンスを生成"""
        return cls(**data)
    
    def to_dict(self) -> dict:
        """辞書に変換（DynamoDB対応）"""
        from decimal import Decimal
        data = asdict(self)
        # float型をDecimal型に変換
        if 'average_reactions' in data and isinstance(data['average_reactions'], float):
            data['average_reactions'] = Decimal(str(data['average_reactions']))
        return data
    
    @staticmethod
    def generate_id() -> str:
        """新しいIDを生成"""
        return str(uuid.uuid4())


@dataclass
class Conversation:
    """会話履歴分析データ"""
    conversation_id: str
    channel_id: str
    message_ts: str
    keywords: List[str]
    participants: List[str]
    reaction_count: int
    sentiment: str  # 'positive', 'neutral', 'negative'
    is_used_for_topic: bool = False
    created_at: str = field(default_factory=lambda: datetime.utcnow().isoformat())
    
    @classmethod
    def from_dict(cls, data: dict) -> 'Conversation':
        """辞書からインスタンスを生成"""
        return cls(**data)
    
    def to_dict(self) -> dict:
        """辞書に変換"""
        return asdict(self)
    
    @staticmethod
    def generate_id() -> str:
        """新しいIDを生成"""
        return str(uuid.uuid4())


@dataclass
class Reaction:
    """リアクション情報"""
    user_id: str
    user_email: str
    reaction: str
    timestamp: str = field(default_factory=lambda: datetime.utcnow().isoformat())
    
    def to_dict(self) -> dict:
        """辞書に変換"""
        return asdict(self)


@dataclass
class EventTracking:
    """イベントトラッキング"""
    event_tracking_id: str
    slack_message_ts: str
    channel_id: str
    topic_id: Optional[str]
    event_title: Optional[str]
    status: str  # 'collecting_reactions', 'scheduling', 'completed', 'cancelled'
    reactions: List[dict] = field(default_factory=list)
    calendar_event_id: Optional[str] = None
    created_at: str = field(default_factory=lambda: datetime.utcnow().isoformat())
    updated_at: str = field(default_factory=lambda: datetime.utcnow().isoformat())
    
    @classmethod
    def from_dict(cls, data: dict) -> 'EventTracking':
        """辞書からインスタンスを生成"""
        return cls(**data)
    
    def to_dict(self) -> dict:
        """辞書に変換"""
        return asdict(self)
    
    @staticmethod
    def generate_id() -> str:
        """新しいIDを生成"""
        return str(uuid.uuid4())
    
    def add_reaction(self, user_id: str, user_email: str, reaction: str):
        """リアクションを追加"""
        reaction_obj = Reaction(
            user_id=user_id,
            user_email=user_email,
            reaction=reaction
        )
        self.reactions.append(reaction_obj.to_dict())
        self.updated_at = datetime.utcnow().isoformat()
    
    def get_participant_emails(self) -> List[str]:
        """参加者のメールアドレスリストを取得"""
        emails = set()
        for reaction in self.reactions:
            if reaction.get('user_email'):
                emails.add(reaction['user_email'])
        return list(emails)
    
    def get_participant_count(self) -> int:
        """参加者数を取得"""
        return len(self.get_participant_emails())


@dataclass
class Question:
    """メンバーへの質問履歴"""
    question_id: str
    user_id: str
    asked_at: str
    question_content: str
    channel_id: str
    message_ts: str
    response_count: int = 0
    reaction_count: int = 0
    
    @classmethod
    def from_dict(cls, data: dict) -> 'Question':
        """辞書からインスタンスを生成"""
        return cls(**data)
    
    def to_dict(self) -> dict:
        """辞書に変換"""
        return asdict(self)
    
    @staticmethod
    def generate_id() -> str:
        """新しいIDを生成"""
        return str(uuid.uuid4())
