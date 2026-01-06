# API Hướng Dẫn - Caregiver Schedule (Lịch Rảnh)

Tài liệu hướng dẫn gọi API quản lý lịch rảnh cho Caregiver trên Mobile App.

## 📋 Mục Lục

- [Thông Tin Chung](#thông-tin-chung)
- [Authentication](#authentication)
- [API Endpoints](#api-endpoints)
  - [1. Cập Nhật Lịch Rảnh](#1-cập-nhật-lịch-rảnh)
  - [2. Lấy Lịch Rảnh](#2-lấy-lịch-rảnh)
- [Ví Dụ Code](#ví-dụ-code)
- [Xử Lý Lỗi](#xử-lý-lỗi)

---

## 🔧 Thông Tin Chung

**Base URL:** `https://your-api-domain.com/api/v1/caregiver-schedule`

**Content-Type:** `application/json`

**Yêu Cầu:** Tất cả API yêu cầu role `CAREGIVER` và phải đăng nhập (có JWT token)

---

## 🔐 Authentication

Tất cả API yêu cầu JWT token trong header:

```
Authorization: Bearer <your_jwt_token>
```

Token được lấy từ API đăng nhập. Token sẽ tự động xác định caregiver hiện tại.

---

## 📡 API Endpoints

### 1. Cập Nhật Lịch Rảnh

Cập nhật lịch rảnh của caregiver hiện tại.

**Endpoint:** `PUT /api/v1/caregiver-schedule/free-schedule`

**Headers:**
```
Authorization: Bearer <token>
Content-Type: application/json
```

#### Request Body

Có 2 cách cập nhật lịch rảnh:

##### Cách 1: Rảnh Toàn Thời Gian

```json
{
  "free_schedule": {
    "available_all_time": true
  }
}
```

##### Cách 2: Chỉ Định Các Khung Giờ Bận

```json
{
  "free_schedule": {
    "available_all_time": false,
    "booked_slots": [
      {
        "date": "2025-12-01",
        "start_time": "09:00",
        "end_time": "12:00"
      },
      {
        "date": "2025-12-01",
        "start_time": "14:00",
        "end_time": "17:00"
      },
      {
        "date": "2025-12-02",
        "start_time": "08:00",
        "end_time": "10:00"
      }
    ]
  }
}
```

**Request Fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `free_schedule` | Object | ✅ | Object chứa thông tin lịch rảnh |
| `free_schedule.available_all_time` | Boolean | ✅ | `true` = rảnh toàn thời gian, `false` = có khung giờ bận |
| `free_schedule.booked_slots` | Array | ⚠️ | Chỉ cần khi `available_all_time = false` |
| `booked_slots[].date` | String | ✅ | Ngày theo format `yyyy-MM-dd` (VD: "2025-12-01") |
| `booked_slots[].start_time` | String | ✅ | Giờ bắt đầu theo format `HH:mm` (VD: "09:00") |
| `booked_slots[].end_time` | String | ✅ | Giờ kết thúc theo format `HH:mm` (VD: "12:00") |

**Lưu ý:**
- Nếu `available_all_time = true`, không cần gửi `booked_slots`
- Nếu `available_all_time = false`, bắt buộc phải có `booked_slots` (có thể là mảng rỗng `[]`)
- Format date: `yyyy-MM-dd` (VD: "2025-12-25")
- Format time: `HH:mm` (24h format, VD: "09:00", "14:30")

#### Response Success (200 OK)

```json
{
  "status": "Success",
  "message": "Cập nhật lịch rảnh thành công",
  "data": {
    // CaregiverProfileResponseDTO object
    "caregiverProfileId": "550e8400-e29b-41d4-a716-446655440000",
    "fullName": "Nguyễn Văn A",
    "email": "caregiver@example.com",
    // ... các field khác của caregiver profile
  }
}
```

#### Response Error

**404 Not Found:**
```json
{
  "status": "Fail",
  "message": "Không tìm thấy hồ sơ người chăm sóc",
  "data": null
}
```

**400 Bad Request:**
```json
{
  "status": "Fail",
  "message": "Free schedule data is required",
  "data": null
}
```

---

### 2. Lấy Lịch Rảnh

Lấy lịch rảnh hiện tại của caregiver.

**Endpoint:** `GET /api/v1/caregiver-schedule/free-schedule`

**Headers:**
```
Authorization: Bearer <token>
```

**Không cần request body**

#### Response Success (200 OK)

##### Trường hợp 1: Rảnh Toàn Thời Gian

```json
{
  "status": "Success",
  "message": "Lấy lịch rảnh thành công",
  "data": {
    "available_all_time": true
  }
}
```

##### Trường hợp 2: Có Khung Giờ Bận

```json
{
  "status": "Success",
  "message": "Lấy lịch rảnh thành công",
  "data": {
    "available_all_time": false,
    "booked_slots": [
      {
        "date": "2025-12-01",
        "start_time": "09:00",
        "end_time": "12:00"
      },
      {
        "date": "2025-12-01",
        "start_time": "14:00",
        "end_time": "17:00"
      }
    ]
  }
}
```

#### Response Error

**404 Not Found:**
```json
{
  "status": "Fail",
  "message": "Không tìm thấy hồ sơ người chăm sóc",
  "data": null
}
```

---

## 💻 Ví Dụ Code

### Flutter/Dart

#### 1. Cập Nhật Lịch Rảnh

```dart
import 'package:http/http.dart' as http;
import 'dart:convert';

class CaregiverScheduleService {
  final String baseUrl = 'https://your-api-domain.com/api/v1/caregiver-schedule';
  final String token; // JWT token từ login

  CaregiverScheduleService(this.token);

  // Cập nhật rảnh toàn thời gian
  Future<Map<String, dynamic>> updateAvailableAllTime() async {
    final url = Uri.parse('$baseUrl/free-schedule');
    
    final body = jsonEncode({
      'free_schedule': {
        'available_all_time': true,
      },
    });

    final response = await http.put(
      url,
      headers: {
        'Authorization': 'Bearer $token',
        'Content-Type': 'application/json',
      },
      body: body,
    );

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to update schedule: ${response.body}');
    }
  }

  // Cập nhật với khung giờ bận
  Future<Map<String, dynamic>> updateWithBookedSlots(
    List<Map<String, String>> bookedSlots,
  ) async {
    final url = Uri.parse('$baseUrl/free-schedule');
    
    final body = jsonEncode({
      'free_schedule': {
        'available_all_time': false,
        'booked_slots': bookedSlots,
      },
    });

    final response = await http.put(
      url,
      headers: {
        'Authorization': 'Bearer $token',
        'Content-Type': 'application/json',
      },
      body: body,
    );

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to update schedule: ${response.body}');
    }
  }

  // Ví dụ sử dụng
  Future<void> example() async {
    // Cách 1: Rảnh toàn thời gian
    await updateAvailableAllTime();

    // Cách 2: Có khung giờ bận
    final bookedSlots = [
      {
        'date': '2025-12-01',
        'start_time': '09:00',
        'end_time': '12:00',
      },
      {
        'date': '2025-12-01',
        'start_time': '14:00',
        'end_time': '17:00',
      },
    ];
    await updateWithBookedSlots(bookedSlots);
  }
}
```

#### 2. Lấy Lịch Rảnh

```dart
Future<Map<String, dynamic>> getFreeSchedule() async {
  final url = Uri.parse('$baseUrl/free-schedule');
  
  final response = await http.get(
    url,
    headers: {
      'Authorization': 'Bearer $token',
    },
  );

  if (response.statusCode == 200) {
    final data = jsonDecode(response.body);
    return data['data']; // Trả về free_schedule object
  } else {
    throw Exception('Failed to get schedule: ${response.body}');
  }
}
```

### React Native / JavaScript

```javascript
const API_BASE_URL = 'https://your-api-domain.com/api/v1/caregiver-schedule';

// Cập nhật lịch rảnh
const updateFreeSchedule = async (token, scheduleData) => {
  try {
    const response = await fetch(`${API_BASE_URL}/free-schedule`, {
      method: 'PUT',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        free_schedule: scheduleData,
      }),
    });

    const data = await response.json();
    
    if (response.ok) {
      return data;
    } else {
      throw new Error(data.message || 'Failed to update schedule');
    }
  } catch (error) {
    console.error('Error updating schedule:', error);
    throw error;
  }
};

// Lấy lịch rảnh
const getFreeSchedule = async (token) => {
  try {
    const response = await fetch(`${API_BASE_URL}/free-schedule`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`,
      },
    });

    const data = await response.json();
    
    if (response.ok) {
      return data.data; // Trả về free_schedule object
    } else {
      throw new Error(data.message || 'Failed to get schedule');
    }
  } catch (error) {
    console.error('Error getting schedule:', error);
    throw error;
  }
};

// Ví dụ sử dụng
// 1. Rảnh toàn thời gian
await updateFreeSchedule(token, {
  available_all_time: true,
});

// 2. Có khung giờ bận
await updateFreeSchedule(token, {
  available_all_time: false,
  booked_slots: [
    {
      date: '2025-12-01',
      start_time: '09:00',
      end_time: '12:00',
    },
    {
      date: '2025-12-01',
      start_time: '14:00',
      end_time: '17:00',
    },
  ],
});

// 3. Lấy lịch rảnh
const schedule = await getFreeSchedule(token);
console.log('Current schedule:', schedule);
```

---

## ⚠️ Xử Lý Lỗi

### Các Mã Lỗi Thường Gặp

| Status Code | Mô Tả | Cách Xử Lý |
|-------------|-------|------------|
| **200** | Thành công | Xử lý response data |
| **400** | Bad Request | Kiểm tra lại format request body, các field required |
| **401** | Unauthorized | Token hết hạn hoặc không hợp lệ, cần đăng nhập lại |
| **403** | Forbidden | User không có quyền CAREGIVER |
| **404** | Not Found | Không tìm thấy caregiver profile |

### Ví Dụ Xử Lý Lỗi (Flutter)

```dart
try {
  final response = await updateFreeSchedule(scheduleData);
  // Xử lý thành công
  print('Schedule updated successfully');
} on http.ClientException catch (e) {
  // Lỗi kết nối
  print('Connection error: $e');
} catch (e) {
  // Lỗi khác
  final errorData = jsonDecode(e.toString());
  if (errorData['status'] == 'Fail') {
    print('Error: ${errorData['message']}');
  }
}
```

---

## 📝 Lưu Ý Quan Trọng

1. **Format Date & Time:**
   - Date: `yyyy-MM-dd` (VD: "2025-12-25")
   - Time: `HH:mm` (24h format, VD: "09:00", "14:30")

2. **Available All Time:**
   - Khi `available_all_time = true`, hệ thống sẽ bỏ qua `booked_slots`
   - Nên không cần gửi `booked_slots` khi `available_all_time = true`

3. **Booked Slots:**
   - Khi `available_all_time = false`, bắt buộc phải có field `booked_slots`
   - Có thể gửi mảng rỗng `[]` nếu không có khung giờ bận nào
   - Các khung giờ có thể overlap (trùng lặp)

4. **Token:**
   - Token có thời hạn, cần refresh khi hết hạn
   - Lưu token an toàn (SecureStorage/Keychain)

5. **Validation:**
   - `start_time` phải nhỏ hơn `end_time`
   - Date phải là ngày hợp lệ
   - Time phải theo format 24h

---

## 🔗 Liên Kết

- Base URL: `https://your-api-domain.com/api/v1/caregiver-schedule`
- Swagger UI: `https://your-api-domain.com/swagger-ui.html` (nếu có)

---

**Cập nhật lần cuối:** 2025-01-XX


