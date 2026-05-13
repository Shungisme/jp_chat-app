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
