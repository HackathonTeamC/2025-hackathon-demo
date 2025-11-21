# タスク3: Slack App構築

## 概要
Slack Appの作成と設定、Python SDK（slack-bolt）を使った基盤実装を行います。

## 目的
- Slack Appの作成と権限設定
- Event Subscriptions、Interactive Components の設定
- slack-bolt を使った共通モジュールの実装

## 前提条件
- Slackワークスペースの管理者権限
- タスク1（インフラセットアップ）完了

## 実装内容

### 1. Slack Appの作成

#### 1.1 App作成手順
1. https://api.slack.com/apps にアクセス
2. 「Create New App」→「From scratch」を選択
3. App Name: `SlackBot Calendar`
4. Workspace を選択

#### 1.2 OAuth & Permissions設定

**Bot Token Scopes:**
```
channels:history    # チャンネル履歴取得（会話分析用）
channels:read       # チャンネル情報取得
chat:write          # メッセージ投稿
reactions:read      # リアクション取得
users:read          # ユーザー情報取得
users:read.email    # ユーザーメール取得（カレンダー招待用）
```

**User Token Scopes（不要）:**
- 今回はBot Tokenのみ使用

#### 1.3 Event Subscriptions設定

**Request URL:**
```
https://your-api-gateway-url.execute-api.ap-northeast-1.amazonaws.com/prod/slack/events
```

**Subscribe to bot events:**
```
reaction_added      # リアクション追加時
message.channels    # チャンネルメッセージ（会話分析用）
app_mention         # @bot メンション
```

#### 1.4 Interactive Components設定

**Request URL:**
```
https://your-api-gateway-url.execute-api.ap-northeast-1.amazonaws.com/prod/slack/interactive
```

#### 1.5 Slash Commands（オプション）

**コマンド設定:**
```
/schedule create    - スケジュール作成
  Request URL: https://.../prod/slack/commands

/schedule from      - メッセージからスケジュール作成
  Request URL: https://.../prod/slack/commands

/schedule config    - 設定変更
  Request URL: https://.../prod/slack/commands
```

### 2. 共通モジュール実装

#### 2.1 Slack Client（src/shared/slack_client.py）

```python
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
        """Secrets Managerから認証情報を取得"""
        secret_name = os.environ['SLACK_SECRET_NAME']
        region = os.environ['AWS_REGION']
        
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
            channel: チャンネルID
            text: メッセージテキスト
            blocks: Block Kit形式のブロック
            thread_ts: スレッドに返信する場合のタイムスタンプ
        
        Returns:
            レスポンス辞書（tsを含む）
        """
        try:
            response = self.client.chat_postMessage(
                channel=channel,
                text=text,
                blocks=blocks,
                thread_ts=thread_ts
            )
            return response.data
        except SlackApiError as e:
            raise Exception(f"Failed to post message: {e.response['error']}")
    
    def get_reactions(self, channel: str, timestamp: str) -> List[Dict]:
        """メッセージのリアクションを取得
        
        Args:
            channel: チャンネルID
            timestamp: メッセージタイムスタンプ
        
        Returns:
            リアクション情報のリスト
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
            user_id: ユーザーID
        
        Returns:
            ユーザー情報（email含む）
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
            limit: 取得件数
            oldest: この時刻以降のメッセージを取得
        
        Returns:
            メッセージリスト
        """
        try:
            response = self.client.conversations_history(
                channel=channel,
                limit=limit,
                oldest=oldest
            )
            return response['messages']
        except SlackApiError as e:
            raise Exception(f"Failed to get channel history: {e.response['error']}")
    
    def list_users(self) -> List[Dict]:
        """ワークスペース内の全ユーザーを取得
        
        Returns:
            ユーザーリスト
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
            emoji: 絵文字名（コロンなし）
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
```

#### 2.2 Block Kit ヘルパー（src/shared/block_builder.py）

```python
from typing import List, Dict, Optional

class BlockBuilder:
    """Slack Block Kit のビルダークラス"""
    
    @staticmethod
    def topic_message(
        topic_text: str,
        emoji: str = "📢",
        reaction_emojis: List[str] = None
    ) -> List[Dict]:
        """話題投稿用のブロック
        
        Args:
            topic_text: 話題のテキスト
            emoji: 先頭の絵文字
            reaction_emojis: リアクション促進用の絵文字リスト
        
        Returns:
            Block Kitのブロック配列
        """
        blocks = [
            {
                "type": "section",
                "text": {
                    "type": "mrkdwn",
                    "text": f"{emoji} {topic_text}"
                }
            }
        ]
        
        if reaction_emojis:
            emoji_text = " ".join([f":{e}:" for e in reaction_emojis])
            blocks.append({
                "type": "context",
                "elements": [
                    {
                        "type": "mrkdwn",
                        "text": f"興味がある方はリアクションしてください！ {emoji_text}"
                    }
                ]
            })
        
        return blocks
    
    @staticmethod
    def meeting_proposal(participant_count: int) -> List[Dict]:
        """ミーティング提案用のブロック
        
        Args:
            participant_count: 現在の参加希望者数
        
        Returns:
            Block Kitのブロック配列
        """
        return [
            {
                "type": "section",
                "text": {
                    "type": "mrkdwn",
                    "text": f"🎉 この話題、盛り上がってますね！（{participant_count}名が興味あり）\n"
                           f"もっと詳しく話したい方はいますか？\n"
                           f"ミーティングを設定する場合は :calendar: でリアクションしてください！"
                }
            }
        ]
    
    @staticmethod
    def schedule_poll(options: List[Dict[str, str]]) -> List[Dict]:
        """日程投票用のブロック
        
        Args:
            options: [{"emoji": "1️⃣", "date": "12/5 (木) 14:00"}, ...]
        
        Returns:
            Block Kitのブロック配列
        """
        text_lines = ["📊 *日程投票*", "どちらが都合良いですか？", ""]
        for opt in options:
            text_lines.append(f"{opt['emoji']} {opt['date']}")
        text_lines.append("\nリアクションで投票してください！（24時間後に締切）")
        
        return [
            {
                "type": "section",
                "text": {
                    "type": "mrkdwn",
                    "text": "\n".join(text_lines)
                }
            }
        ]
    
    @staticmethod
    def calendar_created(
        event_title: str,
        date_time: str,
        location: str,
        participants: List[str],
        calendar_url: str
    ) -> List[Dict]:
        """カレンダー作成完了通知用のブロック
        
        Args:
            event_title: イベント名
            date_time: 日時
            location: 場所/URL
            participants: 参加者名リスト
            calendar_url: カレンダーURL
        
        Returns:
            Block Kitのブロック配列
        """
        participant_text = ", ".join([f"@{name}" for name in participants])
        
        return [
            {
                "type": "section",
                "text": {
                    "type": "mrkdwn",
                    "text": f"✅ *Googleカレンダーにイベントを作成しました！*"
                }
            },
            {
                "type": "section",
                "fields": [
                    {
                        "type": "mrkdwn",
                        "text": f"*📅 イベント*\n{event_title}"
                    },
                    {
                        "type": "mrkdwn",
                        "text": f"*🕒 日時*\n{date_time}"
                    },
                    {
                        "type": "mrkdwn",
                        "text": f"*📍 場所*\n{location}"
                    },
                    {
                        "type": "mrkdwn",
                        "text": f"*👥 参加者*\n{participant_text} ({len(participants)}名)"
                    }
                ]
            },
            {
                "type": "section",
                "text": {
                    "type": "mrkdwn",
                    "text": f"カレンダーの招待メールをご確認ください！\n<{calendar_url}|カレンダーで確認>"
                }
            }
        ]
```

### 3. API Gateway設定

Lambda関数をHTTPSエンドポイントとして公開するためにAPI Gatewayを設定します。

**Terraform（api_gateway.tf）:**
```hcl
resource "aws_api_gateway_rest_api" "slack_bot" {
  name        = "${var.project_name}-api"
  description = "Slack Bot API Gateway"
  
  tags = local.common_tags
}

# /slack リソース
resource "aws_api_gateway_resource" "slack" {
  rest_api_id = aws_api_gateway_rest_api.slack_bot.id
  parent_id   = aws_api_gateway_rest_api.slack_bot.root_resource_id
  path_part   = "slack"
}

# /slack/events リソース
resource "aws_api_gateway_resource" "events" {
  rest_api_id = aws_api_gateway_rest_api.slack_bot.id
  parent_id   = aws_api_gateway_resource.slack.id
  path_part   = "events"
}

# POSTメソッド
resource "aws_api_gateway_method" "events_post" {
  rest_api_id   = aws_api_gateway_rest_api.slack_bot.id
  resource_id   = aws_api_gateway_resource.events.id
  http_method   = "POST"
  authorization = "NONE"
}

# Lambda統合
resource "aws_api_gateway_integration" "events_lambda" {
  rest_api_id = aws_api_gateway_rest_api.slack_bot.id
  resource_id = aws_api_gateway_resource.events.id
  http_method = aws_api_gateway_method.events_post.http_method

  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_function.reaction_handler.invoke_arn
}

# デプロイ
resource "aws_api_gateway_deployment" "prod" {
  depends_on = [
    aws_api_gateway_integration.events_lambda
  ]

  rest_api_id = aws_api_gateway_rest_api.slack_bot.id
  stage_name  = "prod"
}

# Lambda実行権限
resource "aws_lambda_permission" "apigw_lambda" {
  statement_id  = "AllowAPIGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.reaction_handler.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.slack_bot.execution_arn}/*/*"
}

# Output
output "api_gateway_url" {
  value = aws_api_gateway_deployment.prod.invoke_url
}
```

## 成果物
- [ ] Slack App作成完了
- [ ] OAuth権限設定完了
- [ ] Event Subscriptions設定完了
- [ ] slack_client.py 実装完了
- [ ] block_builder.py 実装完了
- [ ] API Gateway設定完了

## 検証方法
```python
# slack_client.py のテスト
from slack_client import SlackClient

client = SlackClient()

# メッセージ投稿テスト
response = client.post_message(
    channel='C01234567',
    text='テスト投稿'
)
print(f"Message posted: {response['ts']}")

# ユーザー情報取得テスト
user = client.get_user_info('U01234567')
print(f"User: {user['name']} <{user['email']}>")
```

## 次のタスク
[タスク4: Google Calendar API連携](./task-04-google-calendar-api.md)

## 参考資料
- [Slack Bolt for Python](https://slack.dev/bolt-python/)
- [Slack API Documentation](https://api.slack.com/)
- [Slack Block Kit Builder](https://app.slack.com/block-kit-builder/)
