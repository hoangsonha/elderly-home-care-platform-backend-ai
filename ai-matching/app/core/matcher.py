"""
Phase 1: Rule-based Matching Engine
Weighted scoring algorithm với hard filters và soft preferences
"""

from typing import List, Dict, Optional
import numpy as np
from app.utils import haversine_km


def convert_schedule_to_dict(schedule: List[Dict]) -> Dict:
    """
    Convert schedule array to dict format.
    
    Input: [{"day": "monday", "slots": [...]}, ...]
    Output: {"monday": [...], ...}
    """
    if isinstance(schedule, dict):
        # Already in dict format
        return schedule
    
    result = {}
    for day_entry in schedule:
        day = day_entry.get('day')
        slots = day_entry.get('slots', [])
        result[day] = slots
    return result


class RuleBasedMatcher:
    """
    Rule-based matching engine cho caregiver recommendation.
    
    Approach:
        1. Hard filters: loại bỏ caregivers không đủ điều kiện
        2. Soft scoring: tính điểm cho từng feature (0-1)
        3. Weighted sum: kết hợp điểm theo trọng số
        4. Ranking: sắp xếp theo điểm giảm dần
    
    Hard Filters (8 filters):
        1. Certificate Groups: Caregiver phải có ít nhất 1 chứng chỉ từ mỗi group trong service_package.qualification.certificate_groups
        2. Distance: distance <= caregiver.service_radius_km
        3. Time: 100% overlap với available slots
        4. Gender: match gender preference (nếu có)
        5. Caregiver Age: caregiver.age nằm trong request.caregiver_age_range (nếu có)
        6. Health Status: request.health_status nằm trong caregiver.preferred_health_status
        7. Elderly Age: request.elderly_age nằm trong caregiver.elderly_age_preference (nếu có)
        8. Required Years Experience: caregiver.years_experience >= request.required_years_experience (nếu có)
        9. Overall Rating Range: caregiver.overall_rating nằm trong request.overall_rating_range (nếu có)
    
    Soft Scoring Features (5 features, weights sum = 1.0):
        - Credential (35%): Bằng cấp, certificates
        - Distance (20%): Khoảng cách địa lý
        - Rating (18%): Đánh giá từ khách hàng
        - Experience (14%): Số năm kinh nghiệm
        - Trust (13%): Trust score
    """
    
    def __init__(self):
        # Weights for scoring features (sum = 1.0)
        self.weights = {
            'credential': 0.35,   # Bằng cấp, certificates
            'distance': 0.20,     # Gần = thuận tiện
            'rating': 0.18,       # Chất lượng đã được verify
            'experience': 0.14,   # Kinh nghiệm
            'trust': 0.13         # Độ tin cậy
        }
    
    def match(
        self, 
        care_request: Dict, 
        caregivers: List[Dict],
        top_n: int = 10
    ) -> List[Dict]:
        """
        Match caregivers to a care request với fallback strategy.
        
        Fallback Strategy:
            1. Tìm với service_radius_km gốc của từng caregiver
            2. Nếu không có → lấy 10 người gần nhất đã fail ở distance
            3. Bỏ qua Filter 3 (distance) cho fallback caregivers
        
        Args:
            care_request: Dict chứa thông tin yêu cầu
            caregivers: List của caregiver profiles
            top_n: Số lượng caregivers trả về (mặc định 10)
        
        Returns:
            List of matched caregivers với scores, sorted by score desc
        """
        # BƯỚC 1: Hard Filter với service_radius_km của từng caregiver
        pass_list = []
        fail_list = []
        filter_failures = {}  # Track filter failures: {filter_name: count}
        total_candidates = len(caregivers)
        
        # Validate request location
        req_location = care_request.get('location')
        if not req_location or req_location.get('lat') is None or req_location.get('lon') is None:
            # Nếu request không có location hợp lệ, return empty results
            return [], self._generate_failure_analysis(filter_failures, total_candidates, care_request)
        
        req_lat = req_location['lat']
        req_lon = req_location['lon']
        
        for cg in caregivers:
            # Validate caregiver location
            cg_location = cg.get('location', {})
            cg_lat = cg_location.get('lat') if isinstance(cg_location, dict) else None
            cg_lon = cg_location.get('lon') if isinstance(cg_location, dict) else None
            
            # Fallback to direct fields if location dict doesn't have lat/lon
            if cg_lat is None:
                cg_lat = cg.get('lat')
            if cg_lon is None:
                cg_lon = cg.get('lon')
            
            # Skip if caregiver doesn't have valid location
            if cg_lat is None or cg_lon is None:
                continue
            
            try:
                distance = haversine_km(
                    req_lat, req_lon,
                    cg_lat, cg_lon
                )
            except (TypeError, ValueError) as e:
                # Skip if distance calculation fails (invalid coordinates)
                continue
            
            service_radius = cg_location.get('service_radius_km', 0) if isinstance(cg_location, dict) else cg.get('service_radius_km', 0)
            
            if distance <= service_radius:
                pass_list.append(cg)
            else:
                cg['distance'] = distance
                fail_list.append(cg)
                filter_failures['distance'] = filter_failures.get('distance', 0) + 1
        
        # BƯỚC 2: Sắp xếp fail_list theo distance (gần nhất trước)
        fail_list.sort(key=lambda x: x['distance'])
        
        # BƯỚC 3: Thử pass_list trước
        results = []
        
        for cg in pass_list:
            score_result, failed_filter = self._score_candidate(care_request, cg)
            
            if score_result is not None:
                results.append({
                    'caregiver': cg,
                    'total_score': score_result['total_score'],
                    'breakdown': score_result['breakdown'],
                    'distance_km': score_result['distance_km']
                })
            else:
                # Track which filter failed
                if failed_filter:
                    filter_failures[failed_filter] = filter_failures.get(failed_filter, 0) + 1
        
        if results:
            # Sort by total_score descending
            results.sort(key=lambda x: x['total_score'], reverse=True)
            return results[:top_n], None  # No failure analysis if we have results
        
        # BƯỚC 4: Fallback - Lấy nhiều lần, mỗi lần 10 người từ fail_list
        fallback_results = []
        remaining_fail_list = fail_list.copy()
        
        while remaining_fail_list and len(fallback_results) < top_n:
            # Lấy 10 người gần nhất từ remaining_fail_list
            batch_size = min(10, len(remaining_fail_list))
            current_batch = remaining_fail_list[:batch_size]
            
            # Xử lý batch hiện tại
            batch_results = []
            for cg in current_batch:
                score_result = self._score_candidate_fallback(care_request, cg)
                
                if score_result is not None:
                    batch_results.append({
                        'caregiver': cg,
                        'total_score': score_result['total_score'],
                        'breakdown': score_result['breakdown'],
                        'distance_km': score_result['distance_km'],
                        'radius_multiplier': 'fallback'
                    })
            
            # Thêm batch results vào fallback_results
            fallback_results.extend(batch_results)
            
            # Loại bỏ batch đã xử lý khỏi remaining_fail_list
            remaining_fail_list = remaining_fail_list[batch_size:]
            
            # Nếu đã có đủ kết quả, dừng lại
            if len(fallback_results) >= top_n:
                break
        
        if fallback_results:
            # Sort by total_score descending
            fallback_results.sort(key=lambda x: x['total_score'], reverse=True)
            return fallback_results[:top_n], None  # No failure analysis if we have fallback results
        
        # Nếu không tìm thấy caregiver nào - generate failure analysis
        failure_analysis = self._generate_failure_analysis(filter_failures, total_candidates, care_request)
        return [], failure_analysis
    
    def _check_time_availability(self, req_time_slots: List[Dict], free_schedule: Dict) -> bool:
        """
        Check nếu time slots yêu cầu có available trong free_schedule của caregiver.
        
        Logic:
            - Nếu available_all_time = true → luôn available
            - Nếu available_all_time = false → check booked_slots
            - Check NGÀY: Phải cùng ngày
            - Check TIME OVERLAP: Nếu có bất kỳ thời gian nào trong booked_slots overlap với time_slots yêu cầu → FAIL
        
        Args:
            req_time_slots: List of time slots [{"day": "2026-01-18", "start": "08:00", "end": "12:00"}, ...]
            free_schedule: Free schedule dict {"available_all_time": bool, "booked_slots": [...]}
        
        Returns:
            True nếu tất cả time slots available (không có overlap)
        """
        # Nếu available all time → luôn available
        if free_schedule.get('available_all_time', False):
            return True
        
        # Lấy booked_slots
        booked_slots = free_schedule.get('booked_slots', [])
        
        # Helper function: Convert time string to minutes
        def time_to_minutes(time_str: str) -> int:
            """Convert "HH:MM" to minutes since midnight"""
            hours, minutes = map(int, time_str.split(':'))
            return hours * 60 + minutes
        
        # Check từng time slot yêu cầu
        for req_slot in req_time_slots:
            req_day = req_slot.get('day')  # Format: "2026-01-18"
            req_start = req_slot.get('start')  # Format: "08:00"
            req_end = req_slot.get('end')  # Format: "12:00"
            
            if not req_day or not req_start or not req_end:
                return False
            
            # Convert request time to minutes
            req_start_min = time_to_minutes(req_start)
            req_end_min = time_to_minutes(req_end)
            
            # Check overlap với booked_slots
            for booked_slot in booked_slots:
                booked_date = booked_slot.get('date')  # Format: "2026-01-18"
                booked_start = booked_slot.get('start_time')  # Format: "11:00"
                booked_end = booked_slot.get('end_time')  # Format: "13:00"
                
                # Check nếu cùng ngày
                if booked_date == req_day:
                    # Convert booked time to minutes
                    booked_start_min = time_to_minutes(booked_start)
                    booked_end_min = time_to_minutes(booked_end)
                    
                    # Check overlap: request slot overlap với booked slot
                    # Overlap nếu: req_start < booked_end AND req_end > booked_start
                    if req_start_min < booked_end_min and req_end_min > booked_start_min:
                        return False  # Có overlap → không available
        
        return True  # Tất cả slots đều available (không có overlap)
    
    def calculate_credential_quality_score(self, cg: Dict, required_level: int) -> float:
        """
        Tính điểm chất lượng credentials dựa trên số lượng credentials đạt level yêu cầu
        
        Logic:
            - Đếm số credentials đạt được required_level
            - Credential có nhiều levels đạt yêu cầu = điểm cao hơn
            - Normalize về 0-1
        """
        credentials = cg.get('credentials', [])
        quality_score = 0.0
        
        for cred in credentials:
            # Chỉ tính credential đã verify
            if cred.get('status') != 'verified':
                continue
                
            # Check expiry date cho certificates
            if cred.get('type') == 'certificate' and cred.get('expiry_date'):
                from datetime import datetime
                try:
                    expiry = datetime.fromisoformat(cred['expiry_date'].replace('Z', '+00:00'))
                    if expiry < datetime.now(expiry.tzinfo):
                        continue  # Skip expired certificates
                except ValueError:
                    continue  # Skip invalid date format
            
            # Lấy applicable_levels
            applicable_levels = cred.get('applicable_levels', [])
            if applicable_levels:
                # Đếm số levels đạt yêu cầu trong credential này
                levels_achieved = sum(1 for level in applicable_levels if level >= required_level)
                if levels_achieved > 0:
                    # Credential có nhiều levels đạt yêu cầu = điểm cao hơn
                    quality_score += levels_achieved / len(applicable_levels)
        
        # Normalize về 0-1 (giả sử max 5 credentials)
        return min(1.0, quality_score / 5.0)
    
    def _score_candidate(
        self, 
        req: Dict, 
        cg: Dict
    ) -> tuple[Optional[Dict], Optional[str]]:
        """
        Score a single caregiver against a care request.
        
        Args:
            req: Care request dict (theo format mới từ requests.json)
            cg: Caregiver dict (theo format mới từ caregivers.json)
        
        Returns:
            Tuple (result_dict, failed_filter):
            - result_dict: Dict với total_score, breakdown, distance_km nếu pass
            - failed_filter: Tên filter bị fail (None nếu pass tất cả)
        """
        
        # ========== HARD FILTERS (bắt buộc) ==========
        
        # Extract data từ caregiver theo cấu trúc mới
        location_info = cg.get('location', {})
        profile_data = cg.get('profileData', {})
        preferences = profile_data.get('preferences', {})
        free_schedule = profile_data.get('free_schedule', {})
        ratings_reviews = profile_data.get('ratings_reviews', {})
        
        # Extract các giá trị cần thiết
        years_experience = profile_data.get('years_experience', 0)
        gender = cg.get('gender', None)
        caregiver_age = cg.get('age', None)
        cg_lat = location_info.get('latitude', location_info.get('lat'))
        cg_lon = location_info.get('longitude', location_info.get('lon'))
        service_radius = location_info.get('service_radius_km', 0)
        qualifications = cg.get('qualifications', [])
        
        # ========== FILTER 1: Certificate Groups ==========
        # Logic: Service Package yêu cầu certificate_groups
        # Mỗi group là một mảng UUIDs (OR trong group)
        # Caregiver phải có ít nhất 1 chứng chỉ từ MỖI group (AND giữa các groups)
        service_package = req.get('service_package', {})
        qualification_req = service_package.get('qualification', {})
        certificate_groups = qualification_req.get('certificate_groups', [])
        
        # Nếu service package có yêu cầu certificate_groups
        if certificate_groups:
            # Helper: Check qualification còn hạn và đã được APPROVED
            def is_valid_qualification(qual):
                # Phải được APPROVED
                if qual.get('status') != 'APPROVED':
                    return False
                # Check expiry date nếu có
                expiry_date = qual.get('expiryDate', qual.get('expiry_date'))
                if expiry_date:
                    from datetime import datetime, date
                    try:
                        # Parse date string (format: "2027-02-10")
                        if isinstance(expiry_date, str):
                            expiry = datetime.strptime(expiry_date, '%Y-%m-%d').date()
                        else:
                            expiry = expiry_date
                        if expiry < date.today():
                            return False  # Hết hạn
                    except (ValueError, TypeError):
                        return False  # Invalid date format
                return True
            
            # Lấy danh sách qualificationTypeIds hợp lệ của caregiver
            valid_qualification_type_ids = set()
            for qual in qualifications:
                if is_valid_qualification(qual):
                    qual_type_id = qual.get('qualificationTypeId')
                    if qual_type_id:
                        valid_qualification_type_ids.add(qual_type_id)
            
            # Check từng group: Caregiver phải có ít nhất 1 chứng chỉ từ MỖI group
            for group in certificate_groups:
                if not group:  # Skip empty groups
                    continue
                
                # Check nếu caregiver có ít nhất 1 chứng chỉ trong group này
                has_qual_from_group = False
                for required_type_id in group:
                    if required_type_id in valid_qualification_type_ids:
                        has_qual_from_group = True
                        break
                
                # Nếu không có chứng chỉ nào từ group này → FAIL
                if not has_qual_from_group:
                    return None, "certificate_groups"
        
        # ========== FILTER 2: Distance ==========
        # Logic: Caregiver quyết định bán kính phục vụ (service_radius_km)
        # Request chỉ được match nếu nằm trong bán kính của caregiver
        distance = haversine_km(
            req['location']['lat'], req['location']['lon'],
            cg_lat, cg_lon
        )
        
        if distance > service_radius:
            return None, "distance"
        
        # ========== FILTER 3: Time Availability ==========
        # Logic: Check NGÀY và TIME OVERLAP
        # - Phải cùng ngày
        # - Nếu có bất kỳ thời gian nào trong booked_slots overlap với time_slots yêu cầu → FAIL
        # Format: time_slots có thể là object {"day": "2026-01-18", "start": "08:00", "end": "12:00"}
        # hoặc array [{"day": "2026-01-18", "start": "08:00", "end": "12:00"}, ...]
        req_time_slots = req.get('time_slots', {})
        
        # Convert single object to array format
        if isinstance(req_time_slots, dict) and 'day' in req_time_slots:
            req_time_slots = [req_time_slots]
        
        # Check time availability (check ngày và overlap time)
        if not self._check_time_availability(req_time_slots, free_schedule):
            return None, "time_availability"
        
        # ========== FILTER 4: Gender Preference ==========
        # Logic: Nếu request có gender_preference, caregiver phải match
        gender_preference = req.get('gender_preference')
        if gender_preference and gender:
            # Normalize: "FEMALE" vs "female", "MALE" vs "male"
            if gender_preference.upper() != gender.upper():
                return None, "gender_preference"
        
        # ========== FILTER 5: Caregiver Age Range ==========
        # Logic: Nếu request có caregiver_age_range, caregiver.age phải nằm trong range
        caregiver_age_range = req.get('caregiver_age_range', None)
        if caregiver_age_range and caregiver_age is not None:
            min_age, max_age = caregiver_age_range
            if caregiver_age < min_age or caregiver_age > max_age:
                return None, "caregiver_age_range"
        
        # ========== FILTER 6: Health Status Preference ==========
        # Logic: Hierarchical - Caregiver có thể nhận health status tốt hơn hoặc bằng mức họ chấp nhận
        # - "weak" → nhận "weak", "moderate", "good"
        # - "moderate" → nhận "moderate", "good"
        # - "good" → chỉ nhận "good"
        preferred_health_status = preferences.get('preferred_health_status', None)
        elderly_health_status = req.get('health_status', None)
        
        if preferred_health_status and elderly_health_status:
            # preferred_health_status có thể là string hoặc array
            if isinstance(preferred_health_status, str):
                preferred_health_status = [preferred_health_status]
            
            # Normalize
            preferred_normalized = [s.lower() for s in preferred_health_status]
            elderly_health_status_normalized = elderly_health_status.lower()
            
            # Health status hierarchy (tốt hơn = số cao hơn)
            health_hierarchy = {
                'weak': 1,
                'moderate': 2,
                'good': 3
            }
            
            elderly_level = health_hierarchy.get(elderly_health_status_normalized, 0)
            if elderly_level == 0:
                return None, "health_status_preference"  # Unknown status
            
            # Check từng preferred status
            accepted = False
            for preferred in preferred_normalized:
                preferred_level = health_hierarchy.get(preferred, 0)
                if preferred_level == 0:
                    continue
                
                # Caregiver chấp nhận nếu elderly_status >= preferred_status
                # (tốt hơn hoặc bằng mức họ chấp nhận)
                # - "weak" (1) → nhận "weak" (1), "moderate" (2), "good" (3)
                # - "moderate" (2) → nhận "moderate" (2), "good" (3)
                # - "good" (3) → chỉ nhận "good" (3)
                if elderly_level >= preferred_level:
                    accepted = True
                    break
            
            if not accepted:
                return None, "health_status_preference"
        
        # ========== FILTER 7: Elderly Age Preference ==========
        # Logic: Tuổi người già phải nằm trong elderly_age_preference của caregiver
        elderly_age_preference = preferences.get('elderly_age_preference', None)
        elderly_age = req.get('elderly_age', None)
        
        if elderly_age_preference and elderly_age is not None:
            min_age = elderly_age_preference.get('min_age')
            max_age = elderly_age_preference.get('max_age')
            if min_age is not None and max_age is not None:
                if elderly_age < min_age or elderly_age > max_age:
                    return None, "elderly_age_preference"
        
        # ========== FILTER 8: Required Years Experience ==========
        # Logic: Caregiver phải có đủ số năm kinh nghiệm yêu cầu
        required_years_experience = req.get('required_years_experience', None)
        if required_years_experience is not None:
            if years_experience < required_years_experience:
                return None, "required_years_experience"
        
        # ========== FILTER 9: Overall Rating Range ==========
        # Logic: Overall rating của caregiver phải nằm trong khoảng yêu cầu
        required_rating_range = req.get('overall_rating_range', None)
        if required_rating_range is not None:
            caregiver_rating = ratings_reviews.get('overall_rating', 0.0)
            min_rating, max_rating = required_rating_range
            if caregiver_rating < min_rating or caregiver_rating > max_rating:
                return None, "overall_rating_range"
        
        # ========== SOFT SCORING (normalize về 0-1) ==========
        
        # 1. Credential score (bằng cấp + certificates)
        credential_score = self._calculate_credential_score(req, cg)
        
        # 2. Distance score - Logic mượt: exponential decay
        import math
        # Công thức: score = e^(-distance/scale)
        # Scale = 8: distance 8km → score ≈ 0.37, distance 16km → score ≈ 0.14
        distance_score = math.exp(-distance / 8.0)
        
        # 3. Rating score
        rating_score = self._calculate_rating_score(cg)
        
        # 4. Experience score - Improved: min 0.1 cho caregiver mới
        experience_score = min(1.0, max(0.1, years_experience / 10.0))
        
        # 5. Trust score (simplified: dựa trên rating + experience + reviews)
        trust_score = self._calculate_trust_score(cg)
        
        # ========== WEIGHTED SUM ==========
        
        total_score = (
            self.weights['credential'] * credential_score +
            self.weights['distance'] * distance_score +
            self.weights['rating'] * rating_score +
            self.weights['experience'] * experience_score +
            self.weights['trust'] * trust_score
        )
        
        return ({
            'total_score': round(total_score, 3),
            'distance_km': round(distance, 2),
            'breakdown': {
                'credential': round(credential_score, 3),
                'distance': round(distance_score, 3),
                'rating': round(rating_score, 3),
                'experience': round(experience_score, 3),
                'trust': round(trust_score, 3)
            }
        }, None)  # Return tuple: (result_dict, failed_filter) - None means no filter failed
    
    def _calculate_credential_score(self, req: Dict, cg: Dict) -> float:
        """
        Tính điểm credential dựa trên số lượng qualifications (chứng chỉ) hợp lệ.
        
        Logic:
            - Mỗi qualification APPROVED và chưa hết hạn = 0.2 điểm
            - Max: 5 qualifications = 1.0 điểm
            - Normalize về 0-1: score = len(valid_qualifications) * 0.2
        """
        qualifications = cg.get('qualifications', [])
        POINTS_PER_QUALIFICATION = 0.2  # Mỗi qualification = 0.2 điểm
        MAX_QUALIFICATIONS = 5  # Max 5 qualifications = 1.0 điểm
        
        # Lọc qualifications hợp lệ
        def is_valid_qualification(qual):
            # Phải được APPROVED
            if qual.get('status') != 'APPROVED':
                return False
            # Check expiry date nếu có
            expiry_date = qual.get('expiryDate', qual.get('expiry_date'))
            if expiry_date:
                from datetime import datetime, date
                try:
                    if isinstance(expiry_date, str):
                        expiry = datetime.strptime(expiry_date, '%Y-%m-%d').date()
                    else:
                        expiry = expiry_date
                    if expiry < date.today():
                        return False  # Hết hạn
                except (ValueError, TypeError):
                    return False  # Invalid date format
            return True
        
        valid_qualifications = [q for q in qualifications if is_valid_qualification(q)]
        
        # Mỗi qualification hợp lệ = 0.2 điểm, max 5 qualifications
        num_valid = min(len(valid_qualifications), MAX_QUALIFICATIONS)
        score = num_valid * POINTS_PER_QUALIFICATION
        
        # Normalize về 0-1 (đã normalize sẵn)
        return min(1.0, score)
    
    def _calculate_rating_score(self, cg: Dict) -> float:
        """
        Tính điểm rating sử dụng Bayesian Average.
        
        Logic:
            - Bayesian Average: (total_rating + C * m) / (total_reviews + C)
            - C: Confidence constant (25)
            - m: Mean rating của platform (3.5)
            - Công bằng giữa rating và số lượng reviews
        """
        ratings_reviews = cg.get('ratings_reviews', cg)
        total_reviews = ratings_reviews.get('total_reviews', cg.get('total_reviews', 0))
        
        # Nếu không có reviews, trả về điểm mặc định
        if total_reviews == 0:
            return 0.5
        
        # Tính total_rating từ rating_breakdown nếu có
        rating_breakdown = ratings_reviews.get('rating_breakdown', {})
        if rating_breakdown:
            # Tính tổng điểm từ breakdown
            total_rating = (
                rating_breakdown.get('5_star', 0) * 5 +
                rating_breakdown.get('4_star', 0) * 4 +
                rating_breakdown.get('3_star', 0) * 3 +
                rating_breakdown.get('2_star', 0) * 2 +
                rating_breakdown.get('1_star', 0) * 1
            )
        else:
            # Fallback: dùng overall_rating * total_reviews
            overall_rating = ratings_reviews.get('overall_rating', cg.get('rating', 4.0))
            total_rating = overall_rating * total_reviews
        
        # Bayesian constants
        C = 25  # Confidence constant
        m = 3.5  # Mean rating của platform
        
        # Bayesian average calculation
        bayesian_rating = (total_rating + C * m) / (total_reviews + C)
        
        # Normalize về 0-1
        return min(1.0, bayesian_rating / 5.0)
    
    def _calculate_trust_score(self, cg: Dict) -> float:
        """
        Tính trust score dựa trên task completion, cancel/decline rate, bookings và verification
        
        Factors:
            - Task completion rate (40%): Tỷ lệ hoàn thành task (taskCompletionRate)
            - Cancel/Decline rate (30%): Tỷ lệ hủy và từ chối của caregiver (totalCancelOrDeclineBookingRate, càng thấp càng tốt)
            - Total completed bookings (20%): Số lượng booking đã hoàn thành (totalCompletedBookings)
            - Verification (10%): Xác minh danh tính (isVerified)
        """
        # 1. Task completion rate component (40%)
        # Lấy từ taskCompletionRate ở root level của caregiver (đã là %)
        task_completion_rate = cg.get('taskCompletionRate', 0.0)
        # Normalize về 0-1 (nếu đã là % thì chia 100)
        if task_completion_rate > 1.0:
            task_completion_rate = task_completion_rate / 100.0
        completion_component = min(1.0, max(0.0, task_completion_rate))
        
        # 2. Cancel/Decline rate component (30%) - càng thấp càng tốt
        # Lấy từ totalCancelOrDeclineBookingRate ở root level (đã là %)
        cancel_decline_rate = cg.get('totalCancelOrDeclineBookingRate', 0.0)
        # Normalize về 0-1 (nếu đã là % thì chia 100)
        if cancel_decline_rate > 1.0:
            cancel_decline_rate = cancel_decline_rate / 100.0
        # Invert: 0% cancel = 1.0, 100% cancel = 0.0
        cancel_component = max(0.0, 1.0 - cancel_decline_rate)
        
        # 3. Total completed bookings component (20%)
        # Lấy từ totalCompletedBookings ở root level
        total_completed_bookings = cg.get('totalCompletedBookings', 0)
        if total_completed_bookings >= 100:
            bookings_component = 1.0
        elif total_completed_bookings >= 50:
            bookings_component = 0.8
        elif total_completed_bookings >= 20:
            bookings_component = 0.6
        elif total_completed_bookings >= 10:
            bookings_component = 0.4
        else:
            bookings_component = 0.2
        
        # 4. Verification component (10%)
        # Lấy từ isVerified ở root level
        identity_verified = cg.get('isVerified', False)
        verification_component = 1.0 if identity_verified else 0.0
        
        # Weighted combination
        trust = (
            0.4 * completion_component +
            0.3 * cancel_component +
            0.2 * bookings_component +
            0.1 * verification_component
        )
        
        return min(1.0, trust)
    
    def _score_candidate_fallback(
        self, 
        req: Dict, 
        cg: Dict
    ) -> Optional[Dict]:
        """
        Score a fallback caregiver (bỏ qua Filter 2 - Distance).
        
        Args:
            req: Care request dict (theo format mới từ requests.json)
            cg: Caregiver dict (theo format mới từ caregivers.json)
        
        Returns:
            Dict với total_score, breakdown, distance_km
            None nếu không đủ điều kiện (hard filters 1, 3-9, bỏ qua Filter 2)
        """
        
        # ========== HARD FILTERS (bỏ qua Filter 2 - Distance) ==========
        
        # Extract data từ caregiver theo cấu trúc mới
        location_info = cg.get('location', {})
        profile_data = cg.get('profileData', {})
        preferences = profile_data.get('preferences', {})
        free_schedule = profile_data.get('free_schedule', {})
        ratings_reviews = profile_data.get('ratings_reviews', {})
        
        # Extract các giá trị cần thiết
        years_experience = profile_data.get('years_experience', 0)
        gender = cg.get('gender', None)
        caregiver_age = cg.get('age', None)
        cg_lat = location_info.get('latitude', location_info.get('lat'))
        cg_lon = location_info.get('longitude', location_info.get('lon'))
        service_radius = location_info.get('service_radius_km', 0)
        qualifications = cg.get('qualifications', [])
        
        # Tính distance (để dùng cho scoring, không filter)
        distance = haversine_km(
            req['location']['lat'], req['location']['lon'],
            cg_lat, cg_lon
        )
        
        # ========== FILTER 1: Certificate Groups ==========
        # (Giống như _score_candidate)
        service_package = req.get('service_package', {})
        qualification_req = service_package.get('qualification', {})
        certificate_groups = qualification_req.get('certificate_groups', [])
        
        if certificate_groups:
            def is_valid_qualification(qual):
                if qual.get('status') != 'APPROVED':
                    return False
                expiry_date = qual.get('expiryDate', qual.get('expiry_date'))
                if expiry_date:
                    from datetime import datetime, date
                    try:
                        if isinstance(expiry_date, str):
                            expiry = datetime.strptime(expiry_date, '%Y-%m-%d').date()
                        else:
                            expiry = expiry_date
                        if expiry < date.today():
                            return False
                    except (ValueError, TypeError):
                        return False
                return True
            
            valid_qualification_type_ids = set()
            for qual in qualifications:
                if is_valid_qualification(qual):
                    qual_type_id = qual.get('qualificationTypeId')
                    if qual_type_id:
                        valid_qualification_type_ids.add(qual_type_id)
            
            for group in certificate_groups:
                if not group:
                    continue
                
                has_qual_from_group = False
                for required_type_id in group:
                    if required_type_id in valid_qualification_type_ids:
                        has_qual_from_group = True
                        break
                
                if not has_qual_from_group:
                    return None
        
        # ========== FILTER 2: Distance - BỎ QUA (fallback) ==========
        # Không check distance trong fallback mode
        
        # ========== FILTER 3: Time Availability ==========
        req_time_slots = req.get('time_slots', {})
        if isinstance(req_time_slots, dict) and 'day' in req_time_slots:
            req_time_slots = [req_time_slots]
        
        if not self._check_time_availability(req_time_slots, free_schedule):
            return None
        
        # ========== FILTER 4: Gender Preference ==========
        gender_preference = req.get('gender_preference')
        if gender_preference and gender:
            if gender_preference.upper() != gender.upper():
                return None
        
        # ========== FILTER 5: Caregiver Age Range ==========
        caregiver_age_range = req.get('caregiver_age_range', None)
        if caregiver_age_range and caregiver_age is not None:
            min_age, max_age = caregiver_age_range
            if caregiver_age < min_age or caregiver_age > max_age:
                return None
        
        # ========== FILTER 6: Health Status Preference ==========
        # Logic: Hierarchical - Caregiver có thể nhận health status tốt hơn hoặc bằng mức họ chấp nhận
        preferred_health_status = preferences.get('preferred_health_status', None)
        elderly_health_status = req.get('health_status', None)
        
        if preferred_health_status and elderly_health_status:
            if isinstance(preferred_health_status, str):
                preferred_health_status = [preferred_health_status]
            
            preferred_normalized = [s.lower() for s in preferred_health_status]
            elderly_health_status_normalized = elderly_health_status.lower()
            
            # Health status hierarchy (tốt hơn = số cao hơn)
            health_hierarchy = {
                'weak': 1,
                'moderate': 2,
                'good': 3
            }
            
            elderly_level = health_hierarchy.get(elderly_health_status_normalized, 0)
            if elderly_level == 0:
                return None  # Unknown status
            
            # Check từng preferred status
            accepted = False
            for preferred in preferred_normalized:
                preferred_level = health_hierarchy.get(preferred, 0)
                if preferred_level == 0:
                    continue
                
                # Caregiver chấp nhận nếu elderly_status >= preferred_status
                if elderly_level >= preferred_level:
                    accepted = True
                    break
            
            if not accepted:
                return None
        
        # ========== FILTER 7: Elderly Age Preference ==========
        elderly_age_preference = preferences.get('elderly_age_preference', None)
        elderly_age = req.get('elderly_age', None)
        
        if elderly_age_preference and elderly_age is not None:
            min_age = elderly_age_preference.get('min_age')
            max_age = elderly_age_preference.get('max_age')
            if min_age is not None and max_age is not None:
                if elderly_age < min_age or elderly_age > max_age:
                    return None
        
        # ========== FILTER 8: Required Years Experience ==========
        required_years_experience = req.get('required_years_experience', None)
        if required_years_experience is not None:
            if years_experience < required_years_experience:
                return None
        
        # ========== FILTER 9: Overall Rating Range ==========
        required_rating_range = req.get('overall_rating_range', None)
        if required_rating_range is not None:
            caregiver_rating = ratings_reviews.get('overall_rating', 0.0)
            min_rating, max_rating = required_rating_range
            if caregiver_rating < min_rating or caregiver_rating > max_rating:
                return None
        
        # ========== SOFT SCORING (normalize về 0-1) ==========
        
        # 1. Credential score (bằng cấp + certificates)
        credential_score = self._calculate_credential_score(req, cg)
        
        # 2. Distance score - Logic mượt: exponential decay
        import math
        # Công thức: score = e^(-distance/scale)
        # Scale = 8: distance 8km → score ≈ 0.37, distance 16km → score ≈ 0.14
        distance_score = math.exp(-distance / 8.0)
        
        # 3. Rating score
        rating_score = self._calculate_rating_score(cg)
        
        # 4. Experience score - Improved: min 0.1 cho caregiver mới
        experience_score = min(1.0, max(0.1, years_experience / 10.0))
        
        # 5. Trust score (simplified: dựa trên rating + experience + reviews)
        trust_score = self._calculate_trust_score(cg)
        
        # ========== WEIGHTED SUM ==========
        
        total_score = (
            self.weights['credential'] * credential_score +
            self.weights['distance'] * distance_score +
            self.weights['rating'] * rating_score +
            self.weights['experience'] * experience_score +
            self.weights['trust'] * trust_score
        )
        
        return {
            'total_score': round(total_score, 3),
            'distance_km': round(distance, 2),
            'breakdown': {
                'credential': round(credential_score, 3),
                'distance': round(distance_score, 3),
                'rating': round(rating_score, 3),
                'experience': round(experience_score, 3),
                'trust': round(trust_score, 3)
            }
        }
    
    def _generate_failure_analysis(
        self,
        filter_failures: Dict[str, int],
        total_candidates: int,
        care_request: Dict
    ) -> Dict:
        """
        Generate failure analysis với statistics và suggestions.
        
        Args:
            filter_failures: Dict {filter_name: count}
            total_candidates: Tổng số caregivers được check
            care_request: Care request để generate suggestions
        
        Returns:
            Dict với failure analysis structure
        """
        if total_candidates == 0:
            return {
                "total_candidates": 0,
                "filter_statistics": {},
                "primary_reason": {
                    "filter": "no_candidates",
                    "message": "Không có người chăm sóc nào trong hệ thống",
                    "failed_count": 0,
                    "failed_percentage": 0
                },
                "suggestions": []
            }
        
        # Filter messages và suggestions
        filter_messages = {
            "certificate_groups": "Không tìm thấy người chăm sóc phù hợp cho gói dịch vụ bạn chọn. Vui lòng đổi qua 1 gói dịch vụ khác",
            "distance": "Không tìm thấy người chăm sóc nào ở gần bạn. Vui lòng mở rộng phạm vi tìm kiếm",
            "time_availability": "Không tìm thấy người chăm sóc nào có thời gian trống trong khung giờ bạn chọn. Vui lòng thử khung giờ khác",
            "gender_preference": "Không tìm thấy người chăm sóc nào phù hợp với yêu cầu giới tính. Vui lòng thay đổi yêu cầu này",
            "caregiver_age_range": "Không tìm thấy người chăm sóc nào trong độ tuổi bạn yêu cầu. Vui lòng điều chỉnh khoảng tuổi",
            "health_status_preference": "Không tìm thấy người chăm sóc nào chấp nhận tình trạng sức khỏe này. Vui lòng thử tìm kiếm với tình trạng khác",
            "elderly_age_preference": "Không tìm thấy người chăm sóc nào phù hợp với độ tuổi người cao tuổi. Vui lòng điều chỉnh yêu cầu",
            "required_years_experience": "Không tìm thấy người chăm sóc nào có đủ kinh nghiệm. Vui lòng giảm yêu cầu số năm kinh nghiệm",
            "overall_rating_range": "Không tìm thấy người chăm sóc nào có đánh giá trong khoảng bạn yêu cầu. Vui lòng điều chỉnh khoảng đánh giá"
        }
        
        filter_suggestions = {
            "certificate_groups": "Thử chọn gói dịch vụ khác",
            "distance": "Thử thay đổi địa điểm trong hồ sơ của người già",
            "time_availability": "Thử chọn khung giờ khác hoặc ngày khác",
            "gender_preference": "Thay đổi yêu cầu về giới tính để có nhiều lựa chọn hơn trong hồ sơ người già",
            "caregiver_age_range": "Mở rộng khoảng tuổi của người chăm sóc trong hồ sơ người già",
            "health_status_preference": "Thử thay đổi với tình trạng sức khỏe khác trong hồ sơ người già",
            "elderly_age_preference": "Điều chỉnh về độ tuổi người cao tuổi trong hồ sơ người già",
            "required_years_experience": "Giảm số năm kinh nghiệm yêu cầu trong hồ sơ người già",
            "overall_rating_range": "Mở rộng khoảng đánh giá yêu cầu trong hồ sơ người già"
        }
        
        # Calculate statistics
        filter_statistics = {}
        for filter_name, failed_count in filter_failures.items():
            percentage = (failed_count / total_candidates) * 100 if total_candidates > 0 else 0
            filter_statistics[filter_name] = {
                "failed": failed_count,
                "percentage": round(percentage, 2)
            }
        
        # Find primary reason (filter with highest failure count)
        primary_reason = None
        if filter_failures:
            primary_filter = max(filter_failures.items(), key=lambda x: x[1])
            primary_reason = {
                "filter": primary_filter[0],
                "message": filter_messages.get(primary_filter[0], "Không tìm thấy người chăm sóc phù hợp"),
                "failed_count": primary_filter[1],
                "failed_percentage": round((primary_filter[1] / total_candidates) * 100, 2) if total_candidates > 0 else 0
            }
        else:
            # Nếu không có filter failures nhưng vẫn không có results
            # Có thể là do không có caregivers nào pass distance filter
            primary_reason = {
                "filter": "unknown",
                "message": "Không tìm thấy người chăm sóc phù hợp với yêu cầu của bạn",
                "failed_count": total_candidates,
                "failed_percentage": 100.0
            }
        
        # Generate suggestions (top 3 filters by failure count)
        suggestions = []
        sorted_filters = sorted(filter_failures.items(), key=lambda x: x[1], reverse=True)[:3]
        for filter_name, _ in sorted_filters:
            suggestion = filter_suggestions.get(filter_name)
            if suggestion:
                suggestions.append({
                    "filter": filter_name,
                    "suggestion": suggestion
                })
        
        return {
            "total_candidates": total_candidates,
            "filter_statistics": filter_statistics,
            "primary_reason": primary_reason,
            "suggestions": suggestions
        }