"""
Pydantic schemas for request/response validation
"""

from typing import List, Optional, Dict, Any
from pydantic import BaseModel, Field


# ========== REQUEST SCHEMAS ==========

class MatchRequest(BaseModel):
    """Request to match caregivers (for demo with mock data)"""
    request_id: str = Field(..., description="ID của care request")
    top_n: Optional[int] = Field(10, description="Số lượng caregivers trả về", ge=1, le=50)


class MatchPayload(BaseModel):
    """Request từ Spring Boot với care_request + candidates"""
    care_request: Dict[str, Any] = Field(..., description="Care request object")
    candidates: List[Dict[str, Any]] = Field(..., description="List of caregiver candidates")
    top_n: Optional[int] = Field(10, description="Số lượng recommendations", ge=1, le=50)


class MobileMatchRequest(BaseModel):
    """Request từ Mobile App với đầy đủ thông tin care request"""
    seeker_name: str = Field(..., description="Tên người tìm kiếm")
    care_level: int = Field(..., description="Mức độ chăm sóc (1-4)", ge=1, le=4)
    health_status: str = Field(..., description="Tình trạng sức khỏe")
    elderly_age: int = Field(..., description="Tuổi người già", ge=1, le=120)
    caregiver_age_range: Optional[List[int]] = Field(None, description="Khoảng tuổi người chăm sóc [min, max]")
    gender_preference: Optional[str] = Field(None, description="Giới tính ưu tiên")
    required_years_experience: Optional[int] = Field(None, description="Số năm kinh nghiệm yêu cầu")
    overall_rating_range: Optional[List[float]] = Field(None, description="Khoảng đánh giá [min, max]")
    personality: List[str] = Field(default_factory=list, description="Tính cách")
    attitude: List[str] = Field(default_factory=list, description="Thái độ")
    skills: Dict[str, List[str]] = Field(..., description="Kỹ năng yêu cầu")
    time_slots: List[Dict[str, str]] = Field(..., description="Khung thời gian làm việc")
    location: Dict[str, Any] = Field(..., description="Vị trí làm việc")
    budget_per_hour: int = Field(..., description="Ngân sách theo giờ (VND)")
    top_n: Optional[int] = Field(10, description="Số lượng recommendations", ge=1, le=50)


# ========== RESPONSE SCHEMAS ==========

class ScoreBreakdown(BaseModel):
    """Chi tiết điểm số từng feature"""
    credential: float
    skills: float
    distance: float
    rating: float
    experience: float
    price: float
    trust: float


class CaregiverRecommendation(BaseModel):
    """Một caregiver được recommend"""
    rank: int
    caregiver_id: str
    name: str
    age: int
    gender: str
    rating: float
    total_reviews: int
    years_experience: int
    price_per_hour: int
    distance_km: float
    distance: str  # Formatted distance like "2.5 km"
    avatar: str  # Avatar URL or placeholder
    experience: str  # Formatted experience string
    isVerified: bool  # Verification status
    match_score: float
    match_percentage: str
    score_breakdown: ScoreBreakdown


class MatchResponse(BaseModel):
    """Response của matching API"""
    request_id: str
    care_level: int
    seeker_name: str
    location: Dict
    total_matches: int
    recommendations: List[CaregiverRecommendation]


class SimpleMatchResponse(BaseModel):
    """Response đơn giản cho Spring Boot (chỉ cần scores)"""
    total_matches: int
    recommendations: List[Dict[str, Any]]


class HealthResponse(BaseModel):
    """Health check response"""
    status: str
    message: str
