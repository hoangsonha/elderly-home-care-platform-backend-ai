# -*- coding: utf-8 -*-
"""
Debug script để test matcher với request và caregivers từ JSON
"""

import sys
import codecs
import json
from pathlib import Path
from typing import List, Dict

# Add the backend directory to the Python path
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

# Fix for UnicodeEncodeError on Windows
if sys.stdout.encoding != 'utf-8':
    sys.stdout = codecs.getwriter('utf-8')(sys.stdout.detach())

from app.core.matcher import RuleBasedMatcher
from app.algorithms.semantic_matcher import semantic_matcher, normalize_request_skills, normalize_caregiver_skills
from app.utils.distance import haversine_km
from app.utils.time_utils import has_time_overlap

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

def main():
    print("🔍 DEBUG MATCHER WITH JSON DATA")
    print("=" * 50)
    
    # Initialize matcher
    matcher = RuleBasedMatcher()
    
    # CUSTOM REQUEST - EXACT FROM MOBILE
    CUSTOM_REQUEST_EXAMPLE = {
        "attitude": [],
        "budget_per_hour": 100000,
        "care_level": 3,
        "caregiver_age_range": [10, 78],
        "elderly_age": 78,
        "gender_preference": None,
        "health_status": "weak",
        "location": {
            "address": "Quận 7, TP.HCM",
            "lat": 10.735,
            "lon": 106.72
        },
        "overall_rating_range": None,
        "personality": [],
        "required_years_experience": None,
        "seeker_name": "Người dùng",
        "skills": {
            "priority_skills": [],
            "required_skills": ["nau an"]
        },
        "time_slots": [
            {"day": "monday", "end": "12:00", "start": "08:00"},
            {"day": "wednesday", "end": "12:00", "start": "08:00"},
            {"day": "friday", "end": "12:00", "start": "08:00"}
        ]
    }
    
    print(f"📋 REQUEST CONFIGURATION:")
    print(f"   Care Level: {CUSTOM_REQUEST_EXAMPLE['care_level']}")
    print(f"   Required Skills: {CUSTOM_REQUEST_EXAMPLE['skills']['required_skills']}")
    print(f"   Location: {CUSTOM_REQUEST_EXAMPLE['location']['address']}")
    print(f"   Time Slots: {len(CUSTOM_REQUEST_EXAMPLE['time_slots'])} slots")
    
    # Load caregivers from JSON
    try:
        with open('../caregivers.json', 'r', encoding='utf-8') as f:
            caregivers_data = json.load(f)
        print(f"\n👥 CAREGIVERS DATA:")
        print(f"   Total caregivers: {len(caregivers_data)}")
        for cg in caregivers_data:
            print(f"   - {cg['id']}: {cg['personal_info']['full_name']} ({cg['personal_info']['age']} tuổi)")
    except FileNotFoundError:
        print("❌ File caregivers.json not found!")
        return
    except Exception as e:
        print(f"❌ Error loading caregivers.json: {e}")
        return
    
    # Normalize skills
    req_normalized = normalize_request_skills(CUSTOM_REQUEST_EXAMPLE)
    caregivers_normalized = []
    for cg in caregivers_data:
        cg_normalized = normalize_caregiver_skills(cg.copy())
        caregivers_normalized.append(cg_normalized)
    
    print(f"\n🔧 SKILLS NORMALIZATION:")
    print(f"   Request skills: {req_normalized['skills']['required_skills']}")
    for cg in caregivers_normalized:
        cg_skills = [skill.get('name', skill) if isinstance(skill, dict) else skill for skill in cg.get('skills', [])]
        print(f"   {cg['id']} skills: {cg_skills}")
    
    # Initialize tracking
    failed_caregivers = []
    filter_results = {
        'CARE LEVEL': {'passed': [], 'failed': []},
        'DEGREE': {'passed': [], 'failed': []},
        'DISTANCE': {'passed': [], 'failed': []},
        'TIME': {'passed': [], 'failed': []},
        'GENDER': {'passed': [], 'failed': []},
        'AGE': {'passed': [], 'failed': []},
        'HEALTH': {'passed': [], 'failed': []},
        'ELDERLY_AGE': {'passed': [], 'failed': []},
        'EXPERIENCE': {'passed': [], 'failed': []},
        'RATING': {'passed': [], 'failed': []},
        'SKILLS': {'passed': [], 'failed': []}
    }
    
    current_caregivers = caregivers_normalized
    
    # Filter 1: Care level match
    print(f"\nFILTER 1: CARE LEVEL MATCH")
    print(f"   Required Level: {req_normalized['care_level']}")
    passed_caregivers_filter1 = []
    
    for cg in current_caregivers:
        cg_id = cg.get('id', 'N/A')
        max_care_level = matcher.calculate_max_care_level_dynamic(cg)
        
        if max_care_level >= req_normalized['care_level']:
            passed_caregivers_filter1.append(cg)
            filter_results['CARE LEVEL']['passed'].append(cg_id)
        else:
            failed_caregivers.append({
                'id': cg_id,
                'failed_at': 'FILTER 1: CARE LEVEL',
                'reason': f'Max care level {max_care_level} < required {req_normalized["care_level"]}'
            })
            filter_results['CARE LEVEL']['failed'].append(cg_id)
    
    print(f"   PASSED ({len(passed_caregivers_filter1)}): {[cg.get('id') for cg in passed_caregivers_filter1]}")
    print(f"   FAILED ({len(filter_results['CARE LEVEL']['failed'])}): {filter_results['CARE LEVEL']['failed']}")
    
    current_caregivers = passed_caregivers_filter1
    
    # Filter 2: Degree requirement
    print(f"\nFILTER 2: DEGREE REQUIREMENT")
    print(f"   Required Level: {req_normalized['care_level']}")
    passed_caregivers_filter2 = []
    
    if req_normalized['care_level'] >= 3:
        for cg in current_caregivers:
            cg_id = cg.get('id', 'N/A')
            has_degree = False
            credentials = cg.get('credentials', [])
            
            for cred in credentials:
                if cred.get('status') == 'verified' and cred.get('type') == 'degree':
                    has_degree = True
                    break
            
            if has_degree:
                passed_caregivers_filter2.append(cg)
                filter_results['DEGREE']['passed'].append(cg_id)
            else:
                failed_caregivers.append({
                    'id': cg_id,
                    'failed_at': 'FILTER 2: DEGREE REQUIREMENT',
                    'reason': f'Level {req_normalized["care_level"]} requires degree but caregiver has no degree'
                })
                filter_results['DEGREE']['failed'].append(cg_id)
        
        print(f"   PASSED ({len(passed_caregivers_filter2)}): {[cg.get('id') for cg in passed_caregivers_filter2]}")
        print(f"   FAILED ({len(filter_results['DEGREE']['failed'])}): {filter_results['DEGREE']['failed']}")
    else:
        passed_caregivers_filter2 = current_caregivers
        print(f"   PASSED ALL ({len(passed_caregivers_filter2)}): No degree requirement for level {req_normalized['care_level']}")
        for cg in current_caregivers:
            filter_results['DEGREE']['passed'].append(cg.get('id', 'N/A'))
    
    current_caregivers = passed_caregivers_filter2
    
    # Filter 3: Distance - LOGIC MỚI: Tách PASS và FAIL lists
    print(f"\nFILTER 3: DISTANCE")
    passed_caregivers_filter3 = []
    distance_failed_caregivers = []  # New list for caregivers failing distance
    
    for cg in current_caregivers:
        cg_id = cg.get('id', 'N/A')
        location_info = cg.get('location', cg)
        cg_lat = location_info.get('lat', 0)
        cg_lon = location_info.get('lon', 0)
        distance = haversine_km(req_normalized['location']['lat'], req_normalized['location']['lon'], cg_lat, cg_lon)
        service_radius = location_info.get('service_radius_km', cg.get('service_radius_km', 10))
        
        if distance <= service_radius:
            passed_caregivers_filter3.append(cg)
            filter_results['DISTANCE']['passed'].append(cg_id)
        else:
            cg['distance'] = distance  # Store distance for sorting
            distance_failed_caregivers.append(cg)
            failed_caregivers.append({
                'id': cg_id,
                'failed_at': 'FILTER 3: DISTANCE',
                'reason': f'Distance {distance:.2f}km > service radius {service_radius}km'
            })
            filter_results['DISTANCE']['failed'].append(cg_id)
    
    print(f"   PASSED ({len(passed_caregivers_filter3)}): {[cg.get('id') for cg in passed_caregivers_filter3]}")
    print(f"   FAILED ({len(filter_results['DISTANCE']['failed'])}): {filter_results['DISTANCE']['failed']}")
    
    # Sắp xếp distance_failed_caregivers theo distance (gần nhất trước)
    distance_failed_caregivers.sort(key=lambda x: x['distance'])
    fallback_list_display = [(cg.get('id'), f'{cg["distance"]:.2f}km') for cg in distance_failed_caregivers]
    print(f"   DISTANCE FAILED LIST (sorted by distance): {fallback_list_display}")
    
    current_caregivers = passed_caregivers_filter3
    
    # Filter 4: Time availability
    print(f"\nFILTER 4: TIME AVAILABILITY")
    passed_caregivers_filter4 = []
    
    for cg in current_caregivers:
        cg_id = cg.get('id', 'N/A')
        try:
            availability_info = cg.get('availability', {})
            schedule = availability_info.get('schedule', [])
            
            # Convert schedule array to dict if needed (same as matcher)
            if isinstance(schedule, list):
                schedule = convert_schedule_to_dict(schedule)
            
            time_overlap = has_time_overlap(req_normalized['time_slots'], schedule)
            if time_overlap:
                passed_caregivers_filter4.append(cg)
                filter_results['TIME']['passed'].append(cg_id)
            else:
                failed_caregivers.append({
                    'id': cg_id,
                    'failed_at': 'FILTER 4: TIME AVAILABILITY',
                    'reason': 'No time overlap with requested slots'
                })
                filter_results['TIME']['failed'].append(cg_id)
        except Exception as e:
            failed_caregivers.append({
                'id': cg_id,
                'failed_at': 'FILTER 4: TIME AVAILABILITY',
                'reason': f'Time availability check failed: {str(e)}'
            })
            filter_results['TIME']['failed'].append(cg_id)
    
    print(f"   PASSED ({len(passed_caregivers_filter4)}): {[cg.get('id') for cg in passed_caregivers_filter4]}")
    print(f"   FAILED ({len(filter_results['TIME']['failed'])}): {filter_results['TIME']['failed']}")
    
    current_caregivers = passed_caregivers_filter4
    
    # Filter 5: Gender preference
    print(f"\nFILTER 5: GENDER PREFERENCE")
    print(f"   Required Gender: {req_normalized.get('gender_preference', 'None')}")
    passed_caregivers_filter5 = []
    
    for cg in current_caregivers:
        cg_id = cg.get('id', 'N/A')
        personal_info = cg.get('personal_info', cg)
        caregiver_gender = personal_info.get('gender', 'unknown')
        
        if req_normalized.get('gender_preference') and caregiver_gender != req_normalized['gender_preference']:
            failed_caregivers.append({
                'id': cg_id,
                'failed_at': 'FILTER 5: GENDER PREFERENCE',
                'reason': f'Gender {caregiver_gender} != {req_normalized["gender_preference"]}'
            })
            filter_results['GENDER']['failed'].append(cg_id)
        else:
            passed_caregivers_filter5.append(cg)
            filter_results['GENDER']['passed'].append(cg_id)
    
    print(f"   PASSED ({len(passed_caregivers_filter5)}): {[cg.get('id') for cg in passed_caregivers_filter5]}")
    print(f"   FAILED ({len(filter_results['GENDER']['failed'])}): {filter_results['GENDER']['failed']}")
    
    current_caregivers = passed_caregivers_filter5
    
    # Filter 6: Caregiver age range
    print(f"\nFILTER 6: CAREGIVER AGE RANGE")
    print(f"   Required Age Range: {req_normalized.get('caregiver_age_range', 'None')}")
    passed_caregivers_filter6 = []
    
    for cg in current_caregivers:
        cg_id = cg.get('id', 'N/A')
        personal_info = cg.get('personal_info', cg)
        caregiver_age = personal_info.get('age', 0)
        
        if req_normalized.get('caregiver_age_range') and not (req_normalized['caregiver_age_range'][0] <= caregiver_age <= req_normalized['caregiver_age_range'][1]):
            failed_caregivers.append({
                'id': cg_id,
                'failed_at': 'FILTER 6: CAREGIVER AGE RANGE',
                'reason': f'Age {caregiver_age} not in range {req_normalized["caregiver_age_range"]}'
            })
            filter_results['AGE']['failed'].append(cg_id)
        else:
            passed_caregivers_filter6.append(cg)
            filter_results['AGE']['passed'].append(cg_id)
    
    print(f"   PASSED ({len(passed_caregivers_filter6)}): {[cg.get('id') for cg in passed_caregivers_filter6]}")
    print(f"   FAILED ({len(filter_results['AGE']['failed'])}): {filter_results['AGE']['failed']}")
    
    current_caregivers = passed_caregivers_filter6
    
    # Filter 7: Health status preference
    print(f"\nFILTER 7: HEALTH STATUS PREFERENCE")
    print(f"   Elderly Health Status: {req_normalized.get('health_status', 'None')}")
    passed_caregivers_filter7 = []
    
    for cg in current_caregivers:
        cg_id = cg.get('id', 'N/A')
        preferences = cg.get('preferences', {})
        preferred_health_status = preferences.get('preferred_health_status', [])
        elderly_health_status = req_normalized.get('health_status', None)
        
        if preferred_health_status and elderly_health_status and elderly_health_status not in preferred_health_status:
            failed_caregivers.append({
                'id': cg_id,
                'failed_at': 'FILTER 7: HEALTH STATUS PREFERENCE',
                'reason': f'Health status {elderly_health_status} not in preferences {preferred_health_status}'
            })
            filter_results['HEALTH']['failed'].append(cg_id)
        else:
            passed_caregivers_filter7.append(cg)
            filter_results['HEALTH']['passed'].append(cg_id)
    
    print(f"   PASSED ({len(passed_caregivers_filter7)}): {[cg.get('id') for cg in passed_caregivers_filter7]}")
    print(f"   FAILED ({len(filter_results['HEALTH']['failed'])}): {filter_results['HEALTH']['failed']}")
    
    current_caregivers = passed_caregivers_filter7
    
    # Filter 8: Elderly age preference
    print(f"\nFILTER 8: ELDERLY AGE PREFERENCE")
    print(f"   Elderly Age: {req_normalized.get('elderly_age', 'None')}")
    passed_caregivers_filter8 = []
    
    for cg in current_caregivers:
        cg_id = cg.get('id', 'N/A')
        preferences = cg.get('preferences', {})
        elderly_age_preference = preferences.get('elderly_age_preference', None)
        elderly_age = req_normalized.get('elderly_age', None)
        
        if elderly_age_preference and elderly_age:
            min_age, max_age = elderly_age_preference
            if elderly_age < min_age or elderly_age > max_age:
                failed_caregivers.append({
                    'id': cg_id,
                    'failed_at': 'FILTER 8: ELDERLY AGE PREFERENCE',
                    'reason': f'Elderly age {elderly_age} not in preference range {elderly_age_preference}'
                })
                filter_results['ELDERLY_AGE']['failed'].append(cg_id)
            else:
                passed_caregivers_filter8.append(cg)
                filter_results['ELDERLY_AGE']['passed'].append(cg_id)
        else:
            passed_caregivers_filter8.append(cg)
            filter_results['ELDERLY_AGE']['passed'].append(cg_id)
    
    print(f"   PASSED ({len(passed_caregivers_filter8)}): {[cg.get('id') for cg in passed_caregivers_filter8]}")
    print(f"   FAILED ({len(filter_results['ELDERLY_AGE']['failed'])}): {filter_results['ELDERLY_AGE']['failed']}")
    
    current_caregivers = passed_caregivers_filter8
    
    # Filter 9: Required years experience
    print(f"\nFILTER 9: REQUIRED YEARS EXPERIENCE")
    print(f"   Required Experience: {req_normalized.get('required_years_experience', 'None')}")
    passed_caregivers_filter9 = []
    
    for cg in current_caregivers:
        cg_id = cg.get('id', 'N/A')
        professional_info = cg.get('professional_info', cg)
        years_experience = professional_info.get('years_experience', cg.get('years_experience', 0))
        required_years_experience = req_normalized.get('required_years_experience', None)
        
        if required_years_experience is not None and years_experience < required_years_experience:
            failed_caregivers.append({
                'id': cg_id,
                'failed_at': 'FILTER 9: REQUIRED YEARS EXPERIENCE',
                'reason': f'Experience {years_experience} < required {required_years_experience}'
            })
            filter_results['EXPERIENCE']['failed'].append(cg_id)
        else:
            passed_caregivers_filter9.append(cg)
            filter_results['EXPERIENCE']['passed'].append(cg_id)
    
    print(f"   PASSED ({len(passed_caregivers_filter9)}): {[cg.get('id') for cg in passed_caregivers_filter9]}")
    print(f"   FAILED ({len(filter_results['EXPERIENCE']['failed'])}): {filter_results['EXPERIENCE']['failed']}")
    
    current_caregivers = passed_caregivers_filter9
    
    # Filter 10: Overall rating range
    print(f"\nFILTER 10: OVERALL RATING RANGE")
    print(f"   Required Rating Range: {req_normalized.get('overall_rating_range', 'None')}")
    passed_caregivers_filter10 = []
    
    for cg in current_caregivers:
        cg_id = cg.get('id', 'N/A')
        ratings_reviews = cg.get('ratings_reviews', cg)
        caregiver_rating = ratings_reviews.get('overall_rating', cg.get('rating', 0.0))
        required_rating_range = req_normalized.get('overall_rating_range', None)
        
        if required_rating_range is not None:
            min_rating, max_rating = required_rating_range
            if caregiver_rating < min_rating or caregiver_rating > max_rating:
                failed_caregivers.append({
                    'id': cg_id,
                    'failed_at': 'FILTER 10: OVERALL RATING RANGE',
                    'reason': f'Rating {caregiver_rating} not in range {required_rating_range}'
                })
                filter_results['RATING']['failed'].append(cg_id)
            else:
                passed_caregivers_filter10.append(cg)
                filter_results['RATING']['passed'].append(cg_id)
        else:
            passed_caregivers_filter10.append(cg)
            filter_results['RATING']['passed'].append(cg_id)
    
    print(f"   PASSED ({len(passed_caregivers_filter10)}): {[cg.get('id') for cg in passed_caregivers_filter10]}")
    print(f"   FAILED ({len(filter_results['RATING']['failed'])}): {filter_results['RATING']['failed']}")
    
    current_caregivers = passed_caregivers_filter10
    
    # Filter 11: Required Skills
    print(f"\nFILTER 11: REQUIRED SKILLS")
    print(f"   Required Skills: {req_normalized['skills']['required_skills']}")
    passed_caregivers_filter11 = []
    
    for cg in current_caregivers:
        cg_id = cg.get('id', 'N/A')
        req_skills = req_normalized.get('skills', {})
        required_skills = req_skills.get('required_skills', [])
        
        if required_skills:
            cg_skills = cg.get('skills', [])
            cg_skill_names = set()
            for skill in cg_skills:
                if isinstance(skill, dict):
                    cg_skill_names.add(skill.get('name', ''))
                else:
                    cg_skill_names.add(skill)
            
            missing_skills = []
            matched_skills = []
            
            for req_skill in required_skills:
                best_match_score = 0.0
                best_match_skill = None
                
                for cg_skill in cg_skill_names:
                    similarity = semantic_matcher.calculate_similarity(req_skill, cg_skill)
                    if similarity > best_match_score:
                        best_match_score = similarity
                        best_match_skill = cg_skill
                
                # Threshold for PhoBERT v2 semantic matching (0.8 = 80% similarity for strict matching)
                if best_match_score >= 0.8:
                    matched_skills.append(f"{req_skill} -> {best_match_skill} ({best_match_score:.3f})")
                else:
                    missing_skills.append(req_skill)
            
            if missing_skills:
                failed_caregivers.append({
                    'id': cg_id,
                    'failed_at': 'FILTER 11: REQUIRED SKILLS',
                    'reason': f'Missing required skills: {missing_skills}'
                })
                filter_results['SKILLS']['failed'].append(cg_id)
            else:
                passed_caregivers_filter11.append(cg)
                filter_results['SKILLS']['passed'].append(cg_id)
                print(f"   ✅ {cg_id}: All skills matched - {matched_skills}")
        else:
            passed_caregivers_filter11.append(cg)
            filter_results['SKILLS']['passed'].append(cg_id)
    
    print(f"   PASSED ({len(passed_caregivers_filter11)}): {[cg.get('id') for cg in passed_caregivers_filter11]}")
    print(f"   FAILED ({len(filter_results['SKILLS']['failed'])}): {filter_results['SKILLS']['failed']}")
    
    final_passed_caregivers = passed_caregivers_filter11
    
    # FALLBACK LOGIC - Nếu không có kết quả từ PASS list
    if not final_passed_caregivers and distance_failed_caregivers:
        print(f"\nFALLBACK STRATEGY:")
        print("=" * 50)
        print(f"No caregivers passed all filters from PASS list.")
        print(f"Trying fallback with {len(distance_failed_caregivers)} distance-failed caregivers...")
        
        fallback_list_display = [(cg.get('id'), f'{cg["distance"]:.2f}km') for cg in distance_failed_caregivers]
        print(f"   FALLBACK CAREGIVERS (sorted by distance): {fallback_list_display}")
        
        fallback_passed = []
        remaining_fail_list_for_debug = distance_failed_caregivers.copy()
        
        batch_number = 1
        while remaining_fail_list_for_debug and len(fallback_passed) < 10:  # Limit to 10 results for debug output
            batch_size = min(10, len(remaining_fail_list_for_debug))
            current_batch_for_debug = remaining_fail_list_for_debug[:batch_size]
            
            print(f"\n   BATCH {batch_number}: Processing {len(current_batch_for_debug)} caregivers...")
            
            for cg in current_batch_for_debug:
                cg_id = cg.get('id', 'N/A')
                passed_fallback = True
                fail_reason = None
                
                # Filter 1: Care level match - BỎ QUA (đã pass ở round 1)
                # Filter 2: Degree requirement - BỎ QUA (đã pass ở round 1)  
                # Filter 3: Distance - BỎ QUA (đã fail ở round 1)
                
                # Filter 4: Time availability (nếu chưa fail)
                if passed_fallback:
                    try:
                        availability_info = cg.get('availability', {})
                        schedule = availability_info.get('schedule', [])
                        
                        # Convert schedule array to dict if needed (same as matcher)
                        if isinstance(schedule, list):
                            schedule = convert_schedule_to_dict(schedule)
                        
                        time_overlap = has_time_overlap(req_normalized['time_slots'], schedule)
                        if not time_overlap:
                            passed_fallback = False
                            fail_reason = 'No time overlap with requested slots'
                    except Exception:
                        passed_fallback = False
                        fail_reason = 'Time availability check failed'
                
                # Filter 5: Gender preference (nếu chưa fail)
                if passed_fallback:
                    personal_info = cg.get('personal_info', cg)
                    caregiver_gender = personal_info.get('gender', 'unknown')
                    if req_normalized.get('gender_preference') and caregiver_gender != req_normalized['gender_preference']:
                        passed_fallback = False
                        fail_reason = f'Gender {caregiver_gender} != {req_normalized["gender_preference"]}'
                
                # Filter 6: Caregiver age range (nếu chưa fail)
                if passed_fallback:
                    personal_info = cg.get('personal_info', cg)
                    caregiver_age = personal_info.get('age', 0)
                    if req_normalized.get('caregiver_age_range') and not (req_normalized['caregiver_age_range'][0] <= caregiver_age <= req_normalized['caregiver_age_range'][1]):
                        passed_fallback = False
                        fail_reason = f'Age {caregiver_age} not in range {req_normalized["caregiver_age_range"]}'
                
                # Filter 7: Health status preference (nếu chưa fail)
                if passed_fallback:
                    preferences = cg.get('preferences', {})
                    preferred_health_status = preferences.get('preferred_health_status', [])
                    elderly_health_status = req_normalized.get('health_status', None)
                    
                    if preferred_health_status and elderly_health_status and elderly_health_status not in preferred_health_status:
                        passed_fallback = False
                        fail_reason = f'Health status {elderly_health_status} not in preferences {preferred_health_status}'
                
                # Filter 8: Elderly age preference (nếu chưa fail)
                if passed_fallback:
                    preferences = cg.get('preferences', {})
                    elderly_age_preference = preferences.get('elderly_age_preference', None)
                    elderly_age = req_normalized.get('elderly_age', None)
                    
                    if elderly_age_preference and elderly_age:
                        min_age, max_age = elderly_age_preference
                        if elderly_age < min_age or elderly_age > max_age:
                            passed_fallback = False
                            fail_reason = f'Elderly age {elderly_age} not in preference range {elderly_age_preference}'
                
                # Filter 9: Required years experience (nếu chưa fail)
                if passed_fallback:
                    professional_info = cg.get('professional_info', cg)
                    years_experience = professional_info.get('years_experience', cg.get('years_experience', 0))
                    required_years_experience = req_normalized.get('required_years_experience', None)
                    
                    if required_years_experience is not None and years_experience < required_years_experience:
                        passed_fallback = False
                        fail_reason = f'Experience {years_experience} < required {required_years_experience}'
                
                # Filter 10: Overall rating range (nếu chưa fail)
                if passed_fallback:
                    ratings_reviews = cg.get('ratings_reviews', cg)
                    caregiver_rating = ratings_reviews.get('overall_rating', cg.get('rating', 0.0))
                    required_rating_range = req_normalized.get('overall_rating_range', None)
                    
                    if required_rating_range is not None:
                        min_rating, max_rating = required_rating_range
                        if caregiver_rating < min_rating or caregiver_rating > max_rating:
                            passed_fallback = False
                            fail_reason = f'Rating {caregiver_rating} not in range {required_rating_range}'
                
                # Filter 11: Required Skills (nếu chưa fail)
                if passed_fallback:
                    req_skills = req_normalized.get('skills', {})
                    required_skills = req_skills.get('required_skills', [])
                    
                    if required_skills:
                        cg_skills = cg.get('skills', [])
                        cg_skill_names = set()
                        for skill in cg_skills:
                            if isinstance(skill, dict):
                                cg_skill_names.add(skill.get('name', ''))
                            else:
                                cg_skill_names.add(skill)
                        
                        missing_skills = []
                        for req_skill in required_skills:
                            best_match_score = 0.0
                            for cg_skill in cg_skill_names:
                                similarity = semantic_matcher.calculate_similarity(req_skill, cg_skill)
                                best_match_score = max(best_match_score, similarity)
                            
                            if best_match_score < 0.8:
                                missing_skills.append(req_skill)
                        
                        if missing_skills:
                            passed_fallback = False
                            fail_reason = f'Missing required skills: {missing_skills}'
                
                if passed_fallback:
                    fallback_passed.append(cg)
                    print(f"   ✅ {cg_id}: PASSED fallback filters (distance: {cg['distance']:.2f}km)")
                    if len(fallback_passed) >= 10:  # Stop after 10 results for debug output
                        print(f"   📊 Reached limit of 10 fallback results, stopping...")
                        break
                else:
                    print(f"   ❌ {cg_id}: FAILED fallback filters - {fail_reason}")
            
            remaining_fail_list_for_debug = remaining_fail_list_for_debug[batch_size:]
            batch_number += 1
        
        if fallback_passed:
            print(f"\n   FALLBACK SUCCESS: {len(fallback_passed)} caregivers passed fallback filters")
            final_passed_caregivers = fallback_passed
        else:
            print(f"\n   FALLBACK FAILED: No caregivers passed fallback filters")
    
    print(f"\nFINAL RESULT:")
    print(f"   FINAL PASSED: {len(final_passed_caregivers)} caregivers")
    print(f"   TOTAL FAILED: {len(failed_caregivers)} caregivers")
    
    if final_passed_caregivers:
        print(f"\n✅ SUCCESS: Found {len(final_passed_caregivers)} matching caregivers:")
        for cg in final_passed_caregivers:
            print(f"   - {cg['id']}: {cg['personal_info']['full_name']}")
    else:
        print(f"\n❌ NO MATCHES: No caregivers passed all filters")
    
    print(f"\n📊 FILTER STATISTICS:")
    for filter_name, results in filter_results.items():
        print(f"   {filter_name}: {len(results['passed'])} passed, {len(results['failed'])} failed")

if __name__ == "__main__":
    main()
