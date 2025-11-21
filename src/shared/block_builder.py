"""
Slack Block Kit Builder Module

Slack Block Kitを使ったメッセージUIを簡単に作成するための
ヘルパークラスを提供します。
"""

from typing import List, Dict, Optional


class BlockBuilder:
    """Slack Block Kit のビルダークラス"""
    
    @staticmethod
    def topic_message(
        topic_text: str,
        emoji: str = "📢",
        reaction_emojis: Optional[List[str]] = None
    ) -> List[Dict]:
        """話題投稿用のブロック
        
        定期投稿で使用する話題メッセージのBlock Kitを生成します。
        
        Args:
            topic_text: 話題のテキスト
            emoji: 先頭の絵文字（デフォルト: 📢）
            reaction_emojis: リアクション促進用の絵文字リスト
                例: ['thumbsup', 'heart', 'tada']
        
        Returns:
            List[Dict]: Block Kitのブロック配列
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
        
        一定数以上のリアクションが集まった際に、
        ミーティング設定を提案するメッセージを生成します。
        
        Args:
            participant_count: 現在の参加希望者数
        
        Returns:
            List[Dict]: Block Kitのブロック配列
        """
        return [
            {
                "type": "section",
                "text": {
                    "type": "mrkdwn",
                    "text": (
                        f"🎉 この話題、盛り上がってますね！（{participant_count}名が興味あり）\n"
                        f"もっと詳しく話したい方はいますか？\n"
                        f"ミーティングを設定する場合は :calendar: でリアクションしてください！"
                    )
                }
            }
        ]
    
    @staticmethod
    def schedule_poll(options: List[Dict[str, str]]) -> List[Dict]:
        """日程投票用のブロック
        
        複数の日程候補から投票で選択するためのメッセージを生成します。
        
        Args:
            options: 日程オプションのリスト
                例: [{"emoji": "1️⃣", "date": "12/5 (木) 14:00"}, ...]
        
        Returns:
            List[Dict]: Block Kitのブロック配列
        """
        text_lines = [
            "📊 *日程投票*",
            "どちらが都合良いですか？",
            ""
        ]
        
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
        
        Googleカレンダーにイベントが作成された際の
        完了通知メッセージを生成します。
        
        Args:
            event_title: イベント名
            date_time: 日時（フォーマット済み文字列）
            location: 場所/URL
            participants: 参加者名リスト（ユーザーIDまたは名前）
            calendar_url: カレンダーイベントのURL
        
        Returns:
            List[Dict]: Block Kitのブロック配列
        """
        # 参加者をメンション形式に変換（既にメンション形式なら変換しない）
        formatted_participants = []
        for name in participants:
            if name.startswith('<@') and name.endswith('>'):
                formatted_participants.append(name)
            else:
                formatted_participants.append(f"<@{name}>")
        
        participant_text = ", ".join(formatted_participants)
        
        return [
            {
                "type": "section",
                "text": {
                    "type": "mrkdwn",
                    "text": "✅ *Googleカレンダーにイベントを作成しました！*"
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
    
    @staticmethod
    def error_message(error_text: str, details: Optional[str] = None) -> List[Dict]:
        """エラーメッセージ用のブロック
        
        Args:
            error_text: エラーメッセージ
            details: 詳細情報（オプション）
        
        Returns:
            List[Dict]: Block Kitのブロック配列
        """
        blocks = [
            {
                "type": "section",
                "text": {
                    "type": "mrkdwn",
                    "text": f"❌ *エラーが発生しました*\n{error_text}"
                }
            }
        ]
        
        if details:
            blocks.append({
                "type": "context",
                "elements": [
                    {
                        "type": "mrkdwn",
                        "text": f"詳細: {details}"
                    }
                ]
            })
        
        return blocks
    
    @staticmethod
    def info_message(
        title: str,
        message: str,
        emoji: str = "ℹ️"
    ) -> List[Dict]:
        """情報メッセージ用のブロック
        
        Args:
            title: タイトル
            message: メッセージ本文
            emoji: 先頭の絵文字
        
        Returns:
            List[Dict]: Block Kitのブロック配列
        """
        return [
            {
                "type": "section",
                "text": {
                    "type": "mrkdwn",
                    "text": f"{emoji} *{title}*\n{message}"
                }
            }
        ]
    
    @staticmethod
    def divider() -> Dict:
        """区切り線ブロック
        
        Returns:
            Dict: 区切り線ブロック
        """
        return {"type": "divider"}
    
    @staticmethod
    def button(
        text: str,
        action_id: str,
        value: str,
        style: Optional[str] = None
    ) -> Dict:
        """ボタン要素
        
        Args:
            text: ボタンのテキスト
            action_id: アクションID
            value: ボタンの値
            style: スタイル（'primary', 'danger', None）
        
        Returns:
            Dict: ボタン要素
        """
        button = {
            "type": "button",
            "text": {
                "type": "plain_text",
                "text": text
            },
            "action_id": action_id,
            "value": value
        }
        
        if style:
            button["style"] = style
        
        return button
    
    @staticmethod
    def actions_block(elements: List[Dict]) -> Dict:
        """アクションブロック
        
        ボタンなどのインタラクティブ要素を配置するブロック。
        
        Args:
            elements: アクション要素のリスト（ボタンなど）
        
        Returns:
            Dict: アクションブロック
        """
        return {
            "type": "actions",
            "elements": elements
        }
