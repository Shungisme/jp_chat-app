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
                    if (g != null) {
                        for (int i = 1; i < parts.length; i++) g.add(parts[i]);
                        for (String member : g.getMembers()) {
                            ClientHandler h = server.get(member);
                            if (h != null) {
                                h.send(new Message(Message.Type.GROUP_INVITE,
                                        "server", member, g.getName()));
                            }
                        }
                    }
                }
            }
            case GROUP_INVITE -> {
                String[] parts = msg.getContent().split(":", 2);
                if (parts.length == 2 && server.groups().invite(parts[0], parts[1])) {
                    ClientHandler h = server.get(parts[1]);
                    if (h != null) {
                        h.send(new Message(Message.Type.GROUP_INVITE,
                                "server", parts[1], parts[0]));
                    }
                }
            }
            case GROUP_CHAT -> server.broadcastGroup(msg);
            case VOICE_INVITE, VOICE_ACCEPT, VOICE_REJECT, VOICE, VOICE_END,
                 VIDEO_INVITE, VIDEO_ACCEPT, VIDEO_REJECT, VIDEO, VIDEO_END -> {
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
