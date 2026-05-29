package com.chatapp.server;

import javax.swing.*;
import javax.swing.SwingUtilities;
import java.awt.*;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

public class ServerFrame extends JFrame {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Server server = new Server();
    private final JTextField portField = new JTextField(String.valueOf(Server.DEFAULT_PORT), 6);
    private final JButton startButton = new JButton("Khởi động");
    private final JButton stopButton = new JButton("Dừng");
    private final JLabel statusLabel = new JLabel("Đã dừng.");
    private final JTextArea logArea = new JTextArea();
    private final DefaultListModel<String> clientsModel = new DefaultListModel<>();
    private final JList<String> clientsList = new JList<>(clientsModel);

    public ServerFrame() {
        super("ChatApp Server");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(720, 480);
        setLocationRelativeTo(null);

        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        // Top: config + controls
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        top.setBorder(BorderFactory.createTitledBorder("Cấu hình & điều khiển"));
        top.add(new JLabel("Port:"));
        top.add(portField);
        top.add(startButton);
        top.add(stopButton);
        top.add(Box.createHorizontalStrut(16));
        top.add(new JLabel("Trạng thái:"));
        top.add(statusLabel);

        // Center: log + clients
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                wrap(new JScrollPane(logArea), "Nhật ký"),
                wrap(new JScrollPane(clientsList), "Client đang kết nối (0)"));
        split.setResizeWeight(0.7);

        setLayout(new BorderLayout(4, 4));
        add(top, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);

        server.setLogListener(this::appendLog);
        server.setClientsListener(this::updateClients);

        startButton.addActionListener(e -> startServer());
        stopButton.addActionListener(e -> stopServer());
        stopButton.setEnabled(false);
    }

    private JPanel wrap(JComponent c, String title) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(title));
        p.add(c, BorderLayout.CENTER);
        p.putClientProperty("title", title);
        return p;
    }

    private void startServer() {
        try {
            int port = Integer.parseInt(portField.getText().trim());
            server.setPort(port);
            server.start();
            statusLabel.setText("Đang chạy trên cổng " + port);
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            portField.setEnabled(false);
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Port không hợp lệ.",
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Không thể mở cổng: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void stopServer() {
        server.stop();
        statusLabel.setText("Đã dừng.");
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        portField.setEnabled(true);
    }

    private void appendLog(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append("[" + LocalTime.now().format(TIME) + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void updateClients(Set<String> usernames) {
        SwingUtilities.invokeLater(() -> {
            clientsModel.clear();
            for (String u : usernames) clientsModel.addElement(u);
            // Update the titled border count via parent panel
            Component parent = clientsList.getParent();
            while (parent != null && !(parent instanceof JPanel p
                    && "Client đang kết nối".equals(((String) p.getClientProperty("title")).split(" \\(")[0]))) {
                parent = parent.getParent();
            }
            if (parent instanceof JPanel p) {
                p.setBorder(BorderFactory.createTitledBorder(
                        "Client đang kết nối (" + usernames.size() + ")"));
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ServerFrame().setVisible(true));
    }
}
