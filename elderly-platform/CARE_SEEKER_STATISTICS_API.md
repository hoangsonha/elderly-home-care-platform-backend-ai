# Care Seeker Personal Statistics API Documentation

## Endpoint
```
GET /api/v1/statistics/care-seeker/personal
```

## Description
Lấy thống kê cá nhân của care seeker hiện tại, bao gồm số lượng elderly profiles, care services trong tháng, chi phí đã chi, số lịch hẹn đã hoàn thành, và số dịch vụ đang tiến hành.

## Authentication
- **Required**: Yes
- **Role**: `CARE_SEEKER` only
- **Header**: `Authorization: Bearer <JWT_TOKEN>`

## Request
- **Method**: `GET`
- **Headers**:
  ```
  Authorization: Bearer <JWT_TOKEN>
  Content-Type: application/json
  ```
- **Body**: Không có body
- **Query Parameters**: Không có

## Response

### Success Response (200 OK)
```json
{
  "status": "Success",
  "message": "Care seeker personal statistics retrieved successfully",
  "data": {
    "totalElderlyProfiles": 3,
    "totalCareServicesThisMonth": 8,
    "totalSpendingThisMonth": 2500000.0,
    "totalCompletedBookings": 15,
    "totalInProgressServices": 2
  }
}
```

### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `totalElderlyProfiles` | Long | Tổng số elderly profiles của care seeker (không tính các profile đã bị xóa) |
| `totalCareServicesThisMonth` | Long | Tổng số care services trong tháng hiện tại (dựa trên `workDate`) |
| `totalSpendingThisMonth` | Double | Tổng chi phí đã chi trong tháng hiện tại (từ payments có status `SUCCESS`, dựa trên `paidAt`) |
| `totalCompletedBookings` | Long | Tổng số lịch hẹn đã hoàn thành (care-service có status `COMPLETED`) |
| `totalInProgressServices` | Long | Tổng số care services đang tiến hành (care-service có status `IN_PROGRESS`) |

### Error Response - Profile Not Found (404 Not Found)
```json
{
  "status": "Failed",
  "message": "Care seeker profile not found for current user",
  "data": null
}
```

### Error Response - Bad Request (400 Bad Request)
```json
{
  "status": "Failed",
  "message": "Failed to get care seeker personal statistics: <error_message>",
  "data": null
}
```

### Error Response - Unauthorized (401 Unauthorized)
```json
{
  "status": "Failed",
  "message": "Unauthorized: Access denied",
  "data": null
}
```

### Error Response - Forbidden (403 Forbidden)
```json
{
  "status": "Failed",
  "message": "Forbidden: Only CARE_SEEKER role can access this endpoint",
  "data": null
}
```

## Notes

1. **Authentication**: API yêu cầu JWT token trong header `Authorization: Bearer <token>`
2. **Role-based Access**: Chỉ role `CARE_SEEKER` mới có thể truy cập API này
3. **Month Calculation**: 
   - `totalCareServicesThisMonth`: Dựa trên `workDate` của care service
   - `totalSpendingThisMonth`: Dựa trên `paidAt` của payment (chỉ tính payments có status `SUCCESS`)
4. **Default Values**: 
   - Nếu không có dữ liệu, các giá trị sẽ trả về `0` hoặc `0.0`
   - `totalElderlyProfiles`: Chỉ đếm các elderly profiles chưa bị xóa (`deleted = false`)
5. **Status Types**:
   - `COMPLETED`: Care service đã hoàn thành
   - `IN_PROGRESS`: Care service đang tiến hành
   - `SUCCESS`: Payment đã thanh toán thành công

## Example Usage

### Using cURL
```bash
curl -X GET "https://api.example.com/api/v1/statistics/care-seeker/personal" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json"
```

### Using JavaScript (Fetch API)
```javascript
fetch('https://api.example.com/api/v1/statistics/care-seeker/personal', {
  method: 'GET',
  headers: {
    'Authorization': 'Bearer YOUR_JWT_TOKEN',
    'Content-Type': 'application/json'
  }
})
.then(response => response.json())
.then(data => {
  console.log('Statistics:', data);
  if (data.status === 'Success') {
    const stats = data.data;
    console.log('Total Elderly Profiles:', stats.totalElderlyProfiles);
    console.log('Care Services This Month:', stats.totalCareServicesThisMonth);
    console.log('Spending This Month:', stats.totalSpendingThisMonth);
    console.log('Completed Bookings:', stats.totalCompletedBookings);
    console.log('In Progress Services:', stats.totalInProgressServices);
  }
})
.catch(error => {
  console.error('Error:', error);
});
```

### Using Axios
```javascript
import axios from 'axios';

const getCareSeekerStatistics = async () => {
  try {
    const response = await axios.get(
      'https://api.example.com/api/v1/statistics/care-seeker/personal',
      {
        headers: {
          'Authorization': `Bearer ${YOUR_JWT_TOKEN}`,
          'Content-Type': 'application/json'
        }
      }
    );
    
    if (response.data.status === 'Success') {
      const stats = response.data.data;
      console.log('Statistics:', stats);
      return stats;
    }
  } catch (error) {
    console.error('Error fetching statistics:', error.response?.data || error.message);
    throw error;
  }
};
```

