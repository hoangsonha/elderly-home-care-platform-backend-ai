# Docker Setup Guide

## Tổng quan

Project này sử dụng Docker Compose để chạy 3 services:

- **PostgreSQL Database** (Port 5432)
- **Spring Boot Backend** (Port 8080)
- **AI Matching Service** (Port 8000)

## Cấu trúc

```
backend-ai/
├── docker-compose.yml        # Docker Compose config
├── elderly-platform/         # Spring Boot Backend
│   ├── Dockerfile
│   └── src/
│       └── main/resources/
│           ├── application.yml
│           └── keys/
│               └── key_firebase.json
└── ai-matching/              # ADS Matching Service
    ├── Dockerfile
    └── app/
```

## Quick Start

### 1. Khởi động tất cả services

```bash
docker-compose up -d
```

### 2. Xem logs

```bash
# Tất cả services
docker-compose logs -f

# Chỉ backend
docker-compose logs -f backend

# Chỉ AI matching
docker-compose logs -f ai_matching

# Chỉ database
docker-compose logs -f db
```

### 3. Dừng services

```bash
docker-compose down
```

### 4. Rebuild và restart

```bash
# Rebuild images
docker-compose build --no-cache

# Start lại
docker-compose up -d
```

## Services

### PostgreSQL Database (elderly_db)

- **Port**: 5432
- **Database**: elderly_platform
- **Username**: postgres
- **Password**: 123456789
- **Connection String**: `jdbc:postgresql://db:5432/elderly_platform`

### Spring Boot Backend (elderly_backend)

- **Port**: 8080
- **Environment Variables**:
  - `DB_HOSTNAME`: db
  - `DB_PORT`: 5432
  - `DB_USERNAME`: postgres
  - `DB_PASSWORD`: 123456789
- **Health Check**: http://localhost:8080/actuator/health
- **Swagger UI**: http://localhost:8080/swagger-ui.html

### AI Matching Service (elderly_ai_matching)

- **Export**: 8000
- **Docs**: http://localhost:8000/docs
- **Health Check**: http://localhost:8000/health
- **ReDoc**: http://localhost:8000/redoc

## Database Migration

Spring Boot vetomatically tạo schema khi khởi động lần đầu:

- `spring.jpa.hibernate.ddl-auto=update`
- Schema sẽ được tạo trong database `elderly_platform`

## Volumes

- `postgres_data`: Persistent storage cho PostgreSQL data
- Firebase keys: Mount từ `elderly-platform/src/main/resources/keys/`

## Troubleshooting

### 1. Port đã được sử dụng

```bash
# Kiểm tra process đang dùng port
netstat -ano | findstr :5432
netstat -ano | findstr :8080
netstat -ano | findstr :8000

# Kill process
taskkill /PID <PID> /F
```

### 2. Database connection failed

```bash
# Kiểm tra database đã sẵn sàng
docker-compose logs db

# Restart database
docker-compose restart db
```

### 3. Rebuild một service cụ thể

```bash
# Rebuild backend
docker-compose build --no-cache backend
docker-compose up -d backend

# Rebuild AI matching
docker-compose build --no-cache ai_matching
docker-compose up -d ai_matching
```

### 4. Xóa tất cả và bắt đầu lại

```bash
# Stop và xóa containers, networks
docker-compose down -v

# Rebuild và start
docker-compose up -d --build
```

## Development

### Debug backend

```bash
# Xem real-time logs
docker-compose logs -f backend

# Access container
docker exec -it elderly_backend sh

# Check application logs
docker exec -it elderly_backend cat /app/logs/app.log
```

### Debug AI matching

```bash
# Xem real-time logs
docker-compose logs -f ai_matching

# Access container
docker exec -it elderly_ai_matching bash

# Test API
curl http://localhost:8000/health
```

## Production Considerations

⚠️ **Lưu ý**: Cấu hình hiện tại chỉ dành cho development!

Để deploy production, cần:

1. Thay đổi passwords trong `docker-compose.yml`
2. Sử dụng environment variables cho sensitive data
3. Bật SSL/TLS cho các connections
4. Configure proper firewall rules
5. Setup backup cho database
6. Use managed database service (AWS RDS, Azure Database, etc.)

## Network

Các services giao tiếp qua network `elderly-network`:

- Backend có thể connect tới DB qua hostname `db`
- AI Matching có thể connect tới Backend qua hostname `backend`

## API Documentation

- **Backend**: http://localhost:8080/swagger-ui.html
- **AI Matching**: http://localhost:8000/docs

## Contacts

Để được hỗ trợ, vui lòng liên hệ team development.
