package com.chatapp.client;

import com.chatapp.client.ui.Avatar;
import com.chatapp.client.ui.ConversationView;
import com.chatapp.client.ui.Icons;
import com.chatapp.client.ui.MessageBubble;
import com.chatapp.client.ui.Theme;
import com.chatapp.client.ui.UiKit;
import com.chatapp.model.Message;
import com.chatapp.util.FileTransfer;
import com.chatapp.util.HistoryManager;
import com.chatapp.util.VoiceChat;

import javax.sound.sampled.LineUnavailableException;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

/**
 * Group conversation. Shares the bubble-based {@link ConversationView} with
 * {@link ChatPanel}; sender names + avatars are shown per first-of-run so each
 * member is distinguishable. Adds group-only controls: members, voice toggle,
 * video room, and file transfer. Networking / history behaviour is unchanged.
 */
public class GroupPanel extends JPanel {
    private static final String FONT_EMOJI = "Segoe UI Emoji";

    private final ConversationView conversation;
    private final JTextPane input = new JTextPane();
    private final UiKit.CircleButton sendButton = new UiKit.CircleButton("", 38);
    private final JCheckBox enterSendsBox = new JCheckBox("ENTER gửi", true);
    private final JButton voiceButton = Icons.button(Icons.MIC, "Bật / tắt voice nhóm");
    private final JButton videoButton = Icons.button(Icons.VIDEO, "Mở phòng video nhóm");
    private final JButton membersButton = Icons.button(Icons.GROUP, "Xem thành viên nhóm");

    private final VoiceChat voice = new VoiceChat();
    private boolean voiceJoined = false;

    private static final String[] EMOJIS = {
            "😀", "😂", "😍", "😎", "😢", "👍",
            "🙏", "❤", "🔥", "🎉", "😡", "😴"
    };

    private final Client client;
    private final String groupId;
    private final String groupName;
    private final ChatWindowManager manager;

    public GroupPanel(Client client, String groupId, String groupName) {
        this(client, groupId, groupName, null);
    }

    public GroupPanel(Client client, String groupId, String groupName, ChatWindowManager manager) {
        this.client = client;
        this.groupId = groupId;
        this.groupName = groupName;
        this.manager = manager;
        setLayout(new BorderLayout());
        setBackground(Theme.bgApp());

        add(buildHeader(), BorderLayout.NORTH);

        conversation = new ConversationView(true,
                "Chưa có tin nhắn trong " + groupName,
                "Hãy bắt đầu cuộc trò chuyện nhóm 👋");
        add(conversation, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.setBorder(UiKit.pad(Theme.SP_2, Theme.SP_3, Theme.SP_3, Theme.SP_3));
        bottom.add(buildInputBar(), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        installKeyBindings();
        loadHistory();
        updateSendEnabled();
    }

    // ---------------- thread header ----------------

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout(Theme.SP_3, 0));
        header.setBackground(Theme.bgApp());
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.border()),
                UiKit.pad(Theme.SP_2, Theme.SP_3, Theme.SP_2, Theme.SP_3)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SP_2, 0));
        left.setOpaque(false);
        left.add(Avatar.component(groupName, 32, false));
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel name = new JLabel(groupName);
        name.setFont(Theme.name());
        name.setForeground(Theme.textPrimary());
        JLabel sub = new JLabel("Nhóm trò chuyện");
        sub.setFont(Theme.timestamp());
        sub.setForeground(Theme.textSecondary());
        text.add(name);
        text.add(sub);
        left.add(text);
        header.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.SP_1, 0));
        right.setOpaque(false);
        membersButton.addActionListener(e -> showMembers());
        voiceButton.addActionListener(e -> toggleVoice());
        videoButton.addActionListener(e -> openVideoRoom());
        right.add(membersButton);
        right.add(voiceButton);
        right.add(videoButton);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    // ---------------- input bar ----------------

    private UiKit.RoundedPanel buildInputBar() {
        UiKit.RoundedPanel box = new UiKit.RoundedPanel(Theme.RADIUS_CARD, Theme.surfaceCard(),
                new BorderLayout(Theme.SP_2, 0));
        box.setBorder(UiKit.pad(Theme.SP_1, Theme.SP_2, Theme.SP_1, Theme.SP_2));

        JButton emojiButton = Icons.button(Icons.SMILEY, "Chèn emoji");
        emojiButton.addActionListener(e -> showEmojiPopup(emojiButton));
        JButton fileButton = Icons.button(Icons.ATTACH, "Gửi file cho cả nhóm");
        fileButton.addActionListener(this::onPickFile);
        JPanel leftButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftButtons.setOpaque(false);
        leftButtons.add(emojiButton);
        leftButtons.add(fileButton);
        box.add(leftButtons, BorderLayout.WEST);

        input.setFont(Theme.body());
        input.setOpaque(false);
        input.setBorder(UiKit.pad(Theme.SP_2, Theme.SP_1, Theme.SP_2, Theme.SP_1));
        input.setForeground(Theme.textPrimary());
        JScrollPane inputScroll = new JScrollPane(input,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        inputScroll.setOpaque(false);
        inputScroll.getViewport().setOpaque(false);
        inputScroll.setBorder(null);
        inputScroll.setPreferredSize(new Dimension(0, 44));
        box.add(inputScroll, BorderLayout.CENTER);

        sendButton.setToolTipText("Gửi (ENTER) · Shift+ENTER xuống dòng");
        sendButton.addActionListener(this::onSend);
        enterSendsBox.setOpaque(false);
        enterSendsBox.setForeground(Theme.textSecondary());
        enterSendsBox.setFont(Theme.timestamp());
        enterSendsBox.setToolTipText("Bật: ENTER gửi, Shift+ENTER xuống dòng · Tắt: Ctrl+ENTER gửi");
        JPanel rightSide = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.SP_1, 0));
        rightSide.setOpaque(false);
        rightSide.add(enterSendsBox);
        rightSide.add(sendButton);
        box.add(rightSide, BorderLayout.EAST);

        input.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { box.setFocused(true); }
            @Override public void focusLost(FocusEvent e) { box.setFocused(false); }
        });
        input.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { onInputChanged(inputScroll); }
            public void removeUpdate(DocumentEvent e) { onInputChanged(inputScroll); }
            public void changedUpdate(DocumentEvent e) { onInputChanged(inputScroll); }
        });
        return box;
    }

    private void onInputChanged(JScrollPane scroll) {
        updateSendEnabled();
        int lines = input.getText().split("\n", -1).length;
        int h = Math.min(5, Math.max(1, lines)) * 22 + 14;
        scroll.setPreferredSize(new Dimension(0, h));
        scroll.revalidate();
    }

    private void updateSendEnabled() {
        sendButton.setEnabled(!input.getText().trim().isEmpty());
    }

    // ---------------- group actions ----------------

    private void onPickFile(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = chooser.getSelectedFile();
        try {
            Message m = FileTransfer.buildFileMessage(client.getUsername(), groupId,
                    f.toPath(), Message.Type.GROUP_FILE);
            client.send(m);
            Message slim = new Message(Message.Type.FILE,
                    client.getUsername(), groupId, f.getName());
            slim.setTimestamp(m.getTimestamp());
            HistoryManager.appendGroup(client.getUsername(), groupId, slim);
            conversation.addFile(client.getUsername(), true, m.getTimestamp(),
                    f.getName(), f.toPath(), "đã gửi · bấm để mở",
                    MessageBubble.Status.SENT);
        } catch (Exception ex) {
            conversation.addText("[lỗi]", false, System.currentTimeMillis(),
                    ex.getMessage(), MessageBubble.Status.NONE);
        }
    }

    private void openVideoRoom() {
        if (manager != null) manager.openGroupVideoRoom(groupId, groupName);
    }

    private void showMembers() {
        if (manager == null) return;
        manager.queryGroupMembers(groupId, info -> {
            Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
            new GroupMembersDialog(owner, client, groupId, groupName, info).setVisible(true);
        });
    }

    private void toggleVoice() {
        if (voiceJoined) {
            voice.stop();
            voiceButton.setToolTipText("Bật / tắt voice nhóm");
            conversation.addText("[voice]", false, System.currentTimeMillis(),
                    "Bạn rời voice nhóm.", MessageBubble.Status.NONE);
            voiceJoined = false;
        } else {
            try {
                voice.startPlayback();
                voice.startCapture(this::sendVoiceChunk);
                voiceButton.setToolTipText("Đang trong voice — bấm để rời");
                conversation.addText("[voice]", false, System.currentTimeMillis(),
                        "Bạn tham gia voice nhóm.", MessageBubble.Status.NONE);
                voiceJoined = true;
            } catch (LineUnavailableException ex) {
                JOptionPane.showMessageDialog(this,
                        "Không mở được mic / loa: " + ex.getMessage(),
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void sendVoiceChunk(byte[] chunk) {
        try {
            String b64 = Base64.getEncoder().encodeToString(chunk);
            client.send(new Message(Message.Type.GROUP_VOICE,
                    client.getUsername(), groupId, b64));
        } catch (Exception ex) {
            System.err.println("[Voice] group send failed: " + ex.getMessage());
        }
    }

    public void playGroupVoice(Message m) {
        if (!voiceJoined) return;       // not in the room, drop the chunk
        if (m.getSender().equals(client.getUsername())) return; // echo guard
        try {
            byte[] chunk = Base64.getDecoder().decode(m.getContent());
            voice.play(chunk);
        } catch (Exception ex) {
            System.err.println("[Voice] group play failed: " + ex.getMessage());
        }
    }

    public void stopVoice() {
        if (voiceJoined) {
            voice.stop();
            voiceJoined = false;
        }
    }

    // ---------------- history ----------------

    private void loadHistory() {
        List<String> lines = HistoryManager.loadGroup(client.getUsername(), groupId);
        for (String line : lines) {
            String[] parts = line.split("\\|", 4);
            if (parts.length < 4) continue;
            long ts;
            try { ts = Long.parseLong(parts[0]); } catch (NumberFormatException ex) { continue; }
            String sender = parts[1];
            String type = parts[2];
            String content = parts[3].replace("\\n", "\n");
            boolean mine = sender.equals(client.getUsername());
            String body = "FILE".equals(type) || "GROUP_FILE".equals(type)
                    ? "[file] " + content : content;
            conversation.addText(sender, mine, ts, body,
                    mine ? MessageBubble.Status.SENT : MessageBubble.Status.NONE);
        }
    }

    public String getGroupId() { return groupId; }
    public String getGroupName() { return groupName; }
    public void requestFocusOnInput() { input.requestFocusInWindow(); }

    private void installKeyBindings() {
        InputMap im = input.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = input.getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "group.enter");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK), "group.newline");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "group.send");
        am.put("group.enter", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (enterSendsBox.isSelected()) onSend(e);
                else input.replaceSelection("\n");
            }
        });
        am.put("group.newline", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { input.replaceSelection("\n"); }
        });
        am.put("group.send", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { onSend(e); }
        });
    }

    private static AttributeSet emojiStyle() {
        SimpleAttributeSet a = new SimpleAttributeSet();
        StyleConstants.setFontFamily(a, FONT_EMOJI);
        StyleConstants.setFontSize(a, 14);
        return a;
    }

    private void showEmojiPopup(JComponent anchor) {
        JPopupMenu popup = new JPopupMenu();
        JPanel grid = new JPanel(new GridLayout(2, 6, 2, 2));
        grid.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        Font emojiFont = new Font(FONT_EMOJI, Font.PLAIN, 18);
        for (String emoji : EMOJIS) {
            JButton b = new JButton(emoji);
            b.setFont(emojiFont);
            b.setFocusable(false);
            b.setMargin(new Insets(2, 6, 2, 6));
            b.addActionListener(ev -> {
                try {
                    input.getStyledDocument().insertString(
                            input.getCaretPosition(), emoji, emojiStyle());
                } catch (BadLocationException ignored) {}
                popup.setVisible(false);
                input.requestFocusInWindow();
            });
            grid.add(b);
        }
        popup.add(grid);
        popup.show(anchor, 0, -popup.getPreferredSize().height);
    }

    private void onSend(ActionEvent e) {
        String text = input.getText().trim();
        if (text.isEmpty()) return;
        Message m = new Message(Message.Type.GROUP_CHAT, client.getUsername(), groupId, text);
        try {
            client.send(m);
            HistoryManager.appendGroup(client.getUsername(), groupId, m);
            conversation.addText(client.getUsername(), true, m.getTimestamp(), text,
                    MessageBubble.Status.SENT);
            input.setText("");
            updateSendEnabled();
        } catch (Exception ex) {
            conversation.addText("[lỗi]", false, System.currentTimeMillis(),
                    ex.getMessage(), MessageBubble.Status.NONE);
        }
    }

    public void receive(Message m) {
        SwingUtilities.invokeLater(() -> {
            if (m.getType() == Message.Type.GROUP_FILE) {
                try {
                    Path saved = FileTransfer.saveIncoming(m,
                            Path.of("data", "files", "group_" + groupId, m.getSender()));
                    Message slim = new Message(Message.Type.FILE,
                            m.getSender(), groupId,
                            saved.getFileName().toString());
                    slim.setTimestamp(m.getTimestamp());
                    HistoryManager.appendGroup(client.getUsername(), groupId, slim);
                    conversation.addFile(m.getSender(), false, m.getTimestamp(),
                            saved.getFileName().toString(), saved,
                            "đã lưu · bấm để mở", MessageBubble.Status.NONE);
                } catch (Exception ex) {
                    conversation.addText("[lỗi file]", false, System.currentTimeMillis(),
                            ex.getMessage(), MessageBubble.Status.NONE);
                }
            } else {
                HistoryManager.appendGroup(client.getUsername(), groupId, m);
                conversation.addText(m.getSender(), false, m.getTimestamp(),
                        m.getContent(), MessageBubble.Status.NONE);
            }
        });
    }
}
