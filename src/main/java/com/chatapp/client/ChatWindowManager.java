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
