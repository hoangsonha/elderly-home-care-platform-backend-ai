# Elder Care Connect - Backend API

Hệ thống API backend cho ứng dụng Elder Care Connect - kết nối người già với người chăm sóc.

## 🚀 Cài đặt và Chạy

### 1. Cài đặt Python dependencies

```bash
cd backend
pip install -r requirements.txt
```

### 2. Chạy server

#### **📱 Cho Mobile App Connection (Khuyến nghị)**

```powershell
# Cách 1: Dùng PowerShell script
cd backend
.\start.ps1

# Cách 2: Dùng Batch file
cd backend
start.bat

# Cách 3: Manual (nếu script không work)
cd backend
$env:PYTHONPATH = "D:\CapstoneProject\CapstoneProject\Mobile\capstone-project\backend"
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

#### **🌐 Cho Web Development**

```bash
# Chạy localhost (chỉ cho web)
$env:PYTHONPATH = "D:\CapstoneProject\CapstoneProject\Mobile\capstone-project\backend"
python -m uvicorn app.main:app --host 127.0.0.1 --port 8000
```

### 3. Kiểm tra server

#### **✅ Health Check**

```bash
# Test từ localhost
curl http://localhost:8000/health

# Test từ mobile app IP
curl http://192.168.1.5:8000/health
```

#### **📚 API Documentation**

- **Swagger UI**: http://localhost:8000/docs
- **ReDoc**: http://localhost:8000/redoc
- **Mobile URL**: http://192.168.1.5:8000/docs

#### **📱 Mobile App Configuration**

```typescript
// File: services/apiClient.ts
const apiClient = axios.create({
  baseURL: "http://192.168.1.5:8000", // ✅ IP đúng
  timeout: 30000,
  headers: {
    "Content-Type": "application/json",
  },
});
```

## 📁 Cấu trúc Project

```
backend/
├── app/
│   ├── main.py              # FastAPI app chính
│   ├── api/
│   │   └── match.py         # API endpoints cho matching
│   ├── core/
│   │   └── matcher.py       # Logic matching chính
│   ├── models/
│   │   └── schemas.py       # Pydantic models
│   └── utils/
│       └── __init__.py      # Utility functions
├── debug/
│   ├── debug_matcher_filters.py  # File debug matcher
│   └── README.md                 # Hướng dẫn debug
├── caregivers.json          # Data caregivers
├── requests.json           # Data requests
├── requirements.txt        # Python dependencies
└── start.ps1              # Script khởi động
```

## 🔧 Cấu hình

### Environment Variables

- `PYTHONPATH`: Đường dẫn đến thư mục backend
- `HOST`: Host để bind server (mặc định: 0.0.0.0)
- `PORT`: Port để chạy server (mặc định: 8000)

### CORS Configuration

Server đã được cấu hình CORS để cho phép:

- Tất cả origins (`*`)
- Tất cả methods (`*`)
- Tất cả headers (`*`)
- Credentials: `True`

## 📊 API Endpoints

### 1. Health Check

```
GET /health
```

Kiểm tra trạng thái server.

### 2. Match Caregivers (Web)

```
POST /api/match
```

Tìm kiếm caregivers phù hợp cho request.

**Request Body:**

```json
{
  "request_id": "req_001"
}
```

**Response:**

```json
{
  "request_id": "req_001",
  "care_level": 3,
  "seeker_name": "Trần Văn A",
  "location": {
    "address": "Quận 7, TP.HCM",
    "lat": 10.735,
    "lon": 106.72
  },
  "total_matches": 1,
  "recommendations": [
    {
      "rank": 1,
      "caregiver_id": "cg_001",
      "name": "Nguyễn Thị Mai",
      "age": 35,
      "gender": "female",
      "rating": 4.8,
      "total_reviews": 62,
      "years_experience": 7,
      "price_per_hour": 85000,
      "distance_km": 0.29,
      "distance": "290m",
      "avatar": "https://ui-avatars.com/api/?name=NT&background=4ECDC4&color=fff&size=120",
      "experience": "7 năm kinh nghiệm",
      "isVerified": false,
      "match_score": 0.767,
      "match_percentage": "76%",
      "score_breakdown": {
        "credential": 0.8,
        "skills": 0.9,
        "distance": 0.95,
        "time": 1.0,
        "rating": 0.96,
        "experience": 0.7,
        "price": 0.8,
        "trust": 0.85
      }
    }
  ]
}
```

### 3. Match Caregivers (Mobile)

```
POST /api/match-mobile
```

Tìm kiếm caregivers cho mobile app.

**Request Body:**

```json
{
  "seeker_name": "Người dùng",
  "care_level": 3,
  "health_status": "weak",
  "elderly_age": 78,
  "caregiver_age_range": [30, 55],
  "gender_preference": null,
  "required_years_experience": 3,
  "overall_rating_range": [3.5, 5.0],
  "personality": [],
  "attitude": [],
  "skills": {
    "priority_skills": ["chăm sóc vết thương", "đo dấu hiệu sinh tồn"],
    "required_skills": [
      "tiêm insulin",
      "đo đường huyết",
      "đái tháo đường",
      "quản lý thuốc"
    ]
  },
  "time_slots": [
    { "day": "monday", "start": "08:00", "end": "12:00" },
    { "day": "wednesday", "start": "08:00", "end": "12:00" },
    { "day": "friday", "start": "08:00", "end": "12:00" }
  ],
  "location": {
    "address": "Quận 7, TP.HCM",
    "lat": 10.735,
    "lon": 106.72
  },
  "budget_per_hour": 110000,
  "top_n": 10
}
```

## 🧪 Testing và Debug

### Debug Matcher

```bash
cd backend/debug
python debug_matcher_filters.py
```

Xem chi tiết trong `backend/debug/README.md`

### Test API

```bash
# Test health check
curl http://localhost:8000/health

# Test match API
curl -X POST "http://localhost:8000/api/match" \
     -H "Content-Type: application/json" \
     -d '{"request_id": "req_001"}'
```

## 🔍 Matching Algorithm

### Hard Filters (Bắt buộc)

1. **Care Level**: Caregiver phải có khả năng chăm sóc >= yêu cầu
2. **Degree Requirement**: Level 3+ yêu cầu có bằng cấp
3. **Distance**: Trong phạm vi phục vụ của caregiver
4. **Time Availability**: Có thời gian trùng với yêu cầu
5. **Gender Preference**: Phù hợp với giới tính ưu tiên
6. **Age Range**: Tuổi caregiver trong khoảng yêu cầu
7. **Health Status**: Tình trạng sức khỏe phù hợp
8. **Elderly Age**: Tuổi người già phù hợp
9. **Experience**: Số năm kinh nghiệm đủ yêu cầu
10. **Rating**: Điểm đánh giá trong khoảng yêu cầu
11. **Skills**: Có đủ kỹ năng yêu cầu

### Soft Scoring (Tính điểm)

- **Credential Score**: Điểm dựa trên bằng cấp/chứng chỉ
- **Skills Score**: Điểm dựa trên kỹ năng
- **Distance Score**: Điểm dựa trên khoảng cách
- **Time Score**: Điểm dựa trên thời gian
- **Rating Score**: Điểm dựa trên đánh giá
- **Experience Score**: Điểm dựa trên kinh nghiệm
- **Price Score**: Điểm dựa trên giá cả
- **Trust Score**: Điểm dựa trên độ tin cậy

## 🚨 Troubleshooting

### **Lỗi thường gặp**

#### **1. Connection refused**

- ✅ Server chưa start
- ✅ IP address sai trong mobile app
- ✅ Firewall block port 8000
- ✅ Server chạy trên `127.0.0.1` thay vì `0.0.0.0`

#### **2. ModuleNotFoundError**

```bash
# Đảm bảo PYTHONPATH được set
$env:PYTHONPATH = "D:\CapstoneProject\CapstoneProject\Mobile\capstone-project\backend"
```

#### **3. Field required 'time'**

- ✅ Đã fix trong code (xóa field time khỏi ScoreBreakdown)
- ✅ Restart server sau khi update code

#### **4. PowerShell script không chạy**

```bash
# Nếu .\start.ps1 không work, dùng:
start.bat

# Hoặc manual:
$env:PYTHONPATH = "D:\CapstoneProject\CapstoneProject\Mobile\capstone-project\backend"
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000
```

#### **5. Port đã được sử dụng**

```bash
# Kill process trên port 8000
netstat -ano | findstr :8000
taskkill /PID <PID> /F

# Hoặc dùng script tự động:
taskkill /f /im python.exe
```

### **📋 Checklist Mobile App**

- [ ] Server start thành công với `--host 0.0.0.0`
- [ ] Health check trả về `200 OK`: `curl http://192.168.1.5:8000/health`
- [ ] Mobile app có thể connect đến `http://192.168.1.5:8000`
- [ ] API `/api/match-mobile` hoạt động
- [ ] Không có lỗi `Field required: time`

## 📝 Logs

Server sẽ log:

- Request/Response details
- Error messages
- Matching results
- Performance metrics

## 🔄 Updates

### Recent Changes

- ✅ CORS configuration cho mobile app
- ✅ Mobile API endpoint (`/api/match-mobile`)
- ✅ Debug tools trong `backend/debug/`
- ✅ Comprehensive error handling
- ✅ Detailed response schemas

### TODO

- [ ] Authentication & Authorization
- [ ] Database integration
- [ ] Real-time notifications
- [ ] Advanced matching algorithms
- [ ] Performance optimization
