package com.chatapp.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class ServerManagerDialog extends JDialog {
    private final DefaultListModel<ServerConfig.Entry> model = new DefaultListModel<>();
    private final JList<ServerConfig.Entry> list = new JList<>(model);
    private boolean changed = false;

    public ServerManagerDialog(Frame parent) {
        super(parent, "Quản lý server", true);
        setLayout(new BorderLayout(8, 8));
        setSize(420, 320);
        setLocationRelativeTo(parent);

        reload();
        add(new JScrollPane(list), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        JButton addBtn = new JButton("Thêm");
        JButton editBtn = new JButton("Sửa");
        JButton deleteBtn = new JButton("Xoá");
        JButton closeBtn = new JButton("Đóng");
        buttons.add(addBtn);
        buttons.add(editBtn);
        buttons.add(deleteBtn);
        buttons.add(closeBtn);
        add(buttons, BorderLayout.SOUTH);

        addBtn.addActionListener(this::onAdd);
        editBtn.addActionListener(this::onEdit);
        deleteBtn.addActionListener(this::onDelete);
        closeBtn.addActionListener(e -> dispose());
    }

    public boolean isChanged() { return changed; }

    private void reload() {
        model.clear();
        for (ServerConfig.Entry e : ServerConfig.load()) model.addElement(e);
    }

    private ServerConfig.Entry promptForEntry(ServerConfig.Entry initial) {
        JTextField nameField = new JTextField(initial == null ? "" : initial.name, 18);
        JTextField hostField = new JTextField(initial == null ? "127.0.0.1" : initial.host, 18);
        JTextField portField = new JTextField(initial == null ? "9999" : String.valueOf(initial.port), 6);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0; c.gridy = 0; form.add(new JLabel("Tên:"), c);
        c.gridx = 1; form.add(nameField, c);
        c.gridx = 0; c.gridy = 1; form.add(new JLabel("Host:"), c);
        c.gridx = 1; form.add(hostField, c);
        c.gridx = 0; c.gridy = 2; form.add(new JLabel("Port:"), c);
        c.gridx = 1; form.add(portField, c);

        int result = JOptionPane.showConfirmDialog(this, form,
                initial == null ? "Thêm server" : "Sửa server",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return null;

        String name = nameField.getText().trim();
        String host = hostField.getText().trim();
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Port không hợp lệ.",
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        if (name.isEmpty() || host.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Tên và host không được trống.",
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        return new ServerConfig.Entry(name, host, port);
    }

    private void onAdd(ActionEvent e) {
        ServerConfig.Entry entry = promptForEntry(null);
        if (entry == null) return;
        ServerConfig.add(entry);
        changed = true;
        reload();
    }

    private void onEdit(ActionEvent e) {
        int i = list.getSelectedIndex();
        if (i < 0) return;
        ServerConfig.Entry entry = promptForEntry(model.get(i));
        if (entry == null) return;
        ServerConfig.update(i, entry);
        changed = true;
        reload();
    }

    private void onDelete(ActionEvent e) {
        int i = list.getSelectedIndex();
        if (i < 0) return;
        int confirm = JOptionPane.showConfirmDialog(this,
                "Xoá " + model.get(i).label() + "?", "Xác nhận",
                JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        ServerConfig.delete(i);
        changed = true;
        reload();
    }

}
