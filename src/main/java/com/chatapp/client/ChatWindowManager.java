package com.chatapp.client;

import com.chatapp.model.Message;

import javax.swing.*;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

public class ChatWindowManager {
    private final Client client;
    private final Map<String, ChatFrame> windows = new ConcurrentHashMap<>();
    private final Map<String, VoiceCallFrame> voiceFrames = new ConcurrentHashMap<>();
    private final Map<String, VideoCallFrame> videoFrames = new ConcurrentHashMap<>();
    // Peers we just closed a call with — drop in-flight chunks instead of
    // auto-reopening the frame from the peer's still-streaming mic/cam.
    private final Set<String> recentlyClosedVoice = ConcurrentHashMap.newKeySet();
    private final Set<String> recentlyClosedVideo = ConcurrentHashMap.newKeySet();
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
        recentlyClosedVoice.remove(peer);
        return voiceFrames.computeIfAbsent(peer, p -> {
            VoiceCallFrame f = new VoiceCallFrame(client, p, callerSide, this::onVoiceClosed);
            f.setVisible(true);
            return f;
        });
    }

    public VideoCallFrame openVideoWith(String peer, boolean callerSide) {
        recentlyClosedVideo.remove(peer);
        return videoFrames.computeIfAbsent(peer, p -> {
            VideoCallFrame f = new VideoCallFrame(client, p, callerSide, this::onVideoClosed);
            f.setVisible(true);
            return f;
        });
    }

    private void onVoiceClosed(String peer) {
        voiceFrames.remove(peer);
        recentlyClosedVoice.add(peer);
    }

    private void onVideoClosed(String peer) {
        videoFrames.remove(peer);
        recentlyClosedVideo.add(peer);
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
            if (f != null) { f.receive(msg); return; }
            if (recentlyClosedVoice.contains(key)) return;
            openVoiceWith(key, false).receive(msg);
        });
    }

    public void dispatchVideo(Message msg) {
        String key = msg.getSender().equals(client.getUsername())
                ? msg.getTarget() : msg.getSender();
        SwingUtilities.invokeLater(() -> {
            VideoCallFrame f = videoFrames.get(key);
            if (f != null) { f.receive(msg); return; }
            if (recentlyClosedVideo.contains(key)) return;
            openVideoWith(key, false).receive(msg);
        });
    }

    public void handleVoiceEnd(Message msg) {
        String key = msg.getSender();
        SwingUtilities.invokeLater(() -> {
            recentlyClosedVoice.add(key);
            VoiceCallFrame f = voiceFrames.get(key);
            if (f != null) f.dispose();
        });
    }

    public void handleVideoEnd(Message msg) {
        String key = msg.getSender();
        SwingUtilities.invokeLater(() -> {
            recentlyClosedVideo.add(key);
            VideoCallFrame f = videoFrames.get(key);
            if (f != null) f.dispose();
        });
    }

    private void clearUnread(String peer) {
        unread.remove(peer);
        if (badgeListener != null) badgeListener.accept(peer, 0);
    }
}
