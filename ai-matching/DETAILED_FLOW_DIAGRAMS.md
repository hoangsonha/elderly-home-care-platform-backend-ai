# SƠ ĐỒ THUẬT TOÁN MATCHING ENGINE - HƯỚNG DẪN VẼ CHI TIẾT

# File: backend/app/core/matcher.py

## HƯỚNG DẪN VẼ SƠ ĐỒ CHI TIẾT

### CÁCH VẼ SƠ ĐỒ:

1. **Mỗi step = 1 biểu đồ riêng**
2. **Sử dụng flowchart symbols**:
   - 🔵 **Start/End**: Hình tròn
   - 🔷 **Process**: Hình chữ nhật
   - 🔶 **Decision**: Hình thoi
   - 🔸 **Input/Output**: Hình bình hành
   - ➡️ **Flow**: Mũi tên

### SƠ ĐỒ TỔNG QUAN

```
🔵 START
    ↓
🔸 INPUT: care_request, caregivers, top_n
    ↓
🔷 NORMALIZE SKILLS (Vietnamese text normalization)
    │   ├── normalize_request_skills(care_request)
    │   └── normalize_caregiver_skills(caregivers)
    ↓
🔷 BƯỚC 1: Hard Filter - Tách PASS/FAIL lists dựa trên service_radius_km
    │   ↓
    │   🔷 FOR EACH caregiver in caregivers:
    │   │   ↓
    │   │   🔷 Calculate distance = haversine_km(request_location, caregiver_location)
    │   │   ↓
    │   │   🔶 distance <= service_radius_km?
    │   │   ↓
    │   │   ├── ✅ YES → 🔷 Add to PASS list
    │   │   └── ❌ NO → 🔷 Add to FAIL list (lưu distance)
    │   │   ↓
    │   │   🔷 Continue to next caregiver
    ↓
🔷 BƯỚC 2: Sắp xếp FAIL list theo distance (gần nhất trước)
    ↓
🔷 BƯỚC 3: Hard Filter - Xử lý PASS list
    │   ↓
    │   🔷 results = []
    │   ↓
    │   🔷 FOR EACH caregiver in PASS list:
    │   │   ↓
    │   │   🔷 _score_candidate(req, cg, radius_multiplier=1.0)
    │   │   ↓
    │   │   🔶 score_result != None? (pass all hard filters)
    │   │   ↓
    │   │   ├── ✅ YES → 🔷 Add to results
    │   │   └── ❌ NO → 🔷 Skip caregiver
    │   │   ↓
    │   │   🔷 Continue to next caregiver
    │   ↓
    │   🔶 results not empty?
    │   ↓
    │   ├── ✅ YES → 🔷 Sort by total_score (DESC)
    │   │   ↓
    │   │   🔷 Apply TOP N limit
    │   │   ↓
    │   │   🔸 OUTPUT: Ranked results
    │   │   ↓
    │   │   🔵 END (Matching Complete)
    │   └── ❌ NO → 🔷 Continue to Fallback
    ↓
🔷 BƯỚC 4: Hard Filter - Fallback với FAIL list
    │   ↓
    │   🔷 fallback_results = []
    │   ↓
    │   🔷 WHILE remaining_fail_list and len(fallback_results) < top_n:
    │   │   ↓
    │   │   🔷 batch_size = min(10, len(remaining_fail_list))
    │   │   ↓
    │   │   🔷 current_batch = remaining_fail_list[:batch_size]
    │   │   ↓
    │   │   🔷 FOR EACH caregiver in current_batch:
    │   │   │   ↓
    │   │   │   🔷 _score_candidate_fallback(req, cg)
    │   │   │   ↓
    │   │   │   🔶 score_result != None? (pass hard filters 1,2,4-11)
    │   │   │   ↓
    │   │   │   ├── ✅ YES → 🔷 Add to fallback_results
    │   │   │   └── ❌ NO → 🔷 Skip caregiver
    │   │   │   ↓
    │   │   │   🔷 Continue to next caregiver
    │   │   ↓
    │   │   🔷 remaining_fail_list = remaining_fail_list[batch_size:]
    │   │   ↓
    │   │   🔶 len(fallback_results) >= top_n?
    │   │   ↓
    │   │   ├── ✅ YES → 🔷 Break loop
    │   │   └── ❌ NO → 🔷 Continue loop
    │   ↓
    │   🔶 fallback_results not empty?
    │   ↓
    │   ├── ✅ YES → 🔷 Sort by total_score (DESC)
    │   │   ↓
    │   │   🔷 Apply TOP N limit
    │   │   ↓
    │   │   🔸 OUTPUT: Fallback results
    │   │   ↓
    │   │   🔵 END (Fallback Success)
    │   └── ❌ NO → 🔷 No matches found
    ↓
🔸 OUTPUT: Empty list
    ↓
🔵 END (No Matches Found)
```

1. **Mỗi step = 1 biểu đồ riêng**
2. **Sử dụng flowchart symbols**:
   - 🔵 **Start/End**: Hình tròn
   - 🔷 **Process**: Hình chữ nhật
   - 🔶 **Decision**: Hình thoi
   - 🔸 **Input/Output**: Hình bình hành
   - ➡️ **Flow**: Mũi tên

## 1. SƠ ĐỒ CHI TIẾT HARD FILTERS

### FILTER 1: CARE LEVEL CHECK

```
🔵 START
    ↓
🔷 Extract max_care_level from caregiver
    ↓
🔶 max_care_level >= request.care_level?
    ↓
    ├── ✅ YES → 🔷 Continue to Filter 2
    └── ❌ NO → 🔷 REJECT CAREGIVER
        ↓
    🔵 END (Filter Failed)
```

### FILTER 2: DEGREE REQUIREMENT

```
🔵 START
    ↓
🔷 Check if care_level >= 3
    ↓
🔶 care_level >= 3?
    ↓
    ├── ✅ YES → 🔷 Check for verified degree
    │   ↓
    │   🔶 Has verified degree?
    │   ↓
    │   ├── ✅ YES → 🔷 Continue to Filter 3
    │   └── ❌ NO → 🔷 REJECT CAREGIVER
    └── ❌ NO → 🔷 Continue to Filter 3
        ↓
    🔵 END (Filter Passed)
```

### FILTER 3: DISTANCE CHECK

```
🔵 START
    ↓
🔷 Calculate distance using Haversine formula
    ↓
🔷 Get service_radius from caregiver
    ↓
🔷 Apply radius_multiplier (1.0, 1.5, or 2.0)
    ↓
🔶 distance <= effective_radius?
    ↓
    ├── ✅ YES → 🔷 Continue to Filter 4
    └── ❌ NO → 🔷 REJECT CAREGIVER
        ↓
    🔵 END (Filter Failed)
```

### FILTER 4: TIME AVAILABILITY

```
🔵 START
    ↓
🔷 Convert request time_slots to dict format
    ↓
🔷 Convert caregiver schedule to dict format
    ↓
🔷 FOR EACH request time slot:
    │   ↓
    │   🔶 Find matching day in caregiver schedule?
    │   ↓
    │   ├── ✅ YES → 🔶 Time slot overlaps?
    │   │   ↓
    │   │   ├── ✅ YES → 🔷 Continue to next slot
    │   │   └── ❌ NO → 🔷 REJECT CAREGIVER
    │   └── ❌ NO → 🔷 REJECT CAREGIVER
    ↓
🔶 ALL slots have overlap?
    ↓
    ├── ✅ YES → 🔷 Continue to Filter 5
    └── ❌ NO → 🔷 REJECT CAREGIVER
        ↓
    🔵 END (Filter Passed)
```

### FILTER 5: GENDER PREFERENCE

```
🔵 START
    ↓
🔷 Extract gender_preference from request
    ↓
🔶 gender_preference exists?
    ↓
    ├── ✅ YES → 🔷 Extract caregiver gender
    │   ↓
    │   🔶 gender matches preference?
    │   ↓
    │   ├── ✅ YES → 🔷 Continue to Filter 6
    │   └── ❌ NO → 🔷 REJECT CAREGIVER
    └── ❌ NO → 🔷 Continue to Filter 6
        ↓
    🔵 END (Filter Passed)
```

### FILTER 6: CAREGIVER AGE RANGE

```
🔵 START
    ↓
🔷 Extract caregiver_age_range from request
    ↓
🔶 caregiver_age_range exists?
    ↓
    ├── ✅ YES → 🔷 Extract caregiver age
    │   ↓
    │   🔶 age in range [min_age, max_age]?
    │   ↓
    │   ├── ✅ YES → 🔷 Continue to Filter 7
    │   └── ❌ NO → 🔷 REJECT CAREGIVER
    └── ❌ NO → 🔷 Continue to Filter 7
        ↓
    🔵 END (Filter Passed)
```

### FILTER 7: HEALTH STATUS PREFERENCE

```
🔵 START
    ↓
🔷 Extract preferred_health_status from caregiver
    ↓
🔶 preferred_health_status exists?
    ↓
    ├── ✅ YES → 🔷 Extract elderly_health_status from request
    │   ↓
    │   🔶 health_status in preferred list?
    │   ↓
    │   ├── ✅ YES → 🔷 Continue to Filter 8
    │   └── ❌ NO → 🔷 REJECT CAREGIVER
    └── ❌ NO → 🔷 Continue to Filter 8
        ↓
    🔵 END (Filter Passed)
```

### FILTER 8: ELDERLY AGE PREFERENCE

```
🔵 START
    ↓
🔷 Extract elderly_age_preference from caregiver
    ↓
🔶 elderly_age_preference exists?
    ↓
    ├── ✅ YES → 🔷 Extract elderly_age from request
    │   ↓
    │   🔶 elderly_age in range [min_age, max_age]?
    │   ↓
    │   ├── ✅ YES → 🔷 Continue to Filter 9
    │   └── ❌ NO → 🔷 REJECT CAREGIVER
    └── ❌ NO → 🔷 Continue to Filter 9
        ↓
    🔵 END (Filter Passed)
```

### FILTER 9: REQUIRED YEARS EXPERIENCE

```
🔵 START
    ↓
🔷 Extract required_years_experience from request
    ↓
🔶 required_years_experience exists?
    ↓
    ├── ✅ YES → 🔷 Extract caregiver years_experience
    │   ↓
    │   🔶 years_experience >= required?
    │   ↓
    │   ├── ✅ YES → 🔷 Continue to Filter 10
    │   └── ❌ NO → 🔷 REJECT CAREGIVER
    └── ❌ NO → 🔷 Continue to Filter 10
        ↓
    🔵 END (Filter Passed)
```

### FILTER 10: OVERALL RATING RANGE

```
🔵 START
    ↓
🔷 Extract overall_rating_range from request
    ↓
🔶 overall_rating_range exists?
    ↓
    ├── ✅ YES → 🔷 Extract caregiver overall_rating
    │   ↓
    │   🔶 rating in range [min_rating, max_rating]?
    │   ↓
    │   ├── ✅ YES → 🔷 Continue to Filter 11
    │   └── ❌ NO → 🔷 REJECT CAREGIVER
    └── ❌ NO → 🔷 Continue to Filter 11
        ↓
    🔵 END (Filter Passed)
```

### FILTER 11: REQUIRED SKILLS (PHOBERT)

```
🔵 START
    ↓
🔷 Extract required_skills from request
    ↓
🔶 required_skills exists?
    ↓
    ├── ✅ YES → 🔷 FOR EACH required_skill:
    │   │   ↓
    │   │   🔷 Normalize skill text (remove diacritics)
    │   │   ↓
    │   │   🔷 FOR EACH caregiver skill:
    │   │   │   ↓
    │   │   │   🔷 Calculate PhoBERT similarity
    │   │   │   ↓
    │   │   │   🔷 Store best match score
    │   │   ↓
    │   │   🔶 best_match_score >= 0.8?
    │   │   ↓
    │   │   ├── ✅ YES → 🔷 Skill matched, continue to next
    │   │   └── ❌ NO → 🔷 REJECT CAREGIVER (missing skill)
    │   ↓
    │   🔶 ALL skills matched?
    │   ↓
    │   ├── ✅ YES → 🔷 ALL FILTERS PASSED
    │   └── ❌ NO → 🔷 REJECT CAREGIVER
    └── ❌ NO → 🔷 Continue to SOFT SCORING
        ↓
    🔵 END (All Filters Passed)
```

## 2. SƠ ĐỒ CHI TIẾT SOFT SCORING

### FEATURE 1: CREDENTIAL SCORE (30%)

```
🔵 START
    ↓
🔷 Extract credentials from caregiver
    ↓
🔷 Filter valid credentials (verified, not expired)
    ↓
🔷 Calculate degree bonus:
    │   ├── Find highest degree level
    │   └── Add level points (1-4)
    ↓
🔷 Calculate certificate bonus:
    │   ├── Count certificates meeting required_level
    │   └── Add 0.5 points per certificate (max 6 points)
    ↓
🔷 Total score = degree_bonus + certificate_bonus
    ↓
🔷 Normalize: score / 10.0
    ↓
🔷 Apply weight: score × 0.30
    ↓
🔵 END (Credential Score)
```

### FEATURE 2: SKILLS SCORE (25%)

```
🔵 START
    ↓
🔷 Extract priority_skills from request
    ↓
🔶 priority_skills exists?
    ↓
    ├── ✅ YES → 🔷 FOR EACH priority_skill:
    │   │   ↓
    │   │   🔷 Use PhoBERT semantic matching
    │   │   ↓
    │   │   🔷 Find best match with caregiver skills
    │   │   ↓
    │   │   🔶 similarity >= 0.8?
    │   │   ↓
    │   │   ├── ✅ YES → 🔷 Count as matched
    │   │   │   ↓
    │   │   │   🔷 Check if skill has credential_id
    │   │   │   ↓
    │   │   │   🔷 Count skills_with_credentials
    │   │   └── ❌ NO → 🔷 Skip skill
    │   ↓
    │   🔷 base_score = matched_count / total_priority_skills
    │   ↓
    │   🔷 bonus = (skills_with_credentials / matched_count) × 0.2
    │   ↓
    │   🔷 final_score = min(1.0, base_score + bonus)
    └── ❌ NO → 🔷 Return 1.0 (no priority skills)
    ↓
🔷 Apply weight: score × 0.25
    ↓
🔵 END (Skills Score)
```

### FEATURE 3: DISTANCE SCORE (15%)

```
🔵 START
    ↓
🔷 Calculate distance using Haversine formula
    ↓
🔷 Apply exponential decay: e^(-distance/8.0)
    ↓
🔷 Score mapping:
    │   ├── 0km → 1.0
    │   ├── 8km → 0.37
    │   ├── 16km → 0.14
    │   └── 24km → 0.05
    ↓
🔷 Apply weight: score × 0.15
    ↓
🔵 END (Distance Score)
```

### FEATURE 4: RATING SCORE (12%)

```
🔵 START
    ↓
🔷 Extract rating_breakdown from caregiver
    ↓
🔶 rating_breakdown exists?
    ↓
    ├── ✅ YES → 🔷 Calculate total_rating:
    │   │   ├── 5_star × 5
    │   │   ├── 4_star × 4
    │   │   ├── 3_star × 3
    │   │   ├── 2_star × 2
    │   │   └── 1_star × 1
    └── ❌ NO → 🔷 total_rating = overall_rating × total_reviews
    ↓
🔷 Apply Bayesian Average:
    │   ├── C = 25 (confidence constant)
    │   ├── m = 3.5 (platform mean)
    │   └── bayesian_rating = (total_rating + C×m) / (total_reviews + C)
    ↓
🔷 Normalize: bayesian_rating / 5.0
    ↓
🔷 Apply weight: score × 0.12
    ↓
🔵 END (Rating Score)
```

### FEATURE 5: EXPERIENCE SCORE (8%)

```
🔵 START
    ↓
🔷 Extract years_experience from caregiver
    ↓
🔷 Apply formula: min(1.0, max(0.1, years_experience / 10.0))
    ↓
🔷 Score mapping:
    │   ├── 0 years → 0.1 (minimum)
    │   ├── 1 year → 0.2
    │   ├── 5 years → 0.6
    │   ├── 10 years → 1.0 (maximum)
    │   └── > 10 years → 1.0 (capped)
    ↓
🔷 Apply weight: score × 0.08
    ↓
🔵 END (Experience Score)
```

### FEATURE 6: PRICE SCORE (8%)

```
🔵 START
    ↓
🔷 Extract hourly_rate from caregiver
    ↓
🔷 Extract budget_per_hour from request
    ↓
🔶 hourly_rate <= budget?
    ↓
    ├── ✅ YES → 🔷 Within Budget:
    │   │   ├── rate < 50% budget? → 1.0
    │   │   └── rate >= 50% budget? → Linear: 1.0 - (ratio - 0.5) × 0.2
    └── ❌ NO → 🔷 Over Budget:
        │   ├── excess_ratio = (rate - budget) / budget
        │   ├── penalty = 1.0 - excess_ratio
        │   └── max(0.0, penalty)
    ↓
🔷 Apply weight: score × 0.08
    ↓
🔵 END (Price Score)
```

### FEATURE 7: TRUST SCORE (2%)

```
🔵 START
    ↓
🔷 Extract booking_history from caregiver
    ↓
🔷 Calculate completion_component (40%):
    │   └── completion_rate (direct score)
    ↓
🔷 Calculate cancel_component (30%):
    │   ├── seeker_cancel_rate
    │   └── Inverted: 1.0 - (rate × 6.67)
    ↓
🔷 Calculate bookings_component (20%):
    │   ├── >= 100: 1.0
    │   ├── >= 50: 0.8
    │   ├── >= 20: 0.6
    │   ├── >= 10: 0.4
    │   └── < 10: 0.2
    ↓
🔷 Calculate verification_component (10%):
    │   ├── identity_verified = True → 1.0
    │   └── identity_verified = False → 0.5
    ↓
🔷 Weighted sum:
    │   ├── 0.4 × completion_component
    │   ├── 0.3 × cancel_component
    │   ├── 0.2 × bookings_component
    │   └── 0.1 × verification_component
    ↓
🔷 Apply weight: score × 0.02
    ↓
🔵 END (Trust Score)
```

## 3. SƠ ĐỒ WEIGHTED SUM CALCULATION

```
🔵 START
    ↓
🔷 Collect all feature scores:
    │   ├── credential_score × 0.30
    │   ├── skills_score × 0.25
    │   ├── distance_score × 0.15
    │   ├── rating_score × 0.12
    │   ├── experience_score × 0.08
    │   ├── price_score × 0.08
    │   └── trust_score × 0.02
    ↓
🔷 Calculate weighted sum:
    │   total_score = Σ(feature_score × weight)
    ↓
🔷 Round to 3 decimal places
    ↓
🔷 Return result:
    │   ├── total_score
    │   ├── distance_km
    │   └── breakdown (individual scores)
    ↓
🔵 END (Final Score)
```

## 4. SƠ ĐỒ FALLBACK STRATEGY

```
🔵 START
    ↓
🔸 INPUT: care_request, caregivers, top_n
    ↓
🔷 BƯỚC 1: Hard Filter với service_radius_km của từng caregiver
    │   ├── PASS list: caregivers trong service_radius
    │   └── FAIL list: caregivers ngoài service_radius (lưu distance)
    ↓
🔷 BƯỚC 2: Sắp xếp FAIL list theo distance (gần nhất trước)
    ↓
🔷 BƯỚC 3: Thử PASS list trước
    │   ↓
    │   🔷 FOR EACH caregiver in PASS list:
    │   │   ↓
    │   │   🔷 _score_candidate(req, cg, radius_multiplier=1.0)
    │   │   ↓
    │   │   🔶 score_result != None?
    │   │   ↓
    │   │   ├── ✅ YES → 🔷 Add to results
    │   │   └── ❌ NO → 🔷 Skip caregiver
    │   │   ↓
    │   │   🔷 Continue to next caregiver
    │   ↓
    │   🔶 results not empty?
    │   ↓
    │   ├── ✅ YES → 🔷 Sort by total_score (DESC)
    │   │   ↓
    │   │   🔸 OUTPUT: Ranked results
    │   │   ↓
    │   │   🔵 END (Success)
    │   └── ❌ NO → 🔷 Continue to Fallback
    ↓
🔷 BƯỚC 4: Fallback - Lấy nhiều lần, mỗi lần 10 người từ FAIL list
    │   ↓
    │   🔷 fallback_results = []
    │   ↓
    │   🔷 WHILE remaining_fail_list and len(fallback_results) < top_n:
    │   │   ↓
    │   │   🔷 batch_size = min(10, len(remaining_fail_list))
    │   │   ↓
    │   │   🔷 current_batch = remaining_fail_list[:batch_size]
    │   │   ↓
    │   │   🔷 FOR EACH caregiver in current_batch:
    │   │   │   ↓
    │   │   │   🔷 _score_candidate_fallback(req, cg)
    │   │   │   ↓
    │   │   │   🔶 score_result != None?
    │   │   │   ↓
    │   │   │   ├── ✅ YES → 🔷 Add to fallback_results
    │   │   │   └── ❌ NO → 🔷 Skip caregiver
    │   │   │   ↓
    │   │   │   🔷 Continue to next caregiver
    │   │   ↓
    │   │   🔷 remaining_fail_list = remaining_fail_list[batch_size:]
    │   │   ↓
    │   │   🔶 len(fallback_results) >= top_n?
    │   │   ↓
    │   │   ├── ✅ YES → 🔷 Break loop
    │   │   └── ❌ NO → 🔷 Continue loop
    │   ↓
    │   🔶 fallback_results not empty?
    │   ↓
    │   ├── ✅ YES → 🔷 Sort by total_score (DESC)
    │   │   ↓
    │   │   🔸 OUTPUT: Fallback results
    │   │   ↓
    │   │   🔵 END (Fallback Success)
    │   └── ❌ NO → 🔷 No matches found
    ↓
🔸 OUTPUT: Empty list
    ↓
🔵 END (No Matches)
```

## 5. SƠ ĐỒ PHOBERT SEMANTIC MATCHING

```
🔵 START
    ↓
🔷 Input: required_skill, caregiver_skills
    ↓
🔷 Normalize required_skill:
    │   ├── Remove diacritics
    │   ├── Convert to lowercase
    │   └── Clean whitespace
    ↓
🔷 FOR EACH caregiver_skill:
    │   ↓
    │   🔷 Normalize caregiver_skill
    │   ↓
    │   🔷 Calculate cosine similarity using PhoBERT
    │   ↓
    │   🔷 Store best match score
    ↓
🔶 best_match_score >= 0.8?
    ↓
    ├── ✅ YES → 🔷 SKILL MATCHED
    └── ❌ NO → 🔷 SKILL MISSING
        ↓
    🔵 END (Semantic Matching)
```

## 6. SƠ ĐỒ MAIN MATCHING FLOW

```
🔵 START
    ↓
🔸 INPUT: care_request, caregivers, top_n
    ↓
🔷 Input validation:
    │   ├── care_request: Dict
    │   ├── caregivers: List[Dict]
    │   └── top_n: int (default 10)
    ↓
🔷 Skills normalization:
    │   ├── normalize_request_skills(care_request)
    │   └── normalize_caregiver_skills(caregivers)
    ↓
🔷 BƯỚC 1: Hard Filter với service_radius_km của từng caregiver
    │   ├── PASS list: caregivers trong service_radius
    │   └── FAIL list: caregivers ngoài service_radius (lưu distance)
    ↓
🔷 BƯỚC 2: Sắp xếp FAIL list theo distance (gần nhất trước)
    ↓
🔷 BƯỚC 3: Thử PASS list trước
    │   ↓
    │   🔷 results = []
    │   ↓
    │   🔷 FOR EACH caregiver in PASS list:
    │   │   ↓
    │   │   🔷 score_result = _score_candidate(req, cg, radius_multiplier=1.0)
    │   │   ↓
    │   │   🔶 score_result != None?
    │   │   ↓
    │   │   ├── ✅ YES → 🔷 Add to results
    │   │   └── ❌ NO → 🔷 Skip caregiver
    │   │   ↓
    │   │   🔷 Continue to next caregiver
    │   ↓
    │   🔶 results not empty?
    │   ↓
    │   ├── ✅ YES → 🔷 Sort by total_score (DESC)
    │   │   ↓
    │   │   🔷 Apply TOP N limit
    │   │   ↓
    │   │   🔸 OUTPUT: Ranked results
    │   │   ↓
    │   │   🔵 END (Matching Complete)
    │   └── ❌ NO → 🔷 Continue to Fallback
    ↓
🔷 BƯỚC 4: Fallback - Lấy nhiều lần, mỗi lần 10 người từ FAIL list
    │   ↓
    │   🔷 fallback_results = []
    │   ↓
    │   🔷 WHILE remaining_fail_list and len(fallback_results) < top_n:
    │   │   ↓
    │   │   🔷 batch_size = min(10, len(remaining_fail_list))
    │   │   ↓
    │   │   🔷 current_batch = remaining_fail_list[:batch_size]
    │   │   ↓
    │   │   🔷 FOR EACH caregiver in current_batch:
    │   │   │   ↓
    │   │   │   🔷 score_result = _score_candidate_fallback(req, cg)
    │   │   │   ↓
    │   │   │   🔶 score_result != None?
    │   │   │   ↓
    │   │   │   ├── ✅ YES → 🔷 Add to fallback_results
    │   │   │   └── ❌ NO → 🔷 Skip caregiver
    │   │   │   ↓
    │   │   │   🔷 Continue to next caregiver
    │   │   ↓
    │   │   🔷 remaining_fail_list = remaining_fail_list[batch_size:]
    │   │   ↓
    │   │   🔶 len(fallback_results) >= top_n?
    │   │   ↓
    │   │   ├── ✅ YES → 🔷 Break loop
    │   │   └── ❌ NO → 🔷 Continue loop
    │   ↓
    │   🔶 fallback_results not empty?
    │   ↓
    │   ├── ✅ YES → 🔷 Sort by total_score (DESC)
    │   │   ↓
    │   │   🔷 Apply TOP N limit
    │   │   ↓
    │   │   🔸 OUTPUT: Fallback results
    │   │   ↓
    │   │   🔵 END (Fallback Success)
    │   └── ❌ NO → 🔷 No matches found
    ↓
🔸 OUTPUT: Empty list
    ↓
🔵 END (No Matches Found)
```

---

## TÓM TẮT THUẬT TOÁN

**Thuật toán Matching Engine** là một hệ thống phức tạp với 3 giai đoạn chính:

1. **Hard Filters (11 filters)**: Loại bỏ caregivers không đủ điều kiện
2. **Soft Scoring (7 features)**: Tính điểm cho caregivers còn lại
3. **Fallback Strategy**: Mở rộng bán kính tìm kiếm nếu không có kết quả

**Điểm mạnh:**

- Sử dụng PhoBERT cho semantic matching tiếng Việt
- Fallback strategy đảm bảo luôn có kết quả
- Scoring đa chiều với trọng số cân bằng
- Xử lý edge cases và error handling tốt

**Độ phức tạp:**

- Time: O(n × m × k) với n=caregivers, m=skills, k=time slots
- Space: O(n) cho storing results
- PhoBERT inference: O(1) per skill comparison

**Tối ưu hóa:**

- Early termination trong hard filters
- Caching cho semantic matching
- Batch processing cho multiple caregivers
- Memory optimization cho large datasets

---

## HƯỚNG DẪN SỬ DỤNG SƠ ĐỒ

### CÁCH VẼ:

1. **Mỗi sơ đồ = 1 trang riêng**
2. **Sử dụng symbols chuẩn**:
   - 🔵 Start/End (Hình tròn)
   - 🔷 Process (Hình chữ nhật)
   - 🔶 Decision (Hình thoi)
   - ➡️ Flow (Mũi tên)

### THỨ TỰ VẼ:

1. **Sơ đồ tổng quan** (1 trang)
2. **11 Hard Filters** (11 trang riêng)
3. **7 Soft Scoring** (7 trang riêng)
4. **Weighted Sum** (1 trang)
5. **Fallback Strategy** (1 trang)
6. **PhoBERT Matching** (1 trang)
7. **Main Flow** (1 trang)

### TỔNG CỘNG: 23 sơ đồ riêng biệt

Mỗi sơ đồ sẽ có:

- **Title**: Tên filter/feature
- **Input**: Dữ liệu đầu vào
- **Process**: Các bước xử lý
- **Decision**: Điều kiện kiểm tra
- **Output**: Kết quả đầu ra
- **Flow**: Luồng xử lý
