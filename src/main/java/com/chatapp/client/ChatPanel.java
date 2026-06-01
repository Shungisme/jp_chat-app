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
import java.util.List;

/**
 * One-to-one conversation. Renders messages as stacked {@link MessageBubble}
 * components in a {@link ConversationView}, with a modern thread header and a
 * rounded input bar. Networking / history behaviour is unchanged.
 */
public class ChatPanel extends JPanel {
    private static final String FONT_EMOJI = "Segoe UI Emoji";

    private final ConversationView conversation;
    private final JTextPane input = new JTextPane();
    private final UiKit.RoundedPanel inputBox;
    private final UiKit.CircleButton sendButton = new UiKit.CircleButton("", 38);
    private final JCheckBox enterSendsBox = new JCheckBox("ENTER gửi", true);

    private static final String[] EMOJIS = {
            "😀", "😂", "😍", "😎", "😢", "👍",
            "🙏", "❤", "🔥", "🎉", "😡", "😴"
    };
    protected final Client client;
    protected final String peer;
    protected final ChatWindowManager manager;

    public ChatPanel(Client client, String peer, ChatWindowManager manager) {
        this.client = client;
        this.peer = peer;
        this.manager = manager;
        setLayout(new BorderLayout());
        setBackground(Theme.bgApp());

        add(buildHeader(), BorderLayout.NORTH);

        conversation = new ConversationView(false,
                "Hãy gửi lời chào tới " + peer + " 👋",
                "Tin nhắn của bạn sẽ xuất hiện ở đây.");
        add(conversation, BorderLayout.CENTER);

        inputBox = buildInputBar();
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.setBorder(UiKit.pad(Theme.SP_2, Theme.SP_3, Theme.SP_3, Theme.SP_3));
        bottom.add(inputBox, BorderLayout.CENTER);
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
        left.add(Avatar.component(peer, 32, true));
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel name = new JLabel(peer);
        name.setFont(Theme.name());
        name.setForeground(Theme.textPrimary());
        JLabel sub = new JLabel("Đang hoạt động");
        sub.setFont(Theme.timestamp());
        sub.setForeground(Theme.online());
        text.add(name);
        text.add(sub);
        left.add(text);
        header.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.SP_1, 0));
        right.setOpaque(false);
        JButton voice = Icons.button(Icons.MIC, "Gọi thoại");
        JButton video = Icons.button(Icons.VIDEO, "Gọi video");
        JButton history = Icons.button(Icons.CLOCK, "Lịch sử trò chuyện");
        voice.addActionListener(e -> onStartVoiceCall());
        video.addActionListener(e -> onStartVideoCall());
        history.addActionListener(e -> new HistoryFrame(client.getUsername(), peer).setVisible(true));
        right.add(voice);
        right.add(video);
        right.add(history);
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
        JButton fileButton = Icons.button(Icons.ATTACH, "Gửi file");
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

        // focus glow + auto-grow + enable-on-content
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

    private void installKeyBindings() {
        InputMap im = input.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = input.getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "chat.enter");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK), "chat.newline");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "chat.send");
        am.put("chat.enter", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (enterSendsBox.isSelected()) onSend(e);
                else input.replaceSelection("\n");
            }
        });
        am.put("chat.newline", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { input.replaceSelection("\n"); }
        });
        am.put("chat.send", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { onSend(e); }
        });
    }

    public String getPeer() { return peer; }

    public void requestFocusOnInput() { input.requestFocusInWindow(); }

    protected void onStartVoiceCall() {
        if (manager != null) manager.initiateVoiceCall(peer);
    }

    protected void onStartVideoCall() {
        if (manager != null) manager.initiateVideoCall(peer);
    }

    private static AttributeSet emojiStyle() {
        SimpleAttributeSet a = new SimpleAttributeSet();
        StyleConstants.setFontFamily(a, FONT_EMOJI);
        StyleConstants.setFontSize(a, 14);
        return a;
    }

    // ---------------- emoji picker ----------------

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

    // ---------------- chat actions ----------------

    private void loadHistory() {
        List<String> lines = HistoryManager.load(client.getUsername(), peer);
        for (String l : lines) {
            String[] parts = l.split("\\|", 4);
            if (parts.length < 4) continue;
            long ts;
            try { ts = Long.parseLong(parts[0]); } catch (NumberFormatException ex) { continue; }
            String sender = parts[1];
            String type = parts[2];
            String content = parts[3].replace("\\n", "\n");
            boolean mine = sender.equals(client.getUsername());
            if ("FILE".equals(type)) {
                Path saved = Path.of("data", "files", mine ? client.getUsername() : sender, content);
                conversation.addFile(sender, mine, ts, content, saved, "đã gửi trước đó",
                        mine ? MessageBubble.Status.SENT : MessageBubble.Status.NONE);
            } else {
                conversation.addText(sender, mine, ts, content,
                        mine ? MessageBubble.Status.SENT : MessageBubble.Status.NONE);
            }
        }
    }

    protected void onSend(ActionEvent e) {
        String text = input.getText().trim();
        if (text.isEmpty()) return;
        Message m = new Message(Message.Type.CHAT, client.getUsername(), peer, text);
        try {
            client.send(m);
            HistoryManager.append(client.getUsername(), peer, m);
            conversation.addText(client.getUsername(), true, m.getTimestamp(), text,
                    MessageBubble.Status.SENT);
            input.setText("");
            updateSendEnabled();
        } catch (Exception ex) {
            conversation.addText("[lỗi]", false, System.currentTimeMillis(),
                    ex.getMessage(), MessageBubble.Status.NONE);
        }
    }

    protected void onPickFile(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = chooser.getSelectedFile();
        try {
            Message m = FileTransfer.buildFileMessage(client.getUsername(), peer, f.toPath());
            client.send(m);
            HistoryManager.append(client.getUsername(), peer,
                    new Message(Message.Type.FILE, client.getUsername(), peer, f.getName()));
            conversation.addFile(client.getUsername(), true, System.currentTimeMillis(),
                    f.getName(), f.toPath(), "đã gửi", MessageBubble.Status.SENT);
        } catch (Exception ex) {
            conversation.addText("[lỗi file]", false, System.currentTimeMillis(),
                    ex.getMessage(), MessageBubble.Status.NONE);
        }
    }

    public void receive(Message m) {
        SwingUtilities.invokeLater(() -> {
            if (m.getType() == Message.Type.FILE) {
                try {
                    Path saved = FileTransfer.saveIncoming(m,
                            Path.of("data", "files", m.getSender()));
                    Message slim = new Message(Message.Type.FILE,
                            m.getSender(), client.getUsername(),
                            saved.getFileName().toString());
                    slim.setTimestamp(m.getTimestamp());
                    HistoryManager.append(client.getUsername(), peer, slim);
                    conversation.addFile(m.getSender(), false, m.getTimestamp(),
                            saved.getFileName().toString(), saved, "đã lưu",
                            MessageBubble.Status.NONE);
                } catch (Exception ex) {
                    conversation.addText("[lỗi file]", false, System.currentTimeMillis(),
                            ex.getMessage(), MessageBubble.Status.NONE);
                }
            } else {
                HistoryManager.append(client.getUsername(), peer, m);
                conversation.addText(m.getSender(), false, m.getTimestamp(),
                        m.getContent(), MessageBubble.Status.NONE);
            }
        });
    }
}
