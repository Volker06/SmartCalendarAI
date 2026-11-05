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

## Cấu hình khóa API Gemini

File `local.properties` không được đưa lên GitHub (chứa khóa riêng). Cách tạo:

1. Mở dự án bằng Android Studio, file `local.properties` sẽ tự được tạo ở thư mục gốc
   (nếu chưa có: chuột phải thư mục gốc > New > File > đặt tên `local.properties`).
2. Thêm vào cuối file:

   ```
   GEMINI_API_KEY=khóa_của_bạn
   GEMINI_MODEL=gemini-3.5-flash-lite
   ```

3. Lấy khóa miễn phí tại https://aistudio.google.com/apikey
4. Bấm Sync Gradle rồi chạy lại ứng dụng.

Xem thêm file mẫu `local.properties.example`. Nếu chưa có khóa, ứng dụng vẫn chạy bình thường,
chỉ riêng màn hình trợ lý AI sẽ báo chưa có khóa API.

> Lưu ý: GitHub chỉ lưu mã nguồn. Dữ liệu sự kiện lưu trong cơ sở dữ liệu Room trên từng thiết bị,
> nên khi clone về và chạy, lịch sẽ trống.
