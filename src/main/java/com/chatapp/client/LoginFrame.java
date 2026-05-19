package com.chatapp.client;

import com.chatapp.model.Message;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.Socket;
import java.net.URL;

public class LoginFrame extends JFrame {
    private final JTextField usernameField = new JTextField(18);
    private final JPasswordField passwordField = new JPasswordField(18);
    private final JButton loginButton = new JButton("Đăng nhập");
    private final JButton registerButton = new JButton("Đăng ký");

    public LoginFrame() {
        super("ChatApp — Đăng nhập");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        URL iconUrl = getClass().getResource("/icons/app.png");
        if (iconUrl != null) setIconImage(new ImageIcon(iconUrl).getImage());

        JPanel root = new JPanel(new GridBagLayout());
        root.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));
        setContentPane(root);

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0; c.gridy = 0; root.add(new JLabel("Tài khoản:"), c);
        c.gridx = 1; root.add(usernameField, c);
        c.gridx = 0; c.gridy = 1; root.add(new JLabel("Mật khẩu:"), c);
        c.gridx = 1; root.add(passwordField, c);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(registerButton);
        buttons.add(loginButton);
        c.gridx = 0; c.gridy = 2; c.gridwidth = 2;
        c.insets = new Insets(12, 6, 0, 6);
        root.add(buttons, c);

        registerButton.addActionListener(e -> new RegisterFrame().setVisible(true));
        loginButton.addActionListener(this::onLogin);
        getRootPane().setDefaultButton(loginButton);

        pack();
        setLocationRelativeTo(null);
    }

    protected void onLogin(ActionEvent e) {
        String u = usernameField.getText().trim();
        String p = new String(passwordField.getPassword());
        if (u.isEmpty() || p.isEmpty()) {
            error("Vui lòng nhập đủ tài khoản và mật khẩu.");
            return;
        }
        try (Socket s = new Socket("127.0.0.1", 9999);
             ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {
            out.writeObject(new Message(Message.Type.LOGIN, u, "server", u + ":" + p));
            out.flush();
            Object reply = in.readObject();
            if (reply instanceof Message ack && "LOGIN_OK".equals(ack.getContent())) {
                JOptionPane.showMessageDialog(this, "Đăng nhập thành công.");
                dispose();
            } else {
                error("Sai tài khoản hoặc mật khẩu.");
            }
        } catch (Exception ex) {
            error("Không thể kết nối tới server: " + ex.getMessage());
        }
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
