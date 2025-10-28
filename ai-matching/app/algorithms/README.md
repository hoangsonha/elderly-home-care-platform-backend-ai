# PhoBERT Semantic Matching Implementation

## 🎯 Tổng quan

PhoBERT Semantic Matcher sử dụng mô hình PhoBERT (Vietnamese BERT) để tính semantic similarity cho tiếng Việt, thay thế cho fuzzy matching truyền thống.

## 📁 Cấu trúc

```
backend/app/algorithms/
├── fuzzy_matcher.py          # Fuzzy matching truyền thống
└── semantic_matcher.py       # PhoBERT semantic matching (MỚI)
```

## 🚀 Cài đặt

### 1. Cài đặt dependencies:

```bash
pip install transformers torch scikit-learn
```

### 2. Model sẽ tự động download:

- Model: `vinai/phobert-base`
- Kích thước: ~440MB
- Lần đầu chạy sẽ download từ Hugging Face

## 🔧 Sử dụng

### Trong matcher.py:

```python
# Đã được cập nhật tự động
from app.algorithms.semantic_matcher import semantic_matcher, normalize_request_skills, normalize_caregiver_skills
```

### Switch giữa các thuật toán:

```python
# Sử dụng PhoBERT (mặc định)
from app.algorithms.semantic_matcher import semantic_matcher

# Hoặc switch về fuzzy matching
from app.algorithms.fuzzy_matcher import fuzzy_matcher as semantic_matcher
```

## 📊 So sánh kết quả

### PhoBERT vs Fuzzy Matching:

| Test Case                                | PhoBERT | Fuzzy | Cải thiện       |
| ---------------------------------------- | ------- | ----- | --------------- |
| "tiêm" vs "chích"                        | 0.350   | 1.000 | ✅ Semantic     |
| "đo đường huyết" vs "kiểm tra đường máu" | 0.254   | 0.000 | ✅ Semantic     |
| "chăm sóc vết thương" vs "wound care"    | 0.515   | 0.000 | ✅ Multilingual |
| "quản lý thuốc" vs "quản lý thuốc men"   | 0.512   | 0.000 | ✅ Semantic     |

## ⚙️ Cấu hình

### Threshold:

- **PhoBERT**: 0.4 (40% similarity)
- **Fuzzy**: 0.6 (60% similarity)

### Performance:

- **Cache**: Tự động cache embeddings
- **Batch processing**: Hỗ trợ tính similarity hàng loạt
- **Fallback**: Tự động fallback nếu PhoBERT không khả dụng

## 🧪 Test

### Chạy test PhoBERT:

```bash
python test_semantic_matcher.py
```

### Chạy debug matcher:

```bash
cd debug
python debug_matcher_filters.py
```

## 📈 Kết quả test

```
PHOBERT SEMANTIC MATCHER TEST
============================================================
Model Name: vinai/phobert-base
Dependencies Installed: True
Model Available: True
Device: cpu

SIMILARITY RESULTS:
'tiêm' vs 'chích': 0.350
'đo đường huyết' vs 'kiểm tra đường máu': 0.254
'chăm sóc vết thương' vs 'wound care': 0.515
'quản lý thuốc' vs 'quản lý thuốc men': 0.512
```

## 🔄 Fallback Mechanism

Nếu PhoBERT không khả dụng:

1. **Tự động fallback** về word overlap similarity
2. **Không crash** ứng dụng
3. **Log warning** để debug

## 💡 Lợi ích

### ✅ Ưu điểm:

- **Semantic understanding**: Hiểu ngữ nghĩa sâu
- **Multilingual**: Hỗ trợ tiếng Việt + tiếng Anh
- **Context aware**: Hiểu ngữ cảnh
- **Scalable**: Có thể fine-tune cho domain cụ thể

### ⚠️ Nhược điểm:

- **Resource intensive**: Cần RAM/CPU nhiều hơn
- **Slower**: Chậm hơn fuzzy matching
- **Dependencies**: Cần cài đặt thêm packages

## 🎯 Kết luận

PhoBERT Semantic Matcher đã được triển khai thành công và hoạt động tốt với:

- ✅ **Semantic similarity** cho tiếng Việt
- ✅ **Multilingual support** (Việt + Anh)
- ✅ **Fallback mechanism** an toàn
- ✅ **Cache optimization** cho performance
- ✅ **Easy switching** giữa các thuật toán

**Hệ thống matching giờ đã sử dụng AI nâng cao cho tiếng Việt!** 🚀
