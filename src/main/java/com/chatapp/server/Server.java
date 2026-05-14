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
