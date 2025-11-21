"""
Calendar Utility Functions

日時の解析、フォーマット、計算などのユーティリティ関数を提供します。
日本語の日時表現に対応しています。
"""

from datetime import datetime, timedelta
from typing import Optional
import re
import pytz


def parse_japanese_datetime(date_str: str) -> Optional[datetime]:
    """日本語の日時文字列をパース
    
    複数の形式の日時文字列を解析してdatetimeオブジェクトに変換します。
    タイムゾーンはAsia/Tokyoとして扱われます。
    
    サポートする形式:
    - "12/5 14:00" (今年の日付として解釈)
    - "2025/12/05 14:00"
    - "12月5日 14時" (今年の日付として解釈)
    - "12月5日 14:00"
    - "2025年12月5日 14:00"
    
    Args:
        date_str: 日時文字列
    
    Returns:
        Optional[datetime]: パースされたdatetimeオブジェクト（失敗時はNone）
    """
    if not date_str:
        return None
    
    date_str = date_str.strip()
    jst = pytz.timezone('Asia/Tokyo')
    current_year = datetime.now().year
    
    # パターン1: "12/5 14:00"
    try:
        dt = datetime.strptime(date_str, "%m/%d %H:%M")
        dt = dt.replace(year=current_year)
        return jst.localize(dt)
    except ValueError:
        pass
    
    # パターン2: "2025/12/05 14:00"
    try:
        dt = datetime.strptime(date_str, "%Y/%m/%d %H:%M")
        return jst.localize(dt)
    except ValueError:
        pass
    
    # パターン3: "12/5 14:00:00" (秒付き)
    try:
        dt = datetime.strptime(date_str, "%m/%d %H:%M:%S")
        dt = dt.replace(year=current_year)
        return jst.localize(dt)
    except ValueError:
        pass
    
    # パターン4: "2025/12/05 14:00:00" (秒付き)
    try:
        dt = datetime.strptime(date_str, "%Y/%m/%d %H:%M:%S")
        return jst.localize(dt)
    except ValueError:
        pass
    
    # パターン5: "12月5日 14時" または "12月5日 14時00分"
    match = re.match(r'(\d+)月(\d+)日\s+(\d+)時(?:(\d+)分)?', date_str)
    if match:
        month, day, hour, minute = match.groups()
        minute = minute or '0'
        try:
            dt = datetime(current_year, int(month), int(day), int(hour), int(minute))
            return jst.localize(dt)
        except ValueError:
            pass
    
    # パターン6: "12月5日 14:00"
    match = re.match(r'(\d+)月(\d+)日\s+(\d+):(\d+)', date_str)
    if match:
        month, day, hour, minute = match.groups()
        try:
            dt = datetime(current_year, int(month), int(day), int(hour), int(minute))
            return jst.localize(dt)
        except ValueError:
            pass
    
    # パターン7: "2025年12月5日 14:00"
    match = re.match(r'(\d+)年(\d+)月(\d+)日\s+(\d+):(\d+)', date_str)
    if match:
        year, month, day, hour, minute = match.groups()
        try:
            dt = datetime(int(year), int(month), int(day), int(hour), int(minute))
            return jst.localize(dt)
        except ValueError:
            pass
    
    # パターン8: "2025-12-05 14:00" (ISO形式的な)
    try:
        dt = datetime.strptime(date_str, "%Y-%m-%d %H:%M")
        return jst.localize(dt)
    except ValueError:
        pass
    
    # パターン9: "12-05 14:00"
    try:
        dt = datetime.strptime(date_str, "%m-%d %H:%M")
        dt = dt.replace(year=current_year)
        return jst.localize(dt)
    except ValueError:
        pass
    
    return None


def format_datetime_japanese(dt: datetime) -> str:
    """datetimeを日本語形式でフォーマット
    
    Args:
        dt: datetimeオブジェクト
    
    Returns:
        str: "2025年12月5日(木) 14:00" 形式の文字列
    """
    jst = pytz.timezone('Asia/Tokyo')
    
    # timezone-aware datetimeに変換
    if dt.tzinfo is None:
        dt_jst = jst.localize(dt)
    else:
        dt_jst = dt.astimezone(jst)
    
    weekdays = ['月', '火', '水', '木', '金', '土', '日']
    weekday = weekdays[dt_jst.weekday()]
    
    return dt_jst.strftime(f"%Y年%m月%d日({weekday}) %H:%M")


def format_datetime_short(dt: datetime) -> str:
    """datetimeを短い日本語形式でフォーマット
    
    Args:
        dt: datetimeオブジェクト
    
    Returns:
        str: "12/5 (木) 14:00" 形式の文字列
    """
    jst = pytz.timezone('Asia/Tokyo')
    
    # timezone-aware datetimeに変換
    if dt.tzinfo is None:
        dt_jst = jst.localize(dt)
    else:
        dt_jst = dt.astimezone(jst)
    
    weekdays = ['月', '火', '水', '木', '金', '土', '日']
    weekday = weekdays[dt_jst.weekday()]
    
    return dt_jst.strftime(f"%-m/%-d ({weekday}) %H:%M")


def calculate_end_time(start_time: datetime, duration_minutes: int) -> datetime:
    """終了時刻を計算
    
    Args:
        start_time: 開始時刻
        duration_minutes: 所要時間（分）
    
    Returns:
        datetime: 終了時刻
    """
    return start_time + timedelta(minutes=duration_minutes)


def parse_duration(duration_str: str) -> Optional[int]:
    """所要時間の文字列をパース
    
    サポートする形式:
    - "1時間" → 60分
    - "2時間30分" → 150分
    - "90分" → 90分
    - "1.5時間" → 90分
    
    Args:
        duration_str: 所要時間の文字列
    
    Returns:
        Optional[int]: 分単位の所要時間（失敗時はNone）
    """
    if not duration_str:
        return None
    
    duration_str = duration_str.strip()
    total_minutes = 0
    
    # パターン1: "1時間30分"
    match = re.match(r'(\d+)時間(?:(\d+)分)?', duration_str)
    if match:
        hours, minutes = match.groups()
        total_minutes = int(hours) * 60
        if minutes:
            total_minutes += int(minutes)
        return total_minutes
    
    # パターン2: "90分"
    match = re.match(r'(\d+)分', duration_str)
    if match:
        return int(match.group(1))
    
    # パターン3: "1.5時間"
    match = re.match(r'(\d+\.?\d*)時間', duration_str)
    if match:
        hours = float(match.group(1))
        return int(hours * 60)
    
    # パターン4: "90" (数字のみ、分として解釈)
    try:
        return int(duration_str)
    except ValueError:
        pass
    
    return None


def get_next_weekday(base_date: datetime, weekday: int) -> datetime:
    """指定した曜日の次の日付を取得
    
    Args:
        base_date: 基準日
        weekday: 曜日（0=月曜, 6=日曜）
    
    Returns:
        datetime: 次の指定曜日の日付
    """
    days_ahead = weekday - base_date.weekday()
    if days_ahead <= 0:  # 今日またはそれ以前
        days_ahead += 7
    return base_date + timedelta(days=days_ahead)


def is_business_day(dt: datetime) -> bool:
    """営業日（平日）かどうかを判定
    
    Args:
        dt: 判定する日付
    
    Returns:
        bool: 平日ならTrue、土日ならFalse
    """
    return dt.weekday() < 5  # 0-4が月-金


def get_jst_now() -> datetime:
    """現在時刻をJSTで取得
    
    Returns:
        datetime: JST（Asia/Tokyo）での現在時刻
    """
    jst = pytz.timezone('Asia/Tokyo')
    return datetime.now(jst)
