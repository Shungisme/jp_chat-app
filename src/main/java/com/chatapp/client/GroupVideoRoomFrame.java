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
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class GroupVideoRoomFrame extends JFrame {
    private static final int FRAME_INTERVAL_MS = 100;

    private final Client client;
    private final String groupId;
    private final String groupName;
    private final Consumer<String> closeCallback;
    private final VideoCapture capture = new VideoCapture();
    private final JLabel localView = new JLabel("(local)", SwingConstants.CENTER);
    private final JPanel tilesPanel = new JPanel(new GridLayout(0, 2, 8, 8));
    private final Map<String, JLabel> remoteTiles = new HashMap<>();
    private final JButton camButton = new JButton("Bật webcam");
    private final JButton endButton = new JButton("Rời phòng");
    private final JLabel statusLabel = new JLabel(" ", SwingConstants.CENTER);
    private Timer captureTimer;
    private boolean camOn = false;

    public GroupVideoRoomFrame(Client client, String groupId, String groupName,
                               Consumer<String> closeCallback) {
        super("Video nhóm — " + groupName);
        this.client = client;
        this.groupId = groupId;
        this.groupName = groupName;
        this.closeCallback = closeCallback;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(820, 480);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        Dimension viewSize = VideoCapture.FRAME_SIZE;
        localView.setPreferredSize(viewSize);
        localView.setOpaque(true);
        localView.setBackground(new Color(40, 40, 40));
        localView.setForeground(Color.LIGHT_GRAY);

        JPanel left = new JPanel(new BorderLayout(4, 4));
        JLabel localCaption = new JLabel("Bạn", SwingConstants.CENTER);
        localCaption.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        left.add(localCaption, BorderLayout.NORTH);
        left.add(localView, BorderLayout.CENTER);
        left.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 4));

        tilesPanel.setBorder(BorderFactory.createTitledBorder("Thành viên khác"));
        JScrollPane scroll = new JScrollPane(tilesPanel);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, scroll);
        split.setDividerLocation(VideoCapture.FRAME_SIZE.width + 24);
        add(split, BorderLayout.CENTER);

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
                    client.send(new Message(Message.Type.GROUP_VIDEO_LEAVE,
                            client.getUsername(), groupId, ""));
                } catch (Exception ignored) {}
                if (closeCallback != null) closeCallback.accept(groupId);
            }
        });
    }

    /**
     * Sends GROUP_VIDEO_JOIN to register presence. Called after the frame is
     * tracked in {@link ChatWindowManager}'s map, mirroring the voice room.
     */
    public void start() {
        try {
            client.send(new Message(Message.Type.GROUP_VIDEO_JOIN,
                    client.getUsername(), groupId, ""));
        } catch (Exception ignored) {
            // Best-effort presence — if the join fails the room still works
            // for whoever opened it; they just won't trigger a START notice.
        }

        if (!VideoCapture.isAvailable()) {
            statusLabel.setText("Thư viện webcam chưa có — chỉ xem được người khác.");
            camButton.setEnabled(false);
            camButton.setToolTipText("Build với 'mvn package' để dùng webcam.");
        } else {
            statusLabel.setText("Trong phòng video nhóm " + groupName);
        }
    }

    public String getGroupId() { return groupId; }

    private void toggleCam() {
        if (camOn) {
            stopCapture();
            camButton.setText("Bật webcam");
            camOn = false;
        } else {
            if (!VideoCapture.isAvailable()) return;
            if (!capture.open()) {
                JOptionPane.showMessageDialog(this,
                        "Không thể mở webcam.", "Lỗi", JOptionPane.ERROR_MESSAGE);
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
            client.send(new Message(Message.Type.GROUP_VIDEO,
                    client.getUsername(), groupId, b64));
        } catch (Exception ex) {
            System.err.println("[Video] group send failed: " + ex.getMessage());
        }
    }

    public void receive(Message m) {
        String sender = m.getSender();
        if (sender.equals(client.getUsername())) return;
        try {
            byte[] jpeg = Base64.getDecoder().decode(m.getContent());
            BufferedImage img = VideoCapture.decodeJpeg(jpeg);
            if (img == null) return;
            SwingUtilities.invokeLater(() -> {
                JLabel tile = remoteTiles.get(sender);
                if (tile == null) {
                    tile = new JLabel("", SwingConstants.CENTER);
                    tile.setOpaque(true);
                    tile.setBackground(new Color(40, 40, 40));
                    tile.setPreferredSize(VideoCapture.FRAME_SIZE);
                    JPanel wrap = new JPanel(new BorderLayout(2, 2));
                    JLabel cap = new JLabel(sender, SwingConstants.CENTER);
                    cap.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));
                    wrap.add(cap, BorderLayout.NORTH);
                    wrap.add(tile, BorderLayout.CENTER);
                    tilesPanel.add(wrap);
                    tilesPanel.revalidate();
                    remoteTiles.put(sender, tile);
                }
                tile.setIcon(new ImageIcon(img));
            });
        } catch (Exception ex) {
            System.err.println("[Video] group receive failed: " + ex.getMessage());
        }
    }
}
