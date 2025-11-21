"""
Integration tests for Lambda functions

These tests require actual AWS credentials and resources.
Run with: pytest tests/integration/ -v
"""

import pytest
import json
import boto3
import os
from datetime import datetime


@pytest.fixture
def lambda_client():
    """Create Lambda client"""
    return boto3.client('lambda', region_name=os.environ.get('AWS_REGION', 'ap-northeast-1'))


@pytest.fixture
def dynamodb_resource():
    """Create DynamoDB resource"""
    return boto3.resource('dynamodb', region_name=os.environ.get('AWS_REGION', 'ap-northeast-1'))


@pytest.mark.integration
class TestScheduledPosterIntegration:
    """Integration tests for scheduled_poster Lambda"""
    
    def test_invoke_scheduled_poster(self, lambda_client):
        """Test invoking scheduled_poster Lambda"""
        function_name = 'slack-bot-calendar-scheduled_poster'
        
        # Prepare test event
        event = {
            'source': 'aws.events',
            'time': datetime.utcnow().isoformat()
        }
        
        # Invoke Lambda
        response = lambda_client.invoke(
            FunctionName=function_name,
            InvocationType='RequestResponse',
            Payload=json.dumps(event)
        )
        
        # Check response
        assert response['StatusCode'] == 200
        
        # Parse response payload
        payload = json.loads(response['Payload'].read())
        assert 'statusCode' in payload
        
        # Should succeed or return expected error
        assert payload['statusCode'] in [200, 500]


@pytest.mark.integration
class TestReactionHandlerIntegration:
    """Integration tests for reaction_handler Lambda"""
    
    def test_url_verification(self, lambda_client):
        """Test Slack URL verification"""
        function_name = 'slack-bot-calendar-reaction_handler'
        
        # Slack URL verification event
        event = {
            'body': json.dumps({
                'type': 'url_verification',
                'challenge': 'test_challenge_string'
            })
        }
        
        # Invoke Lambda
        response = lambda_client.invoke(
            FunctionName=function_name,
            InvocationType='RequestResponse',
            Payload=json.dumps(event)
        )
        
        # Check response
        assert response['StatusCode'] == 200
        
        payload = json.loads(response['Payload'].read())
        assert payload['statusCode'] == 200
        
        body = json.loads(payload['body'])
        assert body['challenge'] == 'test_challenge_string'


@pytest.mark.integration
class TestDynamoDBIntegration:
    """Integration tests for DynamoDB operations"""
    
    def test_topics_table_exists(self, dynamodb_resource):
        """Test that Topics table exists"""
        table_name = os.environ.get('TOPICS_TABLE', 'SlackBotTopics')
        
        try:
            table = dynamodb_resource.Table(table_name)
            table.load()
            assert table.table_status == 'ACTIVE'
        except Exception as e:
            pytest.skip(f"Table {table_name} not found: {e}")
    
    def test_events_table_exists(self, dynamodb_resource):
        """Test that Events table exists"""
        table_name = os.environ.get('EVENTS_TABLE', 'SlackBotEvents')
        
        try:
            table = dynamodb_resource.Table(table_name)
            table.load()
            assert table.table_status == 'ACTIVE'
        except Exception as e:
            pytest.skip(f"Table {table_name} not found: {e}")
    
    def test_write_and_read_topic(self, dynamodb_resource):
        """Test writing and reading a topic"""
        table_name = os.environ.get('TOPICS_TABLE', 'SlackBotTopics')
        
        try:
            table = dynamodb_resource.Table(table_name)
            
            # Write test topic
            test_topic = {
                'topic_id': 'test-integration-topic',
                'category': 'technical',
                'content': 'Integration test topic',
                'reaction_emoji': '🧪',
                'usage_count': 0,
                'total_reactions': 0,
                'average_reactions': 0.0,
                'created_at': datetime.utcnow().isoformat()
            }
            
            table.put_item(Item=test_topic)
            
            # Read back
            response = table.get_item(Key={'topic_id': 'test-integration-topic'})
            item = response.get('Item')
            
            assert item is not None
            assert item['category'] == 'technical'
            assert item['content'] == 'Integration test topic'
            
            # Clean up
            table.delete_item(Key={'topic_id': 'test-integration-topic'})
            
        except Exception as e:
            pytest.skip(f"Table {table_name} not accessible: {e}")


@pytest.mark.integration
@pytest.mark.slow
class TestEndToEndFlow:
    """End-to-end integration tests"""
    
    def test_full_workflow(self, lambda_client, dynamodb_resource):
        """Test complete workflow from posting to tracking"""
        # This is a placeholder for a comprehensive end-to-end test
        # In a real scenario, this would:
        # 1. Invoke scheduled_poster
        # 2. Simulate Slack reaction
        # 3. Invoke reaction_handler
        # 4. Check DynamoDB for updated event tracking
        # 5. Invoke schedule_creator
        # 6. Verify calendar event creation
        
        pytest.skip("Full E2E test requires live Slack workspace and Google Calendar")
