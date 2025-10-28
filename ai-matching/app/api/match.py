"""
Matching API endpoints
"""

import json
import os
import time
from pathlib import Path
from fastapi import APIRouter, HTTPException
from app.models.schemas import (
    MatchRequest, MatchResponse, MatchPayload, SimpleMatchResponse,
    CaregiverRecommendation, ScoreBreakdown, MobileMatchRequest
)
from app.core.matcher import RuleBasedMatcher

router = APIRouter()
matcher = RuleBasedMatcher()

# Get base directory (backend/)
BASE_DIR = Path(__file__).resolve().parent.parent.parent

# Load data from JSON files
def load_caregivers():
    """Load caregivers from JSON file"""
    file_path = BASE_DIR / 'caregivers.json'
    with open(file_path, 'r', encoding='utf-8') as f:
        return json.load(f)

def load_requests():
    """Load requests from JSON file"""
    file_path = BASE_DIR / 'requests.json'
    with open(file_path, 'r', encoding='utf-8') as f:
        return json.load(f)


@router.get("/requests")
async def get_requests():
    """Lấy danh sách tất cả care requests (mock data)"""
    requests = load_requests()
    return {
        "total": len(requests),
        "requests": requests
    }


@router.get("/caregivers")
async def get_caregivers():
    """Lấy danh sách tất cả caregivers (mock data)"""
    caregivers = load_caregivers()
    return {
        "total": len(caregivers),
        "caregivers": caregivers
    }


@router.post("/match", response_model=MatchResponse)
async def match_caregivers(request: MatchRequest):
    """
    Match caregivers cho một care request.
    
    **Phase 1: Rule-based matching**
    
    Algorithm:
        1. Hard filters: level, degree, distance, time, gender
        2. Soft scoring: 8 features với weighted sum
        3. Ranking: sắp xếp theo điểm giảm dần
        4. Return: Top N caregivers
    
    **Request Body:**
    ```json
    {
        "request_id": "req_001",
        "top_n": 10
    }
    ```
    
    **Response:**
    - List caregivers được recommend
    - Mỗi caregiver có:
        - Thông tin cơ bản (tên, tuổi, giới tính...)
        - Match score (0-1)
        - Score breakdown (chi tiết từng feature)
        - Khoảng cách (km)
    """
    
    try:
        # Tìm care request
        requests = load_requests()
        care_request = next(
            (r for r in requests if r['id'] == request.request_id),
            None
        )
        
        if not care_request:
            raise HTTPException(
                status_code=404, 
                detail=f"Care request với ID '{request.request_id}' không tồn tại"
            )
        
        # Run matching algorithm
        caregivers = load_caregivers()
        results = matcher.match(care_request, caregivers, top_n=request.top_n)
        
        # Format response
        recommendations = []
        for i, result in enumerate(results, 1):
            cg = result['caregiver']
            breakdown = result['breakdown']
            
            # Extract nested data với fallback
            personal_info = cg.get('personal_info', cg)
            professional_info = cg.get('professional_info', cg)
            ratings_reviews = cg.get('ratings_reviews', cg)
            credentials = cg.get('credentials', [])
            
            
            # Format distance
            distance_km = result['distance_km']
            if distance_km < 1:
                distance_str = f"{int(distance_km * 1000)}m"
            else:
                distance_str = f"{distance_km:.1f} km"
            
            # Get years experience - try multiple sources
            years_exp = (
                professional_info.get('years_experience') or 
                cg.get('years_experience') or 
                cg.get('experience_years') or 
                0
            )
            
            # Format experience string
            if years_exp == 0:
                experience_str = "Mới vào nghề"
            elif years_exp == 1:
                experience_str = "1 năm kinh nghiệm"
            else:
                experience_str = f"{years_exp} năm kinh nghiệm"
            
            # Check if verified (has verified credentials)
            is_verified = False
            for cred in credentials:
                if cred.get('verified', False) and cred.get('status') == 'verified':
                    is_verified = True
                    break
            
            # Generate avatar placeholder
            name = personal_info.get('full_name', cg.get('name', 'Unknown'))
            initials = ''.join([word[0].upper() for word in name.split()[:2]])
            avatar_url = f"https://ui-avatars.com/api/?name={initials}&background=4ECDC4&color=fff&size=120"
            
            recommendations.append(
                CaregiverRecommendation(
                    rank=i,
                    caregiver_id=cg['id'],
                    name=name,
                    age=personal_info.get('age', cg.get('age', 0)),
                    gender=personal_info.get('gender', cg.get('gender', 'unknown')),
                    rating=ratings_reviews.get('overall_rating', cg.get('rating', 0.0)),
                    total_reviews=ratings_reviews.get('total_reviews', cg.get('total_reviews', 0)),
                    years_experience=years_exp,
                    price_per_hour=professional_info.get('price_per_hour', professional_info.get('hourly_rate', cg.get('price_per_hour', cg.get('hourly_rate', 0)))),
                    distance_km=distance_km,
                    distance=distance_str,
                    avatar=avatar_url,
                    experience=experience_str,
                    isVerified=is_verified,
                    match_score=result['total_score'],
                    match_percentage=f"{int(result['total_score'] * 100)}%",
                    score_breakdown=ScoreBreakdown(**breakdown)
                )
            )
    
        return MatchResponse(
            request_id=request.request_id,
            care_level=care_request['care_level'],
            seeker_name=care_request['seeker_name'],
            location=care_request['location'],
            total_matches=len(recommendations),
            recommendations=recommendations
        )
    
    except Exception as e:
        print(f"ERROR in match_caregivers: {e}")
        import traceback
        traceback.print_exc()
        raise HTTPException(
            status_code=500,
            detail=f"Internal server error: {str(e)}"
        )


@router.post("/match-from-spring", response_model=SimpleMatchResponse)
async def match_from_spring_boot(payload: MatchPayload):
    """
    Endpoint cho Spring Boot gọi - nhận care_request + candidates
    
    **Use case:** Spring Boot đã lọc sơ bộ candidates từ DB, gửi sang Python để AI ranking
    
    **Request Body:**
    ```json
    {
        "care_request": {
            "id": "req_001",
            "care_level": 3,
            "location": {"lat": 10.7350, "lon": 106.7200},
            "time_slots": [...],
            "budget_per_hour": 90000
        },
        "candidates": [
            {caregiver_1},
            {caregiver_2},
            ...
        ],
        "top_n": 10
    }
    ```
    
    **Response:** List caregivers với scores, KHÔNG cần format lại (Spring Boot tự format)
    """
    
    care_request = payload.care_request
    candidates = payload.candidates
    top_n = payload.top_n
    
    # Validate
    if not candidates:
        return SimpleMatchResponse(
            total_matches=0,
            recommendations=[]
        )
    
    # Run matching
    results = matcher.match(care_request, candidates, top_n=top_n)
    
    # Format response đơn giản (Spring Boot sẽ tự enrich data)
    recommendations = []
    for i, result in enumerate(results, 1):
        cg = result['caregiver']
        recommendations.append({
            'rank': i,
            'caregiver_id': cg.get('id'),
            'match_score': result['total_score'],
            'match_percentage': f"{int(result['total_score'] * 100)}%",
            'distance_km': result['distance_km'],
            'score_breakdown': result['breakdown']
        })
    
    return SimpleMatchResponse(
        total_matches=len(recommendations),
        recommendations=recommendations
    )


@router.post("/match-mobile", response_model=MatchResponse)
async def match_caregivers_mobile(request: MobileMatchRequest):
    """
    Match caregivers cho Mobile App - nhận trực tiếp request body từ UI
    
    **Use case:** Mobile App gửi trực tiếp thông tin care request
    
    **Request Body:**
    ```json
    {
        "seeker_name": "Người dùng",
        "care_level": 3,
        "health_status": "moderate",
        "elderly_age": 75,
        "caregiver_age_range": [25, 50],
        "gender_preference": null,
        "required_years_experience": 3,
        "overall_rating_range": [3.0, 4.0],
        "personality": ["thân thiện", "kiên nhẫn"],
        "attitude": ["tích cực", "chăm chỉ"],
        "skills": {
            "required_skills": ["tiêm insulin", "đo đường huyết"],
            "priority_skills": ["nấu ăn", "massage"]
        },
        "time_slots": [
            {"day": "monday", "start": "06:00", "end": "12:00"},
            {"day": "tuesday", "start": "12:00", "end": "18:00"}
        ],
        "location": {
            "lat": 10.7350,
            "lon": 106.7200,
            "address": "Quận 7, TP.HCM"
        },
        "budget_per_hour": 150000,
        "top_n": 10
    }
    ```
    
    **Response:** List caregivers với scores và thông tin chi tiết
    """
    
    try:
        # Convert mobile request to care_request format
        care_request = {
            "id": f"mobile_{int(time.time())}",  # Generate unique ID
            "seeker_name": request.seeker_name,
            "care_level": request.care_level,
            "health_status": request.health_status,
            "elderly_age": request.elderly_age,
            "caregiver_age_range": request.caregiver_age_range,
            "gender_preference": request.gender_preference,
            "required_years_experience": request.required_years_experience,
            "overall_rating_range": request.overall_rating_range,
            "personality": request.personality,
            "attitude": request.attitude,
            "skills": request.skills,
            "time_slots": request.time_slots,
            "location": request.location,
            "budget_per_hour": request.budget_per_hour,
            "top_n": request.top_n or 10
        }
        
        # Run matching algorithm
        caregivers = load_caregivers()
        results = matcher.match(care_request, caregivers, top_n=request.top_n)
        
        # Format response
        recommendations = []
        for i, result in enumerate(results, 1):
            cg = result['caregiver']
            
            # Get degree info
            has_degree = False
            degree_name = None
            if 'professional_info' in cg:
                credentials = cg['professional_info'].get('credentials', [])
                for cred in credentials:
                    if cred.get('type') == 'degree' and cred.get('verified', False):
                        has_degree = True
                        degree_name = cred.get('name', '')
                        break
            
            # Get rating info
            ratings_reviews = cg.get('ratings_reviews', cg)
            rating = ratings_reviews.get('overall_rating', cg.get('rating', 0.0))
            total_reviews = ratings_reviews.get('total_reviews', cg.get('total_reviews', 0))
            
            # Extract additional info for UI
            personal_info = cg.get('personal_info', cg)
            professional_info = cg.get('professional_info', cg)
            
            # Check if verified (has verified credentials)
            is_verified = False
            credentials = cg.get('credentials', [])
            for cred in credentials:
                if cred.get('verified', False) and cred.get('status') == 'verified':
                    is_verified = True
                    break
            
            # Format distance
            distance_km = result['distance_km']
            if distance_km < 1:
                distance_str = f"{int(distance_km * 1000)}m"
            else:
                distance_str = f"{distance_km:.1f} km"
            
            # Get years experience - try multiple sources
            years_exp = (
                professional_info.get('years_experience') or 
                cg.get('years_experience') or 
                cg.get('experience_years') or 
                0
            )
            
            # Format experience string
            if years_exp == 0:
                experience_str = "Mới vào nghề"
            elif years_exp == 1:
                experience_str = "1 năm kinh nghiệm"
            else:
                experience_str = f"{years_exp} năm kinh nghiệm"
            
            # Generate avatar placeholder
            name = personal_info.get('full_name', cg.get('name', 'Unknown'))
            initials = ''.join([word[0].upper() for word in name.split()[:2]])
            avatar_url = f"https://ui-avatars.com/api/?name={initials}&background=4ECDC4&color=fff&size=120"
            
            recommendations.append(
                CaregiverRecommendation(
                    rank=i,
                    caregiver_id=cg.get('id', ''),
                    name=name,
                    age=personal_info.get('age', cg.get('age', 0)),
                    gender=personal_info.get('gender', cg.get('gender', 'unknown')),
                    rating=rating,
                    total_reviews=total_reviews,
                    years_experience=years_exp,
                    price_per_hour=professional_info.get('price_per_hour', cg.get('hourly_rate', 0)),
                    distance_km=distance_km,
                    distance=distance_str,
                    avatar=avatar_url,
                    experience=experience_str,
                    isVerified=is_verified,
                    match_score=result['total_score'],
                    match_percentage=f"{int(result['total_score'] * 100)}%",
                    score_breakdown=ScoreBreakdown(**result['breakdown'])
                )
            )
        
        return MatchResponse(
            request_id=care_request['id'],
            care_level=care_request['care_level'],
            seeker_name=care_request['seeker_name'],
            location=care_request['location'],
            total_matches=len(recommendations),
            recommendations=recommendations
        )
    
    except Exception as e:
        print(f"ERROR in match_caregivers_mobile: {e}")
        import traceback
        traceback.print_exc()
        raise HTTPException(
            status_code=500,
            detail=f"Internal server error: {str(e)}"
        )
