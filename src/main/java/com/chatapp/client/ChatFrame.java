package com.chatapp.client;

import com.chatapp.model.Message;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ChatFrame extends JFrame {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final JTextArea history = new JTextArea();
    private final JTextField input = new JTextField();
    private final JButton sendButton = new JButton("Gửi");
    protected final Client client;
    protected final String peer;

    public ChatFrame(Client client, String peer) {
        super("Chat với " + peer);
        this.client = client;
        this.peer = peer;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(480, 360);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(4, 4));

        history.setEditable(false);
        history.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        history.setMargin(new Insets(6, 6, 6, 6));
        add(new JScrollPane(history), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(4, 0));
        bottom.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        bottom.add(input, BorderLayout.CENTER);
        bottom.add(sendButton, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        sendButton.addActionListener(this::onSend);
        input.addActionListener(this::onSend);
    }

    protected void onSend(ActionEvent e) {
        String text = input.getText().trim();
        if (text.isEmpty()) return;
        Message m = new Message(Message.Type.CHAT, client.getUsername(), peer, text);
        try {
            client.send(m);
            appendLine("Tôi", text);
            input.setText("");
        } catch (Exception ex) {
            appendLine("[lỗi]", ex.getMessage());
        }
    }

    public void receive(Message m) {
        SwingUtilities.invokeLater(() -> appendLine(m.getSender(), m.getContent()));
    }

    protected void appendLine(String who, String text) {
        history.append("[" + LocalTime.now().format(TIME) + "] " + who + ": " + text + "\n");
        history.setCaretPosition(history.getDocument().getLength());
    }
}
