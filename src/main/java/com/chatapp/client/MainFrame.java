package com.chatapp.client;

import com.chatapp.model.Message;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class MainFrame extends JFrame {
    protected final DefaultListModel<String> usersModel = new DefaultListModel<>();
    private final JList<String> usersList = new JList<>(usersModel);
    private final Map<String, Integer> badges = new HashMap<>();
    protected Client client;
    protected ChatWindowManager windows;

    public MainFrame(Client client) {
        super("ChatApp — " + client.getUsername());
        this.client = client;
        this.windows = new ChatWindowManager(client);
        this.windows.setBadgeListener(this::setBadge);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(360, 480);
        setLocationRelativeTo(null);

        URL iconUrl = getClass().getResource("/icons/app.png");
        if (iconUrl != null) setIconImage(new ImageIcon(iconUrl).getImage());

        setLayout(new BorderLayout());
        JLabel header = new JLabel("Đang online (double-click để chat):");
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));
        add(header, BorderLayout.NORTH);

        usersList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        add(new JScrollPane(usersList), BorderLayout.CENTER);

        usersList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                String name = String.valueOf(value);
                Integer n = badges.get(name);
                String text = (n != null && n > 0) ? name + "  (" + n + ")" : name;
                return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
            }
        });

        usersList.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String peer = usersList.getSelectedValue();
                    if (peer != null) windows.openWith(peer);
                }
            }
        });
    }

    private void setBadge(String peer, int count) {
        if (count <= 0) badges.remove(peer); else badges.put(peer, count);
        usersList.repaint();
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
            case VOICE -> windows.dispatchVoice(msg);
            case VOICE_END -> windows.handleVoiceEnd(msg);
            case VIDEO -> windows.dispatchVideo(msg);
            case VIDEO_END -> windows.handleVideoEnd(msg);
            default -> {}
        }
    }
}
