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
