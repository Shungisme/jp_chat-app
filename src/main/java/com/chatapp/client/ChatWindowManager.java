package com.chatapp.client;

import com.chatapp.model.Message;
import com.chatapp.util.HistoryManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ChatWindowManager {
    private final Client client;
    private final Map<String, ChatPanel> panels = new ConcurrentHashMap<>();
    private final Map<String, VoiceCallFrame> voiceFrames = new ConcurrentHashMap<>();
    private final Map<String, VideoCallFrame> videoFrames = new ConcurrentHashMap<>();
    private final Map<String, JDialog> outgoingVoiceCalls = new ConcurrentHashMap<>();
    private final Map<String, JDialog> outgoingVideoCalls = new ConcurrentHashMap<>();
    private final Map<String, GroupPanel> groupPanels = new ConcurrentHashMap<>();
    private final Map<String, GroupVideoRoomFrame> groupVideoRooms = new ConcurrentHashMap<>();
    private final Map<String, GroupVoiceRoomFrame> groupVoiceRooms = new ConcurrentHashMap<>();
    private final Map<String, String> joinedGroups = new ConcurrentHashMap<>();
    private Consumer<Map<String, String>> groupsListener;
    private MainFrame mainFrame;
    // Peers we just closed a call with — drop in-flight chunks instead of
    // auto-reopening the frame from the peer's still-streaming mic/cam.
    private final Set<String> recentlyClosedVoice = ConcurrentHashMap.newKeySet();
    private final Set<String> recentlyClosedVideo = ConcurrentHashMap.newKeySet();
    private final Map<String, AtomicInteger> unread = new ConcurrentHashMap<>();
    private BiConsumer<String, Integer> badgeListener;
    private volatile List<String> onlineUsers = List.of();
    // Voice playback runs off the EDT because SourceDataLine.write() blocks
    // until the speaker buffer accepts the chunk — on the EDT that freezes the
    // UI for ~100 ms per chunk and makes the call feel broken.
    private final ExecutorService voicePool = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "voice-playback");
        t.setDaemon(true);
        return t;
    });

    public ChatWindowManager(Client client) {
        this.client = client;
    }

    public void setBadgeListener(BiConsumer<String, Integer> listener) {
        this.badgeListener = listener;
    }

    public void setMainFrame(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    public void setGroupsListener(Consumer<Map<String, String>> listener) {
        this.groupsListener = listener;
    }

    public void setOnlineUsers(List<String> users) {
        this.onlineUsers = users == null ? List.of() : List.copyOf(users);
    }

    public List<String> getOnlineUsers() {
        return onlineUsers;
    }

    public Map<String, String> joinedGroups() {
        return Map.copyOf(joinedGroups);
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

    // ---------------- voice / video call handshake ----------------

    public void initiateVoiceCall(String peer) {
        initiateCall(peer, Message.Type.VOICE_INVITE, Message.Type.VOICE_REJECT,
                "thoại", outgoingVoiceCalls);
    }

    public void initiateVideoCall(String peer) {
        initiateCall(peer, Message.Type.VIDEO_INVITE, Message.Type.VIDEO_REJECT,
                "video", outgoingVideoCalls);
    }

    private void initiateCall(String peer, Message.Type inviteType, Message.Type rejectType,
                              String label, Map<String, JDialog> bag) {
        if (bag.containsKey(peer) || voiceFrames.containsKey(peer) || videoFrames.containsKey(peer)) return;
        try {
            client.send(new Message(inviteType, client.getUsername(), peer, ""));
        } catch (Exception ex) {
            if (mainFrame != null) {
                JOptionPane.showMessageDialog(mainFrame,
                        "Không thể gửi lời mời: " + ex.getMessage(),
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
            return;
        }
        JDialog d = buildCallingDialog(peer, label, () -> {
            try { client.send(new Message(rejectType, client.getUsername(), peer, "")); }
            catch (Exception ignored) {}
            JDialog dd = bag.remove(peer);
            if (dd != null) dd.dispose();
        });
        bag.put(peer, d);
        d.setVisible(true);
    }

    private JDialog buildCallingDialog(String peer, String label, Runnable onCancel) {
        JDialog d = new JDialog(mainFrame, "Đang gọi " + label, false);
        d.setSize(320, 150);
        d.setLocationRelativeTo(mainFrame);
        d.setLayout(new BorderLayout(8, 8));
        JLabel msg = new JLabel("<html><div style='text-align:center;'>"
                + "Đang gọi <b>" + peer + "</b>...<br>Chờ phản hồi.</div></html>",
                SwingConstants.CENTER);
        msg.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        msg.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));
        d.add(msg, BorderLayout.CENTER);
        JButton cancel = new JButton("Huỷ");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttons.add(cancel);
        d.add(buttons, BorderLayout.SOUTH);
        cancel.addActionListener(e -> onCancel.run());
        d.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { onCancel.run(); }
        });
        return d;
    }

    public void handleVoiceInvite(Message msg) {
        String from = msg.getSender();
        SwingUtilities.invokeLater(() -> new IncomingCallDialog(mainFrame, from, "thoại",
                () -> {
                    try { client.send(new Message(Message.Type.VOICE_ACCEPT, client.getUsername(), from, "")); }
                    catch (Exception ignored) {}
                    recentlyClosedVoice.remove(from);
                    openVoiceWith(from, false);
                },
                () -> {
                    try { client.send(new Message(Message.Type.VOICE_REJECT, client.getUsername(), from, "")); }
                    catch (Exception ignored) {}
                }
        ).setVisible(true));
    }

    public void handleVideoInvite(Message msg) {
        String from = msg.getSender();
        SwingUtilities.invokeLater(() -> new IncomingCallDialog(mainFrame, from, "video",
                () -> {
                    try { client.send(new Message(Message.Type.VIDEO_ACCEPT, client.getUsername(), from, "")); }
                    catch (Exception ignored) {}
                    recentlyClosedVideo.remove(from);
                    openVideoWith(from, false);
                },
                () -> {
                    try { client.send(new Message(Message.Type.VIDEO_REJECT, client.getUsername(), from, "")); }
                    catch (Exception ignored) {}
                }
        ).setVisible(true));
    }

    public void handleVoiceAccept(Message msg) {
        String from = msg.getSender();
        SwingUtilities.invokeLater(() -> {
            JDialog d = outgoingVoiceCalls.remove(from);
            if (d != null) d.dispose();
            recentlyClosedVoice.remove(from);
            openVoiceWith(from, true);
        });
    }

    public void handleVideoAccept(Message msg) {
        String from = msg.getSender();
        SwingUtilities.invokeLater(() -> {
            JDialog d = outgoingVideoCalls.remove(from);
            if (d != null) d.dispose();
            recentlyClosedVideo.remove(from);
            openVideoWith(from, true);
        });
    }

    public void handleVoiceReject(Message msg) {
        String from = msg.getSender();
        SwingUtilities.invokeLater(() -> {
            JDialog d = outgoingVoiceCalls.remove(from);
            if (d != null) d.dispose();
            if (mainFrame != null) {
                JOptionPane.showMessageDialog(mainFrame,
                        from + " đã từ chối cuộc gọi.",
                        "Cuộc gọi bị từ chối", JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }

    public void handleVideoReject(Message msg) {
        String from = msg.getSender();
        SwingUtilities.invokeLater(() -> {
            JDialog d = outgoingVideoCalls.remove(from);
            if (d != null) d.dispose();
            if (mainFrame != null) {
                JOptionPane.showMessageDialog(mainFrame,
                        from + " đã từ chối cuộc gọi video.",
                        "Cuộc gọi bị từ chối", JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }

    // ---------------- call frame plumbing ----------------

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
                // Persist before badging — otherwise the user clicks the badge
                // to open the tab, ChatPanel.loadHistory() runs against a file
                // that never received this message, and the body is silently
                // lost. ChatPanel.receive() does the append when the panel is
                // already open; this branch mirrors that for the closed case.
                HistoryManager.append(client.getUsername(), key, msg);
                int n = unread.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet();
                if (badgeListener != null) badgeListener.accept(key, n);
            } else {
                p.receive(msg);
            }
        });
    }

    // Files always auto-open the chat tab — otherwise the file would be lost
    // if the receiver hadn't already opened a chat with the sender.
    public void dispatchFile(Message msg) {
        String key = msg.getSender().equals(client.getUsername())
                ? msg.getTarget() : msg.getSender();
        SwingUtilities.invokeLater(() -> openWith(key).receive(msg));
    }

    // No auto-open — chunks only flow once both sides have accepted.
    public void dispatchVoice(Message msg) {
        String key = msg.getSender().equals(client.getUsername())
                ? msg.getTarget() : msg.getSender();
        voicePool.execute(() -> {
            VoiceCallFrame f = voiceFrames.get(key);
            if (f != null) f.receive(msg);
        });
    }

    public void dispatchVideo(Message msg) {
        String key = msg.getSender().equals(client.getUsername())
                ? msg.getTarget() : msg.getSender();
        SwingUtilities.invokeLater(() -> {
            VideoCallFrame f = videoFrames.get(key);
            if (f != null) f.receive(msg);
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

    // ---------------- group chat ----------------

    public GroupPanel openGroup(String groupId, String groupName) {
        GroupPanel p = groupPanels.computeIfAbsent(groupId, id -> {
            GroupPanel np = new GroupPanel(client, id, groupName, this);
            if (mainFrame != null) mainFrame.openGroupTab(id, groupName, np);
            return np;
        });
        if (mainFrame != null) mainFrame.selectGroupTab(groupId);
        return p;
    }

    public void closeGroup(String groupId) {
        GroupPanel p = groupPanels.remove(groupId);
        if (p != null) {
            // Group voice now lives in GroupVoiceRoomFrame, not the panel —
            // closing the panel does not affect an open voice room.
            if (mainFrame != null) mainFrame.closeGroupTab(groupId);
        }
    }

    public void dispatchGroupVoice(Message msg) {
        String groupId = msg.getTarget();
        if (groupId == null || groupId.isBlank()) return;
        voicePool.execute(() -> {
            GroupVoiceRoomFrame f = groupVoiceRooms.get(groupId);
            if (f != null) f.play(msg);
        });
    }

    public GroupVoiceRoomFrame openGroupVoiceRoom(String groupId, String groupName) {
        GroupVoiceRoomFrame existing = groupVoiceRooms.get(groupId);
        if (existing != null) { existing.toFront(); return existing; }
        // Register the frame in the map BEFORE sending GROUP_VOICE_JOIN —
        // otherwise the server's GROUP_VOICE_ROOM reply can race the reader
        // thread and arrive while groupVoiceRooms.get(groupId) is still null,
        // dropping the snapshot and leaving this joiner without a roster.
        GroupVoiceRoomFrame f = new GroupVoiceRoomFrame(client, groupId, groupName,
                groupVoiceRooms::remove);
        GroupVoiceRoomFrame prev = groupVoiceRooms.putIfAbsent(groupId, f);
        if (prev != null) {
            f.dispose();
            prev.toFront();
            return prev;
        }
        f.setVisible(true);
        f.start();
        return f;
    }

    public void dispatchGroupVoiceJoin(Message msg) {
        String groupId = msg.getTarget();
        String who = msg.getSender();
        if (groupId == null || who == null) return;
        GroupVoiceRoomFrame f = groupVoiceRooms.get(groupId);
        if (f != null) f.participantJoined(who);
    }

    public void dispatchGroupVoiceLeave(Message msg) {
        String groupId = msg.getTarget();
        String who = msg.getSender();
        if (groupId == null || who == null) return;
        GroupVoiceRoomFrame f = groupVoiceRooms.get(groupId);
        if (f != null) f.participantLeft(who);
    }

    public void dispatchGroupVoiceStart(Message msg) {
        dispatchCallStart(msg, false);
    }

    public void dispatchGroupVoiceEnd(Message msg) {
        dispatchCallEnd(msg, false);
    }

    public void dispatchGroupVideoStart(Message msg) {
        dispatchCallStart(msg, true);
    }

    public void dispatchGroupVideoEnd(Message msg) {
        dispatchCallEnd(msg, true);
    }

    // Renders the centered "X đã bật voice/video" bubble in the group panel.
    // The bubble's join action opens the corresponding voice / video room
    // frame; clicking again while the user is already in is harmless because
    // the open methods are idempotent (computeIfAbsent).
    private void dispatchCallStart(Message msg, boolean video) {
        String groupId = msg.getTarget();
        String initiator = msg.getSender();
        long ts = msg.getTimestamp();
        if (groupId == null || initiator == null) return;
        SwingUtilities.invokeLater(() -> {
            String name = joinedGroups.getOrDefault(groupId, groupId);
            GroupPanel p = openGroup(groupId, name);
            Runnable join = video
                    ? () -> openGroupVideoRoom(groupId, name)
                    : () -> openGroupVoiceRoom(groupId, name);
            p.addCallStartNotice(initiator, ts, video, join);
        });
    }

    // Renders the "Voice/Video nhóm đã kết thúc" bubble and force-disposes any
    // frame still open for that group — a stale frame would otherwise stay up
    // with nobody to hear its audio.
    private void dispatchCallEnd(Message msg, boolean video) {
        String groupId = msg.getTarget();
        long ts = msg.getTimestamp();
        if (groupId == null) return;
        SwingUtilities.invokeLater(() -> {
            String name = joinedGroups.getOrDefault(groupId, groupId);
            GroupPanel p = openGroup(groupId, name);
            p.addCallEndNotice(ts, video);
            if (video) {
                GroupVideoRoomFrame vf = groupVideoRooms.remove(groupId);
                if (vf != null) vf.dispose();
            } else {
                GroupVoiceRoomFrame vf = groupVoiceRooms.remove(groupId);
                if (vf != null) vf.dispose();
            }
        });
    }

    // GROUP_VOICE_ROOM content: "groupId|user1,user2,..." (roster may be empty)
    public void dispatchGroupVoiceRoom(Message msg) {
        String content = msg.getContent();
        if (content == null) return;
        String[] parts = content.split("\\|", 2);
        if (parts.length < 1) return;
        String groupId = parts[0];
        java.util.Set<String> roster = new java.util.LinkedHashSet<>();
        if (parts.length == 2 && !parts[1].isBlank()) {
            for (String u : parts[1].split(",")) {
                String t = u.trim();
                if (!t.isEmpty()) roster.add(t);
            }
        }
        GroupVoiceRoomFrame f = groupVoiceRooms.get(groupId);
        if (f != null) f.applyRoster(roster);
    }

    public GroupVideoRoomFrame openGroupVideoRoom(String groupId, String groupName) {
        GroupVideoRoomFrame existing = groupVideoRooms.get(groupId);
        if (existing != null) { existing.toFront(); return existing; }
        GroupVideoRoomFrame f = new GroupVideoRoomFrame(client, groupId, groupName,
                groupVideoRooms::remove);
        GroupVideoRoomFrame prev = groupVideoRooms.putIfAbsent(groupId, f);
        if (prev != null) {
            f.dispose();
            prev.toFront();
            return prev;
        }
        f.setVisible(true);
        f.start();
        return f;
    }

    public void dispatchGroupVideo(Message msg) {
        String groupId = msg.getTarget();
        if (groupId == null || groupId.isBlank()) return;
        SwingUtilities.invokeLater(() -> {
            GroupVideoRoomFrame f = groupVideoRooms.get(groupId);
            if (f != null) f.receive(msg);
        });
    }

    // ---------------- members management ----------------

    private final Map<String, java.util.function.Consumer<GroupInfo>> pendingInfoRequests
            = new ConcurrentHashMap<>();

    public void queryGroupMembers(String groupId, java.util.function.Consumer<GroupInfo> onResult) {
        pendingInfoRequests.put(groupId, onResult);
        try {
            client.send(new Message(Message.Type.GROUP_QUERY,
                    client.getUsername(), groupId, ""));
        } catch (Exception ex) {
            pendingInfoRequests.remove(groupId);
            onResult.accept(null);
        }
    }

    public void handleGroupInfo(Message msg) {
        String content = msg.getContent();
        if (content == null || content.isBlank()) return;
        String[] parts = content.split("\\|", 3);
        if (parts.length < 2) return;
        String groupId = parts[0].trim();
        String owner = parts[1].trim();
        java.util.List<String> members = new java.util.ArrayList<>();
        if (parts.length == 3 && !parts[2].isBlank()) {
            for (String m : parts[2].split(",")) {
                String mt = m.trim();
                if (!mt.isEmpty()) members.add(mt);
            }
        }
        GroupInfo info = new GroupInfo(groupId, owner, members);
        java.util.function.Consumer<GroupInfo> cb = pendingInfoRequests.remove(groupId);
        if (cb != null) SwingUtilities.invokeLater(() -> cb.accept(info));
    }

    public void handleGroupRemoved(Message msg) {
        String groupId = msg.getContent();
        if (groupId == null || groupId.isBlank()) return;
        SwingUtilities.invokeLater(() -> {
            String name = joinedGroups.remove(groupId);
            if (groupsListener != null) groupsListener.accept(Map.copyOf(joinedGroups));
            closeGroup(groupId);
            if (mainFrame != null) {
                JOptionPane.showMessageDialog(mainFrame,
                        "Bạn đã bị xoá khỏi nhóm " + (name != null ? name : groupId) + ".",
                        "Đã rời nhóm", JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }

    // Parses payload "id|name" used by GROUP_INVITE.
    private static String[] splitGroupPayload(String payload) {
        if (payload == null) return null;
        String[] parts = payload.split("\\|", 2);
        if (parts.length != 2) return null;
        String id = parts[0].trim();
        String name = parts[1].trim();
        if (id.isEmpty() || name.isEmpty()) return null;
        return new String[] { id, name };
    }

    public void handleGroupInvite(Message msg) {
        String[] g = splitGroupPayload(msg.getContent());
        if (g == null) return;
        SwingUtilities.invokeLater(() -> {
            boolean fresh = joinedGroups.put(g[0], g[1]) == null;
            if (fresh && groupsListener != null) {
                groupsListener.accept(Map.copyOf(joinedGroups));
            }
            openGroup(g[0], g[1]);
        });
    }

    // GROUP_LIST is sent at login to restore membership. Populate the list
    // but do NOT auto-open tabs — the user picks which groups to re-open.
    // Content format: "id1|name1;id2|name2;..."
    public void handleGroupList(Message msg) {
        String content = msg.getContent();
        if (content == null || content.isBlank()) return;
        SwingUtilities.invokeLater(() -> {
            boolean changed = false;
            for (String entry : content.split(";")) {
                String[] g = splitGroupPayload(entry);
                if (g != null && joinedGroups.put(g[0], g[1]) == null) changed = true;
            }
            if (changed && groupsListener != null) {
                groupsListener.accept(Map.copyOf(joinedGroups));
            }
        });
    }

    public void dispatchGroupChat(Message msg) {
        String groupId = msg.getTarget();
        if (groupId == null || groupId.isBlank()) return;
        SwingUtilities.invokeLater(() -> {
            String name = joinedGroups.get(groupId);
            if (name == null) return;       // unknown group — drop
            openGroup(groupId, name).receive(msg);
        });
    }

    private void clearUnread(String peer) {
        unread.remove(peer);
        if (badgeListener != null) badgeListener.accept(peer, 0);
    }

    // Disposes any standalone windows the manager owns. ChatPanels and
    // GroupPanels live inside the MainFrame's tab pane, so they go away
    // when MainFrame.dispose() runs.
    public void closeAll() {
        SwingUtilities.invokeLater(() -> {
            for (VoiceCallFrame f : voiceFrames.values()) f.dispose();
            for (VideoCallFrame f : videoFrames.values()) f.dispose();
            for (GroupVideoRoomFrame f : groupVideoRooms.values()) f.dispose();
            for (GroupVoiceRoomFrame f : groupVoiceRooms.values()) f.dispose();
            for (JDialog d : outgoingVoiceCalls.values()) d.dispose();
            for (JDialog d : outgoingVideoCalls.values()) d.dispose();
            voiceFrames.clear();
            videoFrames.clear();
            groupVideoRooms.clear();
            groupVoiceRooms.clear();
            outgoingVoiceCalls.clear();
            outgoingVideoCalls.clear();
        });
    }
}
