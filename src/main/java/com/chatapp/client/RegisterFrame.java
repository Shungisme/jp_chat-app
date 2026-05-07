package com.chatapp.client;

import javax.swing.*;
import java.awt.*;

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

        pack();
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RegisterFrame().setVisible(true));
    }
}
