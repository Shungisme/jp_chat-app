package com.chatapp.client;

import com.chatapp.model.Message;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MainFrame extends JFrame {
    protected final DefaultListModel<String> usersModel = new DefaultListModel<>();
    private final JList<String> usersList = new JList<>(usersModel);
    protected Client client;
    protected ChatWindowManager windows;

    public MainFrame(Client client) {
        super("ChatApp — " + client.getUsername());
        this.client = client;
        this.windows = new ChatWindowManager(client);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(360, 480);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());
        add(new JLabel("Đang online (double-click để chat):"), BorderLayout.NORTH);
        add(new JScrollPane(usersList), BorderLayout.CENTER);

        usersList.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String peer = usersList.getSelectedValue();
                    if (peer != null) windows.openWith(peer);
                }
            }
        });
    }

    public void onMessage(Message msg) {
        switch (msg.getType()) {
            case USER_LIST -> SwingUtilities.invokeLater(() -> {
                usersModel.clear();
                if (msg.getContent() != null && !msg.getContent().isBlank()) {
                    for (String u : msg.getContent().split(",")) {
                        if (!u.equals(client.getUsername())) usersModel.addElement(u);
                    }
                }
            });
            case CHAT -> windows.dispatch(msg);
            default -> {}
        }
    }
}
