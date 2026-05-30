# Ứng dụng Chat Desktop (Java Swing)

Đồ án môn Lập trình Java — ứng dụng chat thời gian thực dành cho desktop sử dụng
Java Swing cho giao diện và Socket TCP cho truyền tin. Hỗ trợ chat 1-1 (nhiều
tab), chat nhóm, gửi file, lịch sử chat, gọi thoại và gọi video.

## Tính năng

| # | Tính năng                                                            | Branch                       |
|---|----------------------------------------------------------------------|------------------------------|
| 1 | Đăng ký tài khoản từ client                                          | `feature/user-registration`  |
| 2 | Đăng nhập, phiên làm việc                                            | `feature/user-login`         |
| 3 | Server đa luồng (TCP)                                                | `feature/server-core`        |
| 4 | Chat 1-1 thời gian thực                                              | `feature/direct-chat`        |
| 5 | Chat nhiều user (chia tab)                                           | `feature/enhance`            |
| 6 | Tạo nhóm, mời thành viên, chat nhóm                                  | `feature/group-chat`         |
| 7 | Gửi file (mọi loại) qua socket                                       | `feature/file-transfer`      |
| 8 | Lịch sử chat cục bộ, xoá từng dòng                                   | `feature/chat-history`       |
| 9 | Gọi thoại (voice chat) — mic + loa qua socket                        | `feature/voice-chat`         |
| 10 | Gọi video (webcam) — JPEG ~10 fps                                   | `feature/webcam`             |
| 11 | UI server: cấu hình port, start/stop, danh sách client kết nối      | `feature/enhance`            |
| 12 | UI client: chọn server, danh sách user online                       | `feature/enhance`            |
| 13 | Quản lý danh sách server (thêm / sửa / xoá, lưu file config)        | `feature/enhance`            |
| 14 | Nhập tin nhắn nhiều dòng + bật/tắt ENTER gửi                        | `feature/enhance`            |
| 15 | Bảng emoji (12 emoji)                                                | `feature/enhance`            |

## Yêu cầu

- JDK 17 trở lên
- Maven 3.6+ (để build và tải dependency cho webcam)
- (Tuỳ chọn) Webcam vật lý — nếu không có, nút gọi video sẽ hiển thị
  thông báo và phần lại vẫn xem được video do peer gửi tới.

## Build

```bash
mvn clean package
```

Lệnh trên sinh ra `target/chatapp.jar` (đã shade các dependency, bao gồm thư
viện webcam Sarxos).

## Chạy ứng dụng

### 1. Mở Server UI

```bash
java -cp target/chatapp.jar com.chatapp.server.ServerFrame
```

Cửa sổ Server xuất hiện. Nhập port (mặc định `9999`), bấm **Khởi động**.
Phần phải hiển thị danh sách user đang kết nối; phần dưới hiển thị nhật ký.

> Nếu muốn chạy server ở chế độ headless (không UI), dùng lệnh cũ:
> `java -cp target/chatapp.jar com.chatapp.server.Server`

### 2. Mở Client

```bash
java -jar target/chatapp.jar
```

Cửa sổ **Đăng nhập** xuất hiện với:

- **Combobox Server** — chọn server đã lưu trong file config
  (`data/servers.txt`). Bấm **Quản lý...** để thêm / sửa / xoá server.
- **Tài khoản / Mật khẩu** — đăng nhập, hoặc bấm **Đăng ký** để tạo tài
  khoản mới trên server đang chọn.

Sau khi đăng nhập thành công, `MainFrame` mở với:

- **Bên trái** — danh sách user đang online (double-click để mở chat).
- **Bên phải** — `JTabbedPane`: mỗi user là một tab, có nút **✕** để đóng tab.

### 3. Trong khung chat (mỗi tab)

| Hành động | Cách thực hiện |
|---|---|
| Gửi tin nhắn | Gõ vào ô input, bấm **Gửi** (mặc định ENTER cũng gửi) |
| Xuống dòng | Shift+ENTER (khi `ENTER gửi` bật) hoặc ENTER (khi tắt) |
| Gửi với phím tắt khi tắt ENTER gửi | Ctrl+ENTER |
| Chèn emoji | Bấm nút **😀** chọn từ bảng 12 emoji |
| Gửi file | Bấm **📎**, chọn file (mọi loại) |
| Gọi thoại | Bấm **🎙** — `VoiceCallFrame` mở, peer auto-nhận cuộc gọi |
| Gọi video | Bấm **📹** — `VideoCallFrame` mở (cần webcam) |
| Xem lịch sử chat | Bấm **Lịch sử** — xem / xoá từng dòng |

Đóng cửa sổ gọi (voice / video) sẽ gửi tín hiệu `*_END` để bên kia cũng
đóng — không còn hiện tượng cửa sổ tự mở lại.

## Cấu trúc thư mục

```
src/main/java/com/chatapp
├── server   - Server, ServerFrame, ClientHandler, UserStore, GroupStore
├── client   - LoginFrame, RegisterFrame, MainFrame, ChatPanel, GroupFrame,
│             HistoryFrame, VoiceCallFrame, VideoCallFrame,
│             ServerConfig, ServerManagerDialog, ChatWindowManager, Client
├── model    - Message, User, Group
└── util     - FileTransfer, HistoryManager, VoiceChat, VideoCapture

src/main/resources
├── app.properties
└── icons/   - icon assets (PNG)

data/        - sinh ra khi chạy (không commit)
├── users.txt          - tài khoản đã đăng ký (server)
├── servers.txt        - danh sách server đã lưu (client)
└── history/<a>__<b>.log - lịch sử chat giữa từng cặp user
```

## Quy trình Git

Mỗi tính năng được phát triển trên một feature branch riêng và merge vào `main`
bằng `--no-ff` để giữ lại lịch sử nhánh:

```bash
git log --oneline --graph --all
```

## Tác giả

Đồ án cá nhân — môn Lập trình Java, học kỳ 2 năm học 2025–2026.
