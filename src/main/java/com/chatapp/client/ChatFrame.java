package com.chatapp.client;

import com.chatapp.model.Message;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class ChatFrame extends JFrame {
    private final JTextArea history = new JTextArea();
    private final JTextField input = new JTextField();
    private final JButton sendButton = new JButton("Gửi");
    private final Client client;
    protected final String peer;

    public ChatFrame(Client client, String peer) {
        super("Chat với " + peer);
        this.client = client;
        this.peer = peer;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(480, 360);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        history.setEditable(false);
        add(new JScrollPane(history), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
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
            appendLine("Tôi: " + text);
            input.setText("");
        } catch (Exception ex) {
            appendLine("[lỗi gửi]: " + ex.getMessage());
        }
    }

    public void receive(Message m) {
        SwingUtilities.invokeLater(() -> appendLine(m.getSender() + ": " + m.getContent()));
    }

    protected void appendLine(String line) {
        history.append(line + "\n");
    }
}
