"""
Unit tests for calendar utilities
"""

import pytest
from datetime import datetime, timedelta
from src.shared.calendar_utils import (
    parse_japanese_datetime,
    format_datetime_japanese,
    format_datetime_short,
    calculate_end_time,
    parse_duration,
    get_next_weekday,
    is_business_day,
    get_jst_now
)


class TestParseJapaneseDateTime:
    """Test parse_japanese_datetime function"""
    
    def test_parse_slash_format(self):
        """Test parsing '12/5 14:00' format"""
        result = parse_japanese_datetime("12/5 14:00")
        assert result is not None
        assert result.month == 12
        assert result.day == 5
        assert result.hour == 14
        assert result.minute == 0
    
    def test_parse_full_date(self):
        """Test parsing '2025/12/05 14:00' format"""
        result = parse_japanese_datetime("2025/12/05 14:00")
        assert result is not None
        assert result.year == 2025
        assert result.month == 12
        assert result.day == 5
    
    def test_parse_japanese_format(self):
        """Test parsing '12月5日 14時' format"""
        result = parse_japanese_datetime("12月5日 14時")
        assert result is not None
        assert result.month == 12
        assert result.day == 5
        assert result.hour == 14
    
    def test_parse_invalid_format(self):
        """Test parsing invalid format"""
        result = parse_japanese_datetime("invalid date")
        assert result is None
    
    def test_parse_empty_string(self):
        """Test parsing empty string"""
        result = parse_japanese_datetime("")
        assert result is None


class TestFormatDateTime:
    """Test datetime formatting functions"""
    
    def test_format_datetime_japanese(self):
        """Test Japanese format"""
        dt = datetime(2025, 12, 5, 14, 30)
        result = format_datetime_japanese(dt)
        assert "2025年" in result
        assert "12月" in result
        assert "5日" in result
        assert "14:30" in result
    
    def test_format_datetime_short(self):
        """Test short format"""
        dt = datetime(2025, 12, 5, 14, 30)
        result = format_datetime_short(dt)
        assert "12/5" in result
        assert "14:30" in result


class TestCalculateEndTime:
    """Test calculate_end_time function"""
    
    def test_calculate_end_time_60min(self):
        """Test calculating end time for 60 minutes"""
        start = datetime(2025, 12, 5, 14, 0)
        end = calculate_end_time(start, 60)
        
        assert end == datetime(2025, 12, 5, 15, 0)
    
    def test_calculate_end_time_90min(self):
        """Test calculating end time for 90 minutes"""
        start = datetime(2025, 12, 5, 14, 0)
        end = calculate_end_time(start, 90)
        
        assert end == datetime(2025, 12, 5, 15, 30)
    
    def test_calculate_end_time_crosses_day(self):
        """Test calculating end time that crosses midnight"""
        start = datetime(2025, 12, 5, 23, 30)
        end = calculate_end_time(start, 60)
        
        assert end.day == 6
        assert end.hour == 0
        assert end.minute == 30


class TestParseDuration:
    """Test parse_duration function"""
    
    def test_parse_hours(self):
        """Test parsing hours"""
        assert parse_duration("2時間") == 120
        assert parse_duration("1時間") == 60
    
    def test_parse_minutes(self):
        """Test parsing minutes"""
        assert parse_duration("30分") == 30
        assert parse_duration("90分") == 90
    
    def test_parse_hours_and_minutes(self):
        """Test parsing hours and minutes"""
        assert parse_duration("2時間30分") == 150
        assert parse_duration("1時間15分") == 75
    
    def test_parse_invalid(self):
        """Test parsing invalid duration"""
        result = parse_duration("invalid")
        assert result is None
    
    def test_parse_english_format(self):
        """Test parsing English format"""
        assert parse_duration("2 hours") == 120
        assert parse_duration("30 minutes") == 30


class TestGetNextWeekday:
    """Test get_next_weekday function"""
    
    def test_get_next_monday(self):
        """Test getting next Monday"""
        # Start from a known date (e.g., Friday)
        friday = datetime(2025, 12, 5)  # Assuming this is a Friday
        next_monday = get_next_weekday(friday, 0)  # 0 = Monday
        
        assert next_monday.weekday() == 0
        assert next_monday > friday
    
    def test_get_same_weekday(self):
        """Test getting same weekday (should return next week)"""
        monday = datetime(2025, 12, 1)  # Assuming this is a Monday
        next_monday = get_next_weekday(monday, 0)  # 0 = Monday
        
        assert next_monday.weekday() == 0
        assert next_monday > monday
        assert (next_monday - monday).days == 7


class TestIsBusinessDay:
    """Test is_business_day function"""
    
    def test_weekday(self):
        """Test weekday (should be business day)"""
        # Create a known weekday
        monday = datetime(2025, 12, 1)  # Monday
        assert is_business_day(monday) is True
    
    def test_weekend(self):
        """Test weekend (should not be business day)"""
        # Create a known weekend day
        saturday = datetime(2025, 12, 6)  # Saturday
        sunday = datetime(2025, 12, 7)  # Sunday
        
        assert is_business_day(saturday) is False
        assert is_business_day(sunday) is False


class TestGetJstNow:
    """Test get_jst_now function"""
    
    def test_get_jst_now(self):
        """Test getting current time in JST"""
        jst_now = get_jst_now()
        
        assert jst_now is not None
        assert isinstance(jst_now, datetime)
        # JST should be UTC+9
        utc_now = datetime.utcnow()
        # Allow some difference due to execution time
        assert abs((jst_now - utc_now).total_seconds() - 9*3600) < 60
