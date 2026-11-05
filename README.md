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

## Clone dự án về máy

### Cách 1: Dùng Android Studio (khuyến nghị)
1. Mở Android Studio, ở màn hình chào chọn **Get from VCS**
   (hoặc **File > New > Project from Version Control**).
2. Dán đường dẫn repository:
   https://github.com/Volker06/SmartCalendarAI.git
3. Chọn thư mục lưu trên máy, bấm **Clone**.
4. Khi hỏi **Trust Project**, chọn **Trust Project**, rồi chờ Gradle sync xong.

### Cách 2: Dùng dòng lệnh
git clone https://github.com/Volker06/SmartCalendarAI.git

Sau đó mở thư mục `SmartCalendarAI` bằng Android Studio (File > Open),
chọn đúng thư mục có file `settings.gradle.kts`.

### Yêu cầu môi trường
- Android Studio bản mới, Android SDK Platform 35.
- Gradle JDK 17 hoặc 21 (Settings > Build, Execution, Deployment > Build Tools > Gradle).
- Máy ảo Android 8.0 trở lên, hoặc điện thoại thật.

Sau khi clone xong, làm tiếp phần **Cấu hình khóa API Gemini** bên dưới
rồi bấm Run để chạy ứng dụng.

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