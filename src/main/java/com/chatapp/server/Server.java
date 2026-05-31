package com.chatapp.server;

import com.chatapp.model.Group;
import com.chatapp.model.Message;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
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
