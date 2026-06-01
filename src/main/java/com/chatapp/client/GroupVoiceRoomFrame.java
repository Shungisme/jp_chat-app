package com.chatapp.client;

import com.chatapp.client.ui.Avatar;
import com.chatapp.client.ui.Theme;
import com.chatapp.client.ui.UiKit;
import com.chatapp.model.Message;
import com.chatapp.util.VoiceChat;

import javax.sound.sampled.LineUnavailableException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Group voice room popup. Mirrors the 1:1 {@link VoiceCallFrame} but with a
 * live participant roster fed by GROUP_VOICE_JOIN / LEAVE / ROOM presence
 * messages. The frame owns the {@link VoiceChat} instance and the join/leave
 * protocol — closing the window sends GROUP_VOICE_LEAVE.
 */
public class GroupVoiceRoomFrame extends JFrame {
    private final Client client;
    private final String groupId;
    private final String groupName;
    private final Consumer<String> closeCallback;
    private final VoiceChat voice = new VoiceChat();
    private final JPanel participantsPanel = new JPanel();
    private final JLabel statusLabel = new JLabel();
    private final JButton micButton = new JButton("Bật micro");
    private final JButton leaveButton = new JButton("Rời phòng");
    private final Set<String> participants = new LinkedHashSet<>();
    private boolean micOn = false;

    public GroupVoiceRoomFrame(Client client, String groupId, String groupName,
                               Consumer<String> closeCallback) {
        super("Voice nhóm — " + groupName);
        this.client = client;
        this.groupId = groupId;
        this.groupName = groupName;
        this.closeCallback = closeCallback;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(420, 500);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Theme.bgApp());
        setLayout(new BorderLayout());

        add(buildHeader(), BorderLayout.NORTH);

        participantsPanel.setOpaque(false);
        participantsPanel.setLayout(new BoxLayout(participantsPanel, BoxLayout.Y_AXIS));
        participantsPanel.setBorder(UiKit.pad(Theme.SP_2, Theme.SP_4, Theme.SP_2, Theme.SP_4));
        JScrollPane scroll = new JScrollPane(participantsPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Theme.bgApp());
        add(scroll, BorderLayout.CENTER);

        add(buildFooter(), BorderLayout.SOUTH);

        micButton.addActionListener(e -> toggleMic());
        leaveButton.addActionListener(e -> dispose());
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) { onClose(); }
        });
    }

    /**
     * Sends GROUP_VOICE_JOIN and seeds the local participant list. Must be
     * called <em>after</em> the frame is registered in
     * {@link ChatWindowManager}'s map so the server's GROUP_VOICE_ROOM reply
     * can find this frame — otherwise the roster snapshot is dropped and the
     * joiner only ever sees themselves.
     */
    public void start() {
        try {
            voice.startPlayback();
            client.send(new Message(Message.Type.GROUP_VOICE_JOIN,
                    client.getUsername(), groupId, ""));
            participants.add(client.getUsername());
            rebuildList();
            updateStatus();
        } catch (LineUnavailableException ex) {
            JOptionPane.showMessageDialog(this, "Không mở được loa: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            SwingUtilities.invokeLater(this::dispose);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Không gửi được lời tham gia voice: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            SwingUtilities.invokeLater(this::dispose);
        }
    }

    public String getGroupId() { return groupId; }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout(Theme.SP_3, 0));
        header.setBackground(Theme.bgApp());
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.border()),
                UiKit.pad(Theme.SP_3, Theme.SP_4, Theme.SP_3, Theme.SP_4)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SP_2, 0));
        left.setOpaque(false);
        left.add(Avatar.component(groupName, 40, false));
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel name = new JLabel("Voice · " + groupName);
        name.setFont(Theme.name());
        name.setForeground(Theme.textPrimary());
        statusLabel.setFont(Theme.timestamp());
        statusLabel.setForeground(Theme.textSecondary());
        text.add(name);
        text.add(statusLabel);
        left.add(text);
        header.add(left, BorderLayout.WEST);
        return header;
    }

    private JComponent buildFooter() {
        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER, Theme.SP_2, 0));
        south.setOpaque(false);
        south.setBorder(UiKit.pad(Theme.SP_2, Theme.SP_4, Theme.SP_3, Theme.SP_4));
        micButton.putClientProperty("JButton.buttonType", "default");
        leaveButton.putClientProperty("JButton.buttonType", "borderless");
        leaveButton.setForeground(Theme.danger());
        south.add(micButton);
        south.add(leaveButton);
        return south;
    }

    private void toggleMic() {
        if (micOn) {
            voice.stopCapture();
            micButton.setText("Bật micro");
            micOn = false;
        } else {
            try {
                voice.startCapture(this::sendChunk);
                micButton.setText("Tắt micro");
                micOn = true;
            } catch (LineUnavailableException ex) {
                JOptionPane.showMessageDialog(this, "Không mở được micro: " + ex.getMessage(),
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void sendChunk(byte[] chunk) {
        try {
            String b64 = Base64.getEncoder().encodeToString(chunk);
            client.send(new Message(Message.Type.GROUP_VOICE,
                    client.getUsername(), groupId, b64));
        } catch (Exception ex) {
            System.err.println("[Voice] group send failed: " + ex.getMessage());
        }
    }

    // Called from the voice-playback executor — no Swing access here.
    public void play(Message m) {
        if (m.getSender().equals(client.getUsername())) return;
        try {
            byte[] chunk = Base64.getDecoder().decode(m.getContent());
            voice.play(chunk);
        } catch (Exception ex) {
            System.err.println("[Voice] group play failed: " + ex.getMessage());
        }
    }

    // Called when GROUP_VOICE_ROOM snapshot arrives. Replaces the list.
    public void applyRoster(Set<String> roster) {
        SwingUtilities.invokeLater(() -> {
            participants.clear();
            participants.add(client.getUsername());
            if (roster != null) participants.addAll(roster);
            rebuildList();
            updateStatus();
        });
    }

    public void participantJoined(String username) {
        SwingUtilities.invokeLater(() -> {
            if (participants.add(username)) {
                rebuildList();
                updateStatus();
            }
        });
    }

    public void participantLeft(String username) {
        SwingUtilities.invokeLater(() -> {
            if (participants.remove(username)) {
                rebuildList();
                updateStatus();
            }
        });
    }

    private void updateStatus() {
        int n = participants.size();
        statusLabel.setText(n == 1
                ? "Chỉ mình bạn trong voice"
                : n + " thành viên đang trong voice");
    }

    private void rebuildList() {
        participantsPanel.removeAll();
        for (String user : participants) {
            participantsPanel.add(buildRow(user, user.equals(client.getUsername())));
            participantsPanel.add(Box.createVerticalStrut(Theme.SP_1));
        }
        participantsPanel.revalidate();
        participantsPanel.repaint();
    }

    private JComponent buildRow(String username, boolean self) {
        UiKit.RoundedPanel row = new UiKit.RoundedPanel(Theme.RADIUS_CARD, Theme.surfaceCard(),
                new BorderLayout(Theme.SP_2, 0));
        row.setBorder(UiKit.pad(Theme.SP_2, Theme.SP_3, Theme.SP_2, Theme.SP_3));
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SP_2, 0));
        left.setOpaque(false);
        left.add(Avatar.component(username, 36, true));
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel nm = new JLabel(self ? username + " (bạn)" : username);
        nm.setFont(Theme.name());
        nm.setForeground(Theme.textPrimary());
        JLabel sub = new JLabel("Đang trong voice");
        sub.setFont(Theme.timestamp());
        sub.setForeground(Theme.online());
        text.add(nm);
        text.add(sub);
        left.add(text);
        row.add(left, BorderLayout.WEST);
        return row;
    }

    private void onClose() {
        try {
            client.send(new Message(Message.Type.GROUP_VOICE_LEAVE,
                    client.getUsername(), groupId, ""));
        } catch (Exception ignored) {}
        voice.stop();
        if (closeCallback != null) closeCallback.accept(groupId);
    }
}
