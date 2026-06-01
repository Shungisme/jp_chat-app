package com.chatapp.client;

import com.chatapp.client.ui.Avatar;
import com.chatapp.client.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class IncomingCallDialog extends JDialog {
    private Timer pulse;

    public IncomingCallDialog(Frame parent, String from, String callType,
                              Runnable onAccept, Runnable onReject) {
        super(parent, "Cuộc gọi " + callType + " đến", false);
        setSize(360, 320);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.bgApp());

        // pulsing-ring avatar
        JPanel avatarArea = new JPanel(new GridBagLayout()) {
            float t = 0f;
            { pulse = new Timer(40, e -> { t = (t + 0.03f) % 1f; repaint(); }); pulse.start(); }
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int cx = getWidth() / 2, cy = getHeight() / 2;
                for (int i = 0; i < 2; i++) {
                    float phase = (t + i * 0.5f) % 1f;
                    int radius = (int) (40 + phase * 50);
                    int alpha = (int) (90 * (1 - phase));
                    g2.setColor(new Color(Theme.accent().getRed(), Theme.accent().getGreen(),
                            Theme.accent().getBlue(), Math.max(0, alpha)));
                    g2.drawOval(cx - radius, cy - radius, radius * 2, radius * 2);
                }
                Avatar.paint(g2, from, cx - 36, cy - 36, 72, false);
                g2.dispose();
            }
        };
        avatarArea.setOpaque(false);
        add(avatarArea, BorderLayout.CENTER);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel name = new JLabel(from);
        name.setFont(Theme.font(Font.BOLD, 18));
        name.setForeground(Theme.textPrimary());
        name.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel desc = new JLabel("đang gọi " + callType + " cho bạn…");
        desc.setFont(Theme.meta());
        desc.setForeground(Theme.textSecondary());
        desc.setAlignmentX(Component.CENTER_ALIGNMENT);
        text.add(name);
        text.add(Box.createVerticalStrut(Theme.SP_1));
        text.add(desc);
        text.setBorder(BorderFactory.createEmptyBorder(0, 0, Theme.SP_2, 0));
        add(text, BorderLayout.NORTH);

        JButton accept = new JButton("Trả lời");
        accept.setBackground(Theme.online());
        accept.setForeground(Color.WHITE);
        accept.putClientProperty("JButton.buttonType", "default");
        JButton reject = new JButton("Từ chối");
        reject.setBackground(Theme.danger());
        reject.setForeground(Color.WHITE);

        JPanel buttons = new JPanel(new GridLayout(1, 2, Theme.SP_3, 0));
        buttons.setOpaque(false);
        buttons.setBorder(BorderFactory.createEmptyBorder(Theme.SP_3, Theme.SP_6, Theme.SP_6, Theme.SP_6));
        buttons.add(reject);
        buttons.add(accept);
        add(buttons, BorderLayout.SOUTH);

        accept.addActionListener(e -> { stopPulse(); dispose(); onAccept.run(); });
        reject.addActionListener(e -> { stopPulse(); dispose(); onReject.run(); });
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { stopPulse(); onReject.run(); }
        });

        toFront();
        setAlwaysOnTop(true);
    }

    private void stopPulse() {
        if (pulse != null) pulse.stop();
    }
}
