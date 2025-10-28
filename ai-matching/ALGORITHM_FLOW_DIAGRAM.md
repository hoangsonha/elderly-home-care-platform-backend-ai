# SƠ ĐỒ THUẬT TOÁN MATCHING ENGINE

# File: backend/app/core/matcher.py

## 1. SƠ ĐỒ TỔNG QUAN

```
INPUT: Care Request + List Caregivers
    ↓
NORMALIZE SKILLS (Vietnamese → English)
    ↓
FALLBACK STRATEGY (3 rounds)
    ↓
FOR EACH CAREGIVER:
    ├── HARD FILTERS (11 filters)
    │   ├── ✅ PASS → SOFT SCORING
    │   └── ❌ FAIL → NEXT CAREGIVER
    └── SOFT SCORING (7 features)
        ├── Weighted Sum Calculation
        └── Add to Results
    ↓
SORT BY SCORE (DESC)
    ↓
RETURN TOP N RESULTS
```

## 2. SƠ ĐỒ CHI TIẾT HARD FILTERS

```
CAREGIVER INPUT
    ↓
FILTER 1: Care Level
├── max_care_level >= request.care_level?
├── ✅ YES → Continue
└── ❌ NO → REJECT

FILTER 2: Degree Requirement (Level 3+)
├── care_level >= 3?
├── ├── YES → Has verified degree?
│   │   ├── ✅ YES → Continue
│   │   └── ❌ NO → REJECT
└── └── NO → Continue

FILTER 3: Distance
├── distance <= service_radius * multiplier?
├── ✅ YES → Continue
└── ❌ NO → REJECT

FILTER 4: Time Availability
├── 100% time slots overlap?
├── ✅ YES → Continue
└── ❌ NO → REJECT

FILTER 5: Gender Preference
├── gender_preference exists?
├── ├── YES → gender matches?
│   │   ├── ✅ YES → Continue
│   │   └── ❌ NO → REJECT
└── └── NO → Continue

FILTER 6: Caregiver Age Range
├── caregiver_age_range exists?
├── ├── YES → age in range?
│   │   ├── ✅ YES → Continue
│   │   └── ❌ NO → REJECT
└── └── NO → Continue

FILTER 7: Health Status Preference
├── preferred_health_status exists?
├── ├── YES → health_status in preferred?
│   │   ├── ✅ YES → Continue
│   │   └── ❌ NO → REJECT
└── └── NO → Continue

FILTER 8: Elderly Age Preference
├── elderly_age_preference exists?
├── ├── YES → elderly_age in range?
│   │   ├── ✅ YES → Continue
│   │   └── ❌ NO → REJECT
└── └── NO → Continue

FILTER 9: Required Years Experience
├── required_years_experience exists?
├── ├── YES → years_experience >= required?
│   │   ├── ✅ YES → Continue
│   │   └── ❌ NO → REJECT
└── └── NO → Continue

FILTER 10: Overall Rating Range
├── overall_rating_range exists?
├── ├── YES → rating in range?
│   │   ├── ✅ YES → Continue
│   │   └── ❌ NO → REJECT
└── └── NO → Continue

FILTER 11: Required Skills (PhoBERT)
├── required_skills exists?
├── ├── YES → FOR EACH required_skill:
│   │   ├── Calculate similarity with ALL caregiver skills
│   │   ├── Best match >= 0.8 threshold?
│   │   │   ├── ✅ YES → Continue to next skill
│   │   │   └── ❌ NO → REJECT (missing skill)
│   │   └── ALL skills matched? → Continue
└── └── NO → Continue

ALL FILTERS PASSED → SOFT SCORING
```

## 3. SƠ ĐỒ SOFT SCORING

```
SOFT SCORING CALCULATION
    ↓
FEATURE 1: Credential Score (30%)
├── Degree bonus: max_degree_level (1-4 points)
├── Certificate bonus: 0.5 points per valid cert
├── Normalize: score / 10.0
└── Weight: × 0.30

FEATURE 2: Skills Score (25%)
├── Priority skills matching using PhoBERT
├── Base score: matched_count / total_priority_skills
├── Bonus: skills_with_credentials ratio × 0.2
└── Weight: × 0.25

FEATURE 3: Distance Score (15%)
├── Formula: e^(-distance/8.0)
├── Exponential decay function
└── Weight: × 0.15

FEATURE 4: Rating Score (12%)
├── Bayesian Average: (total_rating + C×m) / (total_reviews + C)
├── C = 25, m = 3.5
├── Normalize: bayesian_rating / 5.0
└── Weight: × 0.12

FEATURE 5: Experience Score (8%)
├── Formula: min(1.0, max(0.1, years_experience / 10.0))
├── Min 0.1 for new caregivers
└── Weight: × 0.08

FEATURE 6: Price Score (8%)
├── Price <= budget: 0.8 - 1.0 (linear)
├── Price > budget: penalty (exponential decay)
└── Weight: × 0.08

FEATURE 7: Trust Score (2%)
├── Completion rate (40%)
├── Seeker cancel rate (30%) - inverted
├── Total bookings (20%)
├── Verification (10%)
└── Weight: × 0.02

WEIGHTED SUM CALCULATION
├── total_score = Σ(feature_score × weight)
└── Return: {total_score, distance_km, breakdown}
```

## 4. SƠ ĐỒ FALLBACK STRATEGY

```
START MATCHING
    ↓
ROUND 1: service_radius × 1.0
├── Apply all filters with original radius
├── Found caregivers?
│   ├── ✅ YES → Sort & Return TOP N
│   └── ❌ NO → ROUND 2
    ↓
ROUND 2: service_radius × 1.5
├── Apply all filters with expanded radius
├── Found caregivers?
│   ├── ✅ YES → Sort & Return TOP N
│   └── ❌ NO → ROUND 3
    ↓
ROUND 3: service_radius × 2.0
├── Apply all filters with max radius
├── Found caregivers?
│   ├── ✅ YES → Sort & Return TOP N
│   └── ❌ NO → Return EMPTY LIST
```

## 5. SƠ ĐỒ PHOBERT SEMANTIC MATCHING

```
REQUIRED SKILL: "kiểm tra huyết áp"
    ↓
NORMALIZE TEXT
├── Remove diacritics: "kiem tra huyet ap"
├── Lowercase & trim
└── Clean whitespace
    ↓
FOR EACH CAREGIVER SKILL:
├── Normalize caregiver skill
├── Calculate cosine similarity
├── Store best match score
└── Continue to next skill
    ↓
BEST MATCH SCORE >= 0.8?
├── ✅ YES → SKILL MATCHED
└── ❌ NO → SKILL MISSING
    ↓
ALL REQUIRED SKILLS MATCHED?
├── ✅ YES → PASS FILTER
└── ❌ NO → REJECT CAREGIVER
```

## 6. SƠ ĐỒ BAYESIAN RATING CALCULATION

```
CAREGIVER RATING DATA
    ↓
CALCULATE TOTAL RATING
├── From rating_breakdown:
│   ├── 5_star × 5
│   ├── 4_star × 4
│   ├── 3_star × 3
│   ├── 2_star × 2
│   └── 1_star × 1
└── Sum = total_rating
    ↓
BAYESIAN AVERAGE FORMULA
├── C = 25 (confidence constant)
├── m = 3.5 (platform mean)
├── bayesian_rating = (total_rating + C×m) / (total_reviews + C)
└── Normalize: bayesian_rating / 5.0
```

## 7. SƠ ĐỒ TRUST SCORE CALCULATION

```
TRUST FACTORS
    ↓
COMPLETION RATE (40%)
├── completion_rate from booking_history
└── Direct score: 0.0 - 1.0
    ↓
SEEKER CANCEL RATE (30%)
├── seeker_cancel_rate from booking_history
├── Inverted: 1.0 - (rate × 6.67)
└── Lower cancel rate = higher score
    ↓
TOTAL BOOKINGS (20%)
├── total_bookings from booking_history
├── >= 100: 1.0
├── >= 50: 0.8
├── >= 20: 0.6
├── >= 10: 0.4
└── < 10: 0.2
    ↓
VERIFICATION (10%)
├── identity_verified from verification
├── True: 1.0
└── False: 0.5
    ↓
WEIGHTED SUM
├── 0.4 × completion_component
├── 0.3 × cancel_component
├── 0.2 × bookings_component
└── 0.1 × verification_component
```

## 8. SƠ ĐỒ PRICE SCORE CALCULATION

```
PRICE COMPARISON
    ↓
hourly_rate <= budget?
├── ✅ YES (Within Budget)
│   ├── rate < 50% budget? → 1.0
│   └── rate >= 50% budget? → Linear: 1.0 - (ratio - 0.5) × 0.2
└── ❌ NO (Over Budget)
    ├── excess_ratio = (rate - budget) / budget
    ├── penalty = 1.0 - excess_ratio
    └── max(0.0, penalty)
```

## 9. SƠ ĐỒ DISTANCE SCORE CALCULATION

```
DISTANCE CALCULATION
    ↓
HAVERSINE FORMULA
├── Calculate distance in km
└── Exponential decay: e^(-distance/8.0)
    ↓
SCORE MAPPING
├── 0km → 1.0
├── 8km → 0.37
├── 16km → 0.14
└── 24km → 0.05
```

## 10. SƠ ĐỒ CREDENTIAL SCORE CALCULATION

```
CREDENTIAL ANALYSIS
    ↓
VALIDATE CREDENTIALS
├── status = 'verified'
├── Check expiry_date for certificates
└── Filter valid credentials
    ↓
DEGREE BONUS (1-4 points)
├── Find highest degree level
├── applicable_levels max value
└── Direct bonus points
    ↓
CERTIFICATE BONUS (0.5 points each)
├── Count certificates meeting required_level
├── Max 12 certificates (6 points total)
└── 0.5 points per valid certificate
    ↓
NORMALIZE
├── Max possible: 4 (degree) + 6 (certs) = 10
└── Final score: total_points / 10.0
```

## 11. SƠ ĐỒ SKILLS SCORE CALCULATION

```
PRIORITY SKILLS MATCHING
    ↓
FOR EACH PRIORITY SKILL:
├── Use PhoBERT semantic matching
├── Find best match with caregiver skills
├── Similarity >= 0.8 threshold?
│   ├── ✅ YES → Count as matched
│   └── ❌ NO → Skip skill
└── Check credential mapping bonus
    ↓
BASE SCORE CALCULATION
├── base_score = matched_count / total_priority_skills
└── Handle empty priority_skills → return 1.0
    ↓
CREDENTIAL BONUS
├── Count matched skills with credential_id
├── credential_ratio = skills_with_credentials / matched_count
├── bonus = credential_ratio × 0.2
└── Final score = min(1.0, base_score + bonus)
```

## 12. SƠ ĐỒ EXPERIENCE SCORE CALCULATION

```
EXPERIENCE SCORING
    ↓
YEARS EXPERIENCE INPUT
    ↓
FORMULA: min(1.0, max(0.1, years_experience / 10.0))
    ↓
SCORE MAPPING
├── 0 years → 0.1 (minimum)
├── 1 year → 0.2
├── 5 years → 0.6
├── 10 years → 1.0 (maximum)
└── > 10 years → 1.0 (capped)
```

## 13. SƠ ĐỒ FINAL RANKING

```
ALL CAREGIVERS SCORED
    ↓
SORT BY TOTAL SCORE (DESCENDING)
    ↓
APPLY TOP N LIMIT
    ↓
FORMAT RESULTS
├── caregiver: original data
├── total_score: weighted sum
├── breakdown: individual scores
├── distance_km: calculated distance
└── radius_multiplier: fallback info
    ↓
RETURN RANKED LIST
```

## 14. SƠ ĐỒ NORMALIZATION PROCESS

```
VIETNAMESE SKILLS NORMALIZATION
    ↓
REQUEST SKILLS
├── required_skills: normalize each skill
├── priority_skills: normalize each skill
└── Apply Vietnamese → English mapping
    ↓
CAREGIVER SKILLS
├── For each caregiver
├── skills array: normalize each skill
└── Apply Vietnamese → English mapping
    ↓
NORMALIZATION STEPS
├── Remove diacritics (ă → a, ế → e)
├── Convert to lowercase
├── Trim whitespace
├── Apply skill mapping dictionary
└── Clean multiple spaces
```

## 15. SƠ ĐỒ TIME OVERLAP CHECK

```
TIME AVAILABILITY CHECK
    ↓
REQUEST TIME SLOTS
├── Format: [{"day": "monday", "start": "08:00", "end": "12:00"}]
└── Convert to time objects
    ↓
CAREGIVER SCHEDULE
├── Format: {"monday": [{"start": "08:00", "end": "17:00"}]}
└── Convert to time objects
    ↓
OVERLAP CHECK
├── FOR EACH request slot:
│   ├── Find matching day in caregiver schedule
│   ├── Check if slot overlaps with any caregiver slot
│   ├── ✅ OVERLAP → Continue to next request slot
│   └── ❌ NO OVERLAP → FAIL (return False)
└── ALL SLOTS OVERLAP → PASS (return True)
```

## 16. SƠ ĐỒ DYNAMIC CARE LEVEL CALCULATION

```
DYNAMIC MAX CARE LEVEL
    ↓
CAREGIVER CREDENTIALS
    ↓
FOR EACH CREDENTIAL:
├── status = 'verified'?
├── ├── ✅ YES → Continue
│   └── ❌ NO → Skip credential
├── type = 'certificate'?
├── ├── ✅ YES → Check expiry_date
│   │   ├── Not expired? → Continue
│   │   └── Expired? → Skip credential
│   └── ❌ NO → Continue
├── Get applicable_levels array
├── Find max level in array
└── Update global max_level
    ↓
RETURN MAX CARE LEVEL
```

## 17. SƠ ĐỒ CREDENTIAL QUALITY SCORE

```
CREDENTIAL QUALITY CALCULATION
    ↓
REQUIRED LEVEL INPUT
    ↓
FOR EACH VALID CREDENTIAL:
├── Get applicable_levels array
├── Count levels >= required_level
├── Calculate ratio: levels_achieved / total_levels
├── Add ratio to quality_score
└── Continue to next credential
    ↓
NORMALIZE QUALITY SCORE
├── Divide by 5 (max expected credentials)
├── Cap at 1.0
└── Return normalized score
```

## 18. SƠ ĐỒ SCHEDULE CONVERSION

```
SCHEDULE FORMAT CONVERSION
    ↓
INPUT CHECK
├── Already dict format? → Return as-is
└── Array format? → Convert to dict
    ↓
ARRAY TO DICT CONVERSION
├── FOR EACH day_entry:
│   ├── Extract 'day' field
│   ├── Extract 'slots' array
│   └── result[day] = slots
└── Return converted dict
    ↓
OUTPUT FORMAT
└── {"monday": [...], "tuesday": [...], ...}
```

## 19. SƠ ĐỒ MAIN MATCHING FLOW

```
MAIN MATCHING FUNCTION
    ↓
INPUT VALIDATION
├── care_request: Dict
├── caregivers: List[Dict]
└── top_n: int (default 10)
    ↓
SKILLS NORMALIZATION
├── normalize_request_skills(care_request)
└── normalize_caregiver_skills(caregivers)
    ↓
FALLBACK LOOP
├── multipliers = [1.0, 1.5, 2.0]
├── FOR EACH multiplier:
│   ├── results = []
│   ├── FOR EACH caregiver:
│   │   ├── score_result = _score_candidate(req, cg, multiplier)
│   │   ├── score_result != None?
│   │   │   ├── ✅ YES → Add to results
│   │   │   └── ❌ NO → Skip caregiver
│   │   └── Continue to next caregiver
│   ├── results not empty?
│   │   ├── ✅ YES → Sort & Return TOP N
│   │   └── ❌ NO → Try next multiplier
│   └── Continue to next multiplier
└── No results found → Return empty list
```

## 20. SƠ ĐỒ ERROR HANDLING

```
ERROR HANDLING STRATEGY
    ↓
INPUT VALIDATION ERRORS
├── Missing required fields → Return empty list
├── Invalid data types → Return empty list
└── Malformed requests → Return empty list
    ↓
CALCULATION ERRORS
├── Division by zero → Use default values
├── Invalid coordinates → Skip distance calculation
├── Missing credentials → Use default care level
└── Invalid time formats → Skip time overlap
    ↓
SEMANTIC MATCHING ERRORS
├── PhoBERT model errors → Use exact string matching
├── Unicode errors → Skip normalization
├── Empty skill lists → Return default scores
└── Network timeouts → Use cached results
    ↓
GRACEFUL DEGRADATION
├── Partial results → Return available matches
├── Fallback algorithms → Use simpler matching
├── Default scores → Ensure system continues
└── Error logging → Track issues for debugging
```

## 21. SƠ ĐỒ PERFORMANCE OPTIMIZATION

```
PERFORMANCE OPTIMIZATIONS
    ↓
EARLY TERMINATION
├── Hard filters fail → Skip soft scoring
├── Distance too far → Skip remaining filters
├── No time overlap → Skip caregiver entirely
└── Missing required skills → Skip caregiver
    ↓
CACHING STRATEGIES
├── PhoBERT embeddings → Cache similarity results
├── Distance calculations → Cache haversine results
├── Credential validation → Cache verification status
└── Schedule parsing → Cache time overlap results
    ↓
BATCH PROCESSING
├── Process multiple caregivers in parallel
├── Batch semantic matching requests
├── Vectorized distance calculations
└── Bulk credential validation
    ↓
MEMORY OPTIMIZATION
├── Lazy loading of caregiver data
├── Stream processing for large datasets
├── Garbage collection of intermediate results
└── Memory-mapped file access for large JSON
```

## 22. SƠ ĐỒ TESTING STRATEGY

```
TESTING APPROACH
    ↓
UNIT TESTS
├── Individual filter functions
├── Scoring calculations
├── Normalization functions
└── Edge case handling
    ↓
INTEGRATION TESTS
├── End-to-end matching flow
├── Fallback strategy validation
├── Performance benchmarks
└── Memory usage monitoring
    ↓
VALIDATION TESTS
├── Vietnamese skill matching accuracy
├── PhoBERT threshold optimization
├── Real-world scenario testing
└── User acceptance testing
    ↓
DEBUGGING TOOLS
├── Detailed logging of filter results
├── Score breakdown visualization
├── Performance profiling
└── Error tracking and reporting
```

## 23. SƠ ĐỒ CONFIGURATION MANAGEMENT

```
CONFIGURATION PARAMETERS
    ↓
SCORING WEIGHTS
├── credential: 0.30 (30%)
├── skills: 0.25 (25%)
├── distance: 0.15 (15%)
├── rating: 0.12 (12%)
├── experience: 0.08 (8%)
├── price: 0.08 (8%)
└── trust: 0.02 (2%)
    ↓
THRESHOLDS
├── PhoBERT similarity: 0.8 (80%)
├── Distance decay scale: 8.0 km
├── Bayesian confidence: 25
├── Platform mean rating: 3.5
└── Min experience score: 0.1
    ↓
FALLBACK MULTIPLIERS
├── Round 1: 1.0 (original radius)
├── Round 2: 1.5 (50% expansion)
└── Round 3: 2.0 (100% expansion)
    ↓
LIMITS AND BOUNDS
├── Max credentials: 5
├── Max certificates: 12
├── Max distance score: 1.0
├── Min trust score: 0.0
└── Max price penalty: 1.0
```

## 24. SƠ ĐỒ MONITORING AND METRICS

```
MONITORING METRICS
    ↓
PERFORMANCE METRICS
├── Average matching time
├── Memory usage per request
├── PhoBERT inference time
├── Database query time
└── Cache hit rates
    ↓
QUALITY METRICS
├── Match accuracy rate
├── False positive rate
├── User satisfaction scores
├── Booking conversion rates
└── Skill matching precision
    ↓
BUSINESS METRICS
├── Total matches per day
├── Average match score
├── Fallback usage frequency
├── Top matching criteria
└── Geographic distribution
    ↓
ERROR METRICS
├── Filter failure rates
├── Semantic matching errors
├── Timeout occurrences
├── Memory overflow events
└── Invalid data incidents
```

## 25. SƠ ĐỒ DEPLOYMENT ARCHITECTURE

```
DEPLOYMENT ARCHITECTURE
    ↓
API LAYER
├── FastAPI application
├── Request validation
├── Response formatting
└── Error handling
    ↓
BUSINESS LOGIC LAYER
├── RuleBasedMatcher class
├── Hard filters implementation
├── Soft scoring algorithms
└── Fallback strategies
    ↓
ALGORITHM LAYER
├── PhoBERT semantic matcher
├── Haversine distance calculator
├── Time overlap checker
└── Credential validator
    ↓
DATA LAYER
├── Caregiver JSON database
├── Skills mapping dictionary
├── Configuration parameters
└── Cache storage
    ↓
INFRASTRUCTURE LAYER
├── Python runtime environment
├── Hugging Face transformers
├── NumPy for calculations
└── System monitoring tools
```

## 26. SƠ ĐỒ SCALABILITY CONSIDERATIONS

```
SCALABILITY STRATEGIES
    ↓
HORIZONTAL SCALING
├── Load balancing across instances
├── Stateless service design
├── Shared cache layer
└── Database sharding
    ↓
VERTICAL SCALING
├── CPU optimization for calculations
├── Memory optimization for large datasets
├── GPU acceleration for PhoBERT
└── SSD storage for faster I/O
    ↓
CACHING STRATEGIES
├── Redis for session data
├── Memcached for calculation results
├── CDN for static resources
└── Application-level caching
    ↓
ASYNC PROCESSING
├── Background job processing
├── Queue-based task distribution
├── Event-driven architecture
└── Real-time updates
    ↓
DATABASE OPTIMIZATION
├── Indexing on frequently queried fields
├── Query optimization
├── Connection pooling
└── Read replicas for scaling
```

## 27. SƠ ĐỒ SECURITY CONSIDERATIONS

```
SECURITY MEASURES
    ↓
INPUT VALIDATION
├── Sanitize user inputs
├── Validate data types
├── Check for SQL injection
└── Prevent XSS attacks
    ↓
AUTHENTICATION
├── API key validation
├── Rate limiting
├── Request signing
└── Session management
    ↓
DATA PROTECTION
├── Encrypt sensitive data
├── Anonymize personal information
├── Secure data transmission
└── Access control lists
    ↓
AUDIT LOGGING
├── Track all API calls
├── Log security events
├── Monitor suspicious activity
└── Compliance reporting
    ↓
ERROR HANDLING
├── Don't expose internal details
├── Generic error messages
├── Secure error logging
└── Graceful degradation
```

## 28. SƠ ĐỒ MAINTENANCE AND UPDATES

```
MAINTENANCE WORKFLOW
    ↓
REGULAR UPDATES
├── PhoBERT model updates
├── Skills mapping updates
├── Configuration tuning
└── Performance optimizations
    ↓
MONITORING
├── Health checks
├── Performance metrics
├── Error tracking
└── User feedback
    ↓
TESTING
├── Automated test suites
├── Integration testing
├── Performance testing
└── User acceptance testing
    ↓
DEPLOYMENT
├── Blue-green deployment
├── Canary releases
├── Rollback procedures
└── Feature flags
    ↓
DOCUMENTATION
├── API documentation
├── Algorithm documentation
├── Configuration guides
└── Troubleshooting guides
```

## 29. SƠ ĐỒ TROUBLESHOOTING GUIDE

```
COMMON ISSUES AND SOLUTIONS
    ↓
NO MATCHES FOUND
├── Check fallback multipliers
├── Verify hard filter logic
├── Review skill normalization
└── Check data quality
    ↓
LOW MATCH SCORES
├── Adjust scoring weights
├── Review threshold values
├── Check credential validation
└── Verify skill matching
    ↓
PERFORMANCE ISSUES
├── Optimize PhoBERT usage
├── Implement caching
├── Reduce data processing
└── Scale infrastructure
    ↓
ACCURACY ISSUES
├── Tune PhoBERT threshold
├── Update skills mapping
├── Review filter logic
└── Validate test cases
    ↓
MEMORY ISSUES
├── Optimize data structures
├── Implement lazy loading
├── Reduce batch sizes
└── Monitor memory usage
```

## 30. SƠ ĐỒ FUTURE ENHANCEMENTS

```
FUTURE IMPROVEMENTS
    ↓
ALGORITHM ENHANCEMENTS
├── Machine learning integration
├── User preference learning
├── Dynamic weight adjustment
└── Advanced semantic matching
    ↓
FEATURE ADDITIONS
├── Multi-language support
├── Advanced filtering options
├── Real-time matching
└── Predictive analytics
    ↓
PERFORMANCE IMPROVEMENTS
├── GPU acceleration
├── Distributed processing
├── Advanced caching
└── Database optimization
    ↓
USER EXPERIENCE
├── Personalized recommendations
├── Interactive filtering
├── Visual match explanations
└── Feedback integration
    ↓
INTEGRATION EXPANSION
├── Third-party APIs
├── External data sources
├── Mobile app optimization
└── Web dashboard
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
