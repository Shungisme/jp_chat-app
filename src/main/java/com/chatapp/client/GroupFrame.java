package com.chatapp.client;

import com.chatapp.model.Message;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class GroupFrame extends JFrame {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final JTextArea history = new JTextArea();
    private final JTextField input = new JTextField();
    private final JButton sendButton = new JButton("Gửi");
    private final Client client;
    private final String groupName;

    public GroupFrame(Client client, String groupName) {
        super("Nhóm: " + groupName);
        this.client = client;
        this.groupName = groupName;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(520, 400);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(4, 4));

        history.setEditable(false);
        history.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 13));
        add(new JScrollPane(history), BorderLayout.CENTER);
        input.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 13));

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

    private void appendLine(String who, String text) {
        history.append("[" + LocalTime.now().format(TIME) + "] " + who + ": " + text + "\n");
        history.setCaretPosition(history.getDocument().getLength());
    }

    public String getGroupName() { return groupName; }
}
