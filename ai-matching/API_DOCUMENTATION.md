# API Documentation - Elder Care Connect

Tài liệu chi tiết về các API endpoints của hệ thống Elder Care Connect.

## 🌐 Base URL

```
http://localhost:8000
```

Hoặc từ mobile device:

```
http://192.168.2.224:8000
```

## 📋 Endpoints Overview

| Method | Endpoint            | Description               |
| ------ | ------------------- | ------------------------- |
| GET    | `/health`           | Health check              |
| POST   | `/api/match`        | Match caregivers (Web)    |
| POST   | `/api/match-mobile` | Match caregivers (Mobile) |

## 🔍 API Details

### 1. Health Check

**Endpoint:** `GET /health`

**Description:** Kiểm tra trạng thái server

**Response:**

```json
{
  "status": "healthy",
  "timestamp": "2025-01-09T15:30:00Z",
  "version": "1.0.0"
}
```

---

### 2. Match Caregivers (Web)

**Endpoint:** `POST /api/match`

**Description:** Tìm kiếm caregivers phù hợp cho request từ web interface

**Request Body:**

```json
{
  "request_id": "req_001"
}
```

**Request Schema:**

```json
{
  "type": "object",
  "properties": {
    "request_id": {
      "type": "string",
      "description": "ID của request trong requests.json",
      "example": "req_001"
    }
  },
  "required": ["request_id"]
}
```

**Response Schema:**

```json
{
  "type": "object",
  "properties": {
    "request_id": {
      "type": "string",
      "description": "ID của request"
    },
    "care_level": {
      "type": "integer",
      "description": "Mức độ chăm sóc (1-4)",
      "minimum": 1,
      "maximum": 4
    },
    "seeker_name": {
      "type": "string",
      "description": "Tên người tìm kiếm"
    },
    "location": {
      "type": "object",
      "properties": {
        "address": { "type": "string" },
        "lat": { "type": "number" },
        "lon": { "type": "number" }
      }
    },
    "total_matches": {
      "type": "integer",
      "description": "Tổng số caregivers phù hợp"
    },
    "recommendations": {
      "type": "array",
      "items": {
        "$ref": "#/components/schemas/CaregiverRecommendation"
      }
    }
  }
}
```

**Example Response:**

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

---

### 3. Match Caregivers (Mobile)

**Endpoint:** `POST /api/match-mobile`

**Description:** Tìm kiếm caregivers cho mobile application

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

**Request Schema:**

```json
{
  "type": "object",
  "properties": {
    "seeker_name": {
      "type": "string",
      "description": "Tên người tìm kiếm"
    },
    "care_level": {
      "type": "integer",
      "description": "Mức độ chăm sóc (1-4)",
      "minimum": 1,
      "maximum": 4
    },
    "health_status": {
      "type": "string",
      "enum": ["good", "moderate", "weak"],
      "description": "Tình trạng sức khỏe"
    },
    "elderly_age": {
      "type": "integer",
      "description": "Tuổi người già",
      "minimum": 50,
      "maximum": 100
    },
    "caregiver_age_range": {
      "type": "array",
      "items": { "type": "integer" },
      "minItems": 2,
      "maxItems": 2,
      "description": "Khoảng tuổi caregiver [min, max]"
    },
    "gender_preference": {
      "type": "string",
      "enum": ["male", "female", null],
      "description": "Giới tính ưu tiên"
    },
    "required_years_experience": {
      "type": "integer",
      "minimum": 0,
      "description": "Số năm kinh nghiệm yêu cầu"
    },
    "overall_rating_range": {
      "type": "array",
      "items": { "type": "number" },
      "minItems": 2,
      "maxItems": 2,
      "description": "Khoảng điểm đánh giá [min, max]"
    },
    "personality": {
      "type": "array",
      "items": { "type": "string" },
      "description": "Danh sách tính cách ưu tiên"
    },
    "attitude": {
      "type": "array",
      "items": { "type": "string" },
      "description": "Danh sách thái độ ưu tiên"
    },
    "skills": {
      "type": "object",
      "properties": {
        "priority_skills": {
          "type": "array",
          "items": { "type": "string" },
          "description": "Kỹ năng ưu tiên"
        },
        "required_skills": {
          "type": "array",
          "items": { "type": "string" },
          "description": "Kỹ năng bắt buộc"
        }
      },
      "required": ["priority_skills", "required_skills"]
    },
    "time_slots": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "day": {
            "type": "string",
            "enum": [
              "monday",
              "tuesday",
              "wednesday",
              "thursday",
              "friday",
              "saturday",
              "sunday"
            ]
          },
          "start": {
            "type": "string",
            "pattern": "^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$",
            "description": "Thời gian bắt đầu (HH:MM)"
          },
          "end": {
            "type": "string",
            "pattern": "^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$",
            "description": "Thời gian kết thúc (HH:MM)"
          }
        },
        "required": ["day", "start", "end"]
      },
      "description": "Danh sách khung giờ làm việc"
    },
    "location": {
      "type": "object",
      "properties": {
        "address": { "type": "string" },
        "lat": { "type": "number" },
        "lon": { "type": "number" }
      },
      "required": ["address", "lat", "lon"]
    },
    "budget_per_hour": {
      "type": "integer",
      "minimum": 0,
      "description": "Ngân sách mỗi giờ (VND)"
    },
    "top_n": {
      "type": "integer",
      "minimum": 1,
      "maximum": 50,
      "default": 10,
      "description": "Số lượng kết quả tối đa"
    }
  },
  "required": [
    "seeker_name",
    "care_level",
    "health_status",
    "elderly_age",
    "caregiver_age_range",
    "required_years_experience",
    "overall_rating_range",
    "personality",
    "attitude",
    "skills",
    "time_slots",
    "location",
    "budget_per_hour"
  ]
}
```

**Response:** Giống như `/api/match` nhưng được tối ưu cho mobile app.

---

## 📊 Data Models

### CaregiverRecommendation

```json
{
  "type": "object",
  "properties": {
    "rank": {
      "type": "integer",
      "description": "Thứ hạng"
    },
    "caregiver_id": {
      "type": "string",
      "description": "ID của caregiver"
    },
    "name": {
      "type": "string",
      "description": "Tên caregiver"
    },
    "age": {
      "type": "integer",
      "description": "Tuổi"
    },
    "gender": {
      "type": "string",
      "enum": ["male", "female"],
      "description": "Giới tính"
    },
    "rating": {
      "type": "number",
      "minimum": 0,
      "maximum": 5,
      "description": "Điểm đánh giá"
    },
    "total_reviews": {
      "type": "integer",
      "minimum": 0,
      "description": "Tổng số đánh giá"
    },
    "years_experience": {
      "type": "integer",
      "minimum": 0,
      "description": "Số năm kinh nghiệm"
    },
    "price_per_hour": {
      "type": "integer",
      "minimum": 0,
      "description": "Giá mỗi giờ (VND)"
    },
    "distance_km": {
      "type": "number",
      "minimum": 0,
      "description": "Khoảng cách (km)"
    },
    "distance": {
      "type": "string",
      "description": "Khoảng cách định dạng (VD: '2.5 km')"
    },
    "avatar": {
      "type": "string",
      "format": "uri",
      "description": "URL avatar"
    },
    "experience": {
      "type": "string",
      "description": "Mô tả kinh nghiệm"
    },
    "isVerified": {
      "type": "boolean",
      "description": "Trạng thái xác thực"
    },
    "match_score": {
      "type": "number",
      "minimum": 0,
      "maximum": 1,
      "description": "Điểm phù hợp tổng thể"
    },
    "match_percentage": {
      "type": "string",
      "description": "Phần trăm phù hợp (VD: '76%')"
    },
    "score_breakdown": {
      "$ref": "#/components/schemas/ScoreBreakdown"
    }
  }
}
```

### ScoreBreakdown

```json
{
  "type": "object",
  "properties": {
    "credential": {
      "type": "number",
      "minimum": 0,
      "maximum": 1,
      "description": "Điểm bằng cấp/chứng chỉ"
    },
    "skills": {
      "type": "number",
      "minimum": 0,
      "maximum": 1,
      "description": "Điểm kỹ năng"
    },
    "distance": {
      "type": "number",
      "minimum": 0,
      "maximum": 1,
      "description": "Điểm khoảng cách"
    },
    "time": {
      "type": "number",
      "minimum": 0,
      "maximum": 1,
      "description": "Điểm thời gian"
    },
    "rating": {
      "type": "number",
      "minimum": 0,
      "maximum": 1,
      "description": "Điểm đánh giá"
    },
    "experience": {
      "type": "number",
      "minimum": 0,
      "maximum": 1,
      "description": "Điểm kinh nghiệm"
    },
    "price": {
      "type": "number",
      "minimum": 0,
      "maximum": 1,
      "description": "Điểm giá cả"
    },
    "trust": {
      "type": "number",
      "minimum": 0,
      "maximum": 1,
      "description": "Điểm độ tin cậy"
    }
  }
}
```

## 🚨 Error Responses

### 400 Bad Request

```json
{
  "detail": "Validation error: field 'care_level' must be between 1 and 4"
}
```

### 404 Not Found

```json
{
  "detail": "Request with ID 'req_999' not found"
}
```

### 500 Internal Server Error

```json
{
  "detail": "Internal server error: Unable to process matching request"
}
```

## 🧪 Testing

### cURL Examples

**Health Check:**

```bash
curl http://localhost:8000/health
```

**Match API:**

```bash
curl -X POST "http://localhost:8000/api/match" \
     -H "Content-Type: application/json" \
     -d '{"request_id": "req_001"}'
```

**Mobile Match API:**

```bash
curl -X POST "http://localhost:8000/api/match-mobile" \
     -H "Content-Type: application/json" \
     -d '{
       "seeker_name": "Test User",
       "care_level": 2,
       "health_status": "good",
       "elderly_age": 70,
       "caregiver_age_range": [25, 60],
       "gender_preference": null,
       "required_years_experience": 2,
       "overall_rating_range": [3.0, 5.0],
       "personality": [],
       "attitude": [],
       "skills": {
         "priority_skills": ["nấu ăn"],
         "required_skills": ["hỗ trợ vệ sinh"]
       },
       "time_slots": [
         {"day": "monday", "start": "09:00", "end": "17:00"}
       ],
       "location": {
         "address": "Quận 1, TP.HCM",
         "lat": 10.7769,
         "lon": 106.7009
       },
       "budget_per_hour": 80000,
       "top_n": 5
     }'
```

### JavaScript Examples

**Fetch API:**

```javascript
// Health check
const healthResponse = await fetch("http://localhost:8000/health");
const healthData = await healthResponse.json();

// Match API
const matchResponse = await fetch("http://localhost:8000/api/match", {
  method: "POST",
  headers: {
    "Content-Type": "application/json",
  },
  body: JSON.stringify({
    request_id: "req_001",
  }),
});
const matchData = await matchResponse.json();
```

**Axios:**

```javascript
import axios from "axios";

// Health check
const healthData = await axios.get("http://localhost:8000/health");

// Match API
const matchData = await axios.post("http://localhost:8000/api/match", {
  request_id: "req_001",
});
```

## 📱 Mobile Integration

### React Native Example

```javascript
import axios from "axios";

const apiClient = axios.create({
  baseURL: "http://192.168.2.224:8000",
  timeout: 30000,
  headers: {
    "Content-Type": "application/json",
  },
});

const matchCaregivers = async (requestData) => {
  try {
    const response = await apiClient.post("/api/match-mobile", requestData);
    return response.data;
  } catch (error) {
    console.error("API Error:", error.response?.data || error.message);
    throw error;
  }
};
```

## 🔒 Security

- **CORS**: Đã được cấu hình để cho phép tất cả origins
- **Input Validation**: Sử dụng Pydantic để validate input
- **Error Handling**: Comprehensive error handling và logging
- **Rate Limiting**: Chưa implement (TODO)

## 📈 Performance

- **Response Time**: < 500ms cho requests thông thường
- **Concurrent Requests**: Hỗ trợ multiple concurrent requests
- **Memory Usage**: Optimized cho large datasets
- **Caching**: Chưa implement (TODO)

## 🔄 Versioning

- **Current Version**: 1.0.0
- **API Versioning**: Chưa implement (TODO)
- **Backward Compatibility**: Maintained trong cùng major version
