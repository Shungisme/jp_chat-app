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
    private final Map<String, ChatPanel> panels = new ConcurrentHashMap<>();
    private final Map<String, VoiceCallFrame> voiceFrames = new ConcurrentHashMap<>();
    private final Map<String, VideoCallFrame> videoFrames = new ConcurrentHashMap<>();
    private MainFrame mainFrame;
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

    public void setMainFrame(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    public ChatPanel openWith(String peer) {
        ChatPanel p = panels.computeIfAbsent(peer, pe -> {
            ChatPanel panel = new ChatPanel(client, pe, this);
            if (mainFrame != null) mainFrame.openChatTab(pe, panel);
            return panel;
        });
        if (mainFrame != null) mainFrame.selectChatTab(peer);
        clearUnread(peer);
        return p;
    }

    public void close(String peer) {
        ChatPanel p = panels.remove(peer);
        if (p != null && mainFrame != null) mainFrame.closeChatTab(peer);
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
            ChatPanel p = panels.get(key);
            if (p == null) {
                int n = unread.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet();
                if (badgeListener != null) badgeListener.accept(key, n);
            } else {
                p.receive(msg);
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
