package com.chatapp.client;

import com.chatapp.model.Message;
import com.chatapp.util.VoiceChat;

import javax.sound.sampled.LineUnavailableException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Base64;
import java.util.function.Consumer;

public class VoiceCallFrame extends JFrame {
    private final Client client;
    private final String peer;
    private final VoiceChat voice = new VoiceChat();
    private final JButton micButton = new JButton("Bật micro");
    private final JButton endButton = new JButton("Kết thúc");
    private final JLabel statusLabel = new JLabel("Đang kết nối...", SwingConstants.CENTER);
    private final Consumer<String> closeCallback;
    private boolean micOn = false;
    private boolean firstReceived = false;

    public VoiceCallFrame(Client client, String peer, boolean callerSide,
                          Consumer<String> closeCallback) {
        super("Gọi thoại — " + peer);
        this.client = client;
        this.peer = peer;
        this.closeCallback = closeCallback;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(320, 180);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));
        add(statusLabel, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        buttons.add(micButton);
        buttons.add(endButton);
        buttons.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        add(buttons, BorderLayout.SOUTH);

        micButton.addActionListener(e -> toggleMic());
        endButton.addActionListener(e -> dispose());

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) {
                voice.stop();
                if (closeCallback != null) closeCallback.accept(peer);
            }
        });

        try {
            voice.startPlayback();
            statusLabel.setText(callerSide
                    ? "Đang gọi " + peer + "..."
                    : "Cuộc gọi đến từ " + peer);
            if (callerSide) toggleMic();
        } catch (LineUnavailableException ex) {
            JOptionPane.showMessageDialog(this, "Không thể mở loa: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            dispose();
        }
    }

    private void toggleMic() {
        if (micOn) {
            voice.stopCapture();
            micButton.setText("Bật micro");
            micOn = false;
        } else {
            try {
                voice.startCapture(this::sendChunk);
                micButton.setText("Tắt micro");
                micOn = true;
            } catch (LineUnavailableException ex) {
                JOptionPane.showMessageDialog(this, "Không thể mở micro: " + ex.getMessage(),
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void sendChunk(byte[] chunk) {
        try {
            String b64 = Base64.getEncoder().encodeToString(chunk);
            client.send(new Message(Message.Type.VOICE, client.getUsername(), peer, b64));
        } catch (Exception ex) {
            System.err.println("[Voice] send failed: " + ex.getMessage());
        }
    }

    public void receive(Message m) {
        try {
            byte[] chunk = Base64.getDecoder().decode(m.getContent());
            voice.play(chunk);
            if (!firstReceived) {
                firstReceived = true;
                SwingUtilities.invokeLater(() ->
                        statusLabel.setText("Đang nói chuyện với " + peer));
            }
        } catch (Exception ex) {
            System.err.println("[Voice] receive failed: " + ex.getMessage());
        }
    }
}
