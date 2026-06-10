# KeyEmotion Python AI Engine

Đây là bộ não AI của dự án `KeyEmotion_through_SocialMedia`.
Python chạy như một API server local bằng FastAPI để phần Java gửi dữ liệu bài đăng mạng xã hội sang và nhận kết quả JSON.

## Workflow

1. API Bridge
   - Dùng FastAPI.
   - Java gọi `POST /analyze` và gửi bài đăng mạng xã hội dạng JSON.
   - Python trả về cảm xúc, mức độ khẩn cấp, địa điểm, nhu cầu cứu trợ và hành động đề xuất.

2. Làm sạch văn bản
   - File: `services/text_cleaning_service.py`
   - Xóa URL, mention `@name`, hashtag `#tag`, icon, ký tự đặc biệt và khoảng trắng thừa.
   - Mục tiêu là đưa dữ liệu mạng xã hội về dạng dễ phân tích hơn trước khi đưa vào AI.

3. Phân tích cảm xúc và mức độ khẩn cấp
   - File chính: `services/nlp_service.py`
   - Nếu có `GEMINI_API_KEY`, hệ thống gọi Gemini API để phân tích cảm xúc, địa điểm và nhu cầu.
   - Sau đó kết hợp thêm bộ từ khóa kích hoạt tiếng Việt, gồm cả có dấu và không dấu.
   - Ví dụ: `cứu với`, `cuu voi`, `sập`, `sap`, `lũ`, `lu`, `ngập`, `ngap`, `đói`, `doi`, `y tế`, `y te`, `khẩn cấp`, `khan cap`.
   - Nếu điểm tiêu cực cao và có từ khóa thiên tai/cứu trợ thì gán mức khẩn cấp cao.
   - Nếu chưa có API key hoặc mất mạng, hệ thống tự fallback sang mock heuristic local.

4. Trích xuất địa điểm và nhu cầu
   - File: `services/ner_service.py`
   - Cách chính: Gemini extraction trả JSON gồm `locations`, `categories`, `urgency`.
   - Cách dự phòng: regex fallback để bắt các địa điểm tiếng Việt như `thôn A`, `xã Bình Minh`, `huyện Sơn Động`.
   - Nhu cầu cứu trợ được detect bằng keyword tiếng Việt có dấu và không dấu.
   - Category trong JSON vẫn giữ enum tiếng Anh để Java parse ổn định: `food`, `water`, `medical`, `shelter`, `rescue`, `transport`, `sanitation`, `unknown`.

## Cài đặt từ đầu trên Windows

### 1. Cài Python

Tải Python tại:

```text
https://www.python.org/downloads/
```

Khi cài, nhớ tick:

```text
Add python.exe to PATH
```

Kiểm tra:

```powershell
python --version
pip --version
```

### 2. Tạo virtual environment

Đi vào folder engine:

```powershell
cd C:\path\to\KeyEmotion_through_SocialMedia\python_ai_engine
```

Tạo môi trường ảo:

```powershell
python -m venv .venv
```

Bật môi trường ảo:

```powershell
.\.venv\Scripts\activate
```

Nếu thành công, terminal sẽ có `(.venv)` ở đầu dòng.

### 3. Cài thư viện

```powershell
pip install -r requirements.txt
```

Trong `requirements.txt` cần có:

```text
fastapi==0.115.6
uvicorn[standard]==0.34.0
pydantic==2.10.4
pydantic-settings==2.7.1
python-dotenv==1.0.1
google-genai==0.6.0
```

### 4. Tạo file môi trường

Copy file mẫu:

```powershell
copy .env.example .env
```

Mở `.env` và điền:

```env
GEMINI_API_KEY=your_gemini_api_key_here
GEMINI_MODEL=gemini-2.0-flash
USE_MOCK_AI=false
HOST=127.0.0.1
PORT=8000
```

Nếu chưa có Gemini key hoặc muốn demo không cần mạng:

```env
USE_MOCK_AI=true
```

### 5. Lấy Gemini API key

1. Vào Google AI Studio:

```text
https://aistudio.google.com/app/apikey
```

2. Đăng nhập bằng tài khoản Google.
3. Bấm `Create API key`.
4. Chọn project có sẵn, hoặc để AI Studio tạo project mới.
5. Copy API key và dán vào file `.env`:

```env
GEMINI_API_KEY=dan_key_cua_m_vao_day
USE_MOCK_AI=false
```

## Chạy server FastAPI

```powershell
uvicorn main:app --reload --host 127.0.0.1 --port 8000
```

Mở Swagger UI để test API:

```text
http://127.0.0.1:8000/docs
```


Chạy demo:

```powershell
python demo.py
```

Hoặc test endpoint:

```powershell
curl -X POST http://127.0.0.1:8000/analyze ^
  -H "Content-Type: application/json" ^
  -d @sample_request.json
```

## API cho Java gọi

Endpoint:

```text
POST http://127.0.0.1:8000/analyze
```

Request body mẫu:

```json
{
  "post": {
    "id": "post-yagi-001",
    "platform": "facebook",
    "author": "nguoi_dan_dia_phuong",
    "text": "Thôn A xã Bình Minh bị ngập nặng sau bão Yagi. Nhiều hộ dân đang mắc kẹt, thiếu lương thực, nước sạch và thuốc men khẩn cấp.",
    "keyword": "bão Yagi",
    "location_hint": "thôn A xã Bình Minh",
    "reactions": {
      "sad": 120,
      "angry": 32,
      "care": 75,
      "like": 20
    },
    "comments": [
      "Cứu với, đường vào thôn bị nước cuốn hỏng rồi.",
      "Cần xe cứu trợ chở nước sạch và thuốc.",
      "Khoảng 300 người đang cần hỗ trợ."
    ],
    "shares": 88
  }
}
```

Response mẫu:

```json
{
  "post_id": "post-yagi-001",
  "keyword": "bão Yagi",
  "dominant_emotion": "fear",
  "negative_score": 1.0,
  "confidence": 0.76,
  "humanitarian_signal": {
    "is_emergency": true,
    "urgency": "critical",
    "categories": ["food", "water", "medical", "rescue", "transport"],
    "locations": ["thôn A xã Bình Minh"],
    "affected_people_estimate": 300,
    "recommended_action": "Điều phối đội cứu trợ và hàng hóa gồm lương thực, nước sạch, y tế, cứu hộ, vận chuyển đến thôn A xã Bình Minh ngay lập tức."
  },
  "summary": "thôn A xã Bình Minh có mức độ khẩn cấp rất khẩn cấp, ghi nhận nhu cầu: lương thực, nước sạch, y tế, cứu hộ, vận chuyển.",
  "source": "mock"
}
```

