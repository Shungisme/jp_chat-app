# Ứng dụng Chat Desktop (Java Swing)

Đồ án môn Lập trình Java — ứng dụng chat thời gian thực dành cho desktop sử dụng
Java Swing cho giao diện và Socket TCP cho truyền tin. Hỗ trợ chat 1-1, chat
nhóm, gửi file và lưu lịch sử chat cục bộ.

## Tính năng đã hoàn thiện

| # | Tính năng                                  | Branch                       |
|---|--------------------------------------------|------------------------------|
| 1 | Đăng ký tài khoản                          | `feature/user-registration`  |
| 2 | Đăng nhập, phiên làm việc                  | `feature/user-login`         |
| 3 | Server đa luồng (TCP)                      | `feature/server-core`        |
| 4 | Chat 1-1 thời gian thực                    | `feature/direct-chat`        |
| 5 | Mở nhiều cửa sổ chat đồng thời             | `feature/multi-chat`         |
| 6 | Tạo nhóm, mời thành viên, chat nhóm        | `feature/group-chat`         |
| 7 | Gửi file (mọi loại) qua socket             | `feature/file-transfer`      |
| 8 | Lịch sử chat cục bộ, xoá từng dòng         | `feature/chat-history`       |
| 9 | UI: icon, font, system tray notification   | `feature/ui-polish`          |

## Build & Run

```bash
mvn clean package
java -jar target/chatapp.jar               # mặc định mở LoginFrame
java -cp target/chatapp.jar com.chatapp.server.Server  # chạy server
```

## Cấu trúc thư mục

```
src/main/java/com/chatapp
├── server   - Server TCP, ClientHandler, UserStore, GroupStore
├── client   - LoginFrame, RegisterFrame, MainFrame, ChatFrame, GroupFrame, HistoryFrame
├── model    - Message, User, Group
└── util     - FileTransfer, HistoryManager
src/main/resources
├── app.properties
└── icons/        - icon assets (PNG)
```

## Quy trình Git

Mỗi tính năng được phát triển trên một feature branch riêng và merge vào `main`
bằng `--no-ff` để giữ lại lịch sử nhánh:

```
git log --oneline --graph --all
```

## Tác giả

Đồ án cá nhân — môn Lập trình Java, học kỳ 2 năm học 2025–2026.
