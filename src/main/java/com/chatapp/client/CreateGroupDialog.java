package com.chatapp.client;

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
        setLayout(new BorderLayout(4, 4));

        JPanel top = new JPanel(new GridLayout(2, 2, 4, 4));
        top.add(new JLabel("Tên nhóm:"));
        top.add(nameField);
        top.add(new JLabel("Tìm thành viên:"));
        top.add(searchField);
        add(top, BorderLayout.NORTH);

        candidateList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        rebuildCandidates("");
        add(new JScrollPane(candidateList), BorderLayout.CENTER);

        JButton create = new JButton("Tạo");
        create.addActionListener(e -> submit());
        add(create, BorderLayout.SOUTH);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { rebuildCandidates(searchField.getText()); }
            public void removeUpdate(DocumentEvent e) { rebuildCandidates(searchField.getText()); }
            public void changedUpdate(DocumentEvent e) { rebuildCandidates(searchField.getText()); }
        });

        pack();
        setLocationRelativeTo(parent);
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
