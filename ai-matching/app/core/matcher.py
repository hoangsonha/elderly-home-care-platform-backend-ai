"""
Phase 1: Rule-based Matching Engine
Weighted scoring algorithm với hard filters và soft preferences
"""

from typing import List, Dict, Optional
import numpy as np
from app.utils import haversine_km, has_time_overlap
from app.algorithms.semantic_matcher import semantic_matcher, normalize_request_skills, normalize_caregiver_skills


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
    
    Hard Filters (11 filters):
        1. Care Level: caregiver.max_care_level >= request.care_level
        2. Degree: Level 3+ bắt buộc có bằng cấp
        3. Distance: distance <= caregiver.service_radius_km
        4. Time: 100% overlap với available slots
        5. Gender: match gender preference (nếu có)
        6. Caregiver Age: caregiver.age nằm trong request.caregiver_age_range (nếu có)
        7. Health Status: request.health_status nằm trong caregiver.preferred_health_status
        8. Elderly Age: request.elderly_age nằm trong caregiver.elderly_age_preference (nếu có)
        9. Required Years Experience: caregiver.years_experience >= request.required_years_experience (nếu có)
        10. Overall Rating Range: caregiver.overall_rating nằm trong request.overall_rating_range (nếu có)
        11. Required Skills: 100% required_skills phải có trong caregiver skills
    
    Soft Scoring Features (7 features, weights sum = 1.0):
        - Credential (30%): Bằng cấp, care level
        - Skills (25%): Priority skills matching
        - Distance (15%): Khoảng cách địa lý
        - Rating (12%): Đánh giá từ khách hàng
        - Experience (8%): Số năm kinh nghiệm
        - Price (8%): Gần budget
        - Trust (2%): Trust score
    """
    
    def __init__(self):
        # Weights for scoring features (sum = 1.0)
        self.weights = {
            'credential': 0.30,   # Bằng cấp, care level (+5%)
            'skills': 0.25,       # Priority skills matching (+5%)
            'distance': 0.15,     # Gần = thuận tiện (+3%)
            'rating': 0.12,       # Chất lượng đã được verify (+2%)
            'experience': 0.08,   # Kinh nghiệm (giữ nguyên)
            'price': 0.08,        # Gần budget (giữ nguyên)
            'trust': 0.02         # Độ tin cậy (-3%)
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
        # Normalize Vietnamese skills by removing diacritics for matching
        care_request = normalize_request_skills(care_request)
        
        # Normalize caregivers skills by removing diacritics
        caregivers_normalized = []
        for cg in caregivers:
            cg_normalized = normalize_caregiver_skills(cg.copy())
            caregivers_normalized.append(cg_normalized)
        
        # BƯỚC 1: Hard Filter với service_radius_km của từng caregiver
        pass_list = []
        fail_list = []
        
        for cg in caregivers_normalized:
            distance = haversine_km(
                care_request['location']['lat'], care_request['location']['lon'],
                cg.get('location', {}).get('lat', cg.get('lat')),
                cg.get('location', {}).get('lon', cg.get('lon'))
            )
            
            service_radius = cg.get('location', {}).get('service_radius_km', cg.get('service_radius_km', 0))
            
            if distance <= service_radius:
                pass_list.append(cg)
            else:
                cg['distance'] = distance
                fail_list.append(cg)
        
        # BƯỚC 2: Sắp xếp fail_list theo distance (gần nhất trước)
        fail_list.sort(key=lambda x: x['distance'])
        
        # BƯỚC 3: Thử pass_list trước
        results = []
        for cg in pass_list:
            score_result = self._score_candidate(care_request, cg)
            
            if score_result is not None:
                results.append({
                    'caregiver': cg,
                    'total_score': score_result['total_score'],
                    'breakdown': score_result['breakdown'],
                    'distance_km': score_result['distance_km']
                })
        
        if results:
            # Sort by total_score descending
            results.sort(key=lambda x: x['total_score'], reverse=True)
            return results[:top_n]
        
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
            return fallback_results[:top_n]
        
        # Nếu không tìm thấy caregiver nào
        return []
    
    def calculate_max_care_level_dynamic(self, cg: Dict) -> int:
        """
        Tính max_care_level động dựa trên credentials hiện tại
        
        Logic:
            - Chỉ tính credential đã verified
            - Check expiry date cho certificates
            - Lấy max level từ applicable_levels
        """
        credentials = cg.get('credentials', [])
        max_level = 0
        
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
                max_level = max(max_level, max(applicable_levels))
        
        return max_level
    
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
    ) -> Optional[Dict]:
        """
        Score a single caregiver against a care request.
        
        Args:
            req: Care request dict
            cg: Caregiver dict
        
        Returns:
            Dict với total_score, breakdown, distance_km
            None nếu không đủ điều kiện (hard filters)
        """
        
        # ========== HARD FILTERS (bắt buộc) ==========
        
        # Extract nested data
        professional_info = cg.get('professional_info', cg)  # Fallback to root level
        personal_info = cg.get('personal_info', cg)
        location_info = cg.get('location', cg)
        availability_info = cg.get('availability', {})
        
        # Tính max_care_level động thay vì dùng giá trị cố định
        max_care_level = self.calculate_max_care_level_dynamic(cg)
        years_experience = professional_info.get('years_experience', cg.get('years_experience'))
        hourly_rate = professional_info.get('price_per_hour', professional_info.get('hourly_rate', cg.get('hourly_rate', cg.get('price_per_hour'))))
        gender = personal_info.get('gender', cg.get('gender'))
        cg_lat = location_info.get('lat', cg.get('lat'))
        cg_lon = location_info.get('lon', cg.get('lon'))
        schedule = availability_info.get('schedule', cg.get('availability', {}))
        
        # Convert schedule array to dict if needed
        if isinstance(schedule, list):
            schedule = convert_schedule_to_dict(schedule)
        
        # Filter 1: Care level match
        if max_care_level < req['care_level']:
            return None
        
        # Filter 2: Degree requirement cho level 3+
        # Level 3-4 BẮT BUỘC phải có bằng cấp
        credentials = cg.get('credentials', [])
        
        # Helper: Check credential còn hạn
        def is_valid_credential(cred):
            if cred.get('status') != 'verified':
                return False
            # Certificate phải check expiry_date
            if cred.get('type') == 'certificate' and cred.get('expiry_date'):
                from datetime import datetime
                expiry = datetime.fromisoformat(cred['expiry_date'].replace('Z', '+00:00'))
                if expiry < datetime.now(expiry.tzinfo):
                    return False  # Hết hạn
            return True
        
        valid_credentials = [c for c in credentials if is_valid_credential(c)]
        has_degree = any(c.get('type') == 'degree' for c in valid_credentials)
        
        if req['care_level'] >= 3 and not has_degree:
            return None
        
        # Filter 3: Distance - Logic đúng: caregiver quyết định bán kính phục vụ
        distance = haversine_km(
            req['location']['lat'], req['location']['lon'],
            cg_lat, cg_lon
        )
        service_radius = location_info.get('service_radius_km', cg.get('service_radius_km', 0))
        
        # Caregiver chỉ nhận được request nếu trong bán kính phục vụ của họ
        if distance > service_radius:
            return None
        
        # Filter 4: Time availability
        # Tất cả time slots yêu cầu phải nằm trong availability của caregiver
        if not has_time_overlap(req['time_slots'], schedule):
            return None
        
        # Filter 5: Gender preference (nếu có)
        if req.get('gender_preference') and req['gender_preference'] != gender:
            return None
        
        # Filter 6: Caregiver age range preference (optional)
        # Request có thể yêu cầu độ tuổi của caregiver (ví dụ: 25-50 tuổi)
        caregiver_age_range = req.get('caregiver_age_range', None)
        caregiver_age = personal_info.get('age', cg.get('age', None))
        
        if caregiver_age_range and caregiver_age:
            min_age, max_age = caregiver_age_range
            if caregiver_age < min_age or caregiver_age > max_age:
                return None
        
        # Filter 7: Health status preference
        # Caregiver chỉ nhận nếu health_status của người già nằm trong preferred_health_status
        preferences = cg.get('preferences', {})
        preferred_health_status = preferences.get('preferred_health_status', [])
        elderly_health_status = req.get('health_status', None)
        
        if preferred_health_status and elderly_health_status:
            if elderly_health_status not in preferred_health_status:
                return None
        
        # Filter 8: Elderly age preference
        # Caregiver chỉ nhận nếu elderly_age của người già nằm trong elderly_age_preference
        elderly_age_preference = preferences.get('elderly_age_preference', None)
        elderly_age = req.get('elderly_age', None)
        
        if elderly_age_preference and elderly_age:
            min_age, max_age = elderly_age_preference
            if elderly_age < min_age or elderly_age > max_age:
                return None
        
        # Filter 9: Required Years Experience (Hard Filter)
        # Caregiver PHẢI có đủ số năm kinh nghiệm yêu cầu
        required_years_experience = req.get('required_years_experience', None)
        
        if required_years_experience is not None:
            if years_experience < required_years_experience:
                return None  # Không đủ kinh nghiệm
        
        # Filter 10: Overall Rating Range (Hard Filter)
        # Caregiver PHẢI có đánh giá nằm trong khoảng yêu cầu
        required_rating_range = req.get('overall_rating_range', None)
        
        if required_rating_range is not None:
            # Lấy overall_rating của caregiver
            ratings_reviews = cg.get('ratings_reviews', cg)
            caregiver_rating = ratings_reviews.get('overall_rating', cg.get('rating', 0.0))
            
            min_rating, max_rating = required_rating_range
            if caregiver_rating < min_rating or caregiver_rating > max_rating:
                return None  # Đánh giá không nằm trong khoảng yêu cầu
        
        # Filter 11: Required Skills (Hard Filter)
        # Caregiver PHẢI có 100% required_skills
        req_skills = req.get('skills', {})
        required_skills = req_skills.get('required_skills', [])
        
        if required_skills:
            # Get caregiver's skill names (extract from skill objects)
            cg_skills = cg.get('skills', [])
            cg_skill_names = set()
            for skill in cg_skills:
                if isinstance(skill, dict):
                    cg_skill_names.add(skill.get('name', ''))
                else:
                    cg_skill_names.add(skill)
            
            # Check if ALL required skills are present using semantic matching (PhoBERT)
            missing_skills = []
            for req_skill in required_skills:
                best_match_score = 0.0
                for cg_skill in cg_skill_names:
                    similarity = semantic_matcher.calculate_similarity(req_skill, cg_skill)
                    best_match_score = max(best_match_score, similarity)
                
                # Threshold for PhoBERT v2 semantic matching (0.8 = 80% similarity for strict matching)
                if best_match_score < 0.8:
                    missing_skills.append(req_skill)
            
            if missing_skills:
                return None  # Không đủ required skills
        
        # ========== SOFT SCORING (normalize về 0-1) ==========
        
        # 1. Credential score (bằng cấp + level)
        credential_score = self._calculate_credential_score(req, cg)
        
        # 2. Skills score (priority skills matching)
        skills_score = self._calculate_skills_score(req, cg)
        
        # 3. Distance score - Logic mượt: exponential decay
        import math
        # Công thức: score = e^(-distance/scale)
        # Scale = 8: distance 8km → score ≈ 0.37, distance 16km → score ≈ 0.14
        distance_score = math.exp(-distance / 8.0)
        
        # 4. Time score - Đã được xử lý ở hard filter
        # Không cần tính điểm vì đã pass hard filter = có sẵn 100% time slots
        
        # 5. Rating score
        rating_score = self._calculate_rating_score(cg)
        
        # 6. Experience score - Improved: min 0.1 cho caregiver mới
        experience_score = min(1.0, max(0.1, years_experience / 10.0))
        
        # 7. Price score (gần budget = tốt)
        price_score = self._calculate_price_score(req, cg, hourly_rate)
        
        # 8. Trust score (simplified: dựa trên rating + experience + reviews)
        trust_score = self._calculate_trust_score(cg)
        
        # ========== WEIGHTED SUM ==========
        
        total_score = (
            self.weights['credential'] * credential_score +
            self.weights['skills'] * skills_score +
            self.weights['distance'] * distance_score +
            self.weights['rating'] * rating_score +
            self.weights['experience'] * experience_score +
            self.weights['price'] * price_score +
            self.weights['trust'] * trust_score
        )
        
        return {
            'total_score': round(total_score, 3),
            'distance_km': round(distance, 2),
            'breakdown': {
                'credential': round(credential_score, 3),
                'skills': round(skills_score, 3),
                'distance': round(distance_score, 3),
                'rating': round(rating_score, 3),
                'experience': round(experience_score, 3),
                'price': round(price_score, 3),
                'trust': round(trust_score, 3)
            }
        }
    
    def _calculate_skills_score(self, req: Dict, cg: Dict) -> float:
        """
        Tính điểm skills dựa trên priority_skills matching.
        
        Logic:
            - Required skills đã được check ở hard filter (100% match)
            - Priority skills: tính % match
            - Bonus nếu skill có credential mapping (chất lượng cao hơn)
        
        Formula:
            base_score = matched_priority_skills / total_priority_skills
            bonus = (skills_with_credentials / matched_priority_skills) * 0.2
            final_score = min(1.0, base_score + bonus)
        """
        req_skills = req.get('skills', {})
        priority_skills = req_skills.get('priority_skills', [])
        
        # Nếu không có priority skills, trả về 1.0 (không ảnh hưởng)
        if not priority_skills:
            return 1.0
        
        # Get caregiver's skills dùng map để khi tìm ra skill giống thì mình k cần phải truy xuất lại lấy thông tin cridential để cộng điểm
        cg_skills = cg.get('skills', [])
        cg_skill_map = {}  # skill_name -> skill_obj
        for skill in cg_skills:
            if isinstance(skill, dict):
                skill_name = skill.get('name', '')
                cg_skill_map[skill_name] = skill
            else:
                cg_skill_map[skill] = {'name': skill}
        
        # Count matched priority skills
        matched_count = 0
        skills_with_credentials = 0
        
        for priority_skill in priority_skills:
            # Use semantic matching (PhoBERT) for priority skills
            best_match_score = 0.0
            best_match_skill = None
            
            for cg_skill_name in cg_skill_map.keys():
                similarity = semantic_matcher.calculate_similarity(priority_skill, cg_skill_name)
                if similarity > best_match_score:
                    best_match_score = similarity
                    best_match_skill = cg_skill_name
            
            # Threshold for PhoBERT v2 semantic matching (0.8 = 80% similarity for strict matching)
            if best_match_score >= 0.8:
                    matched_count += 1
                    # Check if skill has credential mapping (higher quality)
                    skill_obj = cg_skill_map[best_match_skill]
                    if isinstance(skill_obj, dict) and skill_obj.get('credential_id'):
                        skills_with_credentials += 1
        
        # Base score: % match
        base_score = matched_count / len(priority_skills) if priority_skills else 1.0
        
        # Bonus for skills with credentials (max 0.2 bonus)
        bonus = 0.0
        if matched_count > 0:
            credential_ratio = skills_with_credentials / matched_count
            bonus = credential_ratio * 0.2
        
        # Final score
        return min(1.0, base_score + bonus)
    
    def _calculate_credential_score(self, req: Dict, cg: Dict) -> float:
        """
        Tính điểm credential dựa trên degree và certificates.
        
        Logic:
            - Degree: Level 1 = 1 điểm, Level 2 = 2 điểm, Level 3 = 3 điểm, Level 4 = 4 điểm
            - Certificate: Mỗi certificate đạt yêu cầu = 0.5 điểm (max 12 certs = 6 điểm)
            - Max total: 4 + 6 = 10 điểm
            - Normalize về 0-1: score / 10.0
        """
        credentials = cg.get('credentials', [])
        required_level = req['care_level']
        score = 0.0
        MAX_CREDENTIAL_SCORE = 10.0  # Max: degree 4 + 12 certs
        
        # Lọc credentials hợp lệ
        def is_valid_credential(cred):
            if cred.get('status') != 'verified':
                return False
            if cred.get('type') == 'certificate' and cred.get('expiry_date'):
                from datetime import datetime
                try:
                    expiry = datetime.fromisoformat(cred['expiry_date'].replace('Z', '+00:00'))
                    if expiry < datetime.now(expiry.tzinfo):
                        return False
                except ValueError:
                    return False
            return True
        
        valid_credentials = [c for c in credentials if is_valid_credential(c)]
        
        # 1. Degree bonus: Level 1 = 1 điểm, Level 2 = 2 điểm, Level 3 = 3 điểm, Level 4 = 4 điểm
        degrees = [c for c in valid_credentials if c.get('type') == 'degree']
        if degrees:
            # Lấy degree có level cao nhất
            max_degree_level = 0
            for degree in degrees:
                applicable_levels = degree.get('applicable_levels', [])
                if applicable_levels:
                    max_degree_level = max(max_degree_level, max(applicable_levels))
            
            # Degree bonus theo level (1-4 điểm)
            degree_bonus = max_degree_level
            score += degree_bonus
        
        # 2. Certificate bonus: Mỗi certificate đạt yêu cầu = 0.5 điểm
        certificates = [c for c in valid_credentials if c.get('type') == 'certificate']
        
        cert_count = 0
        for cert in certificates:
            applicable_levels = cert.get('applicable_levels', [])
            if applicable_levels:
                # Chỉ cần có ít nhất 1 level đạt yêu cầu là được cộng điểm
                if any(level >= required_level for level in applicable_levels):
                    score += 0.5  # Mỗi certificate đạt yêu cầu = +0.5 điểm
                    cert_count += 1
                    # Giới hạn max 12 certificates (6 điểm)
                    if cert_count >= 12:
                        break
        
        # Normalize về 0-1
        return min(1.0, score / MAX_CREDENTIAL_SCORE)
    
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
    
    def _calculate_price_score(self, req: Dict, cg: Dict, hourly_rate: float = None) -> float:
        """
        Tính điểm price - normalize về 0-1.
        
        Logic:
            - Rẻ hơn budget: Điểm cao hơn (tuyến tính)
            - Đúng budget: 1.0
            - Đắt hơn budget: Penalty (giảm điểm)
            - Đắt gấp đôi hoặc hơn: 0.0
            - Budget null: Trả về 1.0 (không ảnh hưởng điểm)
        """
        budget = req.get('budget_per_hour')
        
        # Nếu budget_per_hour là null hoặc không có, trả về 1.0
        if budget is None:
            return 1.0
        if hourly_rate is None:
            professional_info = cg.get('professional_info', cg)
            hourly_rate = professional_info.get('price_per_hour', professional_info.get('hourly_rate', cg.get('price_per_hour', cg.get('hourly_rate', 0))))
        
        if hourly_rate <= budget:
            # Giá <= budget: điểm từ 0.8 - 1.0
            # Giá = 50% budget → 1.0
            # Giá = budget → 0.9
            if hourly_rate < budget * 0.5:
                return 1.0  # Rất rẻ
            else:
                # Tuyến tính từ 1.0 (50% budget) xuống 0.9 (100% budget)
                ratio = hourly_rate / budget
                return 1.0 - (ratio - 0.5) * 0.2  # 0.9 - 1.0
        else:
            # Giá > budget: penalty
            # Giá gấp đôi budget → 0.0
            excess_ratio = (hourly_rate - budget) / budget
            return max(0.0, 1.0 - excess_ratio)
    
    def _calculate_trust_score(self, cg: Dict) -> float:
        """
        Tính trust score dựa trên booking history và verification
        
        Factors:
            - Completion rate (40%): Tỷ lệ hoàn thành booking
            - Seeker cancel rate (30%): Tỷ lệ hủy bởi seeker (càng thấp càng tốt)
            - Total bookings (20%): Số lượng booking (kinh nghiệm thực tế)
            - Verification (10%): Xác minh danh tính
        """
        booking_history = cg.get('booking_history', {})
        verification = cg.get('verification', {})
        
        # 1. Completion rate component (40%)
        completion_rate = booking_history.get('completion_rate', 0.0)
        completion_component = completion_rate
        
        # 2. Seeker cancel rate component (30%) - càng thấp càng tốt
        seeker_cancel_rate = booking_history.get('seeker_cancel_rate', 0.0)
        # Invert: 0% cancel = 1.0, 15% cancel = 0.0
        cancel_component = max(0.0, 1.0 - (seeker_cancel_rate * 6.67))
        
        # 3. Total bookings component (20%)
        total_bookings = booking_history.get('total_bookings', 0)
        if total_bookings >= 100:
            bookings_component = 1.0
        elif total_bookings >= 50:
            bookings_component = 0.8
        elif total_bookings >= 20:
            bookings_component = 0.6
        elif total_bookings >= 10:
            bookings_component = 0.4
        else:
            bookings_component = 0.2
        
        # 4. Verification component (10%)
        identity_verified = verification.get('identity_verified', False)
        verification_component = 1.0 if identity_verified else 0.5
        
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
        Score a fallback caregiver (bỏ qua Filter 3 - Distance).
        
        Args:
            req: Care request dict
            cg: Caregiver dict
        
        Returns:
            Dict với total_score, breakdown, distance_km
            None nếu không đủ điều kiện (hard filters 1,2,4-11)
        """
        
        # ========== HARD FILTERS (bỏ qua Filter 3 - Distance) ==========
        
        # Extract nested data
        professional_info = cg.get('professional_info', cg)  # Fallback to root level
        personal_info = cg.get('personal_info', cg)
        location_info = cg.get('location', cg)
        availability_info = cg.get('availability', {})
        
        # Tính max_care_level động thay vì dùng giá trị cố định
        max_care_level = self.calculate_max_care_level_dynamic(cg)
        years_experience = professional_info.get('years_experience', cg.get('years_experience'))
        hourly_rate = professional_info.get('price_per_hour', professional_info.get('hourly_rate', cg.get('hourly_rate', cg.get('price_per_hour'))))
        gender = personal_info.get('gender', cg.get('gender'))
        cg_lat = location_info.get('lat', cg.get('lat'))
        cg_lon = location_info.get('lon', cg.get('lon'))
        schedule = availability_info.get('schedule', cg.get('availability', {}))
        
        # Convert schedule array to dict if needed
        if isinstance(schedule, list):
            schedule = convert_schedule_to_dict(schedule)
        
        # Filter 1: Care level match - BỎ QUA (đã pass ở round 1)
        # Filter 2: Degree requirement - BỎ QUA (đã pass ở round 1)
        # Filter 3: Distance - BỎ QUA (đã fail ở round 1)
        distance = haversine_km(
            req['location']['lat'], req['location']['lon'],
            cg_lat, cg_lon
        )
        
        # Filter 4: Time availability
        # Tất cả time slots yêu cầu phải nằm trong availability của caregiver
        if not has_time_overlap(req['time_slots'], schedule):
            return None
        
        # Filter 5: Gender preference (nếu có)
        if req.get('gender_preference') and req['gender_preference'] != gender:
            return None
        
        # Filter 6: Caregiver age range preference (optional)
        # Request có thể yêu cầu độ tuổi của caregiver (ví dụ: 25-50 tuổi)
        caregiver_age_range = req.get('caregiver_age_range', None)
        caregiver_age = personal_info.get('age', cg.get('age', None))
        
        if caregiver_age_range and caregiver_age:
            min_age, max_age = caregiver_age_range
            if caregiver_age < min_age or caregiver_age > max_age:
                return None
        
        # Filter 7: Health status preference
        # Caregiver chỉ nhận nếu health_status của người già nằm trong preferred_health_status
        preferences = cg.get('preferences', {})
        preferred_health_status = preferences.get('preferred_health_status', [])
        elderly_health_status = req.get('health_status', None)
        
        if preferred_health_status and elderly_health_status:
            if elderly_health_status not in preferred_health_status:
                return None
        
        # Filter 8: Elderly age preference
        # Caregiver chỉ nhận nếu elderly_age của người già nằm trong elderly_age_preference
        elderly_age_preference = preferences.get('elderly_age_preference', None)
        elderly_age = req.get('elderly_age', None)
        
        if elderly_age_preference and elderly_age:
            min_age, max_age = elderly_age_preference
            if elderly_age < min_age or elderly_age > max_age:
                return None
        
        # Filter 9: Required Years Experience (Hard Filter)
        # Caregiver PHẢI có đủ số năm kinh nghiệm yêu cầu
        required_years_experience = req.get('required_years_experience', None)
        
        if required_years_experience is not None:
            if years_experience < required_years_experience:
                return None  # Không đủ kinh nghiệm
        
        # Filter 10: Overall Rating Range (Hard Filter)
        # Caregiver PHẢI có đánh giá nằm trong khoảng yêu cầu
        required_rating_range = req.get('overall_rating_range', None)
        
        if required_rating_range is not None:
            # Lấy overall_rating của caregiver
            ratings_reviews = cg.get('ratings_reviews', cg)
            caregiver_rating = ratings_reviews.get('overall_rating', cg.get('rating', 0.0))
            
            min_rating, max_rating = required_rating_range
            if caregiver_rating < min_rating or caregiver_rating > max_rating:
                return None  # Đánh giá không nằm trong khoảng yêu cầu
        
        # Filter 11: Required Skills (Hard Filter)
        # Caregiver PHẢI có 100% required_skills
        req_skills = req.get('skills', {})
        required_skills = req_skills.get('required_skills', [])
        
        if required_skills:
            # Get caregiver's skill names (extract from skill objects)
            cg_skills = cg.get('skills', [])
            cg_skill_names = set()
            for skill in cg_skills:
                if isinstance(skill, dict):
                    cg_skill_names.add(skill.get('name', ''))
                else:
                    cg_skill_names.add(skill)
            
            # Check if ALL required skills are present using semantic matching (PhoBERT)
            missing_skills = []
            for req_skill in required_skills:
                best_match_score = 0.0
                for cg_skill in cg_skill_names:
                    similarity = semantic_matcher.calculate_similarity(req_skill, cg_skill)
                    best_match_score = max(best_match_score, similarity)
                
                # Threshold for PhoBERT v2 semantic matching (0.8 = 80% similarity for strict matching)
                if best_match_score < 0.8:
                    missing_skills.append(req_skill)
            
            if missing_skills:
                return None  # Không đủ required skills
        
        # ========== SOFT SCORING (normalize về 0-1) ==========
        
        # 1. Credential score (bằng cấp + level)
        credential_score = self._calculate_credential_score(req, cg)
        
        # 2. Skills score (priority skills matching)
        skills_score = self._calculate_skills_score(req, cg)
        
        # 3. Distance score - Logic mượt: exponential decay
        import math
        # Công thức: score = e^(-distance/scale)
        # Scale = 8: distance 8km → score ≈ 0.37, distance 16km → score ≈ 0.14
        distance_score = math.exp(-distance / 8.0)
        
        # 4. Time score - Đã được xử lý ở hard filter
        # Không cần tính điểm vì đã pass hard filter = có sẵn 100% time slots
        
        # 5. Rating score
        rating_score = self._calculate_rating_score(cg)
        
        # 6. Experience score - Improved: min 0.1 cho caregiver mới
        experience_score = min(1.0, max(0.1, years_experience / 10.0))
        
        # 7. Price score (gần budget = tốt)
        price_score = self._calculate_price_score(req, cg, hourly_rate)
        
        # 8. Trust score (simplified: dựa trên rating + experience + reviews)
        trust_score = self._calculate_trust_score(cg)
        
        # ========== WEIGHTED SUM ==========
        
        total_score = (
            self.weights['credential'] * credential_score +
            self.weights['skills'] * skills_score +
            self.weights['distance'] * distance_score +
            self.weights['rating'] * rating_score +
            self.weights['experience'] * experience_score +
            self.weights['price'] * price_score +
            self.weights['trust'] * trust_score
        )
        
        return {
            'total_score': round(total_score, 3),
            'distance_km': round(distance, 2),
            'breakdown': {
                'credential': round(credential_score, 3),
                'skills': round(skills_score, 3),
                'distance': round(distance_score, 3),
                'rating': round(rating_score, 3),
                'experience': round(experience_score, 3),
                'price': round(price_score, 3),
                'trust': round(trust_score, 3)
            }
        }


# Vietnamese to English Skills Mapping
VIETNAMESE_TO_ENGLISH_SKILLS = {
    # Medical skills
    "tiêm insulin": "injection",
    "tiêm thuốc": "injection", 
    "tiêm": "injection",
    "chăm sóc vết thương": "wound_care",
    "chăm sóc vết thương hở": "wound_care",
    "quản lý thuốc": "medication_management",
    "quản lý thuốc men": "medication_management",
    "đo dấu hiệu sinh tồn": "vital_signs_monitoring",
    "đo mạch": "vital_signs_monitoring",
    "chăm sóc catheter": "catheter_care",
    "cho ăn qua ống": "tube_feeding",
    "hỗ trợ oxy": "oxygen_therapy",
    "đo đường huyết": "blood_sugar_monitoring",
    "kiểm tra đường huyết": "blood_sugar_monitoring",
    "vật lý trị liệu": "physical_therapy",
    "chăm sóc khí quản": "tracheostomy_care",
    "chăm sóc máy thở": "ventilator_care",
    "truyền dịch": "iv_therapy",
    "truyền nước": "iv_therapy",
    
    # Basic care skills
    "đo huyết áp": "blood_pressure_monitoring",
    "kiểm tra huyết áp": "blood_pressure_monitoring",
    "hỗ trợ vệ sinh": "personal_hygiene",
    "vệ sinh cá nhân": "personal_hygiene",
    "tắm rửa": "bathing_assistance",
    "tắm": "bathing_assistance",
    "chuẩn bị bữa ăn dinh dưỡng": "meal_preparation",
    "nấu ăn": "meal_preparation",
    "chuẩn bị thức ăn": "meal_preparation",
    "hỗ trợ đi lại": "mobility_assistance",
    "hỗ trợ di chuyển": "mobility_assistance",
    "thay quần áo": "dressing_assistance",
    "hỗ trợ ăn uống": "feeding_assistance",
    "cho ăn": "feeding_assistance",
    "vận động nhẹ nhàng": "gentle_exercise",
    "tập thể dục nhẹ": "gentle_exercise",
    "đồng hành": "companionship",
    "trò chuyện": "companionship",
    "giám sát an toàn": "safety_monitoring",
    "theo dõi an toàn": "safety_monitoring",
    "nhắc nhở uống thuốc": "medication_reminders",
    "nhắc thuốc": "medication_reminders",
    "chăm sóc da": "skin_care",
    "phòng ngừa loét": "pressure_sore_prevention",
    "theo dõi sức khỏe": "health_monitoring",
    "kiểm tra sức khỏe": "health_monitoring",
    
    # Special conditions
    "đái tháo đường": "diabetes_care",
    "tiểu đường": "diabetes_care",
    "sa sút trí tuệ": "dementia_care",
    "alzheimer": "alzheimer_care",
    "parkinson": "parkinson_care",
    "đột quỵ phục hồi": "stroke_recovery",
    "phục hồi sau đột quỵ": "stroke_recovery",
    "cao huyết áp": "hypertension_management",
    "tăng huyết áp": "hypertension_management",
    "ung thư": "cancer_care",
    "chăm sóc cuối đời": "hospice_care",
    "hỗ trợ tâm lý": "mental_health_support",
    "tự kỷ": "autism_care",
    "chăm sóc tự kỷ": "autism_care",
}
