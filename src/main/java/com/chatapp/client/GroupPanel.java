package com.chatapp.client;

import com.chatapp.model.Message;
import com.chatapp.util.FileTransfer;
import com.chatapp.util.HistoryManager;
import com.chatapp.util.VoiceChat;

import javax.sound.sampled.LineUnavailableException;
import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class GroupPanel extends JPanel {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final String FONT_TEXT = "Segoe UI";
    private static final String FONT_EMOJI = "Segoe UI Emoji";
    private static final int FONT_SIZE = 13;
    private static final String FILE_PATH_ATTR = "chatapp.file.path";

    private final JTextPane history = new JTextPane();
    private final JTextPane input = new JTextPane();
    private final JButton sendButton = new JButton("Gửi");
    private final JButton fileButton = new JButton("📎");
    private final JButton voiceButton = new JButton("🎙");
    private final JButton emojiButton = new JButton("😀");
    private final JCheckBox enterSendsBox = new JCheckBox("ENTER gửi", true);

    private final VoiceChat voice = new VoiceChat();
    private boolean voiceJoined = false;

    private static final String[] EMOJIS = {
            "😀", "😂", "😍", "😎", "😢", "👍",
            "🙏", "❤", "🔥", "🎉", "😡", "😴"
    };

    private final Client client;
    private final String groupId;
    private final String groupName;

    public GroupPanel(Client client, String groupId, String groupName) {
        this.client = client;
        this.groupId = groupId;
        this.groupName = groupName;
        setLayout(new BorderLayout(4, 4));

        JLabel header = new JLabel("Nhóm: " + groupName);
        header.setFont(new Font(FONT_TEXT, Font.BOLD, 13));
        header.setBorder(BorderFactory.createEmptyBorder(6, 8, 4, 8));
        add(header, BorderLayout.NORTH);

        history.setEditable(false);
        history.setFont(new Font(FONT_TEXT, Font.PLAIN, FONT_SIZE));
        history.setMargin(new Insets(6, 6, 6, 6));
        history.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { onHistoryClick(e); }
        });
        add(new JScrollPane(history), BorderLayout.CENTER);

        input.setFont(new Font(FONT_TEXT, Font.PLAIN, FONT_SIZE));
        input.setMargin(new Insets(4, 6, 4, 6));
        JScrollPane inputScroll = new JScrollPane(input);
        inputScroll.setPreferredSize(new Dimension(0, 64));

        emojiButton.setFont(new Font(FONT_EMOJI, Font.PLAIN, 13));
        emojiButton.setMargin(new Insets(0, 6, 0, 6));
        emojiButton.setToolTipText("Chèn emoji");

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        toolbar.add(emojiButton);
        toolbar.add(enterSendsBox);
        JLabel hint = new JLabel("(tắt: Ctrl+ENTER để gửi)");
        hint.setFont(new Font(FONT_TEXT, Font.PLAIN, 11));
        hint.setForeground(new Color(110, 110, 110));
        toolbar.add(hint);

        JPanel bottom = new JPanel(new BorderLayout(4, 4));
        bottom.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        fileButton.setToolTipText("Gửi file cho cả nhóm");
        voiceButton.setToolTipText("Bật / tắt voice nhóm");
        buttons.add(fileButton);
        buttons.add(voiceButton);
        buttons.add(sendButton);

        JPanel topRow = new JPanel(new BorderLayout(4, 0));
        topRow.add(toolbar, BorderLayout.WEST);
        topRow.add(buttons, BorderLayout.EAST);

        bottom.add(topRow, BorderLayout.NORTH);
        bottom.add(inputScroll, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        sendButton.addActionListener(this::onSend);
        fileButton.addActionListener(this::onPickFile);
        voiceButton.addActionListener(e -> toggleVoice());
        emojiButton.addActionListener(e -> showEmojiPopup());

        installKeyBindings();
        loadHistory();
    }

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
            appendFileLine("Tôi", f.getName(), f.toPath(), "đã gửi (bấm để mở)");
        } catch (Exception ex) {
            appendLine("[lỗi file]", ex.getMessage());
        }
    }

    private void toggleVoice() {
        if (voiceJoined) {
            voice.stop();
            voiceButton.setText("🎙");
            appendLine("[voice]", "Bạn rời voice nhóm.");
            voiceJoined = false;
        } else {
            try {
                voice.startPlayback();
                voice.startCapture(this::sendVoiceChunk);
                voiceButton.setText("🛑");
                appendLine("[voice]", "Bạn tham gia voice nhóm.");
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

    private void onHistoryClick(MouseEvent e) {
        int offset = history.viewToModel2D(e.getPoint());
        if (offset < 0) return;
        Element el = history.getStyledDocument().getCharacterElement(offset);
        Object path = el.getAttributes().getAttribute(FILE_PATH_ATTR);
        if (path == null) return;
        Path p = Path.of(path.toString());
        if (!Files.exists(p)) {
            JOptionPane.showMessageDialog(this,
                    "File không còn ở: " + p, "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            Desktop.getDesktop().open(p.toFile());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Không mở được file: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadHistory() {
        List<String> lines = HistoryManager.loadGroup(client.getUsername(), groupId);
        ZoneId zone = ZoneId.systemDefault();
        for (String line : lines) {
            String[] parts = line.split("\\|", 4);
            if (parts.length < 4) continue;
            long ts;
            try { ts = Long.parseLong(parts[0]); } catch (NumberFormatException ex) { continue; }
            String sender = parts[1];
            String type = parts[2];
            String content = parts[3].replace("\\n", "\n");
            String who = sender.equals(client.getUsername()) ? "Tôi" : sender;
            String time = Instant.ofEpochMilli(ts).atZone(zone).format(TIME);
            String body = "FILE".equals(type) || "GROUP_FILE".equals(type)
                    ? "[file] " + content : content;
            appendStyled("(cũ) [" + time + "] " + who + ": " + body + "\n");
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

    private static boolean isEmojiCodePoint(int cp) {
        return (cp >= 0x2600 && cp <= 0x27BF) || cp >= 0x1F000;
    }

    private static AttributeSet textStyle() {
        SimpleAttributeSet a = new SimpleAttributeSet();
        StyleConstants.setFontFamily(a, FONT_TEXT);
        StyleConstants.setFontSize(a, FONT_SIZE);
        return a;
    }

    private static AttributeSet emojiStyle() {
        SimpleAttributeSet a = new SimpleAttributeSet();
        StyleConstants.setFontFamily(a, FONT_EMOJI);
        StyleConstants.setFontSize(a, FONT_SIZE);
        return a;
    }

    private void appendStyled(String text) {
        StyledDocument doc = history.getStyledDocument();
        AttributeSet tx = textStyle();
        AttributeSet emo = emojiStyle();
        int i = 0;
        try {
            while (i < text.length()) {
                int cp = text.codePointAt(i);
                int n = Character.charCount(cp);
                AttributeSet a = isEmojiCodePoint(cp) ? emo : tx;
                doc.insertString(doc.getLength(), text.substring(i, i + n), a);
                i += n;
            }
        } catch (BadLocationException ignored) {}
    }

    private void appendLine(String who, String text) {
        appendStyled("[" + LocalTime.now().format(TIME) + "] " + who + ": " + text + "\n");
        history.setCaretPosition(history.getDocument().getLength());
    }

    private static AttributeSet linkStyle(Path savedPath) {
        SimpleAttributeSet a = new SimpleAttributeSet();
        StyleConstants.setFontFamily(a, FONT_TEXT);
        StyleConstants.setFontSize(a, FONT_SIZE);
        StyleConstants.setForeground(a, new Color(20, 90, 200));
        StyleConstants.setUnderline(a, true);
        a.addAttribute(FILE_PATH_ATTR, savedPath.toAbsolutePath().toString());
        return a;
    }

    private void appendLink(String visibleText, Path savedPath) {
        StyledDocument doc = history.getStyledDocument();
        try {
            doc.insertString(doc.getLength(), visibleText, linkStyle(savedPath));
        } catch (BadLocationException ignored) {}
    }

    private void appendFileLine(String who, String filename, Path savedPath, String suffix) {
        appendStyled("[" + LocalTime.now().format(TIME) + "] " + who + ": [file] ");
        appendLink(filename, savedPath);
        appendStyled(" — " + suffix + "\n");
        history.setCaretPosition(history.getDocument().getLength());
    }

    private void showEmojiPopup() {
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
        popup.show(emojiButton, 0, -popup.getPreferredSize().height);
    }

    private void onSend(ActionEvent e) {
        String text = input.getText().trim();
        if (text.isEmpty()) return;
        Message m = new Message(Message.Type.GROUP_CHAT, client.getUsername(), groupId, text);
        try {
            client.send(m);
            HistoryManager.appendGroup(client.getUsername(), groupId, m);
            appendLine("Tôi", text);
            input.setText("");
        } catch (Exception ex) {
            appendLine("[lỗi]", ex.getMessage());
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
                    appendFileLine(m.getSender(), saved.getFileName().toString(),
                            saved, "đã lưu, bấm để mở");
                } catch (Exception ex) {
                    appendLine("[lỗi file]", ex.getMessage());
                }
            } else {
                HistoryManager.appendGroup(client.getUsername(), groupId, m);
                appendLine(m.getSender(), m.getContent());
            }
        });
    }
}
