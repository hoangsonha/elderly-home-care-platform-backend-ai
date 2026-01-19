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


class ServicePackageQualification(BaseModel):
    """Qualification requirements trong service package"""
    skills: Optional[List[str]] = Field(None, description="Kỹ năng yêu cầu (deprecated)")
    certificate_groups: Optional[List[List[str]]] = Field(None, description="Nhóm chứng chỉ yêu cầu")


class ServicePackageInfo(BaseModel):
    """Thông tin service package"""
    servicePackageId: str = Field(..., description="ID của service package")
    packageName: Optional[str] = Field(None, description="Tên gói")
    description: Optional[str] = Field(None, description="Mô tả")
    durationHours: Optional[int] = Field(None, description="Số giờ")
    packageType: Optional[str] = Field(None, description="Loại gói")
    price: Optional[float] = Field(None, description="Giá")
    note: Optional[str] = Field(None, description="Ghi chú")
    qualification: Optional[ServicePackageQualification] = Field(None, description="Yêu cầu chứng chỉ")
    status: Optional[str] = Field(None, description="Trạng thái")
    serviceTasks: Optional[List[Dict[str, Any]]] = Field(None, description="Danh sách tasks")


class MobileMatchRequest(BaseModel):
    """Request từ Mobile App với đầy đủ thông tin care request"""
    seeker_name: str = Field(..., description="Tên người tìm kiếm")
    health_status: str = Field(..., description="Tình trạng sức khỏe")
    elderly_age: int = Field(..., description="Tuổi người già", ge=1, le=120)
    caregiver_age_range: Optional[List[int]] = Field(None, description="Khoảng tuổi người chăm sóc [min, max]")
    gender_preference: Optional[str] = Field(None, description="Giới tính ưu tiên")
    required_years_experience: Optional[int] = Field(None, description="Số năm kinh nghiệm yêu cầu")
    overall_rating_range: Optional[List[float]] = Field(None, description="Khoảng đánh giá [min, max]")
    time_slots: Dict[str, str] = Field(..., description="Khung thời gian làm việc (có thể là object hoặc array)")
    location: Dict[str, Any] = Field(..., description="Vị trí làm việc")
    service_package: ServicePackageInfo = Field(..., description="Thông tin service package")
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


class FilterStatistics(BaseModel):
    """Thống kê filter failures"""
    failed: int
    percentage: float


class PrimaryReason(BaseModel):
    """Lý do chính không tìm thấy matches"""
    filter: str
    message: str
    failed_count: int
    failed_percentage: float


class Suggestion(BaseModel):
    """Gợi ý để cải thiện kết quả"""
    filter: str
    suggestion: str


class FailureAnalysis(BaseModel):
    """Phân tích lý do không tìm thấy matches"""
    total_candidates: int
    filter_statistics: Dict[str, FilterStatistics]
    primary_reason: PrimaryReason
    suggestions: List[Suggestion]


class SimpleMatchResponse(BaseModel):
    """Response đơn giản cho Spring Boot (chỉ cần scores)"""
    total_matches: int
    recommendations: List[Dict[str, Any]]
    failure_analysis: Optional[FailureAnalysis] = None


class HealthResponse(BaseModel):
    """Health check response"""
    status: str
    message: str
