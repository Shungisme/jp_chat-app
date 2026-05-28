package com.chatapp.client;

import com.chatapp.model.Message;
import com.chatapp.util.VideoCapture;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.Base64;
import java.util.function.Consumer;

public class VideoCallFrame extends JFrame {
    private static final int FRAME_INTERVAL_MS = 100;

    private final Client client;
    private final String peer;
    private final Consumer<String> closeCallback;
    private final VideoCapture capture = new VideoCapture();
    private final JLabel localView = new JLabel("(local)", SwingConstants.CENTER);
    private final JLabel remoteView = new JLabel("(remote)", SwingConstants.CENTER);
    private final JButton camButton = new JButton("Bật webcam");
    private final JButton endButton = new JButton("Kết thúc");
    private final JLabel statusLabel = new JLabel(" ", SwingConstants.CENTER);
    private Timer captureTimer;
    private boolean camOn = false;

    public VideoCallFrame(Client client, String peer, boolean callerSide,
                          Consumer<String> closeCallback) {
        super("Gọi video — " + peer);
        this.client = client;
        this.peer = peer;
        this.closeCallback = closeCallback;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(720, 360);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        Dimension viewSize = VideoCapture.FRAME_SIZE;
        localView.setPreferredSize(viewSize);
        remoteView.setPreferredSize(viewSize);
        localView.setOpaque(true);
        remoteView.setOpaque(true);
        localView.setBackground(new Color(40, 40, 40));
        remoteView.setBackground(new Color(40, 40, 40));
        localView.setForeground(Color.LIGHT_GRAY);
        remoteView.setForeground(Color.LIGHT_GRAY);

        JPanel videos = new JPanel(new GridLayout(1, 2, 8, 0));
        videos.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
        videos.add(wrap(localView, "Bạn"));
        videos.add(wrap(remoteView, peer));
        add(videos, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout());
        south.add(statusLabel, BorderLayout.NORTH);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        buttons.add(camButton);
        buttons.add(endButton);
        south.add(buttons, BorderLayout.SOUTH);
        add(south, BorderLayout.SOUTH);

        camButton.addActionListener(e -> toggleCam());
        endButton.addActionListener(e -> dispose());

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) {
                stopCapture();
                try {
                    client.send(new Message(Message.Type.VIDEO_END,
                            client.getUsername(), peer, ""));
                } catch (Exception ignored) {}
                if (closeCallback != null) closeCallback.accept(peer);
            }
        });

        if (!VideoCapture.isAvailable()) {
            statusLabel.setText("Thư viện webcam chưa có — chỉ xem được video gửi tới.");
            camButton.setEnabled(false);
            camButton.setToolTipText("Build với 'mvn package' để dùng webcam.");
        } else {
            statusLabel.setText(callerSide
                    ? "Đang gọi video " + peer + "..."
                    : "Cuộc gọi video đến từ " + peer);
            if (callerSide) toggleCam();
        }
    }

    private JPanel wrap(JLabel view, String caption) {
        JPanel p = new JPanel(new BorderLayout());
        JLabel cap = new JLabel(caption, SwingConstants.CENTER);
        cap.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        p.add(cap, BorderLayout.NORTH);
        p.add(view, BorderLayout.CENTER);
        return p;
    }

    private void toggleCam() {
        if (camOn) {
            stopCapture();
            camButton.setText("Bật webcam");
            camOn = false;
        } else {
            if (!VideoCapture.isAvailable()) {
                JOptionPane.showMessageDialog(this,
                        "Thư viện webcam (Sarxos) chưa có trong classpath.\n" +
                                "Build với 'mvn package' rồi chạy lại.",
                        "Webcam không khả dụng", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!capture.open()) {
                JOptionPane.showMessageDialog(this,
                        "Không thể mở webcam — kiểm tra thiết bị và quyền truy cập.",
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            startCaptureLoop();
            camButton.setText("Tắt webcam");
            camOn = true;
        }
    }

    private void startCaptureLoop() {
        captureTimer = new Timer(FRAME_INTERVAL_MS, e -> {
            BufferedImage img = capture.grab();
            if (img != null) {
                localView.setIcon(new ImageIcon(img));
                localView.setText(null);
                byte[] jpeg = VideoCapture.encodeJpeg(img);
                if (jpeg != null) sendFrame(jpeg);
            }
        });
        captureTimer.start();
    }

    private void stopCapture() {
        if (captureTimer != null) {
            captureTimer.stop();
            captureTimer = null;
        }
        capture.close();
    }

    private void sendFrame(byte[] jpeg) {
        try {
            String b64 = Base64.getEncoder().encodeToString(jpeg);
            client.send(new Message(Message.Type.VIDEO, client.getUsername(), peer, b64));
        } catch (Exception ex) {
            System.err.println("[Video] send failed: " + ex.getMessage());
        }
    }

    public void receive(Message m) {
        try {
            byte[] jpeg = Base64.getDecoder().decode(m.getContent());
            BufferedImage img = VideoCapture.decodeJpeg(jpeg);
            if (img != null) {
                SwingUtilities.invokeLater(() -> {
                    remoteView.setIcon(new ImageIcon(img));
                    remoteView.setText(null);
                    statusLabel.setText("Đang nói chuyện video với " + peer);
                });
            }
        } catch (Exception ex) {
            System.err.println("[Video] receive failed: " + ex.getMessage());
        }
    }
}
