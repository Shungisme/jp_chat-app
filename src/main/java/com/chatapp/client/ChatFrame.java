package com.chatapp.client;

import com.chatapp.model.Message;
import com.chatapp.util.FileTransfer;
import com.chatapp.util.HistoryManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ChatFrame extends JFrame {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final JTextArea history = new JTextArea();
    private final JTextField input = new JTextField();
    private final JButton sendButton = new JButton("Gửi");
    private final JButton fileButton = new JButton("📎");
    private final JButton historyButton = new JButton("Lịch sử");
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
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        right.add(historyButton);
        right.add(fileButton);
        right.add(sendButton);
        bottom.add(input, BorderLayout.CENTER);
        bottom.add(right, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        sendButton.addActionListener(this::onSend);
        input.addActionListener(this::onSend);
        fileButton.addActionListener(this::onPickFile);
        historyButton.addActionListener(e -> new HistoryFrame(client.getUsername(), peer).setVisible(true));

        loadHistory();
    }

    private void loadHistory() {
        List<String> lines = HistoryManager.load(client.getUsername(), peer);
        for (String l : lines) history.append("(cũ) " + l + "\n");
    }

    protected void onSend(ActionEvent e) {
        String text = input.getText().trim();
        if (text.isEmpty()) return;
        Message m = new Message(Message.Type.CHAT, client.getUsername(), peer, text);
        try {
            client.send(m);
            HistoryManager.append(client.getUsername(), peer, m);
            appendLine("Tôi", text);
            input.setText("");
        } catch (Exception ex) {
            appendLine("[lỗi]", ex.getMessage());
        }
    }

    protected void onPickFile(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = chooser.getSelectedFile();
        try {
            Message m = FileTransfer.buildFileMessage(client.getUsername(), peer, Path.of(f.getAbsolutePath()));
            client.send(m);
            HistoryManager.append(client.getUsername(), peer,
                    new Message(Message.Type.FILE, client.getUsername(), peer, f.getName()));
            appendLine("Tôi", "[file] " + f.getName());
        } catch (Exception ex) {
            appendLine("[lỗi file]", ex.getMessage());
        }
    }

    public void receive(Message m) {
        SwingUtilities.invokeLater(() -> {
            HistoryManager.append(client.getUsername(), peer, m);
            if (m.getType() == Message.Type.FILE) {
                String[] parts = m.getContent().split("\\|", 2);
                appendLine(m.getSender(), "[file] " + (parts.length > 0 ? parts[0] : "(unknown)"));
            } else {
                appendLine(m.getSender(), m.getContent());
            }
        });
    }

    protected void appendLine(String who, String text) {
        history.append("[" + LocalTime.now().format(TIME) + "] " + who + ": " + text + "\n");
        history.setCaretPosition(history.getDocument().getLength());
    }
}
