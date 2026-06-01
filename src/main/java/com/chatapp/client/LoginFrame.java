package com.chatapp.client;

import com.chatapp.client.ui.Theme;
import com.chatapp.client.ui.UiKit;
import com.chatapp.model.Message;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.net.URL;
import java.util.List;

public class LoginFrame extends JFrame {
    private final JComboBox<ServerConfig.Entry> serverCombo = new JComboBox<>();
    private final JButton manageButton = new JButton("Quản lý…");
    private final JTextField usernameField = new JTextField(18);
    private final JPasswordField passwordField = new JPasswordField(18);
    private final JToggleButton showPassword = new JToggleButton("👁");
    private final JButton loginButton = new JButton("Đăng nhập");
    private final JButton registerButton = new JButton("Đăng ký");
    private final JLabel errorLabel = new JLabel(" ");

    public LoginFrame() {
        super("ChatApp — Đăng nhập");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        URL iconUrl = getClass().getResource("/icons/app.png");
        if (iconUrl != null) setIconImage(new ImageIcon(iconUrl).getImage());

        setContentPane(buildBackground());
        ((JComponent) getContentPane()).setLayout(new GridBagLayout());
        getContentPane().add(buildCard());

        reloadServers();
        registerButton.addActionListener(e -> {
            ServerConfig.Entry sel = selectedServer();
            if (sel == null) { showError("Chọn server trước khi đăng ký."); return; }
            new RegisterFrame(sel.host, sel.port).setVisible(true);
        });
        loginButton.addActionListener(this::onLogin);
        manageButton.addActionListener(e -> {
            ServerManagerDialog dlg = new ServerManagerDialog(this);
            dlg.setVisible(true);
            if (dlg.isChanged()) reloadServers();
        });
        getRootPane().setDefaultButton(loginButton);

        setMinimumSize(new Dimension(440, 560));
        pack();
        setLocationRelativeTo(null);
    }

    /** Soft vertical-gradient content pane. */
    private JPanel buildBackground() {
        return new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                Color top = Theme.isDark() ? new Color(0x12141C) : new Color(0xEEF2FF);
                Color bottom = Theme.bgApp();
                g2.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), bottom));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
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
        c.gridx = 0; c.gridy = 0; c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1; c.insets = new Insets(0, 0, Theme.SP_2, 0);

        JLabel logo = new JLabel("💬  ChatApp");
        logo.setFont(new Font(Theme.FONT_EMOJI, Font.BOLD, 24));
        logo.setForeground(Theme.accent());
        logo.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(logo, c);

        JLabel tagline = new JLabel("Nhắn tin nhanh, gọn, hiện đại");
        tagline.setFont(Theme.meta());
        tagline.setForeground(Theme.textSecondary());
        tagline.setHorizontalAlignment(SwingConstants.CENTER);
        c.gridy++; c.insets = new Insets(0, 0, Theme.SP_6, 0); card.add(tagline, c);

        c.insets = new Insets(0, 0, Theme.SP_1, 0);
        c.gridy++; card.add(fieldLabel("Server"), c);
        JPanel serverRow = new JPanel(new BorderLayout(Theme.SP_2, 0));
        serverRow.setOpaque(false);
        serverRow.add(serverCombo, BorderLayout.CENTER);
        serverRow.add(manageButton, BorderLayout.EAST);
        c.gridy++; c.insets = new Insets(0, 0, Theme.SP_3, 0); card.add(serverRow, c);

        c.insets = new Insets(0, 0, Theme.SP_1, 0);
        c.gridy++; card.add(fieldLabel("Tài khoản"), c);
        usernameField.putClientProperty("JTextField.placeholderText", "Tên đăng nhập");
        styleField(usernameField);
        c.gridy++; c.insets = new Insets(0, 0, Theme.SP_3, 0); card.add(usernameField, c);

        c.insets = new Insets(0, 0, Theme.SP_1, 0);
        c.gridy++; card.add(fieldLabel("Mật khẩu"), c);
        styleField(passwordField);
        passwordField.putClientProperty("JTextField.placeholderText", "••••••••");
        showPassword.setFocusable(false);
        showPassword.setFont(new Font(Theme.FONT_EMOJI, Font.PLAIN, 13));
        showPassword.setToolTipText("Hiện/ẩn mật khẩu");
        char echo = passwordField.getEchoChar();
        showPassword.addActionListener(e ->
                passwordField.setEchoChar(showPassword.isSelected() ? (char) 0 : echo));
        passwordField.putClientProperty("JTextField.trailingComponent", showPassword);
        c.gridy++; c.insets = new Insets(0, 0, Theme.SP_2, 0); card.add(passwordField, c);

        errorLabel.setFont(Theme.timestamp());
        errorLabel.setForeground(Theme.danger());
        c.gridy++; c.insets = new Insets(0, 0, Theme.SP_2, 0); card.add(errorLabel, c);

        loginButton.putClientProperty("JButton.buttonType", "default");
        loginButton.setFont(Theme.font(Font.BOLD, 14));
        loginButton.setPreferredSize(new Dimension(0, 40));
        c.gridy++; c.insets = new Insets(0, 0, Theme.SP_2, 0); card.add(loginButton, c);

        JPanel registerRow = new JPanel(new FlowLayout(FlowLayout.CENTER, Theme.SP_1, 0));
        registerRow.setOpaque(false);
        JLabel noAccount = new JLabel("Chưa có tài khoản?");
        noAccount.setFont(Theme.meta());
        noAccount.setForeground(Theme.textSecondary());
        registerButton.putClientProperty("JButton.buttonType", "borderless");
        registerButton.setForeground(Theme.accent());
        registerRow.add(noAccount);
        registerRow.add(registerButton);
        c.gridy++; c.insets = new Insets(0, 0, 0, 0); card.add(registerRow, c);
        return card;
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.font(Font.BOLD, 12));
        l.setForeground(Theme.textSecondary());
        return l;
    }

    private void styleField(JTextField f) {
        f.setPreferredSize(new Dimension(0, 40));
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
            showError("Vui lòng nhập đủ tài khoản và mật khẩu.");
            return;
        }
        ServerConfig.Entry server = selectedServer();
        if (server == null) {
            showError("Vui lòng chọn server.");
            return;
        }
        clearError();
        setBusy(true);
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
                setBusy(false);
                showError("Sai tài khoản hoặc mật khẩu.");
            }
        } catch (Exception ex) {
            try { client.close(); } catch (Exception ignored) {}
            setBusy(false);
            // keep dialogs only for connection failures
            error("Không thể kết nối tới " + server.label() + ": " + ex.getMessage());
        }
    }

    private void setBusy(boolean busy) {
        loginButton.setEnabled(!busy);
        loginButton.setText(busy ? "Đang kết nối…" : "Đăng nhập");
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
    }

    private void clearError() {
        errorLabel.setText(" ");
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        Theme.setup();
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
