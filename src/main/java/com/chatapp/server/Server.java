package com.chatapp.server;

import com.chatapp.model.Group;
import com.chatapp.model.Message;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class Server {
    public static final int DEFAULT_PORT = 9999;

    private int port = DEFAULT_PORT;
    private ServerSocket serverSocket;
    private volatile boolean running;
    private Thread acceptThread;
    private ExecutorService pool;
    private final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private final UserStore users = new UserStore();
    private final GroupStore groups = new GroupStore();
    // groupId -> usernames currently in the voice / video room. Maintained
    // server-side so a joining user gets a roster snapshot, presence broadcasts
    // only go to members already in the room, disconnect cleanup announces
    // leaves, and the server knows when a room becomes empty (→ END notice).
    private final Map<String, Set<String>> voiceRooms = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> videoRooms = new ConcurrentHashMap<>();
    private Consumer<String> logListener;
    private Consumer<Set<String>> clientsListener;

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public boolean isRunning() { return running; }

    public void setLogListener(Consumer<String> listener) { this.logListener = listener; }
    public void setClientsListener(Consumer<Set<String>> listener) { this.clientsListener = listener; }

    void log(String message) {
        System.out.println(message);
        if (logListener != null) logListener.accept(message);
    }

    void notifyClientsChanged() {
        if (clientsListener != null) clientsListener.accept(Set.copyOf(clients.keySet()));
    }

    // Non-blocking: spins up an accept thread and returns immediately.
    public synchronized void start() throws IOException {
        if (running) return;
        serverSocket = new ServerSocket(port);
        pool = Executors.newCachedThreadPool();
        running = true;
        log("[Server] listening on port " + port);
        acceptThread = new Thread(this::acceptLoop, "server-accept");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket client = serverSocket.accept();
                pool.submit(new ClientHandler(client, this));
            } catch (IOException e) {
                if (running) log("[Server] accept error: " + e.getMessage());
            }
        }
    }

    public synchronized void stop() {
        if (!running) return;
        running = false;
        try {
            for (ClientHandler h : clients.values()) {
                try { h.send(new Message(Message.Type.ERROR, "server", h.getUsername(), "SERVER_STOP")); }
                catch (IOException ignored) {}
            }
            clients.clear();
            if (serverSocket != null) serverSocket.close();
            if (pool != null) pool.shutdownNow();
        } catch (IOException e) {
            log("[Server] stop error: " + e.getMessage());
        }
        notifyClientsChanged();
        log("[Server] stopped");
    }

    public UserStore users() { return users; }
    public GroupStore groups() { return groups; }

    public void register(String username, ClientHandler handler) {
        clients.put(username, handler);
        log("[Server] " + username + " connected (" + clients.size() + " online)");
        notifyClientsChanged();
    }

    public void unregister(String username, ClientHandler handler) {
        if (clients.remove(username, handler)) {
            log("[Server] " + username + " disconnected (" + clients.size() + " online)");
            notifyClientsChanged();
        }
    }

    public ClientHandler get(String username) {
        return clients.get(username);
    }

    public Set<String> connectedUsernames() {
        return Set.copyOf(clients.keySet());
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

    // Returns true if the room was empty before this join — caller uses that
    // signal to broadcast a START notice to the whole group exactly once.
    public boolean joinVoiceRoom(String groupId, String username) {
        return joinRoom(voiceRooms, groupId, username);
    }

    public boolean joinVideoRoom(String groupId, String username) {
        return joinRoom(videoRooms, groupId, username);
    }

    // Returns true if the room became empty as a result of this leave.
    public boolean leaveVoiceRoom(String groupId, String username) {
        return leaveRoom(voiceRooms, groupId, username);
    }

    public boolean leaveVideoRoom(String groupId, String username) {
        return leaveRoom(videoRooms, groupId, username);
    }

    public Set<String> voiceRoomMembers(String groupId) {
        return roomMembers(voiceRooms, groupId);
    }

    public Set<String> videoRoomMembers(String groupId) {
        return roomMembers(videoRooms, groupId);
    }

    public Map<String, Set<String>> dropFromAllVoiceRooms(String username) {
        return dropFromAllRooms(voiceRooms, username);
    }

    public Map<String, Set<String>> dropFromAllVideoRooms(String username) {
        return dropFromAllRooms(videoRooms, username);
    }

    // Voice / video chunks only flow to members currently in the room — non-
    // joiners wouldn't play them and it would just waste bandwidth.
    public void broadcastToVoiceRoom(Message msg) throws IOException {
        broadcastToRoom(voiceRooms, msg);
    }

    public void broadcastToVideoRoom(Message msg) throws IOException {
        broadcastToRoom(videoRooms, msg);
    }

    public void broadcastToGroup(String groupId, Message msg) throws IOException {
        Group g = groups.get(groupId);
        if (g == null) return;
        for (String member : g.getMembers()) {
            ClientHandler h = clients.get(member);
            if (h != null) h.send(msg);
        }
    }

    private static boolean joinRoom(Map<String, Set<String>> rooms, String groupId, String username) {
        Set<String> room = rooms.computeIfAbsent(groupId, k -> ConcurrentHashMap.newKeySet());
        boolean wasEmpty = room.isEmpty();
        room.add(username);
        return wasEmpty;
    }

    private static boolean leaveRoom(Map<String, Set<String>> rooms, String groupId, String username) {
        Set<String> room = rooms.get(groupId);
        if (room == null) return false;
        room.remove(username);
        if (room.isEmpty()) {
            rooms.remove(groupId);
            return true;
        }
        return false;
    }

    private static Set<String> roomMembers(Map<String, Set<String>> rooms, String groupId) {
        Set<String> room = rooms.get(groupId);
        return room == null ? Set.of() : Set.copyOf(room);
    }

    // Returns {groupId -> remaining participants} for rooms the user was in.
    // Each entry that emptied is also removed; the caller uses an emptiness
    // check to know whether to emit an END notice for that group.
    private static Map<String, Set<String>> dropFromAllRooms(
            Map<String, Set<String>> rooms, String username) {
        Map<String, Set<String>> affected = new HashMap<>();
        for (Map.Entry<String, Set<String>> e : rooms.entrySet()) {
            if (e.getValue().remove(username)) {
                affected.put(e.getKey(), Set.copyOf(e.getValue()));
                if (e.getValue().isEmpty()) rooms.remove(e.getKey());
            }
        }
        return affected;
    }

    private void broadcastToRoom(Map<String, Set<String>> rooms, Message msg) throws IOException {
        Set<String> room = rooms.get(msg.getTarget());
        if (room == null) return;
        for (String member : room) {
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

    public static void main(String[] args) {
        // Console mode: keep the old behavior when invoked headless.
        Server s = new Server();
        try {
            s.start();
            Thread.currentThread().join();
        } catch (Exception e) {
            System.err.println("[Server] " + e.getMessage());
        }
    }
}
