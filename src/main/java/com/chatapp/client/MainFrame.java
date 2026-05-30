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
    private final JTabbedPane chatTabs = new JTabbedPane();
    private final Map<String, Integer> badges = new HashMap<>();
    protected Client client;
    protected ChatWindowManager windows;
    protected final ServerConfig.Entry server;

    public MainFrame(Client client) {
        this(client, null);
    }

    public MainFrame(Client client, ServerConfig.Entry server) {
        super("ChatApp — " + client.getUsername()
                + (server != null ? "  @  " + server.label() : ""));
        this.client = client;
        this.server = server;
        this.windows = new ChatWindowManager(client);
        this.windows.setBadgeListener(this::setBadge);
        this.windows.setMainFrame(this);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 560);
        setLocationRelativeTo(null);

        URL iconUrl = getClass().getResource("/icons/app.png");
        if (iconUrl != null) setIconImage(new ImageIcon(iconUrl).getImage());

        // Header
        JPanel header = new JPanel(new GridLayout(0, 1));
        header.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));
        JLabel title = new JLabel("ChatApp — " + client.getUsername());
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.add(title);
        if (server != null) {
            JLabel info = new JLabel("Server: " + server.label());
            info.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            info.setForeground(new Color(90, 90, 90));
            header.add(info);
        }

        // Left: online users
        JPanel left = new JPanel(new BorderLayout(4, 4));
        JLabel usersTitle = new JLabel("Đang online (double-click):");
        usersTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        usersTitle.setBorder(BorderFactory.createEmptyBorder(6, 6, 4, 6));
        left.add(usersTitle, BorderLayout.NORTH);
        usersList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        left.add(new JScrollPane(usersList), BorderLayout.CENTER);
        left.setPreferredSize(new Dimension(220, 0));

        // Right: chat tabs
        chatTabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        JPanel empty = new JPanel(new GridBagLayout());
        empty.add(new JLabel("Chọn người dùng bên trái để bắt đầu chat."));
        chatTabs.addTab("Bắt đầu", empty);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, chatTabs);
        split.setDividerLocation(220);
        split.setOneTouchExpandable(true);

        setLayout(new BorderLayout());
        add(header, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);

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

    public void openChatTab(String peer, ChatPanel panel) {
        SwingUtilities.invokeLater(() -> {
            int existing = indexOfTabByPeer(peer);
            if (existing >= 0) { chatTabs.setSelectedIndex(existing); return; }
            chatTabs.addTab(peer, panel);
            int idx = chatTabs.indexOfComponent(panel);
            chatTabs.setTabComponentAt(idx, buildTabHeader(peer));
            chatTabs.setSelectedIndex(idx);
            panel.requestFocusOnInput();
        });
    }

    public void selectChatTab(String peer) {
        SwingUtilities.invokeLater(() -> {
            int existing = indexOfTabByPeer(peer);
            if (existing >= 0) chatTabs.setSelectedIndex(existing);
        });
    }

    public void closeChatTab(String peer) {
        SwingUtilities.invokeLater(() -> {
            int existing = indexOfTabByPeer(peer);
            if (existing >= 0) chatTabs.remove(existing);
        });
    }

    private int indexOfTabByPeer(String peer) {
        for (int i = 0; i < chatTabs.getTabCount(); i++) {
            Component c = chatTabs.getComponentAt(i);
            if (c instanceof ChatPanel cp && cp.getPeer().equals(peer)) return i;
        }
        return -1;
    }

    private JPanel buildTabHeader(String peer) {
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        header.setOpaque(false);
        JLabel name = new JLabel(peer);
        name.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));
        JButton closeBtn = new JButton("✕");
        closeBtn.setMargin(new Insets(0, 4, 0, 4));
        closeBtn.setFocusable(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
        closeBtn.addActionListener(e -> windows.close(peer));
        header.add(name);
        header.add(closeBtn);
        return header;
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
            case VOICE_INVITE -> windows.handleVoiceInvite(msg);
            case VOICE_ACCEPT -> windows.handleVoiceAccept(msg);
            case VOICE_REJECT -> windows.handleVoiceReject(msg);
            case VOICE -> windows.dispatchVoice(msg);
            case VOICE_END -> windows.handleVoiceEnd(msg);
            case VIDEO_INVITE -> windows.handleVideoInvite(msg);
            case VIDEO_ACCEPT -> windows.handleVideoAccept(msg);
            case VIDEO_REJECT -> windows.handleVideoReject(msg);
            case VIDEO -> windows.dispatchVideo(msg);
            case VIDEO_END -> windows.handleVideoEnd(msg);
            default -> {}
        }
    }
}
