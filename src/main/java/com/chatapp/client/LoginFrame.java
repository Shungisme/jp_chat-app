package com.chatapp.client;

import com.chatapp.model.Message;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.net.URL;
import java.util.List;

public class LoginFrame extends JFrame {
    private final JComboBox<ServerConfig.Entry> serverCombo = new JComboBox<>();
    private final JButton manageButton = new JButton("Quản lý...");
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

        c.gridx = 0; c.gridy = 0; root.add(new JLabel("Server:"), c);
        JPanel serverPanel = new JPanel(new BorderLayout(4, 0));
        serverPanel.add(serverCombo, BorderLayout.CENTER);
        serverPanel.add(manageButton, BorderLayout.EAST);
        c.gridx = 1; root.add(serverPanel, c);

        c.gridx = 0; c.gridy = 1; root.add(new JLabel("Tài khoản:"), c);
        c.gridx = 1; root.add(usernameField, c);
        c.gridx = 0; c.gridy = 2; root.add(new JLabel("Mật khẩu:"), c);
        c.gridx = 1; root.add(passwordField, c);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(registerButton);
        buttons.add(loginButton);
        c.gridx = 0; c.gridy = 3; c.gridwidth = 2;
        c.insets = new Insets(12, 6, 0, 6);
        root.add(buttons, c);

        reloadServers();
        registerButton.addActionListener(e -> {
            ServerConfig.Entry sel = selectedServer();
            if (sel == null) { error("Chọn server trước khi đăng ký."); return; }
            new RegisterFrame(sel.host, sel.port).setVisible(true);
        });
        loginButton.addActionListener(this::onLogin);
        manageButton.addActionListener(e -> {
            ServerManagerDialog dlg = new ServerManagerDialog(this);
            dlg.setVisible(true);
            if (dlg.isChanged()) reloadServers();
        });
        getRootPane().setDefaultButton(loginButton);

        pack();
        setLocationRelativeTo(null);
    }

    private void reloadServers() {
        ServerConfig.Entry previous = (ServerConfig.Entry) serverCombo.getSelectedItem();
        serverCombo.removeAllItems();
        List<ServerConfig.Entry> entries = ServerConfig.load();
        for (ServerConfig.Entry e : entries) serverCombo.addItem(e);
        if (previous != null) {
            for (int i = 0; i < entries.size(); i++) {
                if (entries.get(i).equals(previous)) { serverCombo.setSelectedIndex(i); break; }
            }
        }
    }

    private ServerConfig.Entry selectedServer() {
        return (ServerConfig.Entry) serverCombo.getSelectedItem();
    }

    protected void onLogin(ActionEvent e) {
        String u = usernameField.getText().trim();
        String p = new String(passwordField.getPassword());
        if (u.isEmpty() || p.isEmpty()) {
            error("Vui lòng nhập đủ tài khoản và mật khẩu.");
            return;
        }
        ServerConfig.Entry server = selectedServer();
        if (server == null) {
            error("Vui lòng chọn server.");
            return;
        }

        Client client = new Client(server.host, server.port);
        try {
            client.connect(u);
            client.send(new Message(Message.Type.LOGIN, u, "server", u + ":" + p));
            Message ack = client.receiveOnce();
            if (ack != null && ack.getType() == Message.Type.ACK
                    && "LOGIN_OK".equals(ack.getContent())) {
                MainFrame main = new MainFrame(client, server);
                client.start(main::onMessage);
                main.setVisible(true);
                dispose();
            } else {
                client.close();
                error("Sai tài khoản hoặc mật khẩu.");
            }
        } catch (Exception ex) {
            try { client.close(); } catch (Exception ignored) {}
            error("Không thể kết nối tới " + server.label() + ": " + ex.getMessage());
        }
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
