package com.chatapp.client;

import com.chatapp.model.Message;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainFrame extends JFrame {
    protected final DefaultListModel<String> usersModel = new DefaultListModel<>();
    private final JList<String> usersList = new JList<>(usersModel);
    private final DefaultListModel<GroupEntry> groupsModel = new DefaultListModel<>();
    private final JList<GroupEntry> groupsList = new JList<>(groupsModel);
    private final JButton createGroupButton = new JButton("Tạo nhóm");
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
        this.windows.setGroupsListener(this::updateGroups);
        this.windows.setMainFrame(this);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(960, 600);
        setLocationRelativeTo(null);

        URL iconUrl = getClass().getResource("/icons/app.png");
        if (iconUrl != null) setIconImage(new ImageIcon(iconUrl).getImage());

        // Header
        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));
        JPanel headerText = new JPanel(new GridLayout(0, 1));
        JLabel title = new JLabel("ChatApp — " + client.getUsername());
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        headerText.add(title);
        if (server != null) {
            JLabel info = new JLabel("Server: " + server.label());
            info.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            info.setForeground(new Color(90, 90, 90));
            headerText.add(info);
        }
        header.add(headerText, BorderLayout.CENTER);
        JButton logoutButton = new JButton("Đăng xuất");
        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        headerRight.add(logoutButton);
        header.add(headerRight, BorderLayout.EAST);
        logoutButton.addActionListener(e -> doLogout(false, null));

        // Left top — online users
        JPanel usersPanel = new JPanel(new BorderLayout(4, 4));
        JLabel usersTitle = new JLabel("Đang online (double-click):");
        usersTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        usersTitle.setBorder(BorderFactory.createEmptyBorder(6, 6, 4, 6));
        usersPanel.add(usersTitle, BorderLayout.NORTH);
        usersList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        usersPanel.add(new JScrollPane(usersList), BorderLayout.CENTER);

        // Left bottom — joined groups + create-group button
        JPanel groupsPanel = new JPanel(new BorderLayout(4, 4));
        JLabel groupsTitle = new JLabel("Nhóm của bạn (double-click để mở):");
        groupsTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        groupsTitle.setBorder(BorderFactory.createEmptyBorder(6, 6, 4, 6));
        groupsPanel.add(groupsTitle, BorderLayout.NORTH);
        groupsList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        groupsPanel.add(new JScrollPane(groupsList), BorderLayout.CENTER);
        groupsPanel.add(createGroupButton, BorderLayout.SOUTH);

        JSplitPane leftSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, usersPanel, groupsPanel);
        leftSplit.setResizeWeight(0.6);
        leftSplit.setOneTouchExpandable(true);
        leftSplit.setPreferredSize(new Dimension(240, 0));

        // Right — chat tabs
        chatTabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        JPanel empty = new JPanel(new GridBagLayout());
        empty.add(new JLabel("Chọn người dùng bên trái để bắt đầu chat."));
        chatTabs.addTab("Bắt đầu", empty);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSplit, chatTabs);
        mainSplit.setDividerLocation(240);
        mainSplit.setOneTouchExpandable(true);

        setLayout(new BorderLayout());
        add(header, BorderLayout.NORTH);
        add(mainSplit, BorderLayout.CENTER);

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

        groupsList.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    GroupEntry g = groupsList.getSelectedValue();
                    if (g != null) windows.openGroup(g.id(), g.name());
                }
            }
        });

        createGroupButton.addActionListener(e -> onCreateGroup());
    }

    private void onCreateGroup() {
        List<String> online = new ArrayList<>();
        for (int i = 0; i < usersModel.size(); i++) online.add(usersModel.get(i));
        new CreateGroupDialog(this, client, online).setVisible(true);
    }

    private void updateGroups(Map<String, String> groups) {
        SwingUtilities.invokeLater(() -> {
            groupsModel.clear();
            List<GroupEntry> sorted = new ArrayList<>();
            for (Map.Entry<String, String> e : groups.entrySet()) {
                sorted.add(new GroupEntry(e.getKey(), e.getValue()));
            }
            sorted.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
            for (GroupEntry g : sorted) groupsModel.addElement(g);
        });
    }

    public void openChatTab(String peer, ChatPanel panel) {
        SwingUtilities.invokeLater(() -> {
            int existing = indexOfTabByPeer(peer);
            if (existing >= 0) { chatTabs.setSelectedIndex(existing); return; }
            chatTabs.addTab(peer, panel);
            int idx = chatTabs.indexOfComponent(panel);
            chatTabs.setTabComponentAt(idx, buildTabHeader(peer, () -> windows.close(peer)));
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

    public void openGroupTab(String groupId, String groupName, GroupPanel panel) {
        SwingUtilities.invokeLater(() -> {
            int existing = indexOfTabByGroup(groupId);
            if (existing >= 0) { chatTabs.setSelectedIndex(existing); return; }
            String title = "👥 " + groupName;
            chatTabs.addTab(title, panel);
            int idx = chatTabs.indexOfComponent(panel);
            chatTabs.setTabComponentAt(idx, buildTabHeader(title,
                    () -> windows.closeGroup(groupId)));
            chatTabs.setSelectedIndex(idx);
            panel.requestFocusOnInput();
        });
    }

    public void selectGroupTab(String groupId) {
        SwingUtilities.invokeLater(() -> {
            int existing = indexOfTabByGroup(groupId);
            if (existing >= 0) chatTabs.setSelectedIndex(existing);
        });
    }

    public void closeGroupTab(String groupId) {
        SwingUtilities.invokeLater(() -> {
            int existing = indexOfTabByGroup(groupId);
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

    private int indexOfTabByGroup(String groupId) {
        for (int i = 0; i < chatTabs.getTabCount(); i++) {
            Component c = chatTabs.getComponentAt(i);
            if (c instanceof GroupPanel gp && gp.getGroupId().equals(groupId)) return i;
        }
        return -1;
    }

    private JPanel buildTabHeader(String title, Runnable onClose) {
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        header.setOpaque(false);
        JLabel name = new JLabel(title);
        name.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 12));
        name.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));
        JButton closeBtn = new JButton("✕");
        closeBtn.setMargin(new Insets(0, 4, 0, 4));
        closeBtn.setFocusable(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
        closeBtn.addActionListener(e -> onClose.run());
        header.add(name);
        header.add(closeBtn);
        return header;
    }

    private void setBadge(String peer, int count) {
        if (count <= 0) badges.remove(peer); else badges.put(peer, count);
        usersList.repaint();
    }

    private void doLogout(boolean kicked, String reason) {
        if (!kicked) {
            try {
                client.send(new Message(Message.Type.LOGOUT, client.getUsername(), "server", ""));
            } catch (Exception ignored) {}
        }
        try { client.close(); } catch (Exception ignored) {}
        if (windows != null) windows.closeAll();
        dispose();
        SwingUtilities.invokeLater(() -> {
            LoginFrame lf = new LoginFrame();
            lf.setVisible(true);
            if (kicked && reason != null) {
                JOptionPane.showMessageDialog(lf, reason,
                        "Bị đăng xuất", JOptionPane.WARNING_MESSAGE);
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
            case FILE -> windows.dispatchFile(msg);
            case GROUP_CHAT, GROUP_FILE -> windows.dispatchGroupChat(msg);
            case GROUP_INVITE -> windows.handleGroupInvite(msg);
            case GROUP_LIST -> windows.handleGroupList(msg);
            case KICKED -> SwingUtilities.invokeLater(() -> doLogout(true, msg.getContent()));
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
