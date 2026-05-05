# Ứng dụng Chat Desktop (Java Swing)

Đồ án môn Lập trình Java — xây dựng ứng dụng chat thời gian thực dành cho desktop
sử dụng Java Swing cho giao diện và Socket TCP cho truyền tin.

## Tính năng

1. **Đăng ký & Đăng nhập** — tạo tài khoản, lưu trữ phía server.
2. **Chat 1-1** — nhắn tin trực tiếp với người dùng đang online, có thể mở nhiều
   cửa sổ cùng lúc.
3. **Chat nhóm** — tạo nhóm, mời thành viên, nhắn tin trong nhóm.
4. **Gửi file** — gửi file đính kèm trong quá trình chat.
5. **Lịch sử chat** — xem lại lịch sử, xoá từng tin nhắn riêng lẻ.

## Công nghệ

- Java 17, Swing
- Socket / ServerSocket, đa luồng
- Maven (đóng gói fat JAR bằng shade plugin)

## Build

```
mvn clean package
```

Sau khi build, file `target/chatapp.jar` có thể chạy bằng `java -jar`.

## Cấu trúc thư mục

```
src/main/java/com/chatapp
├── server   - Server TCP, ClientHandler đa luồng
├── client   - Các cửa sổ Swing (Login, Chat, Group, ...)
├── model    - Message, User, Group
└── util     - FileTransfer, HistoryManager
```
