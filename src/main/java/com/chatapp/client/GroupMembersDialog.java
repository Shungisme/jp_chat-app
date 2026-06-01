package com.chatapp.client;

import com.chatapp.model.Message;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class GroupMembersDialog extends JDialog {
    private final Client client;
    private final String groupId;
    private final String groupName;
    private final JPanel listPanel = new JPanel();

    public GroupMembersDialog(Frame parent, Client client,
                              String groupId, String groupName, GroupInfo info) {
        super(parent, "Thành viên nhóm — " + groupName, true);
        this.client = client;
        this.groupId = groupId;
        this.groupName = groupName;
        setSize(360, 380);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(8, 8));

        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(new JScrollPane(listPanel), BorderLayout.CENTER);

        JButton closeBtn = new JButton("Đóng");
        closeBtn.addActionListener(e -> dispose());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(closeBtn);
        add(south, BorderLayout.SOUTH);

        populate(info);
    }

    private void populate(GroupInfo info) {
        listPanel.removeAll();
        if (info == null) {
            listPanel.add(new JLabel("Không lấy được danh sách thành viên."));
            listPanel.revalidate();
            listPanel.repaint();
            return;
        }
        boolean iAmOwner = info.owner().equals(client.getUsername());
        List<String> members = info.members();
        for (String m : members) {
            JPanel row = new JPanel(new BorderLayout());
            row.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            String label = m + (m.equals(info.owner()) ? "  (owner)" : "");
            JLabel name = new JLabel(label);
            row.add(name, BorderLayout.CENTER);
            if (iAmOwner && !m.equals(info.owner())) {
                JButton kick = new JButton("Kick");
                kick.addActionListener(e -> onKick(m));
                row.add(kick, BorderLayout.EAST);
            }
            listPanel.add(row);
        }
        listPanel.revalidate();
        listPanel.repaint();
    }

    private void onKick(String username) {
        int ok = JOptionPane.showConfirmDialog(this,
                "Kick " + username + " ra khỏi nhóm " + groupName + "?",
                "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;
        try {
            client.send(new Message(Message.Type.GROUP_REMOVE,
                    client.getUsername(), groupId, username));
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Không gửi được yêu cầu: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
}
