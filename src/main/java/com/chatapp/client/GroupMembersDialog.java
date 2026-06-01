package com.chatapp.client;

import com.chatapp.client.ui.Avatar;
import com.chatapp.client.ui.Theme;
import com.chatapp.client.ui.UiKit;
import com.chatapp.model.Message;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Owner-aware roster for a group: shows each member with an avatar + online dot,
 * lets the owner kick non-owners, and lets the owner invite more members via a
 * picker over the currently-online users that aren't already in the group.
 */
public class GroupMembersDialog extends JDialog {
    private final Client client;
    private final String groupId;
    private final String groupName;
    private final List<String> onlineUsers;
    private final JPanel listPanel = new JPanel();
    private final JLabel countLabel = new JLabel();
    private GroupInfo info;

    public GroupMembersDialog(Frame parent, Client client,
                              String groupId, String groupName, GroupInfo info,
                              List<String> onlineUsers) {
        super(parent, "Thành viên — " + groupName, true);
        this.client = client;
        this.groupId = groupId;
        this.groupName = groupName;
        this.info = info;
        this.onlineUsers = onlineUsers == null ? List.of() : onlineUsers;

        setSize(440, 520);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.bgApp());

        add(buildHeader(), BorderLayout.NORTH);

        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);
        listPanel.setBorder(UiKit.pad(Theme.SP_2, Theme.SP_4, Theme.SP_2, Theme.SP_4));
        JScrollPane scroll = new JScrollPane(listPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Theme.bgApp());
        add(scroll, BorderLayout.CENTER);

        add(buildFooter(), BorderLayout.SOUTH);

        populate();
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout(Theme.SP_3, 0));
        header.setBackground(Theme.bgApp());
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.border()),
                UiKit.pad(Theme.SP_3, Theme.SP_4, Theme.SP_3, Theme.SP_4)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SP_2, 0));
        left.setOpaque(false);
        left.add(Avatar.component(groupName, 40, false));
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel name = new JLabel(groupName);
        name.setFont(Theme.name());
        name.setForeground(Theme.textPrimary());
        countLabel.setFont(Theme.timestamp());
        countLabel.setForeground(Theme.textSecondary());
        text.add(name);
        text.add(countLabel);
        left.add(text);
        header.add(left, BorderLayout.WEST);

        if (iAmOwner()) {
            JButton add = new JButton("+ Thêm thành viên");
            add.putClientProperty("JButton.buttonType", "default");
            add.setFont(Theme.font(Font.BOLD, 12));
            add.addActionListener(e -> openAddDialog());
            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            right.setOpaque(false);
            right.add(add);
            header.add(right, BorderLayout.EAST);
        }
        return header;
    }

    private JComponent buildFooter() {
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        south.setOpaque(false);
        south.setBorder(UiKit.pad(Theme.SP_2, Theme.SP_4, Theme.SP_3, Theme.SP_4));
        JButton close = new JButton("Đóng");
        close.putClientProperty("JButton.buttonType", "borderless");
        close.addActionListener(e -> dispose());
        south.add(close);
        return south;
    }

    private boolean iAmOwner() {
        return info != null && info.owner().equals(client.getUsername());
    }

    private void populate() {
        listPanel.removeAll();
        if (info == null) {
            JLabel empty = new JLabel("Không lấy được danh sách thành viên.");
            empty.setForeground(Theme.textSecondary());
            empty.setAlignmentX(LEFT_ALIGNMENT);
            listPanel.add(empty);
            countLabel.setText("");
        } else {
            countLabel.setText(info.members().size() + " thành viên");
            boolean meOwner = iAmOwner();
            for (String m : info.members()) {
                listPanel.add(buildMemberRow(m, m.equals(info.owner()), meOwner));
                listPanel.add(Box.createVerticalStrut(Theme.SP_1));
            }
        }
        listPanel.revalidate();
        listPanel.repaint();
    }

    private JComponent buildMemberRow(String username, boolean isOwner, boolean meOwner) {
        UiKit.RoundedPanel row = new UiKit.RoundedPanel(Theme.RADIUS_CARD, Theme.surfaceCard(),
                new BorderLayout(Theme.SP_2, 0));
        row.setBorder(UiKit.pad(Theme.SP_2, Theme.SP_3, Theme.SP_2, Theme.SP_3));
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SP_2, 0));
        left.setOpaque(false);
        left.add(Avatar.component(username, 36, true));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel nm = new JLabel(username);
        nm.setFont(Theme.name());
        nm.setForeground(Theme.textPrimary());
        boolean online = onlineUsers.contains(username);
        String role = isOwner
                ? (online ? "Trưởng nhóm · Online" : "Trưởng nhóm")
                : (online ? "Online" : "Offline");
        JLabel sub = new JLabel(role);
        sub.setFont(Theme.timestamp());
        sub.setForeground(online ? Theme.online() : Theme.textSecondary());
        text.add(nm);
        text.add(sub);
        left.add(text);
        row.add(left, BorderLayout.WEST);

        if (meOwner && !isOwner) {
            JButton kick = new JButton("Kick");
            kick.putClientProperty("JButton.buttonType", "borderless");
            kick.setForeground(Theme.danger());
            kick.addActionListener(e -> onKick(username));
            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            right.setOpaque(false);
            right.add(kick);
            row.add(right, BorderLayout.EAST);
        }
        return row;
    }

    private void openAddDialog() {
        Set<String> current = new HashSet<>(info.members());
        List<String> candidates = new ArrayList<>();
        for (String u : onlineUsers) {
            if (!current.contains(u) && !u.equals(client.getUsername())) candidates.add(u);
        }
        if (candidates.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Không có user online nào ngoài nhóm để mời.",
                    "Thêm thành viên", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        DefaultListModel<String> model = new DefaultListModel<>();
        for (String c : candidates) model.addElement(c);
        JList<String> list = new JList<>(model);
        list.setFont(Theme.body());
        list.setFixedCellHeight(32);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        JScrollPane sc = new JScrollPane(list);
        sc.setPreferredSize(new Dimension(300, 240));

        int ok = JOptionPane.showConfirmDialog(this, sc,
                "Mời vào nhóm " + groupName,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;

        List<String> selected = list.getSelectedValuesList();
        if (selected.isEmpty()) return;

        for (String u : selected) {
            try {
                client.send(new Message(Message.Type.GROUP_INVITE,
                        client.getUsername(), "server", groupId + ":" + u));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Không mời được " + u + ": " + ex.getMessage(),
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        List<String> next = new ArrayList<>(info.members());
        for (String u : selected) if (!next.contains(u)) next.add(u);
        info = new GroupInfo(info.groupId(), info.owner(), next);
        populate();
    }

    private void onKick(String username) {
        int ok = JOptionPane.showConfirmDialog(this,
                "Kick " + username + " ra khỏi nhóm " + groupName + "?",
                "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;
        try {
            client.send(new Message(Message.Type.GROUP_REMOVE,
                    client.getUsername(), groupId, username));
            List<String> next = new ArrayList<>(info.members());
            next.remove(username);
            info = new GroupInfo(info.groupId(), info.owner(), next);
            populate();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Không gửi được yêu cầu: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
}
