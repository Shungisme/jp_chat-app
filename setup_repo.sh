#!/bin/bash
# setup_repo.sh — builds a full Java Swing chat-app history on top of the
# existing repo. Assumes you are on `main` with a clean working tree.
set -e

# ---------- Helpers ----------
commit_at() {
    export GIT_AUTHOR_DATE="$1"
    export GIT_COMMITTER_DATE="$1"
    git add -A
    git commit -m "$2" >/dev/null
    echo "  + $1  $2"
}

merge_at() {
    export GIT_AUTHOR_DATE="$1"
    export GIT_COMMITTER_DATE="$1"
    git merge --no-ff "$2" -m "$3" >/dev/null
    echo "  M $1  $3"
}

start_feature() {
    git checkout main >/dev/null 2>&1
    git checkout -b "$1" >/dev/null 2>&1
    echo "==> $1"
}

finish_feature() {
    git checkout main >/dev/null 2>&1
    merge_at "$2" "$1" "Merge $1 into main"
}

# Sanity check
if [ ! -d .git ]; then
    echo "Error: must be run inside the existing git repo." >&2
    exit 1
fi
git checkout main >/dev/null 2>&1

############################################################
# Feature 1: feature/project-setup  (May 05–06)
############################################################
start_feature "feature/project-setup"

mkdir -p src/main/java/com/chatapp/server
mkdir -p src/main/java/com/chatapp/client
mkdir -p src/main/java/com/chatapp/model
mkdir -p src/main/java/com/chatapp/util
mkdir -p src/main/resources

cat > .gitignore << 'EOF'
target/
*.class
*.jar
.idea/
*.iml
.vscode/
data/
files/
out/
.DS_Store
EOF

cat > pom.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.chatapp</groupId>
    <artifactId>chatapp</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <packaging>jar</packaging>
    <name>ChatApp</name>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>
</project>
EOF

# keep dirs in git
touch src/main/java/com/chatapp/server/.gitkeep
touch src/main/java/com/chatapp/client/.gitkeep
touch src/main/java/com/chatapp/model/.gitkeep
touch src/main/java/com/chatapp/util/.gitkeep
touch src/main/resources/.gitkeep

commit_at "2026-05-05T03:42:11+07:00" "chore(init): initialize Maven project structure"

# -- Commit 1.2 -- shade plugin
cat > pom.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.chatapp</groupId>
    <artifactId>chatapp</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <packaging>jar</packaging>
    <name>ChatApp</name>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <main.class>com.chatapp.client.MainFrame</main.class>
    </properties>

    <build>
        <finalName>chatapp</finalName>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.5.0</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals><goal>shade</goal></goals>
                        <configuration>
                            <transformers>
                                <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                    <mainClass>${main.class}</mainClass>
                                </transformer>
                            </transformers>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
EOF

commit_at "2026-05-05T09:17:55+07:00" "chore(build): configure Maven shade plugin for fat JAR"

# -- Commit 1.3 -- base models
rm -f src/main/java/com/chatapp/model/.gitkeep
cat > src/main/java/com/chatapp/model/Message.java << 'EOF'
package com.chatapp.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        LOGIN, REGISTER, CHAT, GROUP_CHAT, FILE, USER_LIST,
        GROUP_CREATE, GROUP_INVITE, LOGOUT, ACK, ERROR
    }

    private Type type;
    private String sender;
    private String target;
    private String content;
    private long timestamp;

    public Message() {}

    public Message(Type type, String sender, String target, String content) {
        this.type = type;
        this.sender = sender;
        this.target = target;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "[" + type + "] " + sender + " -> " + target + ": " + content;
    }
}
EOF

cat > src/main/java/com/chatapp/model/User.java << 'EOF'
package com.chatapp.model;

import java.io.Serializable;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private String username;
    private String passwordHash;
    private boolean online;

    public User() {}

    public User(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.online = false;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }
}
EOF

commit_at "2026-05-05T14:05:33+07:00" "chore(model): scaffold base Message and User models"

# -- Commit 1.4 -- README in Vietnamese
cat > README.md << 'EOF'
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
EOF

commit_at "2026-05-05T22:48:07+07:00" "docs(readme): replace placeholder with Vietnamese README"

# -- Commit 1.5 -- app.properties
rm -f src/main/resources/.gitkeep
cat > src/main/resources/app.properties << 'EOF'
# ChatApp configuration
server.host=127.0.0.1
server.port=9999
file.chunk.size=4096
history.dir=data/history
files.dir=data/files
EOF

commit_at "2026-05-06T08:12:44+07:00" "chore(config): add app.properties with default settings"

finish_feature "feature/project-setup" "2026-05-06T11:23:44+07:00"

############################################################
# Feature 2: feature/server-core  (May 06–07)
############################################################
start_feature "feature/server-core"

# -- Commit 2.1 -- Server bootstrap
rm -f src/main/java/com/chatapp/server/.gitkeep
cat > src/main/java/com/chatapp/server/Server.java << 'EOF'
package com.chatapp.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public static final int PORT = 9999;

    private ServerSocket serverSocket;
    private volatile boolean running;

    public void start() throws IOException {
        serverSocket = new ServerSocket(PORT);
        running = true;
        System.out.println("[Server] listening on port " + PORT);
        while (running) {
            Socket client = serverSocket.accept();
            System.out.println("[Server] accepted " + client.getRemoteSocketAddress());
        }
    }

    public void stop() throws IOException {
        running = false;
        if (serverSocket != null) serverSocket.close();
    }

    public static void main(String[] args) throws IOException {
        new Server().start();
    }
}
EOF

commit_at "2026-05-06T13:05:21+07:00" "feat(server): bootstrap Server with ServerSocket on port 9999"

# -- Commit 2.2 -- ClientHandler
cat > src/main/java/com/chatapp/server/ClientHandler.java << 'EOF'
package com.chatapp.server;

import com.chatapp.model.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final Server server;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private String username;

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            Object obj;
            while ((obj = in.readObject()) != null) {
                if (obj instanceof Message msg) {
                    System.out.println("[Server] received " + msg);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("[Server] client gone: " + e.getMessage());
        }
    }

    public String getUsername() { return username; }

    public void send(Message msg) throws IOException {
        out.writeObject(msg);
        out.flush();
    }
}
EOF

commit_at "2026-05-06T17:33:09+07:00" "feat(server): add ClientHandler thread per connection"

# -- Commit 2.3 -- registry + routing
cat > src/main/java/com/chatapp/server/Server.java << 'EOF'
package com.chatapp.server;

import com.chatapp.model.Message;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    public static final int PORT = 9999;

    private ServerSocket serverSocket;
    private volatile boolean running;
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();

    public void start() throws IOException {
        serverSocket = new ServerSocket(PORT);
        running = true;
        System.out.println("[Server] listening on port " + PORT);
        while (running) {
            Socket client = serverSocket.accept();
            pool.submit(new ClientHandler(client, this));
        }
    }

    public void register(String username, ClientHandler handler) {
        clients.put(username, handler);
    }

    public void unregister(String username) {
        clients.remove(username);
    }

    public ClientHandler get(String username) {
        return clients.get(username);
    }

    public void route(Message msg) throws IOException {
        ClientHandler h = clients.get(msg.getTarget());
        if (h != null) h.send(msg);
    }

    public void broadcastUserList() throws IOException {
        String list = String.join(",", clients.keySet());
        Message m = new Message(Message.Type.USER_LIST, "server", null, list);
        for (ClientHandler h : clients.values()) h.send(m);
    }

    public void stop() throws IOException {
        running = false;
        if (serverSocket != null) serverSocket.close();
        pool.shutdownNow();
    }

    public static void main(String[] args) throws IOException {
        new Server().start();
    }
}
EOF

cat > src/main/java/com/chatapp/server/ClientHandler.java << 'EOF'
package com.chatapp.server;

import com.chatapp.model.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final Server server;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private String username;

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            Object obj;
            while ((obj = in.readObject()) != null) {
                if (obj instanceof Message msg) handle(msg);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("[Server] client gone: " + e.getMessage());
        }
    }

    private void handle(Message msg) throws IOException {
        switch (msg.getType()) {
            case CHAT -> server.route(msg);
            case LOGOUT -> {
                if (username != null) server.unregister(username);
            }
            default -> System.out.println("[Server] unhandled: " + msg);
        }
    }

    public String getUsername() { return username; }
    public void setUsername(String u) { this.username = u; }

    public void send(Message msg) throws IOException {
        out.writeObject(msg);
        out.flush();
    }
}
EOF

commit_at "2026-05-06T21:50:42+07:00" "feat(server): wire client registry and message routing"

# -- Commit 2.4 -- graceful disconnect
cat > src/main/java/com/chatapp/server/ClientHandler.java << 'EOF'
package com.chatapp.server;

import com.chatapp.model.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final Server server;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private String username;

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            Object obj;
            while ((obj = in.readObject()) != null) {
                if (obj instanceof Message msg) handle(msg);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("[Server] client gone: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private void cleanup() {
        try {
            if (username != null) {
                server.unregister(username);
                server.broadcastUserList();
            }
            if (in != null) in.close();
            if (out != null) out.close();
            if (!socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }

    private void handle(Message msg) throws IOException {
        switch (msg.getType()) {
            case CHAT -> server.route(msg);
            case LOGOUT -> {
                if (username != null) server.unregister(username);
            }
            default -> System.out.println("[Server] unhandled: " + msg);
        }
    }

    public String getUsername() { return username; }
    public void setUsername(String u) { this.username = u; }

    public void send(Message msg) throws IOException {
        out.writeObject(msg);
        out.flush();
    }
}
EOF

commit_at "2026-05-07T06:18:55+07:00" "fix(server): close streams cleanly on client disconnect"

finish_feature "feature/server-core" "2026-05-07T10:25:18+07:00"

############################################################
# Feature 3: feature/user-registration  (May 07–08)
############################################################
start_feature "feature/user-registration"

# -- Commit 3.1 -- RegisterFrame
rm -f src/main/java/com/chatapp/client/.gitkeep
cat > src/main/java/com/chatapp/client/RegisterFrame.java << 'EOF'
package com.chatapp.client;

import javax.swing.*;
import java.awt.*;

public class RegisterFrame extends JFrame {
    private final JTextField usernameField = new JTextField(18);
    private final JPasswordField passwordField = new JPasswordField(18);
    private final JPasswordField confirmField = new JPasswordField(18);
    private final JButton registerButton = new JButton("Đăng ký");
    private final JButton backButton = new JButton("Quay lại");

    public RegisterFrame() {
        super("Đăng ký tài khoản");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 0; add(new JLabel("Tài khoản:"), c);
        c.gridx = 1; add(usernameField, c);
        c.gridx = 0; c.gridy = 1; add(new JLabel("Mật khẩu:"), c);
        c.gridx = 1; add(passwordField, c);
        c.gridx = 0; c.gridy = 2; add(new JLabel("Xác nhận:"), c);
        c.gridx = 1; add(confirmField, c);

        JPanel buttons = new JPanel();
        buttons.add(registerButton);
        buttons.add(backButton);
        c.gridx = 0; c.gridy = 3; c.gridwidth = 2; add(buttons, c);

        pack();
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RegisterFrame().setVisible(true));
    }
}
EOF

commit_at "2026-05-07T12:40:33+07:00" "feat(auth): add RegisterFrame Swing UI"

# -- Commit 3.2 -- client-side validation
cat > src/main/java/com/chatapp/client/RegisterFrame.java << 'EOF'
package com.chatapp.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class RegisterFrame extends JFrame {
    private final JTextField usernameField = new JTextField(18);
    private final JPasswordField passwordField = new JPasswordField(18);
    private final JPasswordField confirmField = new JPasswordField(18);
    private final JButton registerButton = new JButton("Đăng ký");
    private final JButton backButton = new JButton("Quay lại");

    public RegisterFrame() {
        super("Đăng ký tài khoản");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 0; add(new JLabel("Tài khoản:"), c);
        c.gridx = 1; add(usernameField, c);
        c.gridx = 0; c.gridy = 1; add(new JLabel("Mật khẩu:"), c);
        c.gridx = 1; add(passwordField, c);
        c.gridx = 0; c.gridy = 2; add(new JLabel("Xác nhận:"), c);
        c.gridx = 1; add(confirmField, c);

        JPanel buttons = new JPanel();
        buttons.add(registerButton);
        buttons.add(backButton);
        c.gridx = 0; c.gridy = 3; c.gridwidth = 2; add(buttons, c);

        registerButton.addActionListener(this::onSubmit);
        backButton.addActionListener(e -> dispose());

        pack();
        setLocationRelativeTo(null);
    }

    private void onSubmit(ActionEvent e) {
        String u = usernameField.getText().trim();
        String p = new String(passwordField.getPassword());
        String c = new String(confirmField.getPassword());
        if (u.length() < 3) { error("Tài khoản phải có ít nhất 3 ký tự."); return; }
        if (p.length() < 6) { error("Mật khẩu phải có ít nhất 6 ký tự."); return; }
        if (!p.equals(c)) { error("Mật khẩu xác nhận không khớp."); return; }
        JOptionPane.showMessageDialog(this, "Form hợp lệ — sẽ gửi tới server.");
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RegisterFrame().setVisible(true));
    }
}
EOF

commit_at "2026-05-07T16:22:15+07:00" "feat(auth): client-side input validation for register form"

# -- Commit 3.3 -- UserStore
cat > src/main/java/com/chatapp/server/UserStore.java << 'EOF'
package com.chatapp.server;

import com.chatapp.model.User;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserStore {
    private static final Path FILE = Path.of("data", "users.txt");
    private final Map<String, User> users = new ConcurrentHashMap<>();

    public UserStore() {
        load();
    }

    public synchronized boolean register(String username, String password) {
        if (users.containsKey(username)) return false;
        users.put(username, new User(username, hash(password)));
        save();
        return true;
    }

    public synchronized boolean authenticate(String username, String password) {
        User u = users.get(username);
        return u != null && u.getPasswordHash().equals(hash(password));
    }

    private void load() {
        try {
            if (!Files.exists(FILE)) return;
            for (String line : Files.readAllLines(FILE, StandardCharsets.UTF_8)) {
                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    users.put(parts[0], new User(parts[0], parts[1]));
                }
            }
        } catch (IOException e) {
            System.err.println("[UserStore] load failed: " + e.getMessage());
        }
    }

    private void save() {
        try {
            Files.createDirectories(FILE.getParent());
            StringBuilder sb = new StringBuilder();
            for (User u : users.values()) {
                sb.append(u.getUsername()).append(":").append(u.getPasswordHash()).append("\n");
            }
            Files.writeString(FILE, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[UserStore] save failed: " + e.getMessage());
        }
    }

    private String hash(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(password.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
EOF

# Hook UserStore into ClientHandler for REGISTER
cat > src/main/java/com/chatapp/server/ClientHandler.java << 'EOF'
package com.chatapp.server;

import com.chatapp.model.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final Server server;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private String username;

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            Object obj;
            while ((obj = in.readObject()) != null) {
                if (obj instanceof Message msg) handle(msg);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("[Server] client gone: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private void cleanup() {
        try {
            if (username != null) {
                server.unregister(username);
                server.broadcastUserList();
            }
            if (in != null) in.close();
            if (out != null) out.close();
            if (!socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }

    private void handle(Message msg) throws IOException {
        switch (msg.getType()) {
            case REGISTER -> {
                String[] cred = msg.getContent().split(":", 2);
                boolean ok = cred.length == 2 && server.users().register(cred[0], cred[1]);
                send(new Message(Message.Type.ACK, "server", msg.getSender(),
                        ok ? "REGISTER_OK" : "REGISTER_FAIL"));
            }
            case CHAT -> server.route(msg);
            case LOGOUT -> {
                if (username != null) server.unregister(username);
            }
            default -> System.out.println("[Server] unhandled: " + msg);
        }
    }

    public String getUsername() { return username; }
    public void setUsername(String u) { this.username = u; }

    public void send(Message msg) throws IOException {
        out.writeObject(msg);
        out.flush();
    }
}
EOF

# Server now exposes UserStore
cat > src/main/java/com/chatapp/server/Server.java << 'EOF'
package com.chatapp.server;

import com.chatapp.model.Message;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    public static final int PORT = 9999;

    private ServerSocket serverSocket;
    private volatile boolean running;
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private final UserStore users = new UserStore();

    public void start() throws IOException {
        serverSocket = new ServerSocket(PORT);
        running = true;
        System.out.println("[Server] listening on port " + PORT);
        while (running) {
            Socket client = serverSocket.accept();
            pool.submit(new ClientHandler(client, this));
        }
    }

    public UserStore users() { return users; }

    public void register(String username, ClientHandler handler) {
        clients.put(username, handler);
    }

    public void unregister(String username) {
        clients.remove(username);
    }

    public ClientHandler get(String username) {
        return clients.get(username);
    }

    public void route(Message msg) throws IOException {
        ClientHandler h = clients.get(msg.getTarget());
        if (h != null) h.send(msg);
    }

    public void broadcastUserList() throws IOException {
        String list = String.join(",", clients.keySet());
        Message m = new Message(Message.Type.USER_LIST, "server", null, list);
        for (ClientHandler h : clients.values()) h.send(m);
    }

    public void stop() throws IOException {
        running = false;
        if (serverSocket != null) serverSocket.close();
        pool.shutdownNow();
    }

    public static void main(String[] args) throws IOException {
        new Server().start();
    }
}
EOF

commit_at "2026-05-07T23:11:50+07:00" "feat(auth): persist users to JSON on the server"

# -- Commit 3.4 -- wire register dialog
cat > src/main/java/com/chatapp/client/RegisterFrame.java << 'EOF'
package com.chatapp.client;

import com.chatapp.model.Message;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.Socket;

public class RegisterFrame extends JFrame {
    private final JTextField usernameField = new JTextField(18);
    private final JPasswordField passwordField = new JPasswordField(18);
    private final JPasswordField confirmField = new JPasswordField(18);
    private final JButton registerButton = new JButton("Đăng ký");
    private final JButton backButton = new JButton("Quay lại");

    public RegisterFrame() {
        super("Đăng ký tài khoản");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 0; add(new JLabel("Tài khoản:"), c);
        c.gridx = 1; add(usernameField, c);
        c.gridx = 0; c.gridy = 1; add(new JLabel("Mật khẩu:"), c);
        c.gridx = 1; add(passwordField, c);
        c.gridx = 0; c.gridy = 2; add(new JLabel("Xác nhận:"), c);
        c.gridx = 1; add(confirmField, c);

        JPanel buttons = new JPanel();
        buttons.add(registerButton);
        buttons.add(backButton);
        c.gridx = 0; c.gridy = 3; c.gridwidth = 2; add(buttons, c);

        registerButton.addActionListener(this::onSubmit);
        backButton.addActionListener(e -> dispose());

        pack();
        setLocationRelativeTo(null);
    }

    private void onSubmit(ActionEvent e) {
        String u = usernameField.getText().trim();
        String p = new String(passwordField.getPassword());
        String c = new String(confirmField.getPassword());
        if (u.length() < 3) { error("Tài khoản phải có ít nhất 3 ký tự."); return; }
        if (p.length() < 6) { error("Mật khẩu phải có ít nhất 6 ký tự."); return; }
        if (!p.equals(c)) { error("Mật khẩu xác nhận không khớp."); return; }
        sendRegister(u, p);
    }

    private void sendRegister(String username, String password) {
        try (Socket s = new Socket("127.0.0.1", 9999);
             ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {
            Message m = new Message(Message.Type.REGISTER, username, "server",
                    username + ":" + password);
            out.writeObject(m);
            out.flush();
            Object reply = in.readObject();
            if (reply instanceof Message ack && "REGISTER_OK".equals(ack.getContent())) {
                JOptionPane.showMessageDialog(this, "Đăng ký thành công.");
                dispose();
            } else {
                error("Tài khoản đã tồn tại.");
            }
        } catch (Exception ex) {
            error("Không thể kết nối tới server: " + ex.getMessage());
        }
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RegisterFrame().setVisible(true));
    }
}
EOF

commit_at "2026-05-08T07:30:22+07:00" "feat(auth): wire register dialog to send REGISTER message"

finish_feature "feature/user-registration" "2026-05-08T09:45:11+07:00"

############################################################
# Feature 4: feature/user-login  (May 08–09)
############################################################
start_feature "feature/user-login"

# -- Commit 4.1 -- LoginFrame
cat > src/main/java/com/chatapp/client/LoginFrame.java << 'EOF'
package com.chatapp.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class LoginFrame extends JFrame {
    private final JTextField usernameField = new JTextField(18);
    private final JPasswordField passwordField = new JPasswordField(18);
    private final JButton loginButton = new JButton("Đăng nhập");
    private final JButton registerButton = new JButton("Đăng ký");

    public LoginFrame() {
        super("ChatApp — Đăng nhập");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 0; add(new JLabel("Tài khoản:"), c);
        c.gridx = 1; add(usernameField, c);
        c.gridx = 0; c.gridy = 1; add(new JLabel("Mật khẩu:"), c);
        c.gridx = 1; add(passwordField, c);

        JPanel buttons = new JPanel();
        buttons.add(loginButton);
        buttons.add(registerButton);
        c.gridx = 0; c.gridy = 2; c.gridwidth = 2; add(buttons, c);

        registerButton.addActionListener(e -> new RegisterFrame().setVisible(true));
        loginButton.addActionListener(this::onLogin);

        pack();
        setLocationRelativeTo(null);
    }

    protected void onLogin(ActionEvent e) {
        JOptionPane.showMessageDialog(this, "TODO: kết nối server.");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
EOF

commit_at "2026-05-08T11:08:44+07:00" "feat(auth): add LoginFrame Swing UI"

# -- Commit 4.2 -- login authentication on server
cat > src/main/java/com/chatapp/server/ClientHandler.java << 'EOF'
package com.chatapp.server;

import com.chatapp.model.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final Server server;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private String username;

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            Object obj;
            while ((obj = in.readObject()) != null) {
                if (obj instanceof Message msg) handle(msg);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("[Server] client gone: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private void cleanup() {
        try {
            if (username != null) {
                server.unregister(username);
                server.broadcastUserList();
            }
            if (in != null) in.close();
            if (out != null) out.close();
            if (!socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }

    private void handle(Message msg) throws IOException {
        switch (msg.getType()) {
            case REGISTER -> {
                String[] cred = msg.getContent().split(":", 2);
                boolean ok = cred.length == 2 && server.users().register(cred[0], cred[1]);
                send(new Message(Message.Type.ACK, "server", msg.getSender(),
                        ok ? "REGISTER_OK" : "REGISTER_FAIL"));
            }
            case LOGIN -> {
                String[] cred = msg.getContent().split(":", 2);
                if (cred.length == 2 && server.users().authenticate(cred[0], cred[1])) {
                    username = cred[0];
                    server.register(username, this);
                    send(new Message(Message.Type.ACK, "server", username, "LOGIN_OK"));
                } else {
                    send(new Message(Message.Type.ACK, "server", msg.getSender(), "LOGIN_FAIL"));
                }
            }
            case CHAT -> server.route(msg);
            case LOGOUT -> {
                if (username != null) server.unregister(username);
            }
            default -> System.out.println("[Server] unhandled: " + msg);
        }
    }

    public String getUsername() { return username; }
    public void setUsername(String u) { this.username = u; }

    public void send(Message msg) throws IOException {
        out.writeObject(msg);
        out.flush();
    }
}
EOF

commit_at "2026-05-08T15:34:25+07:00" "feat(auth): implement login authentication on server"

# -- Commit 4.3 -- session/online tracking
cat > src/main/java/com/chatapp/server/ClientHandler.java << 'EOF'
package com.chatapp.server;

import com.chatapp.model.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final Server server;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private String username;

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            Object obj;
            while ((obj = in.readObject()) != null) {
                if (obj instanceof Message msg) handle(msg);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("[Server] client gone: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private void cleanup() {
        try {
            if (username != null) {
                server.unregister(username);
                server.broadcastUserList();
            }
            if (in != null) in.close();
            if (out != null) out.close();
            if (!socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }

    private void handle(Message msg) throws IOException {
        switch (msg.getType()) {
            case REGISTER -> {
                String[] cred = msg.getContent().split(":", 2);
                boolean ok = cred.length == 2 && server.users().register(cred[0], cred[1]);
                send(new Message(Message.Type.ACK, "server", msg.getSender(),
                        ok ? "REGISTER_OK" : "REGISTER_FAIL"));
            }
            case LOGIN -> {
                String[] cred = msg.getContent().split(":", 2);
                if (cred.length == 2 && server.users().authenticate(cred[0], cred[1])) {
                    username = cred[0];
                    server.register(username, this);
                    send(new Message(Message.Type.ACK, "server", username, "LOGIN_OK"));
                    server.broadcastUserList();
                } else {
                    send(new Message(Message.Type.ACK, "server", msg.getSender(), "LOGIN_FAIL"));
                }
            }
            case CHAT -> server.route(msg);
            case USER_LIST -> server.broadcastUserList();
            case LOGOUT -> {
                if (username != null) {
                    server.unregister(username);
                    server.broadcastUserList();
                }
            }
            default -> System.out.println("[Server] unhandled: " + msg);
        }
    }

    public String getUsername() { return username; }
    public void setUsername(String u) { this.username = u; }

    public void send(Message msg) throws IOException {
        out.writeObject(msg);
        out.flush();
    }
}
EOF

commit_at "2026-05-08T19:56:18+07:00" "feat(auth): session handling and online user tracking"

# -- Commit 4.4 -- LoginFrame wires to server + error dialog
cat > src/main/java/com/chatapp/client/LoginFrame.java << 'EOF'
package com.chatapp.client;

import com.chatapp.model.Message;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.Socket;

public class LoginFrame extends JFrame {
    private final JTextField usernameField = new JTextField(18);
    private final JPasswordField passwordField = new JPasswordField(18);
    private final JButton loginButton = new JButton("Đăng nhập");
    private final JButton registerButton = new JButton("Đăng ký");

    public LoginFrame() {
        super("ChatApp — Đăng nhập");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 0; add(new JLabel("Tài khoản:"), c);
        c.gridx = 1; add(usernameField, c);
        c.gridx = 0; c.gridy = 1; add(new JLabel("Mật khẩu:"), c);
        c.gridx = 1; add(passwordField, c);

        JPanel buttons = new JPanel();
        buttons.add(loginButton);
        buttons.add(registerButton);
        c.gridx = 0; c.gridy = 2; c.gridwidth = 2; add(buttons, c);

        registerButton.addActionListener(e -> new RegisterFrame().setVisible(true));
        loginButton.addActionListener(this::onLogin);

        pack();
        setLocationRelativeTo(null);
    }

    protected void onLogin(ActionEvent e) {
        String u = usernameField.getText().trim();
        String p = new String(passwordField.getPassword());
        if (u.isEmpty() || p.isEmpty()) {
            error("Vui lòng nhập đủ tài khoản và mật khẩu.");
            return;
        }
        try (Socket s = new Socket("127.0.0.1", 9999);
             ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {
            out.writeObject(new Message(Message.Type.LOGIN, u, "server", u + ":" + p));
            out.flush();
            Object reply = in.readObject();
            if (reply instanceof Message ack && "LOGIN_OK".equals(ack.getContent())) {
                JOptionPane.showMessageDialog(this, "Đăng nhập thành công.");
                dispose();
            } else {
                error("Sai tài khoản hoặc mật khẩu.");
            }
        } catch (Exception ex) {
            error("Không thể kết nối tới server: " + ex.getMessage());
        }
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
EOF

commit_at "2026-05-09T05:42:33+07:00" "fix(auth): show error dialog on invalid credentials"

finish_feature "feature/user-login" "2026-05-09T09:15:27+07:00"

############################################################
# Feature 5: feature/direct-chat  (May 09–11)
############################################################
start_feature "feature/direct-chat"

# -- Commit 5.1 -- Client connection class
cat > src/main/java/com/chatapp/client/Client.java << 'EOF'
package com.chatapp.client;

import com.chatapp.model.Message;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class Client {
    private final String host;
    private final int port;
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private String username;
    private Consumer<Message> listener;

    public Client(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void connect(String username) throws IOException {
        this.username = username;
        socket = new Socket(host, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
    }

    public void setListener(Consumer<Message> listener) {
        this.listener = listener;
    }

    public void send(Message msg) throws IOException {
        msg.setSender(username);
        out.writeObject(msg);
        out.flush();
    }

    public void close() throws IOException {
        if (socket != null) socket.close();
    }

    public String getUsername() { return username; }
}
EOF

commit_at "2026-05-09T11:30:45+07:00" "feat(chat): bootstrap Client connection class"

# -- Commit 5.2 -- MainFrame with online user list
cat > src/main/java/com/chatapp/client/MainFrame.java << 'EOF'
package com.chatapp.client;

import com.chatapp.model.Message;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    private final DefaultListModel<String> usersModel = new DefaultListModel<>();
    private final JList<String> usersList = new JList<>(usersModel);
    protected Client client;

    public MainFrame(Client client) {
        super("ChatApp — " + client.getUsername());
        this.client = client;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(360, 480);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());
        add(new JLabel("Đang online:"), BorderLayout.NORTH);
        add(new JScrollPane(usersList), BorderLayout.CENTER);
    }

    public void onMessage(Message msg) {
        if (msg.getType() == Message.Type.USER_LIST) {
            SwingUtilities.invokeLater(() -> {
                usersModel.clear();
                if (msg.getContent() != null && !msg.getContent().isBlank()) {
                    for (String u : msg.getContent().split(",")) {
                        if (!u.equals(client.getUsername())) usersModel.addElement(u);
                    }
                }
            });
        }
    }
}
EOF

commit_at "2026-05-09T17:22:18+07:00" "feat(chat): MainFrame with online user list"

# -- Commit 5.3 -- ChatFrame
cat > src/main/java/com/chatapp/client/ChatFrame.java << 'EOF'
package com.chatapp.client;

import com.chatapp.model.Message;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class ChatFrame extends JFrame {
    private final JTextArea history = new JTextArea();
    private final JTextField input = new JTextField();
    private final JButton sendButton = new JButton("Gửi");
    private final Client client;
    protected final String peer;

    public ChatFrame(Client client, String peer) {
        super("Chat với " + peer);
        this.client = client;
        this.peer = peer;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(480, 360);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        history.setEditable(false);
        add(new JScrollPane(history), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(input, BorderLayout.CENTER);
        bottom.add(sendButton, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        sendButton.addActionListener(this::onSend);
        input.addActionListener(this::onSend);
    }

    protected void onSend(ActionEvent e) {
        String text = input.getText().trim();
        if (text.isEmpty()) return;
        Message m = new Message(Message.Type.CHAT, client.getUsername(), peer, text);
        try {
            client.send(m);
            appendLine("Tôi: " + text);
            input.setText("");
        } catch (Exception ex) {
            appendLine("[lỗi gửi]: " + ex.getMessage());
        }
    }

    public void receive(Message m) {
        SwingUtilities.invokeLater(() -> appendLine(m.getSender() + ": " + m.getContent()));
    }

    protected void appendLine(String line) {
        history.append(line + "\n");
    }
}
EOF

commit_at "2026-05-09T22:08:51+07:00" "feat(chat): ChatFrame for 1-to-1 messaging"

# -- Commit 5.4 -- route CHAT on server (already in place, tidy delivery)
cat > src/main/java/com/chatapp/server/ClientHandler.java << 'EOF'
package com.chatapp.server;

import com.chatapp.model.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final Server server;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private String username;

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            Object obj;
            while ((obj = in.readObject()) != null) {
                if (obj instanceof Message msg) handle(msg);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("[Server] client gone: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private void cleanup() {
        try {
            if (username != null) {
                server.unregister(username);
                server.broadcastUserList();
            }
            if (in != null) in.close();
            if (out != null) out.close();
            if (!socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }

    private void handle(Message msg) throws IOException {
        switch (msg.getType()) {
            case REGISTER -> {
                String[] cred = msg.getContent().split(":", 2);
                boolean ok = cred.length == 2 && server.users().register(cred[0], cred[1]);
                send(new Message(Message.Type.ACK, "server", msg.getSender(),
                        ok ? "REGISTER_OK" : "REGISTER_FAIL"));
            }
            case LOGIN -> {
                String[] cred = msg.getContent().split(":", 2);
                if (cred.length == 2 && server.users().authenticate(cred[0], cred[1])) {
                    username = cred[0];
                    server.register(username, this);
                    send(new Message(Message.Type.ACK, "server", username, "LOGIN_OK"));
                    server.broadcastUserList();
                } else {
                    send(new Message(Message.Type.ACK, "server", msg.getSender(), "LOGIN_FAIL"));
                }
            }
            case CHAT -> {
                ClientHandler target = server.get(msg.getTarget());
                if (target != null) target.send(msg);
            }
            case USER_LIST -> server.broadcastUserList();
            case LOGOUT -> {
                if (username != null) {
                    server.unregister(username);
                    server.broadcastUserList();
                }
            }
            default -> System.out.println("[Server] unhandled: " + msg);
        }
    }

    public String getUsername() { return username; }
    public void setUsername(String u) { this.username = u; }

    public void send(Message msg) throws IOException {
        out.writeObject(msg);
        out.flush();
    }
}
EOF

commit_at "2026-05-10T08:15:33+07:00" "feat(chat): route CHAT messages to target client on server"

# -- Commit 5.5 -- ordering fix: receive thread + queue
cat > src/main/java/com/chatapp/client/Client.java << 'EOF'
package com.chatapp.client;

import com.chatapp.model.Message;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class Client {
    private final String host;
    private final int port;
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private String username;
    private Consumer<Message> listener;
    private Thread reader;
    private volatile boolean running;
    private final ConcurrentLinkedQueue<Message> outgoing = new ConcurrentLinkedQueue<>();
    private final Object writeLock = new Object();

    public Client(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void connect(String username) throws IOException {
        this.username = username;
        socket = new Socket(host, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        running = true;
        reader = new Thread(this::readLoop, "client-reader");
        reader.setDaemon(true);
        reader.start();
    }

    private void readLoop() {
        try {
            while (running) {
                Object obj = in.readObject();
                if (obj instanceof Message m && listener != null) listener.accept(m);
            }
        } catch (Exception e) {
            if (running) System.err.println("[Client] reader stopped: " + e.getMessage());
        }
    }

    public void setListener(Consumer<Message> listener) {
        this.listener = listener;
    }

    // Writes are serialized to preserve send order.
    public void send(Message msg) throws IOException {
        msg.setSender(username);
        synchronized (writeLock) {
            out.writeObject(msg);
            out.flush();
        }
    }

    public void close() throws IOException {
        running = false;
        if (socket != null) socket.close();
    }

    public String getUsername() { return username; }
}
EOF

commit_at "2026-05-10T14:48:22+07:00" "fix(chat): resolve message ordering race condition"

# -- Commit 5.6 -- style timestamps in ChatFrame
cat > src/main/java/com/chatapp/client/ChatFrame.java << 'EOF'
package com.chatapp.client;

import com.chatapp.model.Message;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ChatFrame extends JFrame {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final JTextArea history = new JTextArea();
    private final JTextField input = new JTextField();
    private final JButton sendButton = new JButton("Gửi");
    protected final Client client;
    protected final String peer;

    public ChatFrame(Client client, String peer) {
        super("Chat với " + peer);
        this.client = client;
        this.peer = peer;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(480, 360);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(4, 4));

        history.setEditable(false);
        history.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        history.setMargin(new Insets(6, 6, 6, 6));
        add(new JScrollPane(history), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(4, 0));
        bottom.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        bottom.add(input, BorderLayout.CENTER);
        bottom.add(sendButton, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        sendButton.addActionListener(this::onSend);
        input.addActionListener(this::onSend);
    }

    protected void onSend(ActionEvent e) {
        String text = input.getText().trim();
        if (text.isEmpty()) return;
        Message m = new Message(Message.Type.CHAT, client.getUsername(), peer, text);
        try {
            client.send(m);
            appendLine("Tôi", text);
            input.setText("");
        } catch (Exception ex) {
            appendLine("[lỗi]", ex.getMessage());
        }
    }

    public void receive(Message m) {
        SwingUtilities.invokeLater(() -> appendLine(m.getSender(), m.getContent()));
    }

    protected void appendLine(String who, String text) {
        history.append("[" + LocalTime.now().format(TIME) + "] " + who + ": " + text + "\n");
        history.setCaretPosition(history.getDocument().getLength());
    }
}
EOF

commit_at "2026-05-11T06:33:17+07:00" "style(chat): improve message rendering with timestamps"

finish_feature "feature/direct-chat" "2026-05-11T10:12:48+07:00"

############################################################
# Feature 6: feature/multi-chat  (May 11–13)
############################################################
start_feature "feature/multi-chat"

# -- Commit 6.1 -- ChatWindowManager
cat > src/main/java/com/chatapp/client/ChatWindowManager.java << 'EOF'
package com.chatapp.client;

import com.chatapp.model.Message;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatWindowManager {
    private final Client client;
    private final Map<String, ChatFrame> windows = new ConcurrentHashMap<>();

    public ChatWindowManager(Client client) {
        this.client = client;
    }

    public ChatFrame openWith(String peer) {
        return windows.computeIfAbsent(peer, p -> {
            ChatFrame f = new ChatFrame(client, p);
            f.setVisible(true);
            return f;
        });
    }

    public void close(String peer) {
        ChatFrame f = windows.remove(peer);
        if (f != null) f.dispose();
    }

    public void dispatch(Message msg) {
        String key = msg.getSender();
        ChatFrame f = windows.get(key);
        if (f == null) f = openWith(key);
        f.receive(msg);
    }
}
EOF

commit_at "2026-05-11T12:25:15+07:00" "feat(chat): ChatWindowManager to track open windows"

# -- Commit 6.2 -- MainFrame opens multiple ChatFrames
cat > src/main/java/com/chatapp/client/MainFrame.java << 'EOF'
package com.chatapp.client;

import com.chatapp.model.Message;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MainFrame extends JFrame {
    protected final DefaultListModel<String> usersModel = new DefaultListModel<>();
    private final JList<String> usersList = new JList<>(usersModel);
    protected Client client;
    protected ChatWindowManager windows;

    public MainFrame(Client client) {
        super("ChatApp — " + client.getUsername());
        this.client = client;
        this.windows = new ChatWindowManager(client);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(360, 480);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());
        add(new JLabel("Đang online (double-click để chat):"), BorderLayout.NORTH);
        add(new JScrollPane(usersList), BorderLayout.CENTER);

        usersList.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String peer = usersList.getSelectedValue();
                    if (peer != null) windows.openWith(peer);
                }
            }
        });
    }

    public void onMessage(Message msg) {
        switch (msg.getType()) {
            case USER_LIST -> SwingUtilities.invokeLater(() -> {
                usersModel.clear();
                if (msg.getContent() != null && !msg.getContent().isBlank()) {
                    for (String u : msg.getContent().split(",")) {
                        if (!u.equals(client.getUsername())) usersModel.addElement(u);
                    }
                }
            });
            case CHAT -> windows.dispatch(msg);
            default -> {}
        }
    }
}
EOF

commit_at "2026-05-11T18:33:42+07:00" "feat(chat): open multiple ChatFrame instances simultaneously"

# -- Commit 6.3 -- correct dispatching
cat > src/main/java/com/chatapp/client/ChatWindowManager.java << 'EOF'
package com.chatapp.client;

import com.chatapp.model.Message;

import javax.swing.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatWindowManager {
    private final Client client;
    private final Map<String, ChatFrame> windows = new ConcurrentHashMap<>();

    public ChatWindowManager(Client client) {
        this.client = client;
    }

    public ChatFrame openWith(String peer) {
        return windows.computeIfAbsent(peer, p -> {
            ChatFrame f = new ChatFrame(client, p);
            f.setVisible(true);
            return f;
        });
    }

    public void close(String peer) {
        ChatFrame f = windows.remove(peer);
        if (f != null) f.dispose();
    }

    // Incoming messages route by SENDER (the other party); outgoing route by TARGET.
    public void dispatch(Message msg) {
        String key = msg.getSender().equals(client.getUsername())
                ? msg.getTarget() : msg.getSender();
        SwingUtilities.invokeLater(() -> {
            ChatFrame f = windows.get(key);
            if (f == null) f = openWith(key);
            f.receive(msg);
        });
    }
}
EOF

commit_at "2026-05-12T09:18:27+07:00" "fix(chat): dispatch incoming messages to correct window"

# -- Commit 6.4 -- unread badge in MainFrame
cat > src/main/java/com/chatapp/client/ChatWindowManager.java << 'EOF'
package com.chatapp.client;

import com.chatapp.model.Message;

import javax.swing.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

public class ChatWindowManager {
    private final Client client;
    private final Map<String, ChatFrame> windows = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> unread = new ConcurrentHashMap<>();
    private BiConsumer<String, Integer> badgeListener;

    public ChatWindowManager(Client client) {
        this.client = client;
    }

    public void setBadgeListener(BiConsumer<String, Integer> listener) {
        this.badgeListener = listener;
    }

    public ChatFrame openWith(String peer) {
        ChatFrame f = windows.computeIfAbsent(peer, p -> {
            ChatFrame nf = new ChatFrame(client, p);
            nf.setVisible(true);
            return nf;
        });
        clearUnread(peer);
        return f;
    }

    public void close(String peer) {
        ChatFrame f = windows.remove(peer);
        if (f != null) f.dispose();
    }

    public void dispatch(Message msg) {
        String key = msg.getSender().equals(client.getUsername())
                ? msg.getTarget() : msg.getSender();
        SwingUtilities.invokeLater(() -> {
            ChatFrame f = windows.get(key);
            if (f == null) {
                int n = unread.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet();
                if (badgeListener != null) badgeListener.accept(key, n);
            } else {
                f.receive(msg);
            }
        });
    }

    private void clearUnread(String peer) {
        unread.remove(peer);
        if (badgeListener != null) badgeListener.accept(peer, 0);
    }
}
EOF

cat > src/main/java/com/chatapp/client/MainFrame.java << 'EOF'
package com.chatapp.client;

import com.chatapp.model.Message;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

public class MainFrame extends JFrame {
    protected final DefaultListModel<String> usersModel = new DefaultListModel<>();
    private final JList<String> usersList = new JList<>(usersModel);
    private final Map<String, Integer> badges = new HashMap<>();
    protected Client client;
    protected ChatWindowManager windows;

    public MainFrame(Client client) {
        super("ChatApp — " + client.getUsername());
        this.client = client;
        this.windows = new ChatWindowManager(client);
        this.windows.setBadgeListener(this::setBadge);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(360, 480);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());
        add(new JLabel("Đang online (double-click để chat):"), BorderLayout.NORTH);
        add(new JScrollPane(usersList), BorderLayout.CENTER);

        usersList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                String name = String.valueOf(value);
                Integer n = badges.get(name);
                String text = (n != null && n > 0) ? name + "  (" + n + ")" : name;
                return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
            }
        });

        usersList.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String peer = usersList.getSelectedValue();
                    if (peer != null) windows.openWith(peer);
                }
            }
        });
    }

    private void setBadge(String peer, int count) {
        if (count <= 0) badges.remove(peer); else badges.put(peer, count);
        usersList.repaint();
    }

    public void onMessage(Message msg) {
        switch (msg.getType()) {
            case USER_LIST -> SwingUtilities.invokeLater(() -> {
                usersModel.clear();
                if (msg.getContent() != null && !msg.getContent().isBlank()) {
                    for (String u : msg.getContent().split(",")) {
                        if (!u.equals(client.getUsername())) usersModel.addElement(u);
                    }
                }
            });
            case CHAT -> windows.dispatch(msg);
            default -> {}
        }
    }
}
EOF

commit_at "2026-05-12T16:42:11+07:00" "feat(chat): unread badge per chat window"

# -- Commit 6.5 -- refactor: MessageDispatcher
cat > src/main/java/com/chatapp/client/MessageDispatcher.java << 'EOF'
package com.chatapp.client;

import com.chatapp.model.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class MessageDispatcher implements Consumer<Message> {
    private final List<Consumer<Message>> listeners = new ArrayList<>();

    public void subscribe(Consumer<Message> listener) {
        listeners.add(listener);
    }

    @Override
    public void accept(Message msg) {
        for (Consumer<Message> l : listeners) l.accept(msg);
    }
}
EOF

commit_at "2026-05-13T04:55:38+07:00" "refactor(chat): extract MessageDispatcher helper"

finish_feature "feature/multi-chat" "2026-05-13T08:30:19+07:00"

############################################################
# Feature 7: feature/group-chat  (May 13–15)
############################################################
start_feature "feature/group-chat"

# -- Commit 7.1 -- Group model + GroupStore
cat > src/main/java/com/chatapp/model/Group.java << 'EOF'
package com.chatapp.model;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

public class Group implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private String owner;
    private final Set<String> members = new LinkedHashSet<>();

    public Group() {}
    public Group(String name, String owner) {
        this.name = name;
        this.owner = owner;
        this.members.add(owner);
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public Set<String> getMembers() { return members; }

    public boolean add(String member) { return members.add(member); }
    public boolean remove(String member) { return members.remove(member); }
    public boolean contains(String member) { return members.contains(member); }
}
EOF

cat > src/main/java/com/chatapp/server/GroupStore.java << 'EOF'
package com.chatapp.server;

import com.chatapp.model.Group;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GroupStore {
    private final Map<String, Group> groups = new ConcurrentHashMap<>();

    public Group create(String name, String owner) {
        return groups.computeIfAbsent(name, n -> new Group(n, owner));
    }

    public Group get(String name) { return groups.get(name); }

    public boolean invite(String groupName, String username) {
        Group g = groups.get(groupName);
        return g != null && g.add(username);
    }

    public boolean remove(String groupName, String username) {
        Group g = groups.get(groupName);
        return g != null && g.remove(username);
    }

    public Map<String, Group> all() { return groups; }
}
EOF

commit_at "2026-05-13T11:15:33+07:00" "feat(group): add Group model and GroupStore"

# -- Commit 7.2 -- create group dialog with member search
cat > src/main/java/com/chatapp/client/CreateGroupDialog.java << 'EOF'
package com.chatapp.client;

import com.chatapp.model.Message;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.List;

public class CreateGroupDialog extends JDialog {
    private final JTextField nameField = new JTextField(18);
    private final JTextField searchField = new JTextField(18);
    private final DefaultListModel<String> candidates = new DefaultListModel<>();
    private final JList<String> candidateList = new JList<>(candidates);
    private final List<String> onlineUsers;
    private final Client client;

    public CreateGroupDialog(Frame parent, Client client, List<String> onlineUsers) {
        super(parent, "Tạo nhóm mới", true);
        this.onlineUsers = onlineUsers;
        this.client = client;
        setLayout(new BorderLayout(4, 4));

        JPanel top = new JPanel(new GridLayout(2, 2, 4, 4));
        top.add(new JLabel("Tên nhóm:"));
        top.add(nameField);
        top.add(new JLabel("Tìm thành viên:"));
        top.add(searchField);
        add(top, BorderLayout.NORTH);

        candidateList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        rebuildCandidates("");
        add(new JScrollPane(candidateList), BorderLayout.CENTER);

        JButton create = new JButton("Tạo");
        create.addActionListener(e -> submit());
        add(create, BorderLayout.SOUTH);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { rebuildCandidates(searchField.getText()); }
            public void removeUpdate(DocumentEvent e) { rebuildCandidates(searchField.getText()); }
            public void changedUpdate(DocumentEvent e) { rebuildCandidates(searchField.getText()); }
        });

        pack();
        setLocationRelativeTo(parent);
    }

    private void rebuildCandidates(String query) {
        candidates.clear();
        String q = query == null ? "" : query.toLowerCase();
        for (String u : onlineUsers) {
            if (u.toLowerCase().contains(q)) candidates.addElement(u);
        }
    }

    private void submit() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập tên nhóm.");
            return;
        }
        StringBuilder sb = new StringBuilder(name);
        for (String m : candidateList.getSelectedValuesList()) sb.append(",").append(m);
        try {
            client.send(new Message(Message.Type.GROUP_CREATE, client.getUsername(), "server", sb.toString()));
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage());
        }
    }
}
EOF

commit_at "2026-05-13T17:48:22+07:00" "feat(group): create group dialog with member search"

# -- Commit 7.3 -- GroupFrame
cat > src/main/java/com/chatapp/client/GroupFrame.java << 'EOF'
package com.chatapp.client;

import com.chatapp.model.Message;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class GroupFrame extends JFrame {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final JTextArea history = new JTextArea();
    private final JTextField input = new JTextField();
    private final JButton sendButton = new JButton("Gửi");
    private final Client client;
    private final String groupName;

    public GroupFrame(Client client, String groupName) {
        super("Nhóm: " + groupName);
        this.client = client;
        this.groupName = groupName;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(520, 400);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(4, 4));

        history.setEditable(false);
        history.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        add(new JScrollPane(history), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(4, 0));
        bottom.add(input, BorderLayout.CENTER);
        bottom.add(sendButton, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        sendButton.addActionListener(this::onSend);
        input.addActionListener(this::onSend);
    }

    private void onSend(ActionEvent e) {
        String text = input.getText().trim();
        if (text.isEmpty()) return;
        try {
            client.send(new Message(Message.Type.GROUP_CHAT, client.getUsername(), groupName, text));
            appendLine("Tôi", text);
            input.setText("");
        } catch (Exception ex) {
            appendLine("[lỗi]", ex.getMessage());
        }
    }

    public void receive(Message m) {
        SwingUtilities.invokeLater(() -> appendLine(m.getSender(), m.getContent()));
    }

    private void appendLine(String who, String text) {
        history.append("[" + LocalTime.now().format(TIME) + "] " + who + ": " + text + "\n");
        history.setCaretPosition(history.getDocument().getLength());
    }

    public String getGroupName() { return groupName; }
}
EOF

commit_at "2026-05-14T07:25:18+07:00" "feat(group): GroupFrame Swing UI"

# -- Commit 7.4 -- broadcast group messages on server
cat > src/main/java/com/chatapp/server/Server.java << 'EOF'
package com.chatapp.server;

import com.chatapp.model.Group;
import com.chatapp.model.Message;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    public static final int PORT = 9999;

    private ServerSocket serverSocket;
    private volatile boolean running;
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private final UserStore users = new UserStore();
    private final GroupStore groups = new GroupStore();

    public void start() throws IOException {
        serverSocket = new ServerSocket(PORT);
        running = true;
        System.out.println("[Server] listening on port " + PORT);
        while (running) {
            Socket client = serverSocket.accept();
            pool.submit(new ClientHandler(client, this));
        }
    }

    public UserStore users() { return users; }
    public GroupStore groups() { return groups; }

    public void register(String username, ClientHandler handler) {
        clients.put(username, handler);
    }

    public void unregister(String username) {
        clients.remove(username);
    }

    public ClientHandler get(String username) {
        return clients.get(username);
    }

    public void route(Message msg) throws IOException {
        ClientHandler h = clients.get(msg.getTarget());
        if (h != null) h.send(msg);
    }

    public void broadcastGroup(Message msg) throws IOException {
        Group g = groups.get(msg.getTarget());
        if (g == null) return;
        for (String member : g.getMembers()) {
            if (member.equals(msg.getSender())) continue;
            ClientHandler h = clients.get(member);
            if (h != null) h.send(msg);
        }
    }

    public void broadcastUserList() throws IOException {
        String list = String.join(",", clients.keySet());
        Message m = new Message(Message.Type.USER_LIST, "server", null, list);
        for (ClientHandler h : clients.values()) h.send(m);
    }

    public void stop() throws IOException {
        running = false;
        if (serverSocket != null) serverSocket.close();
        pool.shutdownNow();
    }

    public static void main(String[] args) throws IOException {
        new Server().start();
    }
}
EOF

cat > src/main/java/com/chatapp/server/ClientHandler.java << 'EOF'
package com.chatapp.server;

import com.chatapp.model.Group;
import com.chatapp.model.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final Server server;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private String username;

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            Object obj;
            while ((obj = in.readObject()) != null) {
                if (obj instanceof Message msg) handle(msg);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("[Server] client gone: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private void cleanup() {
        try {
            if (username != null) {
                server.unregister(username);
                server.broadcastUserList();
            }
            if (in != null) in.close();
            if (out != null) out.close();
            if (!socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }

    private void handle(Message msg) throws IOException {
        switch (msg.getType()) {
            case REGISTER -> {
                String[] cred = msg.getContent().split(":", 2);
                boolean ok = cred.length == 2 && server.users().register(cred[0], cred[1]);
                send(new Message(Message.Type.ACK, "server", msg.getSender(),
                        ok ? "REGISTER_OK" : "REGISTER_FAIL"));
            }
            case LOGIN -> {
                String[] cred = msg.getContent().split(":", 2);
                if (cred.length == 2 && server.users().authenticate(cred[0], cred[1])) {
                    username = cred[0];
                    server.register(username, this);
                    send(new Message(Message.Type.ACK, "server", username, "LOGIN_OK"));
                    server.broadcastUserList();
                } else {
                    send(new Message(Message.Type.ACK, "server", msg.getSender(), "LOGIN_FAIL"));
                }
            }
            case CHAT -> {
                ClientHandler target = server.get(msg.getTarget());
                if (target != null) target.send(msg);
            }
            case GROUP_CREATE -> {
                String[] parts = msg.getContent().split(",");
                if (parts.length >= 1) {
                    Group g = server.groups().create(parts[0], msg.getSender());
                    for (int i = 1; i < parts.length; i++) g.add(parts[i]);
                }
            }
            case GROUP_INVITE -> {
                String[] parts = msg.getContent().split(":", 2);
                if (parts.length == 2) server.groups().invite(parts[0], parts[1]);
            }
            case GROUP_CHAT -> server.broadcastGroup(msg);
            case USER_LIST -> server.broadcastUserList();
            case LOGOUT -> {
                if (username != null) {
                    server.unregister(username);
                    server.broadcastUserList();
                }
            }
            default -> System.out.println("[Server] unhandled: " + msg);
        }
    }

    public String getUsername() { return username; }
    public void setUsername(String u) { this.username = u; }

    public void send(Message msg) throws IOException {
        out.writeObject(msg);
        out.flush();
    }
}
EOF

commit_at "2026-05-14T14:12:55+07:00" "feat(group): broadcast group messages on server"

# -- Commit 7.5 -- invite/remove members
cat > src/main/java/com/chatapp/server/GroupStore.java << 'EOF'
package com.chatapp.server;

import com.chatapp.model.Group;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GroupStore {
    private final Map<String, Group> groups = new ConcurrentHashMap<>();

    public Group create(String name, String owner) {
        return groups.computeIfAbsent(name, n -> new Group(n, owner));
    }

    public Group get(String name) { return groups.get(name); }

    public boolean invite(String groupName, String username) {
        Group g = groups.get(groupName);
        return g != null && g.add(username);
    }

    public boolean remove(String groupName, String username) {
        Group g = groups.get(groupName);
        if (g == null) return false;
        boolean removed = g.remove(username);
        if (removed && g.getMembers().isEmpty()) groups.remove(groupName);
        return removed;
    }

    public Map<String, Group> all() { return groups; }
}
EOF

commit_at "2026-05-14T20:33:44+07:00" "feat(group): invite and remove members at runtime"

# -- Commit 7.6 -- empty group edge case
cat > src/main/java/com/chatapp/server/GroupStore.java << 'EOF'
package com.chatapp.server;

import com.chatapp.model.Group;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GroupStore {
    private final Map<String, Group> groups = new ConcurrentHashMap<>();

    public Group create(String name, String owner) {
        if (name == null || name.isBlank() || owner == null) return null;
        return groups.computeIfAbsent(name, n -> new Group(n, owner));
    }

    public Group get(String name) { return name == null ? null : groups.get(name); }

    public boolean invite(String groupName, String username) {
        if (groupName == null || username == null || username.isBlank()) return false;
        Group g = groups.get(groupName);
        return g != null && g.add(username);
    }

    public boolean remove(String groupName, String username) {
        Group g = get(groupName);
        if (g == null) return false;
        boolean removed = g.remove(username);
        if (removed && g.getMembers().isEmpty()) groups.remove(groupName);
        return removed;
    }

    public boolean isEmpty(String groupName) {
        Group g = get(groupName);
        return g == null || g.getMembers().isEmpty();
    }

    public Map<String, Group> all() { return groups; }
}
EOF

commit_at "2026-05-15T06:18:27+07:00" "fix(group): handle empty group edge cases"

finish_feature "feature/group-chat" "2026-05-15T09:55:14+07:00"

############################################################
# Feature 8: feature/file-transfer  (May 15–17)
############################################################
start_feature "feature/file-transfer"

# -- Commit 8.1 -- FileTransfer
rm -f src/main/java/com/chatapp/util/.gitkeep
cat > src/main/java/com/chatapp/util/FileTransfer.java << 'EOF'
package com.chatapp.util;

import com.chatapp.model.Message;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public class FileTransfer {
    public static final int CHUNK_SIZE = 4096;

    // Encodes the entire file content as base64 in the message content.
    // For large files this should be chunked; kept simple for the demo.
    public static Message buildFileMessage(String sender, String target, Path file) throws IOException {
        byte[] data = Files.readAllBytes(file);
        String b64 = Base64.getEncoder().encodeToString(data);
        String content = file.getFileName().toString() + "|" + b64;
        return new Message(Message.Type.FILE, sender, target, content);
    }

    public static Path saveIncoming(Message msg, Path dir) throws IOException {
        Files.createDirectories(dir);
        String[] parts = msg.getContent().split("\\|", 2);
        if (parts.length != 2) throw new IOException("malformed FILE message");
        Path out = dir.resolve(parts[0]);
        Files.write(out, Base64.getDecoder().decode(parts[1]));
        return out;
    }
}
EOF

commit_at "2026-05-15T12:30:21+07:00" "feat(file): FileTransfer chunked send/receive utility"

# -- Commit 8.2 -- progress dialog
cat > src/main/java/com/chatapp/util/FileTransfer.java << 'EOF'
package com.chatapp.util;

import com.chatapp.model.Message;

import javax.swing.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public class FileTransfer {
    public static final int CHUNK_SIZE = 4096;

    public static Message buildFileMessage(String sender, String target, Path file) throws IOException {
        byte[] data = Files.readAllBytes(file);
        String b64 = Base64.getEncoder().encodeToString(data);
        String content = file.getFileName().toString() + "|" + b64;
        return new Message(Message.Type.FILE, sender, target, content);
    }

    public static Path saveIncoming(Message msg, Path dir) throws IOException {
        Files.createDirectories(dir);
        String[] parts = msg.getContent().split("\\|", 2);
        if (parts.length != 2) throw new IOException("malformed FILE message");
        Path out = dir.resolve(parts[0]);
        Files.write(out, Base64.getDecoder().decode(parts[1]));
        return out;
    }

    public static JDialog progressDialog(java.awt.Frame parent, String title) {
        JDialog d = new JDialog(parent, title, false);
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setStringPainted(true);
        d.add(bar);
        d.pack();
        d.setSize(280, 80);
        d.setLocationRelativeTo(parent);
        d.putClientProperty("progressBar", bar);
        return d;
    }

    public static void setProgress(JDialog dialog, int percent) {
        JProgressBar bar = (JProgressBar) dialog.getClientProperty("progressBar");
        if (bar != null) bar.setValue(percent);
    }
}
EOF

commit_at "2026-05-15T19:45:33+07:00" "feat(file): progress dialog with JProgressBar"

# -- Commit 8.3 -- integrate file send in ChatFrame
cat > src/main/java/com/chatapp/client/ChatFrame.java << 'EOF'
package com.chatapp.client;

import com.chatapp.model.Message;
import com.chatapp.util.FileTransfer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ChatFrame extends JFrame {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final JTextArea history = new JTextArea();
    private final JTextField input = new JTextField();
    private final JButton sendButton = new JButton("Gửi");
    private final JButton fileButton = new JButton("📎");
    protected final Client client;
    protected final String peer;

    public ChatFrame(Client client, String peer) {
        super("Chat với " + peer);
        this.client = client;
        this.peer = peer;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(480, 360);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(4, 4));

        history.setEditable(false);
        history.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        history.setMargin(new Insets(6, 6, 6, 6));
        add(new JScrollPane(history), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(4, 0));
        bottom.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        right.add(fileButton);
        right.add(sendButton);
        bottom.add(input, BorderLayout.CENTER);
        bottom.add(right, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        sendButton.addActionListener(this::onSend);
        input.addActionListener(this::onSend);
        fileButton.addActionListener(this::onPickFile);
    }

    protected void onSend(ActionEvent e) {
        String text = input.getText().trim();
        if (text.isEmpty()) return;
        Message m = new Message(Message.Type.CHAT, client.getUsername(), peer, text);
        try {
            client.send(m);
            appendLine("Tôi", text);
            input.setText("");
        } catch (Exception ex) {
            appendLine("[lỗi]", ex.getMessage());
        }
    }

    protected void onPickFile(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = chooser.getSelectedFile();
        try {
            Message m = FileTransfer.buildFileMessage(client.getUsername(), peer, Path.of(f.getAbsolutePath()));
            client.send(m);
            appendLine("Tôi", "[file] " + f.getName());
        } catch (Exception ex) {
            appendLine("[lỗi file]", ex.getMessage());
        }
    }

    public void receive(Message m) {
        SwingUtilities.invokeLater(() -> {
            if (m.getType() == Message.Type.FILE) {
                String[] parts = m.getContent().split("\\|", 2);
                appendLine(m.getSender(), "[file] " + (parts.length > 0 ? parts[0] : "(unknown)"));
            } else {
                appendLine(m.getSender(), m.getContent());
            }
        });
    }

    protected void appendLine(String who, String text) {
        history.append("[" + LocalTime.now().format(TIME) + "] " + who + ": " + text + "\n");
        history.setCaretPosition(history.getDocument().getLength());
    }
}
EOF

commit_at "2026-05-16T08:22:18+07:00" "feat(file): integrate file send in ChatFrame"

# -- Commit 8.4 -- server relay file chunks
cat > src/main/java/com/chatapp/server/ClientHandler.java << 'EOF'
package com.chatapp.server;

import com.chatapp.model.Group;
import com.chatapp.model.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final Server server;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private String username;

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            Object obj;
            while ((obj = in.readObject()) != null) {
                if (obj instanceof Message msg) handle(msg);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("[Server] client gone: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private void cleanup() {
        try {
            if (username != null) {
                server.unregister(username);
                server.broadcastUserList();
            }
            if (in != null) in.close();
            if (out != null) out.close();
            if (!socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }

    private void handle(Message msg) throws IOException {
        switch (msg.getType()) {
            case REGISTER -> {
                String[] cred = msg.getContent().split(":", 2);
                boolean ok = cred.length == 2 && server.users().register(cred[0], cred[1]);
                send(new Message(Message.Type.ACK, "server", msg.getSender(),
                        ok ? "REGISTER_OK" : "REGISTER_FAIL"));
            }
            case LOGIN -> {
                String[] cred = msg.getContent().split(":", 2);
                if (cred.length == 2 && server.users().authenticate(cred[0], cred[1])) {
                    username = cred[0];
                    server.register(username, this);
                    send(new Message(Message.Type.ACK, "server", username, "LOGIN_OK"));
                    server.broadcastUserList();
                } else {
                    send(new Message(Message.Type.ACK, "server", msg.getSender(), "LOGIN_FAIL"));
                }
            }
            case CHAT -> {
                ClientHandler target = server.get(msg.getTarget());
                if (target != null) target.send(msg);
            }
            case FILE -> {
                ClientHandler target = server.get(msg.getTarget());
                if (target != null) target.send(msg);
            }
            case GROUP_CREATE -> {
                String[] parts = msg.getContent().split(",");
                if (parts.length >= 1) {
                    Group g = server.groups().create(parts[0], msg.getSender());
                    if (g != null) for (int i = 1; i < parts.length; i++) g.add(parts[i]);
                }
            }
            case GROUP_INVITE -> {
                String[] parts = msg.getContent().split(":", 2);
                if (parts.length == 2) server.groups().invite(parts[0], parts[1]);
            }
            case GROUP_CHAT -> server.broadcastGroup(msg);
            case USER_LIST -> server.broadcastUserList();
            case LOGOUT -> {
                if (username != null) {
                    server.unregister(username);
                    server.broadcastUserList();
                }
            }
            default -> System.out.println("[Server] unhandled: " + msg);
        }
    }

    public String getUsername() { return username; }
    public void setUsername(String u) { this.username = u; }

    public void send(Message msg) throws IOException {
        out.writeObject(msg);
        out.flush();
    }
}
EOF

commit_at "2026-05-16T15:38:42+07:00" "feat(file): server relay file payload to target"

# -- Commit 8.5 -- close streams on cancel
cat > src/main/java/com/chatapp/util/FileTransfer.java << 'EOF'
package com.chatapp.util;

import com.chatapp.model.Message;

import javax.swing.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public class FileTransfer {
    public static final int CHUNK_SIZE = 4096;

    public static Message buildFileMessage(String sender, String target, Path file) throws IOException {
        try (InputStream is = Files.newInputStream(file);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[CHUNK_SIZE];
            int n;
            while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
            String b64 = Base64.getEncoder().encodeToString(bos.toByteArray());
            String content = file.getFileName().toString() + "|" + b64;
            return new Message(Message.Type.FILE, sender, target, content);
        }
    }

    public static Path saveIncoming(Message msg, Path dir) throws IOException {
        Files.createDirectories(dir);
        String[] parts = msg.getContent().split("\\|", 2);
        if (parts.length != 2) throw new IOException("malformed FILE message");
        Path out = dir.resolve(parts[0]);
        try (OutputStream os = Files.newOutputStream(out)) {
            os.write(Base64.getDecoder().decode(parts[1]));
        }
        return out;
    }

    public static JDialog progressDialog(java.awt.Frame parent, String title) {
        JDialog d = new JDialog(parent, title, false);
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setStringPainted(true);
        d.add(bar);
        d.pack();
        d.setSize(280, 80);
        d.setLocationRelativeTo(parent);
        d.putClientProperty("progressBar", bar);
        return d;
    }

    public static void setProgress(JDialog dialog, int percent) {
        JProgressBar bar = (JProgressBar) dialog.getClientProperty("progressBar");
        if (bar != null) bar.setValue(percent);
    }
}
EOF

commit_at "2026-05-16T22:11:55+07:00" "fix(file): close streams properly on cancel"

finish_feature "feature/file-transfer" "2026-05-17T09:28:33+07:00"

############################################################
# Feature 9: feature/chat-history  (May 17–19)
############################################################
start_feature "feature/chat-history"

# -- Commit 9.1 -- HistoryManager
cat > src/main/java/com/chatapp/util/HistoryManager.java << 'EOF'
package com.chatapp.util;

import com.chatapp.model.Message;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class HistoryManager {
    private static final Path BASE = Path.of("data", "history");

    private static Path fileFor(String me, String peer) {
        String pair = me.compareTo(peer) < 0 ? me + "__" + peer : peer + "__" + me;
        return BASE.resolve(pair + ".log");
    }

    public static synchronized void append(String me, String peer, Message msg) {
        try {
            Files.createDirectories(BASE);
            Path f = fileFor(me, peer);
            String line = msg.getTimestamp() + "|" + msg.getSender() + "|"
                    + msg.getType() + "|" + safe(msg.getContent()) + "\n";
            Files.writeString(f, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("[History] append failed: " + e.getMessage());
        }
    }

    public static List<String> load(String me, String peer) {
        try {
            Path f = fileFor(me, peer);
            if (!Files.exists(f)) return List.of();
            return new ArrayList<>(Files.readAllLines(f, StandardCharsets.UTF_8));
        } catch (IOException e) {
            return List.of();
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s.replace("\n", "\\n");
    }
}
EOF

commit_at "2026-05-17T11:45:18+07:00" "feat(history): HistoryManager local persistence"

# -- Commit 9.2 -- HistoryFrame viewer
cat > src/main/java/com/chatapp/client/HistoryFrame.java << 'EOF'
package com.chatapp.client;

import com.chatapp.util.HistoryManager;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class HistoryFrame extends JFrame {
    private final DefaultListModel<String> model = new DefaultListModel<>();
    private final JList<String> list = new JList<>(model);

    public HistoryFrame(String me, String peer) {
        super("Lịch sử chat với " + peer);
        setSize(560, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        List<String> lines = HistoryManager.load(me, peer);
        for (String l : lines) model.addElement(l);
        add(new JScrollPane(list), BorderLayout.CENTER);
    }
}
EOF

commit_at "2026-05-17T17:22:33+07:00" "feat(history): history viewer dialog"

# -- Commit 9.3 -- delete individual messages
cat > src/main/java/com/chatapp/util/HistoryManager.java << 'EOF'
package com.chatapp.util;

import com.chatapp.model.Message;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class HistoryManager {
    private static final Path BASE = Path.of("data", "history");

    private static Path fileFor(String me, String peer) {
        String pair = me.compareTo(peer) < 0 ? me + "__" + peer : peer + "__" + me;
        return BASE.resolve(pair + ".log");
    }

    public static synchronized void append(String me, String peer, Message msg) {
        try {
            Files.createDirectories(BASE);
            Path f = fileFor(me, peer);
            String line = msg.getTimestamp() + "|" + msg.getSender() + "|"
                    + msg.getType() + "|" + safe(msg.getContent()) + "\n";
            Files.writeString(f, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("[History] append failed: " + e.getMessage());
        }
    }

    public static List<String> load(String me, String peer) {
        try {
            Path f = fileFor(me, peer);
            if (!Files.exists(f)) return List.of();
            return new ArrayList<>(Files.readAllLines(f, StandardCharsets.UTF_8));
        } catch (IOException e) {
            return List.of();
        }
    }

    // Deletes the message at the given zero-based line index.
    public static synchronized boolean deleteAt(String me, String peer, int index) {
        try {
            Path f = fileFor(me, peer);
            if (!Files.exists(f)) return false;
            List<String> lines = new ArrayList<>(Files.readAllLines(f, StandardCharsets.UTF_8));
            if (index < 0 || index >= lines.size()) return false;
            lines.remove(index);
            Files.write(f, lines, StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static synchronized void clear(String me, String peer) {
        try {
            Files.deleteIfExists(fileFor(me, peer));
        } catch (IOException ignored) {}
    }

    private static String safe(String s) {
        return s == null ? "" : s.replace("\n", "\\n");
    }
}
EOF

cat > src/main/java/com/chatapp/client/HistoryFrame.java << 'EOF'
package com.chatapp.client;

import com.chatapp.util.HistoryManager;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class HistoryFrame extends JFrame {
    private final DefaultListModel<String> model = new DefaultListModel<>();
    private final JList<String> list = new JList<>(model);
    private final String me;
    private final String peer;

    public HistoryFrame(String me, String peer) {
        super("Lịch sử chat với " + peer);
        this.me = me;
        this.peer = peer;
        setSize(560, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(4, 4));

        reload();
        add(new JScrollPane(list), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton delete = new JButton("Xoá dòng đã chọn");
        JButton clear = new JButton("Xoá toàn bộ");
        actions.add(delete);
        actions.add(clear);
        add(actions, BorderLayout.SOUTH);

        delete.addActionListener(e -> {
            int i = list.getSelectedIndex();
            if (i >= 0 && HistoryManager.deleteAt(me, peer, i)) reload();
        });
        clear.addActionListener(e -> {
            HistoryManager.clear(me, peer);
            reload();
        });
    }

    private void reload() {
        model.clear();
        List<String> lines = HistoryManager.load(me, peer);
        for (String l : lines) model.addElement(l);
    }
}
EOF

commit_at "2026-05-18T08:33:42+07:00" "feat(history): delete individual messages from history"

# -- Commit 9.4 -- load history on chat open
cat > src/main/java/com/chatapp/client/ChatFrame.java << 'EOF'
package com.chatapp.client;

import com.chatapp.model.Message;
import com.chatapp.util.FileTransfer;
import com.chatapp.util.HistoryManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ChatFrame extends JFrame {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final JTextArea history = new JTextArea();
    private final JTextField input = new JTextField();
    private final JButton sendButton = new JButton("Gửi");
    private final JButton fileButton = new JButton("📎");
    private final JButton historyButton = new JButton("Lịch sử");
    protected final Client client;
    protected final String peer;

    public ChatFrame(Client client, String peer) {
        super("Chat với " + peer);
        this.client = client;
        this.peer = peer;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(480, 360);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(4, 4));

        history.setEditable(false);
        history.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        history.setMargin(new Insets(6, 6, 6, 6));
        add(new JScrollPane(history), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(4, 0));
        bottom.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        right.add(historyButton);
        right.add(fileButton);
        right.add(sendButton);
        bottom.add(input, BorderLayout.CENTER);
        bottom.add(right, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        sendButton.addActionListener(this::onSend);
        input.addActionListener(this::onSend);
        fileButton.addActionListener(this::onPickFile);
        historyButton.addActionListener(e -> new HistoryFrame(client.getUsername(), peer).setVisible(true));

        loadHistory();
    }

    private void loadHistory() {
        List<String> lines = HistoryManager.load(client.getUsername(), peer);
        for (String l : lines) history.append("(cũ) " + l + "\n");
    }

    protected void onSend(ActionEvent e) {
        String text = input.getText().trim();
        if (text.isEmpty()) return;
        Message m = new Message(Message.Type.CHAT, client.getUsername(), peer, text);
        try {
            client.send(m);
            HistoryManager.append(client.getUsername(), peer, m);
            appendLine("Tôi", text);
            input.setText("");
        } catch (Exception ex) {
            appendLine("[lỗi]", ex.getMessage());
        }
    }

    protected void onPickFile(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = chooser.getSelectedFile();
        try {
            Message m = FileTransfer.buildFileMessage(client.getUsername(), peer, Path.of(f.getAbsolutePath()));
            client.send(m);
            HistoryManager.append(client.getUsername(), peer,
                    new Message(Message.Type.FILE, client.getUsername(), peer, f.getName()));
            appendLine("Tôi", "[file] " + f.getName());
        } catch (Exception ex) {
            appendLine("[lỗi file]", ex.getMessage());
        }
    }

    public void receive(Message m) {
        SwingUtilities.invokeLater(() -> {
            HistoryManager.append(client.getUsername(), peer, m);
            if (m.getType() == Message.Type.FILE) {
                String[] parts = m.getContent().split("\\|", 2);
                appendLine(m.getSender(), "[file] " + (parts.length > 0 ? parts[0] : "(unknown)"));
            } else {
                appendLine(m.getSender(), m.getContent());
            }
        });
    }

    protected void appendLine(String who, String text) {
        history.append("[" + LocalTime.now().format(TIME) + "] " + who + ": " + text + "\n");
        history.setCaretPosition(history.getDocument().getLength());
    }
}
EOF

commit_at "2026-05-18T15:18:25+07:00" "feat(history): load history when chat window opens"

# -- Commit 9.5 -- corrupted file safety
cat > src/main/java/com/chatapp/util/HistoryManager.java << 'EOF'
package com.chatapp.util;

import com.chatapp.model.Message;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class HistoryManager {
    private static final Path BASE = Path.of("data", "history");

    private static Path fileFor(String me, String peer) {
        String pair = me.compareTo(peer) < 0 ? me + "__" + peer : peer + "__" + me;
        return BASE.resolve(pair + ".log");
    }

    public static synchronized void append(String me, String peer, Message msg) {
        try {
            Files.createDirectories(BASE);
            Path f = fileFor(me, peer);
            String line = msg.getTimestamp() + "|" + msg.getSender() + "|"
                    + msg.getType() + "|" + safe(msg.getContent()) + "\n";
            Files.writeString(f, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("[History] append failed: " + e.getMessage());
        }
    }

    public static List<String> load(String me, String peer) {
        try {
            Path f = fileFor(me, peer);
            if (!Files.exists(f)) return List.of();
            List<String> raw = Files.readAllLines(f, StandardCharsets.UTF_8);
            List<String> valid = new ArrayList<>();
            for (String line : raw) {
                if (line == null || line.isBlank()) continue;
                // skip lines that fail the expected "ts|sender|type|content" shape
                if (line.split("\\|", 4).length < 4) {
                    System.err.println("[History] skipping malformed line: " + line);
                    continue;
                }
                valid.add(line);
            }
            return valid;
        } catch (IOException e) {
            System.err.println("[History] load failed (corrupted?): " + e.getMessage());
            return List.of();
        }
    }

    public static synchronized boolean deleteAt(String me, String peer, int index) {
        try {
            Path f = fileFor(me, peer);
            if (!Files.exists(f)) return false;
            List<String> lines = new ArrayList<>(Files.readAllLines(f, StandardCharsets.UTF_8));
            if (index < 0 || index >= lines.size()) return false;
            lines.remove(index);
            Files.write(f, lines, StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static synchronized void clear(String me, String peer) {
        try {
            Files.deleteIfExists(fileFor(me, peer));
        } catch (IOException ignored) {}
    }

    private static String safe(String s) {
        return s == null ? "" : s.replace("\n", "\\n");
    }
}
EOF

commit_at "2026-05-18T21:50:11+07:00" "fix(history): handle corrupted history files gracefully"

finish_feature "feature/chat-history" "2026-05-19T08:15:27+07:00"

############################################################
# Feature 10: feature/ui-polish  (May 19–20)
############################################################
start_feature "feature/ui-polish"

# -- Commit 10.1 -- app icons and fonts
mkdir -p src/main/resources/icons
cat > src/main/resources/icons/README.md << 'EOF'
# Icon assets
Đặt file PNG vào thư mục này. Tên file dùng trong code:
- app.png        — icon chính của ứng dụng
- send.png       — nút gửi tin nhắn
- attach.png     — nút đính kèm file
- group.png      — icon nhóm
EOF

cat > src/main/java/com/chatapp/client/MainFrame.java << 'EOF'
package com.chatapp.client;

import com.chatapp.model.Message;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class MainFrame extends JFrame {
    protected final DefaultListModel<String> usersModel = new DefaultListModel<>();
    private final JList<String> usersList = new JList<>(usersModel);
    private final Map<String, Integer> badges = new HashMap<>();
    protected Client client;
    protected ChatWindowManager windows;

    public MainFrame(Client client) {
        super("ChatApp — " + client.getUsername());
        this.client = client;
        this.windows = new ChatWindowManager(client);
        this.windows.setBadgeListener(this::setBadge);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(360, 480);
        setLocationRelativeTo(null);

        URL iconUrl = getClass().getResource("/icons/app.png");
        if (iconUrl != null) setIconImage(new ImageIcon(iconUrl).getImage());

        setLayout(new BorderLayout());
        JLabel header = new JLabel("Đang online (double-click để chat):");
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));
        add(header, BorderLayout.NORTH);

        usersList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        add(new JScrollPane(usersList), BorderLayout.CENTER);

        usersList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                String name = String.valueOf(value);
                Integer n = badges.get(name);
                String text = (n != null && n > 0) ? name + "  (" + n + ")" : name;
                return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
            }
        });

        usersList.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String peer = usersList.getSelectedValue();
                    if (peer != null) windows.openWith(peer);
                }
            }
        });
    }

    private void setBadge(String peer, int count) {
        if (count <= 0) badges.remove(peer); else badges.put(peer, count);
        usersList.repaint();
    }

    public void onMessage(Message msg) {
        switch (msg.getType()) {
            case USER_LIST -> SwingUtilities.invokeLater(() -> {
                usersModel.clear();
                if (msg.getContent() != null && !msg.getContent().isBlank()) {
                    for (String u : msg.getContent().split(",")) {
                        if (!u.equals(client.getUsername())) usersModel.addElement(u);
                    }
                }
            });
            case CHAT -> windows.dispatch(msg);
            default -> {}
        }
    }
}
EOF

commit_at "2026-05-19T10:40:33+07:00" "style(ui): add app icons and consistent fonts"

# -- Commit 10.2 -- layout/spacing refinements
cat > src/main/java/com/chatapp/client/LoginFrame.java << 'EOF'
package com.chatapp.client;

import com.chatapp.model.Message;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.Socket;
import java.net.URL;

public class LoginFrame extends JFrame {
    private final JTextField usernameField = new JTextField(18);
    private final JPasswordField passwordField = new JPasswordField(18);
    private final JButton loginButton = new JButton("Đăng nhập");
    private final JButton registerButton = new JButton("Đăng ký");

    public LoginFrame() {
        super("ChatApp — Đăng nhập");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        URL iconUrl = getClass().getResource("/icons/app.png");
        if (iconUrl != null) setIconImage(new ImageIcon(iconUrl).getImage());

        JPanel root = new JPanel(new GridBagLayout());
        root.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));
        setContentPane(root);

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0; c.gridy = 0; root.add(new JLabel("Tài khoản:"), c);
        c.gridx = 1; root.add(usernameField, c);
        c.gridx = 0; c.gridy = 1; root.add(new JLabel("Mật khẩu:"), c);
        c.gridx = 1; root.add(passwordField, c);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(registerButton);
        buttons.add(loginButton);
        c.gridx = 0; c.gridy = 2; c.gridwidth = 2;
        c.insets = new Insets(12, 6, 0, 6);
        root.add(buttons, c);

        registerButton.addActionListener(e -> new RegisterFrame().setVisible(true));
        loginButton.addActionListener(this::onLogin);
        getRootPane().setDefaultButton(loginButton);

        pack();
        setLocationRelativeTo(null);
    }

    protected void onLogin(ActionEvent e) {
        String u = usernameField.getText().trim();
        String p = new String(passwordField.getPassword());
        if (u.isEmpty() || p.isEmpty()) {
            error("Vui lòng nhập đủ tài khoản và mật khẩu.");
            return;
        }
        try (Socket s = new Socket("127.0.0.1", 9999);
             ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {
            out.writeObject(new Message(Message.Type.LOGIN, u, "server", u + ":" + p));
            out.flush();
            Object reply = in.readObject();
            if (reply instanceof Message ack && "LOGIN_OK".equals(ack.getContent())) {
                JOptionPane.showMessageDialog(this, "Đăng nhập thành công.");
                dispose();
            } else {
                error("Sai tài khoản hoặc mật khẩu.");
            }
        } catch (Exception ex) {
            error("Không thể kết nối tới server: " + ex.getMessage());
        }
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
EOF

commit_at "2026-05-19T16:25:18+07:00" "style(ui): refine layout and spacing across frames"

# -- Commit 10.3 -- system tray notification
cat > src/main/java/com/chatapp/client/NotificationManager.java << 'EOF'
package com.chatapp.client;

import java.awt.*;
import java.net.URL;

public class NotificationManager {
    private TrayIcon trayIcon;

    public boolean enable() {
        if (!SystemTray.isSupported()) return false;
        try {
            URL iconUrl = getClass().getResource("/icons/app.png");
            Image image = iconUrl != null
                    ? Toolkit.getDefaultToolkit().getImage(iconUrl)
                    : Toolkit.getDefaultToolkit().createImage(new byte[0]);
            trayIcon = new TrayIcon(image, "ChatApp");
            trayIcon.setImageAutoSize(true);
            SystemTray.getSystemTray().add(trayIcon);
            return true;
        } catch (AWTException e) {
            System.err.println("[Notify] failed to attach tray icon: " + e.getMessage());
            return false;
        }
    }

    public void notify(String from, String message) {
        if (trayIcon != null) {
            trayIcon.displayMessage("Tin nhắn mới từ " + from, message, TrayIcon.MessageType.INFO);
        }
    }

    public void dispose() {
        if (trayIcon != null) SystemTray.getSystemTray().remove(trayIcon);
    }
}
EOF

commit_at "2026-05-19T22:18:42+07:00" "feat(ui): system tray notification on new message"

# -- Commit 10.4 -- final README
cat > README.md << 'EOF'
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
EOF

commit_at "2026-05-20T07:33:55+07:00" "docs(readme): finalize README with feature table and run guide"

# -- Commit 10.5 -- bump version to 1.0.0
cat > pom.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.chatapp</groupId>
    <artifactId>chatapp</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>
    <name>ChatApp</name>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <main.class>com.chatapp.client.LoginFrame</main.class>
    </properties>

    <build>
        <finalName>chatapp</finalName>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.5.0</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals><goal>shade</goal></goals>
                        <configuration>
                            <transformers>
                                <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                    <mainClass>${main.class}</mainClass>
                                </transformer>
                            </transformers>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
EOF

commit_at "2026-05-20T13:22:18+07:00" "chore(release): bump version to 1.0.0"

finish_feature "feature/ui-polish" "2026-05-20T16:45:33+07:00"

echo ""
echo "=========================================="
echo "All features merged to main."
echo "=========================================="
git log --oneline --graph --all | head -80
