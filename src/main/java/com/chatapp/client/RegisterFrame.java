package com.chatapp.client;

import com.chatapp.client.ui.Theme;
import com.chatapp.client.ui.UiKit;
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
    private final JLabel errorLabel = new JLabel(" ");
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

        JPanel bg = new JPanel(new GridBagLayout());
        bg.setBackground(Theme.bgApp());
        setContentPane(bg);
        bg.add(buildCard());

        registerButton.addActionListener(this::onSubmit);
        backButton.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(registerButton);

        setMinimumSize(new Dimension(420, 480));
        pack();
        setLocationRelativeTo(null);
    }

    private JComponent buildCard() {
        UiKit.RoundedPanel card = new UiKit.RoundedPanel(Theme.RADIUS_CARD, Theme.surfaceCard()) {
            @Override public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                return new Dimension(360, d.height);
            }
        };
        card.setLayout(new GridBagLayout());
        card.setBorder(BorderFactory.createEmptyBorder(Theme.SP_8, Theme.SP_8, Theme.SP_8, Theme.SP_8));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridy = 0; c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1;

        JLabel logo = new JLabel("Tạo tài khoản");
        logo.setFont(Theme.font(Font.BOLD, 20));
        logo.setForeground(Theme.textPrimary());
        logo.setHorizontalAlignment(SwingConstants.CENTER);
        c.insets = new Insets(0, 0, Theme.SP_6, 0); card.add(logo, c);

        c.insets = new Insets(0, 0, Theme.SP_1, 0);
        c.gridy++; card.add(fieldLabel("Tài khoản"), c);
        usernameField.putClientProperty("JTextField.placeholderText", "Ít nhất 3 ký tự");
        usernameField.setPreferredSize(new Dimension(0, 40));
        c.gridy++; c.insets = new Insets(0, 0, Theme.SP_3, 0); card.add(usernameField, c);

        c.insets = new Insets(0, 0, Theme.SP_1, 0);
        c.gridy++; card.add(fieldLabel("Mật khẩu"), c);
        passwordField.putClientProperty("JTextField.placeholderText", "Ít nhất 6 ký tự");
        passwordField.setPreferredSize(new Dimension(0, 40));
        c.gridy++; c.insets = new Insets(0, 0, Theme.SP_3, 0); card.add(passwordField, c);

        c.insets = new Insets(0, 0, Theme.SP_1, 0);
        c.gridy++; card.add(fieldLabel("Xác nhận mật khẩu"), c);
        confirmField.putClientProperty("JTextField.placeholderText", "Nhập lại mật khẩu");
        confirmField.setPreferredSize(new Dimension(0, 40));
        c.gridy++; c.insets = new Insets(0, 0, Theme.SP_2, 0); card.add(confirmField, c);

        errorLabel.setFont(Theme.timestamp());
        errorLabel.setForeground(Theme.danger());
        c.gridy++; c.insets = new Insets(0, 0, Theme.SP_2, 0); card.add(errorLabel, c);

        registerButton.putClientProperty("JButton.buttonType", "default");
        registerButton.setFont(Theme.font(Font.BOLD, 14));
        registerButton.setPreferredSize(new Dimension(0, 40));
        c.gridy++; c.insets = new Insets(0, 0, Theme.SP_2, 0); card.add(registerButton, c);

        backButton.putClientProperty("JButton.buttonType", "borderless");
        backButton.setForeground(Theme.textSecondary());
        c.gridy++; c.insets = new Insets(0, 0, 0, 0); card.add(backButton, c);
        return card;
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.font(Font.BOLD, 12));
        l.setForeground(Theme.textSecondary());
        return l;
    }

    private void onSubmit(ActionEvent e) {
        String u = usernameField.getText().trim();
        String p = new String(passwordField.getPassword());
        String c = new String(confirmField.getPassword());
        if (u.length() < 3) { showError("Tài khoản phải có ít nhất 3 ký tự."); return; }
        if (p.length() < 6) { showError("Mật khẩu phải có ít nhất 6 ký tự."); return; }
        if (!p.equals(c)) { showError("Mật khẩu xác nhận không khớp."); return; }
        errorLabel.setText(" ");
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
                showError("Tài khoản đã tồn tại.");
            }
        } catch (Exception ex) {
            error("Không thể kết nối tới server: " + ex.getMessage());
        }
    }

    private void showError(String msg) { errorLabel.setText(msg); }

    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        Theme.setup();
        SwingUtilities.invokeLater(() -> new RegisterFrame().setVisible(true));
    }
}
