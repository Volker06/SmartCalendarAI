# Smart Calendar AI

Ứng dụng quản lý lịch thông minh tích hợp trí tuệ nhân tạo (Android, Java).

- Môn học: Phát triển ứng dụng di động
- Thành viên: Đỗ Quốc Việt (524H0137), Trần Minh Thái (524H0029)

## Cấu trúc
- `app/`: ứng dụng Android (Java + XML, MVVM + Room)
- `backend/`: backend FastAPI gọi LLM API

## Chạy ứng dụng
1. Mở thư mục này bằng Android Studio (File > Open).
2. Chờ Gradle sync xong.
3. Chọn emulator hoặc điện thoại thật, bấm Run.

## Trợ lý AI
Ứng dụng gọi trực tiếp Google Gemini API, không cần backend riêng.
1. Lấy khóa API miễn phí tại https://aistudio.google.com/apikey
2. Mở file `local.properties` ở thư mục gốc project (file này không được đưa lên Git) và thêm:
   ```
   GEMINI_API_KEY=khóa_của_bạn
   GEMINI_MODEL=gemini-3.5-flash-lite
   ```
3. Sync Gradle, chạy app, bấm biểu tượng ngôi sao trên thanh tiêu đề để trò chuyện với trợ lý.

## Cấu hình khóa API (không đưa lên Git)

Thêm vào file `local.properties` (đã được .gitignore):

```
GEMINI_API_KEY=your_key_here
GEMINI_MODEL=gemini-3.5-flash-lite
```
