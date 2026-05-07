package com.chatapp.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class RegisterFrame extends JFrame {
    private final JTextField usernameField = new JTextField(18);
    private final JPasswordField passwordField = new JPasswordField(18);
    private final JPasswordField confirmField = new JPasswordField(18);
    private final JButton registerButton = new JButton("Đăng ký");
    private final JButton backButton = new JButton("Quay lại");

    public RegisterFrame() {
        super("Đăng ký tài khoản");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 0; add(new JLabel("Tài khoản:"), c);
        c.gridx = 1; add(usernameField, c);
        c.gridx = 0; c.gridy = 1; add(new JLabel("Mật khẩu:"), c);
        c.gridx = 1; add(passwordField, c);
        c.gridx = 0; c.gridy = 2; add(new JLabel("Xác nhận:"), c);
        c.gridx = 1; add(confirmField, c);

        JPanel buttons = new JPanel();
        buttons.add(registerButton);
        buttons.add(backButton);
        c.gridx = 0; c.gridy = 3; c.gridwidth = 2; add(buttons, c);

        registerButton.addActionListener(this::onSubmit);
        backButton.addActionListener(e -> dispose());

        pack();
        setLocationRelativeTo(null);
    }

    private void onSubmit(ActionEvent e) {
        String u = usernameField.getText().trim();
        String p = new String(passwordField.getPassword());
        String c = new String(confirmField.getPassword());
        if (u.length() < 3) { error("Tài khoản phải có ít nhất 3 ký tự."); return; }
        if (p.length() < 6) { error("Mật khẩu phải có ít nhất 6 ký tự."); return; }
        if (!p.equals(c)) { error("Mật khẩu xác nhận không khớp."); return; }
        JOptionPane.showMessageDialog(this, "Form hợp lệ — sẽ gửi tới server.");
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RegisterFrame().setVisible(true));
    }
}
