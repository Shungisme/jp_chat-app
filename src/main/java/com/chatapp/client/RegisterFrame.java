package com.chatapp.client;

import com.chatapp.model.Message;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.Socket;

public class RegisterFrame extends JFrame {
    private final JTextField usernameField = new JTextField(18);
    private final JPasswordField passwordField = new JPasswordField(18);
    private final JPasswordField confirmField = new JPasswordField(18);
    private final JButton registerButton = new JButton("Đăng ký");
    private final JButton backButton = new JButton("Quay lại");
    private final String host;
    private final int port;

    public RegisterFrame() {
        this("127.0.0.1", 9999);
    }

    public RegisterFrame(String host, int port) {
        super("Đăng ký tài khoản — " + host + ":" + port);
        this.host = host;
        this.port = port;
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
        sendRegister(u, p);
    }

    private void sendRegister(String username, String password) {
        try (Socket s = new Socket(host, port);
             ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {
            Message m = new Message(Message.Type.REGISTER, username, "server",
                    username + ":" + password);
            out.writeObject(m);
            out.flush();
            Object reply = in.readObject();
            if (reply instanceof Message ack && "REGISTER_OK".equals(ack.getContent())) {
                JOptionPane.showMessageDialog(this, "Đăng ký thành công.");
                dispose();
            } else {
                error("Tài khoản đã tồn tại.");
            }
        } catch (Exception ex) {
            error("Không thể kết nối tới server: " + ex.getMessage());
        }
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RegisterFrame().setVisible(true));
    }
}
