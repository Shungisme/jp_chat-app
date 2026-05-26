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
    private final Map<String, VoiceCallFrame> voiceFrames = new ConcurrentHashMap<>();
    private final Map<String, VideoCallFrame> videoFrames = new ConcurrentHashMap<>();
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
            ChatFrame nf = new ChatFrame(client, p, this);
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

    public VoiceCallFrame openVoiceWith(String peer, boolean callerSide) {
        return voiceFrames.computeIfAbsent(peer, p -> {
            VoiceCallFrame f = new VoiceCallFrame(client, p, callerSide, voiceFrames::remove);
            f.setVisible(true);
            return f;
        });
    }

    public VideoCallFrame openVideoWith(String peer, boolean callerSide) {
        return videoFrames.computeIfAbsent(peer, p -> {
            VideoCallFrame f = new VideoCallFrame(client, p, callerSide, videoFrames::remove);
            f.setVisible(true);
            return f;
        });
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

    public void dispatchVoice(Message msg) {
        String key = msg.getSender().equals(client.getUsername())
                ? msg.getTarget() : msg.getSender();
        SwingUtilities.invokeLater(() -> {
            VoiceCallFrame f = voiceFrames.get(key);
            if (f == null) f = openVoiceWith(key, false);
            f.receive(msg);
        });
    }

    public void dispatchVideo(Message msg) {
        String key = msg.getSender().equals(client.getUsername())
                ? msg.getTarget() : msg.getSender();
        SwingUtilities.invokeLater(() -> {
            VideoCallFrame f = videoFrames.get(key);
            if (f == null) f = openVideoWith(key, false);
            f.receive(msg);
        });
    }

    private void clearUnread(String peer) {
        unread.remove(peer);
        if (badgeListener != null) badgeListener.accept(peer, 0);
    }
}
