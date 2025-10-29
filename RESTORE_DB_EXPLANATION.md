# Giải thích script restore-db.sh

## 📋 Tổng quan

Script này dùng để **khôi phục database PostgreSQL** từ file backup đã tạo trước đó.

## 🔄 Luồng hoạt động chi tiết

### **Bước 1: Khởi tạo và kiểm tra input**

```bash
# Nếu có truyền file backup vào command line
./restore-db.sh backups/db_backup_20251029_143000.sql.gz
```

- ✅ Nếu có file → Dùng file đó
- ❌ Nếu không có → Hiển thị danh sách backups để chọn

### **Bước 2: Liệt kê danh sách backups (nếu không có input)**

**Khi bạn chạy:**

```bash
./restore-db.sh
```

Script sẽ:

1. Tìm tất cả file `.sql.gz` trong folder `backups/`
2. Sắp xếp theo thời gian (mới nhất trước)
3. Hiển thị:
   ```
   1. db_backup_20251029_143000.sql.gz (2.5M) - 2025-10-29 14:30:00
   2. db_backup_20251028_120000.sql.gz (2.4M) - 2025-10-28 12:00:00
   ```
4. Bạn chọn số thứ tự (1, 2, ...) hoặc gõ `q` để thoát

### **Bước 3: Xác nhận hành động nguy hiểm**

⚠️ **Đây là bước quan trọng!**

```
⚠️  WARNING: This will replace ALL current database data!
Backup file: backups/db_backup_20251029_143000.sql.gz
Database: elderly_platform
Are you sure you want to restore? Type 'yes' to continue:
```

**Tại sao nguy hiểm?**

- Script sẽ **XÓA HẾT** database hiện tại
- **THAY THẾ** bằng dữ liệu từ backup
- **KHÔNG THỂ UNDO** nếu đã restore

→ Phải gõ chính xác `yes` mới tiếp tục!

### **Bước 4: Kiểm tra container database**

```bash
docker ps | grep elderly_db
```

- ✅ Nếu container đang chạy → Tiếp tục
- ❌ Nếu không chạy → Báo lỗi, yêu cầu start containers trước

**Tại sao cần container?**

- Script cần kết nối vào PostgreSQL để restore
- Container phải chạy mới có thể chạy lệnh `psql`

### **Bước 5: Giải nén backup (nếu cần)**

```bash
# Nếu file là .gz
gunzip -c backup.sql.gz > temp_file.sql

# Nếu file không nén
# Dùng trực tiếp file .sql
```

**Lý do:**

- Backup thường được nén (`.gz`) để tiết kiệm dung lượng
- PostgreSQL cần file `.sql` thuần túy để import

### **Bước 6: Khôi phục database**

**Đây là bước chính - 3 lệnh liên tiếp:**

#### 6.1. Xóa database cũ

```sql
DROP DATABASE IF EXISTS elderly_platform;
```

- Xóa database hiện tại (nếu có)
- `IF EXISTS` = Không báo lỗi nếu database không tồn tại

#### 6.2. Tạo database mới

```sql
CREATE DATABASE elderly_platform;
```

- Tạo database rỗng mới
- Sẵn sàng để import data

#### 6.3. Import data từ backup

```bash
psql -U postgres -d elderly_platform < backup.sql
```

- Đọc file SQL backup
- Chạy từng câu lệnh SQL trong file
- Khôi phục tất cả tables, data, indexes, constraints...

### **Bước 7: Dọn dẹp**

```bash
rm -f temp_file.sql  # Xóa file tạm (nếu có)
```

## 📊 Sơ đồ luồng

```
START
  ↓
Có truyền file backup?
  ├─ YES → Dùng file đó
  └─ NO  → Liệt kê danh sách backups → Chọn file
  ↓
File tồn tại?
  ├─ NO → ❌ Lỗi, thoát
  └─ YES → Tiếp tục
  ↓
⚠️ Hiển thị WARNING + Xác nhận
  ↓
User gõ 'yes'?
  ├─ NO → ❌ Hủy, thoát
  └─ YES → Tiếp tục
  ↓
Container elderly_db đang chạy?
  ├─ NO → ❌ Lỗi, yêu cầu start
  └─ YES → Tiếp tục
  ↓
File backup là .gz?
  ├─ YES → Giải nén → temp_file.sql
  └─ NO  → Dùng trực tiếp
  ↓
DROP DATABASE (xóa cũ)
  ↓
CREATE DATABASE (tạo mới)
  ↓
IMPORT DATA (restore)
  ↓
Xóa temp_file (nếu có)
  ↓
✅ Hoàn thành!
```

## 🎯 Ví dụ sử dụng

### **Cách 1: Chọn từ danh sách**

```bash
cd ~/elderly-home-care-platform-backend-ai
./restore-db.sh

# Script hiển thị:
#   1. db_backup_20251029_143000.sql.gz (2.5M)
#   2. db_backup_20251028_120000.sql.gz (2.4M)
# Enter backup number: 1
# Are you sure? Type 'yes': yes
# ✅ Restored!
```

### **Cách 2: Chỉ định file trực tiếp**

```bash
./restore-db.sh backups/db_backup_20251029_143000.sql.gz

# Hoặc chỉ tên file:
./restore-db.sh db_backup_20251029_143000.sql.gz

# Bỏ qua bước chọn, nhảy thẳng vào xác nhận
```

## ⚠️ Lưu ý quan trọng

1. **Backup trước khi restore:**

   ```bash
   # Luôn backup database hiện tại trước khi restore!
   # Nếu restore sai backup, bạn vẫn có thể restore lại
   ```

2. **Database sẽ bị xóa hoàn toàn:**

   - Tất cả tables
   - Tất cả data
   - Tất cả indexes, constraints
   - → Thay thế 100% bằng data từ backup

3. **Thời gian restore:**

   - Phụ thuộc vào size của backup
   - Database lớn → Mất nhiều thời gian hơn

4. **Backend container:**
   - Sau khi restore, có thể cần restart backend:
   ```bash
   docker-compose restart backend
   ```

## 🔍 Debug nếu có lỗi

**Lỗi thường gặp:**

1. `Container not running` → Start với `docker-compose up -d`
2. `Backup file not found` → Kiểm tra đường dẫn file
3. `Permission denied` → Chạy `chmod +x restore-db.sh`
4. `Restore failed` → Kiểm tra logs: `docker logs elderly_db`
