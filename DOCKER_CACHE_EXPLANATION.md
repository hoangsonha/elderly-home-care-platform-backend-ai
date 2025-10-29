# Giải thích Docker Cache cho Models và Dependencies

## 🎯 Tổng quan

Có **2 loại cache khác nhau**:

1. **Docker Layer Cache** - Cache khi BUILD image
2. **Runtime Cache** - Cache khi CHẠY container (model files)

## 📦 1. Docker Layer Cache (Khi BUILD)

### Cách hoạt động:

```dockerfile
# Layer 1: Base image
FROM python:3.10-slim          # ← Nếu không đổi → Dùng cache

# Layer 2: Install system deps
RUN apt-get update && ...       # ← Nếu không đổi → Dùng cache

# Layer 3: Copy requirements
COPY requirements.txt .         # ← Nếu requirements.txt không đổi → Dùng cache

# Layer 4: Install Python packages
RUN pip install -r requirements.txt  # ← Nếu requirements.txt không đổi → Dùng cache (NHANH!)

# Layer 5: Copy code
COPY . .                        # ← Nếu code đổi → Phải rebuild từ đây
```

### Trong workflow của bạn:

```bash
docker-compose build --no-cache  # ← --no-cache = BỎ QUA tất cả cache!
```

**`--no-cache` nghĩa là gì?**

- ❌ Không dùng Docker layer cache
- ❌ Phải tải lại tất cả dependencies (torch, transformers, ...)
- ⏱️ **Mất rất nhiều thời gian** (có thể 10-15 phút cho AI service)

**Nếu không có `--no-cache`:**

- ✅ Dùng cache nếu `requirements.txt` không đổi
- ✅ Build nhanh hơn (chỉ rebuild layer cuối nếu code đổi)
- ⏱️ **Nhanh hơn nhiều** (1-2 phút)

## 🤖 2. PhoBERT Model Cache (Khi CHẠY container)

### Cách hoạt động:

```python
# Trong semantic_matcher.py
self.model = AutoModel.from_pretrained("vinai/phobert-base-v2")
```

**Khi container chạy lần đầu:**

1. Hugging Face `transformers` tự động download model
2. Model được lưu vào: `/root/.cache/huggingface/transformers/`
3. Kích thước: ~440MB

**Các lần chạy sau:**

- ✅ Model đã có trong container → Không download lại
- ✅ Load từ disk (nhanh)

### ⚠️ Vấn đề: Model cache trong container

**Nếu bạn rebuild container:**

```bash
docker-compose build --no-cache
docker-compose up -d
```

→ Container mới được tạo
→ Model cache trong container CŨ bị mất
→ Phải download lại model (lần đầu chạy)

**Nếu bạn chỉ restart container:**

```bash
docker-compose restart ai_matching
```

→ Container cũ được restart
→ Model cache vẫn còn
→ Không cần download lại ✅

## 💡 Giải pháp tối ưu

### **Option 1: Dùng Volume để persist model cache**

Thêm vào `docker-compose.yml`:

```yaml
ai_matching:
  volumes:
    - ~/.cache/huggingface:/root/.cache/huggingface # ← Persist model cache
```

**Lợi ích:**

- Model được lưu trên VPS filesystem
- Không mất khi rebuild container
- Chỉ download 1 lần!

### **Option 2: Bỏ `--no-cache` trong workflow**

Sửa trong `deploy.yml`:

```yaml
# Thay vì:
docker-compose build --no-cache

# Dùng:
docker-compose build  # ← Dùng cache nếu có thể
```

**Khi nào dùng `--no-cache`:**

- ✅ Khi dependencies thay đổi (requirements.txt)
- ✅ Khi muốn đảm bảo build clean
- ❌ Không cần thiết nếu chỉ đổi code

**Khi nào KHÔNG dùng `--no-cache`:**

- ✅ Chỉ đổi application code
- ✅ Muốn build nhanh
- ✅ Dependencies không thay đổi

## 📊 So sánh

| Tình huống                     | Build time  | Model download     | Tổng thời gian |
| ------------------------------ | ----------- | ------------------ | -------------- |
| `--no-cache`                   | 10-15 phút  | 2-3 phút (lần đầu) | **~15 phút**   |
| Không `--no-cache` (cache hit) | 1-2 phút    | 0 (đã có)          | **1-2 phút**   |
| Volume persist + restart       | 0 (restart) | 0 (đã có)          | **~10 giây**   |

## 🔧 Khuyến nghị

### **Cho deployment thường xuyên (mỗi push code):**

1. **Bỏ `--no-cache`** để build nhanh hơn:

```yaml
docker-compose build # Thay vì build --no-cache
```

2. **Chỉ dùng `--no-cache` khi:**

   - Thay đổi `requirements.txt`
   - Muốn clean build (ít khi cần)

3. **Thêm volume cho model cache** (optional nhưng khuyến khích):

```yaml
volumes:
  - ~/.cache/huggingface:/root/.cache/huggingface
```

## ❓ Trả lời câu hỏi của bạn

> "Khi chạy docker thì docker sẽ tải lại các model hay thư viện hả?"

**Câu trả lời:**

1. **Thư viện Python (torch, transformers, ...):**

   - ❌ Tải lại mỗi lần **build với `--no-cache`**
   - ✅ Không tải lại nếu **build không có `--no-cache` và dependencies không đổi**

2. **PhoBERT Model:**
   - ❌ Phải download lại mỗi lần **container mới được tạo** (rebuild)
   - ✅ Không tải lại nếu **chỉ restart container** (model cache còn trong container cũ)
   - ✅ Không tải lại nếu **dùng volume persist** (model cache trên host)

## 🎯 Tóm tắt

- **Docker layer cache**: Cache dependencies khi BUILD (nếu không dùng `--no-cache`)
- **Model cache**: Cache trong container khi CHẠY (mất khi rebuild, giữ khi restart)
- **Tốt nhất**: Dùng volume để persist model cache → Chỉ download 1 lần!
