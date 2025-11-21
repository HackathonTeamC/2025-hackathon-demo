# タスク6: 話題生成エンジン

## 概要
話題テンプレートの作成と管理、ランダム選択ロジックの最適化を行います。

## 目的
- 雑談・技術的な話題テンプレートの作成
- 話題の効果測定
- 動的な重み付けによる最適選択

## 実装内容

### 1. 話題テンプレートデータ作成

#### 1.1 topics_data.json（src/data/topics_data.json）

```json
[
  {
    "topic_id": "casual-lunch",
    "category": "casual",
    "content": "☕ 今日のランチは何を食べましたか？おすすめがあればシェアしてください！",
    "reaction_emoji": "👍"
  },
  {
    "topic_id": "casual-hobby",
    "category": "casual",
    "content": "🎮 最近ハマっているゲームや趣味はありますか？\n語りたい方はリアクションしてください！",
    "reaction_emoji": "🎉"
  },
  {
    "topic_id": "casual-book",
    "category": "casual",
    "content": "📚 最近読んだ本や漫画で面白かったものを教えてください！\nおすすめしたい方はリアクションしてください！",
    "reaction_emoji": "📖"
  },
  {
    "topic_id": "casual-music",
    "category": "casual",
    "content": "🎵 作業中に聞く音楽のジャンルは？集中できるBGMを共有しましょう！",
    "reaction_emoji": "🎧"
  },
  {
    "topic_id": "casual-cafe",
    "category": "casual",
    "content": "☕ お気に入りのカフェやコーヒーショップはありますか？\nシェアしたい方はリアクションしてください！",
    "reaction_emoji": "❤️"
  },
  {
    "topic_id": "casual-weekend",
    "category": "casual",
    "content": "🌟 週末の予定や楽しみにしていることはありますか？",
    "reaction_emoji": "✨"
  },
  {
    "topic_id": "tech-tools",
    "category": "technical",
    "content": "💻 最近使ってみて良かった開発ツールやライブラリはありますか？\n共有したい方はリアクションしてください！",
    "reaction_emoji": "🚀"
  },
  {
    "topic_id": "tech-code-review",
    "category": "technical",
    "content": "🔧 コードレビューで気をつけていることや、レビューのコツを教えてください！\n興味がある方はリアクションしてください！",
    "reaction_emoji": "👀"
  },
  {
    "topic_id": "tech-debugging",
    "category": "technical",
    "content": "🐛 最近遭遇した面白いバグや、印象に残ったデバッグ体験はありますか？\n話したい方はリアクションしてください！",
    "reaction_emoji": "🐞"
  },
  {
    "topic_id": "tech-performance",
    "category": "technical",
    "content": "📊 パフォーマンス改善やリファクタリングで成功した事例を聞かせてください！\n興味がある方はリアクションしてください！",
    "reaction_emoji": "⚡"
  },
  {
    "topic_id": "tech-learning",
    "category": "technical",
    "content": "🎓 次に学びたい技術や、チームで勉強会をやりたいテーマはありますか？\n興味がある方はリアクションしてください！",
    "reaction_emoji": "📚"
  },
  {
    "topic_id": "tech-architecture",
    "category": "technical",
    "content": "🏗️ システムアーキテクチャやデザインパターンで参考になった事例はありますか？",
    "reaction_emoji": "🎯"
  },
  {
    "topic_id": "tech-testing",
    "category": "technical",
    "content": "🧪 テストの書き方や、テストコードで工夫していることはありますか？",
    "reaction_emoji": "✅"
  },
  {
    "topic_id": "tech-ci-cd",
    "category": "technical",
    "content": "🔄 CI/CDパイプラインで便利な設定や、自動化している作業はありますか？",
    "reaction_emoji": "🤖"
  }
]
```

#### 1.2 データ投入スクリプト（scripts/seed_topics.py）

```python
import boto3
import json
from datetime import datetime
import uuid

def seed_topics(table_name: str, data_file: str):
    """話題データをDynamoDBに投入
    
    Args:
        table_name: テーブル名
        data_file: JSONファイルパス
    """
    dynamodb = boto3.resource('dynamodb', region_name='ap-northeast-1')
    table = dynamodb.Table(table_name)
    
    with open(data_file, 'r', encoding='utf-8') as f:
        topics = json.load(f)
    
    now = datetime.now().isoformat()
    initial_date = '2025-01-01T00:00:00Z'
    
    for topic in topics:
        item = {
            'topic_id': topic['topic_id'],
            'category': topic['category'],
            'content': topic['content'],
            'reaction_emoji': topic['reaction_emoji'],
            'last_used_at': initial_date,
            'usage_count': 0,
            'average_reactions': 0.0,
            'created_at': now,
            'updated_at': now
        }
        
        table.put_item(Item=item)
        print(f"✓ Inserted: {topic['topic_id']}")
    
    print(f"\n{len(topics)} topics seeded successfully!")

if __name__ == '__main__':
    seed_topics('SlackBotTopics', '../src/data/topics_data.json')
```

### 2. 話題効果測定機能

#### 2.1 topic_analytics.py（src/shared/topic_analytics.py）

```python
import boto3
from boto3.dynamodb.conditions import Key
from typing import Dict, List
from datetime import datetime, timedelta

class TopicAnalytics:
    """話題の効果測定クラス"""
    
    def __init__(self, topics_table: str, events_table: str):
        dynamodb = boto3.resource('dynamodb')
        self.topics_table = dynamodb.Table(topics_table)
        self.events_table = dynamodb.Table(events_table)
    
    def calculate_topic_performance(self, topic_id: str) -> Dict:
        """話題のパフォーマンスを計算
        
        Args:
            topic_id: 話題ID
        
        Returns:
            パフォーマンス指標
        """
        # この話題を使ったイベントを取得
        events = self._get_events_by_topic(topic_id)
        
        if not events:
            return {
                'topic_id': topic_id,
                'usage_count': 0,
                'average_reactions': 0.0,
                'meeting_creation_rate': 0.0
            }
        
        total_reactions = 0
        meetings_created = 0
        
        for event in events:
            reactions = event.get('reactions', [])
            total_reactions += len(reactions)
            
            if event.get('calendar_event_id'):
                meetings_created += 1
        
        avg_reactions = total_reactions / len(events) if events else 0
        meeting_rate = meetings_created / len(events) if events else 0
        
        return {
            'topic_id': topic_id,
            'usage_count': len(events),
            'average_reactions': round(avg_reactions, 2),
            'meeting_creation_rate': round(meeting_rate, 2),
            'total_meetings_created': meetings_created
        }
    
    def _get_events_by_topic(self, topic_id: str) -> List[Dict]:
        """話題IDでイベントを検索
        
        Args:
            topic_id: 話題ID
        
        Returns:
            イベントのリスト
        """
        # 全イベントをスキャン（最適化の余地あり）
        response = self.events_table.scan(
            FilterExpression='topic_id = :tid',
            ExpressionAttributeValues={':tid': topic_id}
        )
        return response.get('Items', [])
    
    def update_topic_metrics(self, topic_id: str) -> None:
        """話題のメトリクスを更新
        
        Args:
            topic_id: 話題ID
        """
        performance = self.calculate_topic_performance(topic_id)
        
        self.topics_table.update_item(
            Key={'topic_id': topic_id},
            UpdateExpression='SET average_reactions = :avg, usage_count = :count',
            ExpressionAttributeValues={
                ':avg': performance['average_reactions'],
                ':count': performance['usage_count']
            }
        )
    
    def get_best_performing_topics(self, category: str, limit: int = 5) -> List[Dict]:
        """パフォーマンスの良い話題を取得
        
        Args:
            category: カテゴリ
            limit: 取得数
        
        Returns:
            話題のリスト（パフォーマンス順）
        """
        response = self.topics_table.query(
            IndexName='CategoryIndex',
            KeyConditionExpression=Key('category').eq(category)
        )
        
        topics = response.get('Items', [])
        
        # average_reactionsでソート
        sorted_topics = sorted(
            topics,
            key=lambda x: x.get('average_reactions', 0),
            reverse=True
        )
        
        return sorted_topics[:limit]
```

### 3. 高度な話題選択ロジック

#### 3.1 topic_selector.py（src/shared/topic_selector.py）

```python
import random
from typing import Dict, List, Optional
from datetime import datetime, timedelta

class TopicSelector:
    """話題選択の高度なロジック"""
    
    def __init__(self, topics_db, analytics):
        self.topics_db = topics_db
        self.analytics = analytics
    
    def select_with_diversity(
        self,
        category: str,
        recent_days: int = 14
    ) -> Optional[Dict]:
        """多様性を考慮して話題を選択
        
        Args:
            category: カテゴリ
            recent_days: 重複チェックする日数
        
        Returns:
            選択された話題
        """
        since = (datetime.now() - timedelta(days=recent_days)).isoformat()
        
        # 最近使っていない話題を取得
        candidates = self.topics_db.get_unused_topics(category, since)
        
        if not candidates:
            # 全話題から選択（ただし最も古いものを優先）
            all_topics = self.topics_db.get_all_topics(category)
            candidates = sorted(
                all_topics,
                key=lambda x: x.get('last_used_at', '2000-01-01')
            )[:5]  # 最も古い5件
        
        if not candidates:
            return None
        
        # パフォーマンスベースの重み付け
        weights = []
        for topic in candidates:
            # average_reactions + 1 を重みとする（0を避けるため+1）
            weight = topic.get('average_reactions', 0) + 1
            # 使用回数が少ないものを優先（逆数）
            usage_penalty = 1 / (topic.get('usage_count', 0) + 1)
            final_weight = weight * usage_penalty
            weights.append(final_weight)
        
        # 重み付けランダム選択
        selected = random.choices(candidates, weights=weights, k=1)[0]
        return selected
    
    def select_based_on_time(self) -> str:
        """時間帯に基づいてカテゴリを選択
        
        Returns:
            カテゴリ名
        """
        hour = datetime.now().hour
        
        # 午前中(10:00) -> 雑談多め
        if 9 <= hour < 12:
            return 'casual' if random.random() < 0.6 else 'technical'
        # 午後(15:00) -> 技術的な話題多め
        else:
            return 'technical' if random.random() < 0.6 else 'casual'
```

## 成果物
- [ ] 話題テンプレートJSON作成完了（最低20件）
- [ ] データ投入スクリプト作成完了
- [ ] topic_analytics.py 実装完了
- [ ] topic_selector.py 実装完了
- [ ] 話題の効果測定ロジック実装完了

## 検証方法

```python
# 話題選択のテスト
from topic_selector import TopicSelector
from database import TopicsDB
from topic_analytics import TopicAnalytics

topics_db = TopicsDB('SlackBotTopics')
analytics = TopicAnalytics('SlackBotTopics', 'SlackBotEvents')
selector = TopicSelector(topics_db, analytics)

# カテゴリを時間ベースで選択
category = selector.select_based_on_time()
print(f"Selected category: {category}")

# 話題を選択
topic = selector.select_with_diversity(category)
print(f"Selected topic: {topic['content']}")

# パフォーマンス測定
performance = analytics.calculate_topic_performance(topic['topic_id'])
print(f"Performance: {performance}")
```

## 次のタスク
[タスク7: 会話履歴分析](./task-07-conversation-analyzer.md)

## 参考資料
- [DynamoDB Query/Scan最適化](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/bp-query-scan.html)
