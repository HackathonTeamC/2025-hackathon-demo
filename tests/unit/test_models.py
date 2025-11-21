"""
Unit tests for data models
"""

import pytest
from datetime import datetime
from src.shared.models import Topic, Conversation, EventTracking, Question, Reaction


class TestTopic:
    """Test Topic model"""
    
    def test_create_topic(self):
        """Test creating a topic"""
        topic = Topic(
            topic_id="test-id",
            category="casual",
            content="Test topic content",
            reaction_emoji="👍"
        )
        
        assert topic.topic_id == "test-id"
        assert topic.category == "casual"
        assert topic.content == "Test topic content"
        assert topic.reaction_emoji == "👍"
        assert topic.usage_count == 0
    
    def test_topic_to_dict(self):
        """Test converting topic to dictionary"""
        topic = Topic(
            topic_id="test-id",
            category="technical",
            content="Test content",
            reaction_emoji="🚀"
        )
        
        data = topic.to_dict()
        
        assert isinstance(data, dict)
        assert data['topic_id'] == "test-id"
        assert data['category'] == "technical"
    
    def test_topic_from_dict(self):
        """Test creating topic from dictionary"""
        data = {
            'topic_id': "test-id",
            'category': "casual",
            'content': "Test content",
            'reaction_emoji': "👍",
            'usage_count': 0,
            'total_reactions': 0,
            'average_reactions': 0.0
        }
        
        topic = Topic.from_dict(data)
        
        assert topic.topic_id == "test-id"
        assert topic.category == "casual"
    
    def test_generate_id(self):
        """Test ID generation"""
        id1 = Topic.generate_id()
        id2 = Topic.generate_id()
        
        assert id1 != id2
        assert len(id1) > 0


class TestEventTracking:
    """Test EventTracking model"""
    
    def test_create_event_tracking(self):
        """Test creating event tracking"""
        event = EventTracking(
            event_tracking_id="event-id",
            slack_message_ts="1234567890.123456",
            channel_id="C01234567",
            topic_id="topic-id",
            event_title="Test Event",
            status="collecting_reactions"
        )
        
        assert event.event_tracking_id == "event-id"
        assert event.status == "collecting_reactions"
        assert len(event.reactions) == 0
    
    def test_add_reaction(self):
        """Test adding reaction to event"""
        event = EventTracking(
            event_tracking_id="event-id",
            slack_message_ts="1234567890.123456",
            channel_id="C01234567",
            topic_id=None,
            event_title=None,
            status="collecting_reactions"
        )
        
        event.add_reaction(
            user_id="U12345",
            user_email="user@example.com",
            reaction="👍"
        )
        
        assert len(event.reactions) == 1
        assert event.reactions[0]['user_id'] == "U12345"
        assert event.reactions[0]['user_email'] == "user@example.com"
    
    def test_get_participant_emails(self):
        """Test getting participant emails"""
        event = EventTracking(
            event_tracking_id="event-id",
            slack_message_ts="1234567890.123456",
            channel_id="C01234567",
            topic_id=None,
            event_title=None,
            status="collecting_reactions"
        )
        
        event.add_reaction("U1", "user1@example.com", "👍")
        event.add_reaction("U2", "user2@example.com", "❤️")
        event.add_reaction("U1", "user1@example.com", "🎉")  # Duplicate user
        
        emails = event.get_participant_emails()
        
        assert len(emails) == 2
        assert "user1@example.com" in emails
        assert "user2@example.com" in emails
    
    def test_get_participant_count(self):
        """Test getting participant count"""
        event = EventTracking(
            event_tracking_id="event-id",
            slack_message_ts="1234567890.123456",
            channel_id="C01234567",
            topic_id=None,
            event_title=None,
            status="collecting_reactions"
        )
        
        event.add_reaction("U1", "user1@example.com", "👍")
        event.add_reaction("U2", "user2@example.com", "❤️")
        
        assert event.get_participant_count() == 2


class TestConversation:
    """Test Conversation model"""
    
    def test_create_conversation(self):
        """Test creating conversation"""
        conv = Conversation(
            conversation_id="conv-id",
            channel_id="C01234567",
            message_ts="1234567890.123456",
            keywords=["test", "keywords"],
            participants=["U1", "U2"],
            reaction_count=5,
            sentiment="positive"
        )
        
        assert conv.conversation_id == "conv-id"
        assert len(conv.keywords) == 2
        assert conv.sentiment == "positive"
        assert conv.is_used_for_topic is False


class TestQuestion:
    """Test Question model"""
    
    def test_create_question(self):
        """Test creating question"""
        question = Question(
            question_id="q-id",
            user_id="U12345",
            asked_at=datetime.utcnow().isoformat(),
            question_content="Test question?",
            channel_id="C01234567",
            message_ts="1234567890.123456"
        )
        
        assert question.question_id == "q-id"
        assert question.user_id == "U12345"
        assert question.response_count == 0


class TestReaction:
    """Test Reaction model"""
    
    def test_create_reaction(self):
        """Test creating reaction"""
        reaction = Reaction(
            user_id="U12345",
            user_email="user@example.com",
            reaction="👍"
        )
        
        assert reaction.user_id == "U12345"
        assert reaction.user_email == "user@example.com"
        assert reaction.reaction == "👍"
        assert reaction.timestamp is not None
    
    def test_reaction_to_dict(self):
        """Test converting reaction to dictionary"""
        reaction = Reaction(
            user_id="U12345",
            user_email="user@example.com",
            reaction="👍"
        )
        
        data = reaction.to_dict()
        
        assert isinstance(data, dict)
        assert data['user_id'] == "U12345"
        assert 'timestamp' in data
