package com.chatapp.client;

import com.chatapp.model.Message;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class GroupFrame extends JFrame {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final String FONT_TEXT = "Segoe UI";
    private static final String FONT_EMOJI = "Segoe UI Emoji";
    private static final int FONT_SIZE = 13;

    private final JTextPane history = new JTextPane();
    private final JTextField input = new JTextField();
    private final JButton sendButton = new JButton("Gửi");
    private final Client client;
    private final String groupName;
    private final Consumer<String> closeCallback;

    public GroupFrame(Client client, String groupName) {
        this(client, groupName, null);
    }

    public GroupFrame(Client client, String groupName, Consumer<String> closeCallback) {
        super("Nhóm: " + groupName);
        this.client = client;
        this.groupName = groupName;
        this.closeCallback = closeCallback;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) {
                if (closeCallback != null) closeCallback.accept(groupName);
            }
        });
        setSize(520, 400);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(4, 4));

        history.setEditable(false);
        history.setFont(new Font(FONT_TEXT, Font.PLAIN, FONT_SIZE));
        add(new JScrollPane(history), BorderLayout.CENTER);
        input.setFont(new Font(FONT_EMOJI, Font.PLAIN, FONT_SIZE));

        JPanel bottom = new JPanel(new BorderLayout(4, 0));
        bottom.add(input, BorderLayout.CENTER);
        bottom.add(sendButton, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        sendButton.addActionListener(this::onSend);
        input.addActionListener(this::onSend);
    }

    private void onSend(ActionEvent e) {
        String text = input.getText().trim();
        if (text.isEmpty()) return;
        try {
            client.send(new Message(Message.Type.GROUP_CHAT, client.getUsername(), groupName, text));
            appendLine("Tôi", text);
            input.setText("");
        } catch (Exception ex) {
            appendLine("[lỗi]", ex.getMessage());
        }
    }

    public void receive(Message m) {
        SwingUtilities.invokeLater(() -> appendLine(m.getSender(), m.getContent()));
    }

    private static boolean isEmojiCodePoint(int cp) {
        return (cp >= 0x2600 && cp <= 0x27BF) || cp >= 0x1F000;
    }

    private void appendStyled(String text) {
        StyledDocument doc = history.getStyledDocument();
        SimpleAttributeSet tx = new SimpleAttributeSet();
        StyleConstants.setFontFamily(tx, FONT_TEXT);
        StyleConstants.setFontSize(tx, FONT_SIZE);
        SimpleAttributeSet emo = new SimpleAttributeSet();
        StyleConstants.setFontFamily(emo, FONT_EMOJI);
        StyleConstants.setFontSize(emo, FONT_SIZE);
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

    public String getGroupName() { return groupName; }
}
