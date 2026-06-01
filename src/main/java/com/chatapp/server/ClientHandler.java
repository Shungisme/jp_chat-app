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
                // Announce leaves for every voice / video room this client was
                // in. If a room becomes empty as a result, also broadcast END
                // to the whole group so the call-notice bubble updates.
                dropAndAnnounce(server.dropFromAllVoiceRooms(username),
                        Message.Type.GROUP_VOICE_LEAVE, Message.Type.GROUP_VOICE_END);
                dropAndAnnounce(server.dropFromAllVideoRooms(username),
                        Message.Type.GROUP_VIDEO_LEAVE, Message.Type.GROUP_VIDEO_END);
                server.unregister(username, this);
                server.broadcastUserList();
            }
            if (in != null) in.close();
            if (out != null) out.close();
            if (!socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }

    private void dropAndAnnounce(java.util.Map<String, java.util.Set<String>> affected,
                                 Message.Type leaveType, Message.Type endType) {
        for (java.util.Map.Entry<String, java.util.Set<String>> e : affected.entrySet()) {
            String groupId = e.getKey();
            java.util.Set<String> remaining = e.getValue();
            // Tell anyone still in the room that we're gone.
            Message leave = new Message(leaveType, username, groupId, "");
            for (String m : remaining) {
                ClientHandler h = server.get(m);
                if (h != null) {
                    try { h.send(leave); } catch (IOException ignored) {}
                }
            }
            // Room emptied because of this disconnect — END the whole call.
            if (remaining.isEmpty()) {
                try {
                    server.broadcastToGroup(groupId,
                            new Message(endType, "server", groupId, ""));
                } catch (IOException ignored) {}
            }
        }
    }

    // Called by the server when this session is being kicked. Closing the
    // socket makes the read loop exit; cleanup() runs from the finally block.
    public void disconnect() {
        try {
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
                    // Kick any existing session for this account.
                    ClientHandler existing = server.get(cred[0]);
                    if (existing != null && existing != this) {
                        try {
                            existing.send(new Message(Message.Type.KICKED, "server",
                                    cred[0], "Tài khoản vừa đăng nhập tại nơi khác."));
                        } catch (IOException ignored) {}
                        existing.disconnect();
                    }
                    username = cred[0];
                    server.register(username, this);
                    send(new Message(Message.Type.ACK, "server", username, "LOGIN_OK"));
                    server.broadcastUserList();
                    StringBuilder myGroups = new StringBuilder();
                    for (Group g : server.groups().all().values()) {
                        if (g.contains(username)) {
                            if (myGroups.length() > 0) myGroups.append(";");
                            myGroups.append(g.getId()).append("|").append(g.getName());
                        }
                    }
                    if (myGroups.length() > 0) {
                        send(new Message(Message.Type.GROUP_LIST,
                                "server", username, myGroups.toString()));
                    }
                    // Retroactive START notices: every group call that's
                    // already in progress (room non-empty) appears as a
                    // clickable bubble for the freshly-logged-in user.
                    for (Group g : server.groups().all().values()) {
                        if (!g.contains(username)) continue;
                        java.util.Set<String> voiceRoom = server.voiceRoomMembers(g.getId());
                        if (!voiceRoom.isEmpty()) {
                            send(new Message(Message.Type.GROUP_VOICE_START,
                                    voiceRoom.iterator().next(), g.getId(), ""));
                        }
                        java.util.Set<String> videoRoom = server.videoRoomMembers(g.getId());
                        if (!videoRoom.isEmpty()) {
                            send(new Message(Message.Type.GROUP_VIDEO_START,
                                    videoRoom.iterator().next(), g.getId(), ""));
                        }
                    }
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
                    java.util.List<String> extras = new java.util.ArrayList<>();
                    for (int i = 1; i < parts.length; i++) extras.add(parts[i]);
                    Group g = server.groups().create(parts[0], msg.getSender(), extras);
                    if (g != null) {
                        String payload = g.getId() + "|" + g.getName();
                        for (String member : g.getMembers()) {
                            ClientHandler h = server.get(member);
                            if (h != null) {
                                h.send(new Message(Message.Type.GROUP_INVITE,
                                        "server", member, payload));
                            }
                        }
                    }
                }
            }
            case GROUP_INVITE -> {
                String[] parts = msg.getContent().split(":", 2);
                if (parts.length == 2 && server.groups().invite(parts[0], parts[1])) {
                    Group g = server.groups().get(parts[0]);
                    ClientHandler h = server.get(parts[1]);
                    if (g != null && h != null) {
                        h.send(new Message(Message.Type.GROUP_INVITE,
                                "server", parts[1], g.getId() + "|" + g.getName()));
                    }
                }
            }
            case GROUP_CHAT, GROUP_FILE -> server.broadcastGroup(msg);
            case GROUP_VOICE -> server.broadcastToVoiceRoom(msg);
            case GROUP_VIDEO -> server.broadcastToVideoRoom(msg);
            case GROUP_VOICE_JOIN -> {
                String groupId = msg.getTarget();
                Group g = server.groups().get(groupId);
                if (g == null || username == null || !g.contains(username)) break;
                boolean firstInRoom = server.joinVoiceRoom(groupId, username);
                if (firstInRoom) {
                    server.broadcastToGroup(groupId, new Message(
                            Message.Type.GROUP_VOICE_START, username, groupId, ""));
                }
                String roster = String.join(",", server.voiceRoomMembers(groupId));
                send(new Message(Message.Type.GROUP_VOICE_ROOM,
                        "server", username, groupId + "|" + roster));
                server.broadcastToVoiceRoom(msg);
            }
            case GROUP_VOICE_LEAVE -> {
                String groupId = msg.getTarget();
                if (username == null) break;
                // Broadcast LEAVE first, while membership still includes
                // others, then drop the leaver; if that empties the room,
                // tell the whole group voice ended.
                server.broadcastToVoiceRoom(msg);
                boolean emptied = server.leaveVoiceRoom(groupId, username);
                if (emptied) {
                    server.broadcastToGroup(groupId, new Message(
                            Message.Type.GROUP_VOICE_END, "server", groupId, ""));
                }
            }
            case GROUP_VIDEO_JOIN -> {
                String groupId = msg.getTarget();
                Group g = server.groups().get(groupId);
                if (g == null || username == null || !g.contains(username)) break;
                boolean firstInRoom = server.joinVideoRoom(groupId, username);
                if (firstInRoom) {
                    server.broadcastToGroup(groupId, new Message(
                            Message.Type.GROUP_VIDEO_START, username, groupId, ""));
                }
            }
            case GROUP_VIDEO_LEAVE -> {
                String groupId = msg.getTarget();
                if (username == null) break;
                boolean emptied = server.leaveVideoRoom(groupId, username);
                if (emptied) {
                    server.broadcastToGroup(groupId, new Message(
                            Message.Type.GROUP_VIDEO_END, "server", groupId, ""));
                }
            }
            case GROUP_QUERY -> {
                Group g = server.groups().get(msg.getTarget());
                if (g != null) {
                    String payload = g.getId() + "|" + g.getOwner() + "|"
                            + String.join(",", g.getMembers());
                    send(new Message(Message.Type.GROUP_INFO, "server",
                            msg.getSender(), payload));
                }
            }
            case GROUP_REMOVE -> {
                String groupId = msg.getTarget();
                String kickedUser = msg.getContent();
                Group g = server.groups().get(groupId);
                if (g != null && g.getOwner().equals(msg.getSender())
                        && !kickedUser.equals(g.getOwner())
                        && server.groups().remove(groupId, kickedUser)) {
                    ClientHandler kicked = server.get(kickedUser);
                    if (kicked != null) {
                        kicked.send(new Message(Message.Type.GROUP_REMOVED,
                                "server", kickedUser, groupId));
                    }
                }
            }
            case VOICE_INVITE, VOICE_ACCEPT, VOICE_REJECT, VOICE, VOICE_END,
                 VIDEO_INVITE, VIDEO_ACCEPT, VIDEO_REJECT, VIDEO, VIDEO_END -> {
                ClientHandler target = server.get(msg.getTarget());
                if (target != null) target.send(msg);
            }
            case USER_LIST -> server.broadcastUserList();
            case LOGOUT -> {
                if (username != null) {
                    server.unregister(username, this);
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
