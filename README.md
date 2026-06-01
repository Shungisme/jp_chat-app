# Ứng dụng Chat Desktop (Java Swing)

Đồ án môn **Lập trình Java** — ứng dụng chat thời gian thực trên desktop, viết bằng
Java Swing cho giao diện và Socket TCP cho truyền tin. Server đa luồng phục vụ nhiều
client cùng lúc; client có giao diện hiện đại (bong bóng chat, avatar, chủ đề
Sáng/Tối) hỗ trợ chat 1-1, chat nhóm, gửi file, lịch sử chat, gọi thoại, gọi video
và phòng gọi nhóm.

> Toàn bộ giao diện hiển thị bằng tiếng Việt.

## Tính năng chính

| Nhóm tính năng     | Mô tả                                                                                                                           |
| ------------------ | ------------------------------------------------------------------------------------------------------------------------------- |
| **Tài khoản**      | Đăng ký và đăng nhập từ client; mật khẩu lưu dưới dạng băm SHA-256; tự đăng xuất phiên cũ nếu cùng tài khoản đăng nhập nơi khác |
| **Chat 1-1**       | Nhắn tin thời gian thực, mỗi cuộc trò chuyện là một tab riêng                                                                   |
| **Chat nhóm**      | Tạo nhóm, mời thành viên, nhắn tin nhóm; chủ nhóm có thể mời thêm hoặc xoá thành viên                                           |
| **Gửi file**       | Gửi mọi loại file qua socket (mã hoá Base64), cả trong chat 1-1 lẫn chat nhóm                                                   |
| **Lịch sử chat**   | Lưu lịch sử cục bộ theo từng cặp/nhóm; xem lại, xoá từng dòng hoặc xoá toàn bộ                                                  |
| **Gọi thoại 1-1**  | Mic + loa truyền qua socket (PCM 8 kHz, 16-bit, mono) với mời/nhận/từ chối                                                      |
| **Gọi video 1-1**  | Webcam truyền dưới dạng khung JPEG 320×240                                                                                      |
| **Phòng gọi nhóm** | Phòng thoại nhóm và phòng video nhóm với danh sách người tham gia cập nhật trực tiếp                                            |
| **Hiện diện**      | Danh sách user trực tuyến cập nhật tức thì khi có người vào/ra                                                                  |
| **Thông báo**      | Thông báo khay hệ thống khi có tin nhắn/cuộc gọi mới                                                                            |
| **Giao diện**      | Bong bóng chat, avatar màu theo tên, biểu tượng vector, chuyển chủ đề Sáng/Tối (FlatLaf)                                        |
| **Emoji**          | Bảng chọn emoji nhanh                                                                                                           |
| **Quản lý server** | Lưu nhiều server (tên / host / port); thêm, sửa, xoá ngay trong màn hình đăng nhập                                              |
| **Nhập liệu**      | Nhập nhiều dòng, bật/tắt gửi bằng phím ENTER                                                                                    |

## Yêu cầu môi trường

- **JDK 17** trở lên
- **Maven 3.6+** (để build và tải dependency)
- _(Tuỳ chọn)_ **Webcam** — nếu máy không có webcam, nút gọi video vẫn hoạt động:
  bạn không gửi được hình của mình nhưng vẫn xem được video do người kia gửi tới.

Dependency được Maven tự tải về:

- [Sarxos `webcam-capture`](https://github.com/sarxos/webcam-capture) — chụp hình từ webcam.
- [FlatLaf](https://www.formdev.com/flatlaf/) — giao diện Swing hiện đại, hỗ trợ chủ đề Sáng/Tối.

## Build

```bash
mvn clean package
```

Lệnh trên tạo ra `target/chatapp.jar` — một "fat jar" đã gộp sẵn (shade) tất cả
dependency, có thể chạy độc lập.

## Chạy ứng dụng

Mở **Server trước**, sau đó mở một hoặc nhiều **Client**.

### 1. Mở Server (có giao diện)

```bash
java -cp target/chatapp.jar com.chatapp.server.ServerFrame
```

Cửa sổ Server hiện ra: nhập port (mặc định `9999`) rồi bấm **Khởi động**.
Bên phải hiển thị danh sách client đang kết nối; bên dưới là nhật ký hoạt động.

> Nếu muốn chạy server không giao diện (headless), dùng:
> `java -cp target/chatapp.jar com.chatapp.server.Server`

### 2. Mở Client

```bash
java -jar target/chatapp.jar
```

Vì lớp chính của jar là `com.chatapp.client.LoginFrame`, lệnh trên mở thẳng cửa sổ
**Đăng nhập**:

- **Server** — chọn từ danh sách server đã lưu (`data/servers.txt`).
  Bấm **Quản lý...** để thêm / sửa / xoá server.
- **Tài khoản / Mật khẩu** — đăng nhập, hoặc bấm **Đăng ký** để tạo tài khoản mới
  trên server đang chọn (tài khoản tối thiểu 3 ký tự, mật khẩu tối thiểu 6 ký tự).

Sau khi đăng nhập, cửa sổ chính `MainFrame` mở ra:

- **Bên trái** — mục **Trực tuyến** (user đang online) và mục **Nhóm** (các nhóm
  bạn tham gia), kèm nút tạo nhóm. Double-click một user để mở chat 1-1.
- **Bên phải** — vùng tab: mỗi cuộc trò chuyện (1-1 hoặc nhóm) là một tab, có
  huy hiệu đếm tin chưa đọc.
- **Trên cùng** — avatar, ô tìm kiếm, nút chuyển chủ đề Sáng/Tối và nút đăng xuất.

### 3. Thao tác trong khung chat

| Hành động       | Cách thực hiện                                                   |
| --------------- | ---------------------------------------------------------------- |
| Gửi tin nhắn    | Gõ vào ô nhập, bấm **Gửi** (mặc định ENTER cũng gửi)             |
| Xuống dòng      | Shift+ENTER (khi bật "ENTER gửi") hoặc ENTER (khi tắt)           |
| Chèn emoji      | Bấm nút emoji để chọn                                            |
| Gửi file        | Bấm nút đính kèm, chọn file (mọi loại)                           |
| Gọi thoại       | Bấm nút mic — người kia nhận được lời mời để Nhận / Từ chối      |
| Gọi video       | Bấm nút camera (cần webcam)                                      |
| Xem lịch sử     | Bấm nút lịch sử — xem lại, xoá từng dòng hoặc xoá toàn bộ        |
| Thành viên nhóm | Trong tab nhóm, mở danh sách thành viên; chủ nhóm có thể mời/xoá |
| Phòng gọi nhóm  | Trong tab nhóm, mở phòng thoại hoặc phòng video nhóm             |

Đóng cửa sổ gọi sẽ tự gửi tín hiệu kết thúc để bên kia cũng đóng theo.

## Cấu trúc dự án

```
src/main/java/com/chatapp
├── server   - Server, ServerFrame (UI), ClientHandler, UserStore, GroupStore
├── client   - LoginFrame (điểm vào), RegisterFrame, MainFrame, Client,
│             ChatPanel, ChatWindowManager, MessageDispatcher, NotificationManager,
│             GroupPanel, CreateGroupDialog, GroupMembersDialog,
│             GroupVoiceRoomFrame, GroupVideoRoomFrame, GroupEntry, GroupInfo,
│             VoiceCallFrame, VideoCallFrame, IncomingCallDialog,
│             HistoryFrame, ServerConfig, ServerManagerDialog
│   └── ui    - Theme, UiKit, Icons, Avatar, MessageBubble, BubbleText,
│              ConversationView, DayDivider, RowCell, VectorIconButton
├── model    - Message, User, Group
└── util     - FileTransfer, HistoryManager, VoiceChat, VideoCapture

src/main/resources
├── app.properties   - host/port mặc định, kích thước chunk, thư mục dữ liệu
└── icons/           - tài nguyên biểu tượng (PNG)

data/        - tự sinh khi chạy (đã bỏ qua trong .gitignore)
├── users.txt                       - tài khoản đã đăng ký   (server)
├── groups.txt                      - danh sách nhóm         (server)
├── servers.txt                     - danh sách server đã lưu (client)
└── history/<me>/<a>__<b>.log       - lịch sử chat, tách riêng theo từng user
```

## Kiến trúc & cách hoạt động

- **Server đa luồng**: mỗi client kết nối được phục vụ bởi một `ClientHandler`
  riêng chạy trên thread pool. Server giữ danh bạ kết nối và rôm-bộ (roster) cho
  từng phòng gọi nhóm.
- **Giao tiếp**: client và server trao đổi các đối tượng `Message` qua TCP bằng
  Java Object Serialization. Mỗi `Message` gồm `type`, `sender`, `target`,
  `content` và `timestamp`.
- **Phân loại bản tin**: `Message.Type` định nghĩa đầy đủ các loại — đăng nhập/đăng
  ký, chat 1-1 và nhóm, gửi file, danh sách user, tạo/mời/xoá nhóm, gọi thoại và
  gọi video 1-1, cùng các tín hiệu phòng gọi nhóm (tham gia / rời / bắt đầu / kết
  thúc).
- **Dữ liệu**: tài khoản, nhóm và danh sách server lưu dạng văn bản thuần; nội
  dung nhị phân (file, âm thanh, hình video) được mã hoá Base64 trong `content`.
- **Lịch sử**: lưu cục bộ phía client, tách thư mục theo từng user để hai phiên
  chạy chung thư mục không ghi đè lẫn nhau.

## Tài liệu thêm

- [docs/UI_UX_IMPROVEMENT_PLAN.md](docs/UI_UX_IMPROVEMENT_PLAN.md) — kế hoạch và ghi
  chú cải tiến giao diện.

## Tác giả

Đồ án cá nhân — môn Lập trình Java, học kỳ 2 năm học 2025–2026.
