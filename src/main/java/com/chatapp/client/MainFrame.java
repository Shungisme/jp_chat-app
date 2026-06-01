package com.chatapp.client;

import com.chatapp.client.ui.Avatar;
import com.chatapp.client.ui.Icons;
import com.chatapp.client.ui.RowCell;
import com.chatapp.client.ui.Theme;
import com.chatapp.client.ui.UiKit;
import com.chatapp.client.ui.VectorIconButton;
import com.chatapp.model.Message;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainFrame extends JFrame {
    protected final DefaultListModel<String> usersModel = new DefaultListModel<>();
    private final JList<String> usersList = new JList<>(usersModel);
    private final DefaultListModel<GroupEntry> groupsModel = new DefaultListModel<>();
    private final JList<GroupEntry> groupsList = new JList<>(groupsModel);
    private final JButton createGroupButton = new JButton("Tạo nhóm");
    private final JTabbedPane chatTabs = new JTabbedPane();
    private final Map<String, Integer> badges = new HashMap<>();
    private final List<String> allUsers = new ArrayList<>();
    private final JTextField searchField = new JTextField();
    private int hoverUsers = -1;
    private int hoverGroups = -1;
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
        setSize(1000, 640);
        setMinimumSize(new Dimension(720, 480));
        setLocationRelativeTo(null);

        URL iconUrl = getClass().getResource("/icons/app.png");
        if (iconUrl != null) setIconImage(new ImageIcon(iconUrl).getImage());

        setLayout(new BorderLayout());
        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);

        configureLists();
        createGroupButton.addActionListener(e -> onCreateGroup());
    }

    // ---------------- header (slim app bar) ----------------

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout(Theme.SP_3, 0));
        header.setBackground(Theme.bgApp());
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.border()),
                UiKit.pad(Theme.SP_2, Theme.SP_3, Theme.SP_2, Theme.SP_3)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SP_2, 0));
        left.setOpaque(false);
        left.add(Avatar.component(client.getUsername(), 36, true));
        JPanel who = new JPanel();
        who.setOpaque(false);
        who.setLayout(new BoxLayout(who, BoxLayout.Y_AXIS));
        JLabel name = new JLabel(client.getUsername());
        name.setFont(Theme.name());
        name.setForeground(Theme.textPrimary());
        who.add(name);
        if (server != null) {
            JLabel info = new JLabel(server.label());
            info.setFont(Theme.timestamp());
            info.setForeground(Theme.textSecondary());
            who.add(info);
        }
        left.add(who);
        header.add(left, BorderLayout.WEST);

        searchField.putClientProperty("JTextField.placeholderText", "Tìm cuộc trò chuyện…");
        searchField.putClientProperty("JTextField.leadingIcon",
                Icons.icon(Icons.SEARCH, 16, Theme.textSecondary()));
        searchField.setColumns(24);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { applyFilter(); }
            public void removeUpdate(DocumentEvent e) { applyFilter(); }
            public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });
        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        center.add(searchField);
        header.add(center, BorderLayout.CENTER);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.SP_1, 0));
        right.setOpaque(false);
        VectorIconButton themeToggle = new VectorIconButton(
                (g, w, h, col) -> (Theme.isDark() ? Icons.SUN : Icons.MOON).paint(g, w, h, col),
                "Đổi giao diện sáng/tối", 18);
        themeToggle.addActionListener(e -> { Theme.toggleDark(); themeToggle.repaint(); });
        JButton logout = new JButton("Đăng xuất");
        logout.setForeground(Theme.danger());
        logout.addActionListener(e -> doLogout(false, null));
        right.add(themeToggle);
        right.add(logout);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    // ---------------- body: rail + thread area ----------------

    private JComponent buildBody() {
        // online users section
        JPanel usersPanel = new JPanel(new BorderLayout());
        usersPanel.setOpaque(false);
        usersPanel.add(UiKit.sectionLabel("Trực tuyến"), BorderLayout.NORTH);
        usersList.setOpaque(false);
        usersList.setFixedCellHeight(64);
        JScrollPane usersScroll = new JScrollPane(usersList);
        usersScroll.setBorder(null);
        usersScroll.getViewport().setOpaque(false);
        usersScroll.setOpaque(false);
        usersPanel.add(usersScroll, BorderLayout.CENTER);

        // groups section
        JPanel groupsPanel = new JPanel(new BorderLayout());
        groupsPanel.setOpaque(false);
        groupsPanel.add(UiKit.sectionLabel("Nhóm"), BorderLayout.NORTH);
        groupsList.setOpaque(false);
        groupsList.setFixedCellHeight(64);
        JScrollPane groupsScroll = new JScrollPane(groupsList);
        groupsScroll.setBorder(null);
        groupsScroll.getViewport().setOpaque(false);
        groupsScroll.setOpaque(false);
        groupsPanel.add(groupsScroll, BorderLayout.CENTER);
        createGroupButton.putClientProperty("JButton.buttonType", "default");
        JPanel groupBtnWrap = new JPanel(new BorderLayout());
        groupBtnWrap.setOpaque(false);
        groupBtnWrap.setBorder(UiKit.pad(Theme.SP_2, Theme.SP_3, Theme.SP_3, Theme.SP_3));
        groupBtnWrap.add(createGroupButton, BorderLayout.CENTER);
        groupsPanel.add(groupBtnWrap, BorderLayout.SOUTH);

        JSplitPane leftSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, usersPanel, groupsPanel);
        leftSplit.setResizeWeight(0.6);
        leftSplit.setBorder(null);
        leftSplit.setDividerSize(1);

        JPanel rail = new JPanel(new BorderLayout());
        rail.setBackground(Theme.bgSidebar());
        rail.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.border()));
        rail.add(leftSplit, BorderLayout.CENTER);
        rail.setPreferredSize(new Dimension(280, 0));
        rail.setMinimumSize(new Dimension(240, 0));

        chatTabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        chatTabs.addTab("Bắt đầu", buildWelcome());

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, rail, chatTabs);
        mainSplit.setDividerLocation(280);
        mainSplit.setBorder(null);
        mainSplit.setDividerSize(1);
        return mainSplit;
    }

    private JComponent buildWelcome() {
        JPanel empty = new JPanel(new GridBagLayout());
        empty.setBackground(Theme.bgApp());
        JPanel inner = new JPanel();
        inner.setOpaque(false);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        JLabel glyph = new JLabel("💬");
        glyph.setFont(new Font(Theme.FONT_EMOJI, Font.PLAIN, 56));
        glyph.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel title = new JLabel("Chọn một cuộc trò chuyện để bắt đầu");
        title.setFont(Theme.font(Font.BOLD, 15));
        title.setForeground(Theme.textPrimary());
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel hint = new JLabel("Nhấp đúp một người ở danh sách bên trái");
        hint.setFont(Theme.meta());
        hint.setForeground(Theme.textSecondary());
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);
        inner.add(glyph);
        inner.add(Box.createVerticalStrut(Theme.SP_3));
        inner.add(title);
        inner.add(Box.createVerticalStrut(Theme.SP_1));
        inner.add(hint);
        empty.add(inner);
        return empty;
    }

    // ---------------- list renderers + interaction ----------------

    private void configureLists() {
        RowCell userCell = new RowCell();
        usersList.setCellRenderer((list, value, index, isSelected, focus) -> {
            String n = String.valueOf(value);
            Integer cnt = badges.get(n);
            userCell.configure(n, "Đang hoạt động", true,
                    cnt == null ? 0 : cnt, isSelected, index == hoverUsers);
            return userCell;
        });
        RowCell groupCell = new RowCell();
        groupsList.setCellRenderer((list, value, index, isSelected, focus) -> {
            groupCell.configure(value.name(), "Nhóm trò chuyện", false,
                    0, isSelected, index == hoverGroups);
            return groupCell;
        });

        installHover(usersList, i -> hoverUsers = i, () -> hoverUsers);
        installHover(groupsList, i -> hoverGroups = i, () -> hoverGroups);

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
        // keyboard: Enter opens the selected conversation (accessibility §7)
        usersList.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "open");
        usersList.getActionMap().put("open", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                String peer = usersList.getSelectedValue();
                if (peer != null) windows.openWith(peer);
            }
        });
        groupsList.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "open");
        groupsList.getActionMap().put("open", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                GroupEntry g = groupsList.getSelectedValue();
                if (g != null) windows.openGroup(g.id(), g.name());
            }
        });
    }

    private void installHover(JList<?> list, java.util.function.IntConsumer setter,
                              java.util.function.IntSupplier getter) {
        list.addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int i = list.locationToIndex(e.getPoint());
                if (i != getter.getAsInt()) { setter.accept(i); list.repaint(); }
            }
        });
        list.addMouseListener(new MouseAdapter() {
            @Override public void mouseExited(MouseEvent e) { setter.accept(-1); list.repaint(); }
        });
    }

    private void applyFilter() {
        String q = searchField.getText().trim().toLowerCase();
        usersModel.clear();
        for (String u : allUsers) {
            if (q.isEmpty() || u.toLowerCase().contains(q)) usersModel.addElement(u);
        }
    }

    private void onCreateGroup() {
        List<String> online = new ArrayList<>(allUsers);
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
            chatTabs.setTabComponentAt(idx, buildTabHeader(peer, null, () -> windows.close(peer)));
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
            chatTabs.addTab(groupName, panel);
            int idx = chatTabs.indexOfComponent(panel);
            chatTabs.setTabComponentAt(idx, buildTabHeader(groupName, Icons.GROUP,
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

    private JPanel buildTabHeader(String title, Icons.Painter leading, Runnable onClose) {
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        header.setOpaque(false);
        if (leading != null) {
            JLabel ic = new JLabel(Icons.icon(leading, 16, Theme.textSecondary()));
            header.add(ic);
        }
        JLabel name = new JLabel(title);
        name.setFont(Theme.font(Font.PLAIN, 13));
        name.setForeground(Theme.textPrimary());
        name.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
        VectorIconButton closeBtn = new VectorIconButton(Icons.CLOSE, "Đóng", 10);
        closeBtn.hover(Theme.danger());
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
                allUsers.clear();
                if (msg.getContent() != null && !msg.getContent().isBlank()) {
                    for (String u : msg.getContent().split(",")) {
                        if (!u.equals(client.getUsername())) allUsers.add(u);
                    }
                }
                applyFilter();
            });
            case CHAT -> windows.dispatch(msg);
            case FILE -> windows.dispatchFile(msg);
            case GROUP_CHAT, GROUP_FILE -> windows.dispatchGroupChat(msg);
            case GROUP_VOICE -> windows.dispatchGroupVoice(msg);
            case GROUP_VIDEO -> windows.dispatchGroupVideo(msg);
            case GROUP_INVITE -> windows.handleGroupInvite(msg);
            case GROUP_LIST -> windows.handleGroupList(msg);
            case GROUP_INFO -> windows.handleGroupInfo(msg);
            case GROUP_REMOVED -> windows.handleGroupRemoved(msg);
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
