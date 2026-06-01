package com.chatapp.client;

import com.chatapp.client.ui.Theme;
import com.chatapp.client.ui.UiKit;
import com.chatapp.model.Message;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.List;

public class CreateGroupDialog extends JDialog {
    private final JTextField nameField = new JTextField(18);
    private final JTextField searchField = new JTextField(18);
    private final DefaultListModel<String> candidates = new DefaultListModel<>();
    private final JList<String> candidateList = new JList<>(candidates);
    private final List<String> onlineUsers;
    private final Client client;

    public CreateGroupDialog(Frame parent, Client client, List<String> onlineUsers) {
        super(parent, "Tạo nhóm mới", true);
        this.onlineUsers = onlineUsers;
        this.client = client;
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.bgApp());

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBorder(UiKit.pad(Theme.SP_4, Theme.SP_4, Theme.SP_2, Theme.SP_4));
        nameField.putClientProperty("JTextField.placeholderText", "Tên nhóm");
        nameField.setPreferredSize(new Dimension(0, 38));
        nameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        searchField.putClientProperty("JTextField.placeholderText", "Tìm thành viên…");
        searchField.setPreferredSize(new Dimension(0, 38));
        searchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        top.add(label("Tên nhóm"));
        top.add(Box.createVerticalStrut(Theme.SP_1));
        top.add(nameField);
        top.add(Box.createVerticalStrut(Theme.SP_3));
        top.add(label("Thêm thành viên"));
        top.add(Box.createVerticalStrut(Theme.SP_1));
        top.add(searchField);
        add(top, BorderLayout.NORTH);

        candidateList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        candidateList.setFont(Theme.body());
        candidateList.setFixedCellHeight(34);
        rebuildCandidates("");
        JScrollPane sc = new JScrollPane(candidateList);
        sc.setBorder(UiKit.pad(0, Theme.SP_4, 0, Theme.SP_4));
        add(sc, BorderLayout.CENTER);

        JButton create = new JButton("Tạo nhóm");
        create.putClientProperty("JButton.buttonType", "default");
        create.setFont(Theme.font(Font.BOLD, 14));
        create.setPreferredSize(new Dimension(0, 40));
        create.addActionListener(e -> submit());
        JPanel btnWrap = new JPanel(new BorderLayout());
        btnWrap.setOpaque(false);
        btnWrap.setBorder(UiKit.pad(Theme.SP_3, Theme.SP_4, Theme.SP_4, Theme.SP_4));
        btnWrap.add(create, BorderLayout.CENTER);
        add(btnWrap, BorderLayout.SOUTH);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { rebuildCandidates(searchField.getText()); }
            public void removeUpdate(DocumentEvent e) { rebuildCandidates(searchField.getText()); }
            public void changedUpdate(DocumentEvent e) { rebuildCandidates(searchField.getText()); }
        });

        setMinimumSize(new Dimension(360, 460));
        pack();
        setLocationRelativeTo(parent);
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setFont(Theme.font(Font.BOLD, 12));
        l.setForeground(Theme.textSecondary());
        return l;
    }

    private void rebuildCandidates(String query) {
        candidates.clear();
        String q = query == null ? "" : query.toLowerCase();
        for (String u : onlineUsers) {
            if (u.toLowerCase().contains(q)) candidates.addElement(u);
        }
    }

    private void submit() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập tên nhóm.");
            return;
        }
        StringBuilder sb = new StringBuilder(name);
        for (String m : candidateList.getSelectedValuesList()) sb.append(",").append(m);
        try {
            client.send(new Message(Message.Type.GROUP_CREATE, client.getUsername(), "server", sb.toString()));
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage());
        }
    }
}
