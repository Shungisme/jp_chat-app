package com.chatapp.client;

import com.chatapp.model.Message;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    private final DefaultListModel<String> usersModel = new DefaultListModel<>();
    private final JList<String> usersList = new JList<>(usersModel);
    protected Client client;

    public MainFrame(Client client) {
        super("ChatApp — " + client.getUsername());
        this.client = client;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(360, 480);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());
        add(new JLabel("Đang online:"), BorderLayout.NORTH);
        add(new JScrollPane(usersList), BorderLayout.CENTER);
    }

    public void onMessage(Message msg) {
        if (msg.getType() == Message.Type.USER_LIST) {
            SwingUtilities.invokeLater(() -> {
                usersModel.clear();
                if (msg.getContent() != null && !msg.getContent().isBlank()) {
                    for (String u : msg.getContent().split(",")) {
                        if (!u.equals(client.getUsername())) usersModel.addElement(u);
                    }
                }
            });
        }
    }
}
