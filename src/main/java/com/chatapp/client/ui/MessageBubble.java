package com.chatapp.client.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * One self-painting message bubble. Paints its own rounded, asymmetric-corner
 * background plus a soft 1px shadow, and aligns its content. Knows whether it is
 * mine (right-aligned, accent fill) or the peer's (left-aligned, neutral fill),
 * shows an optional sender name on the first bubble of a run, and a
 * timestamp + delivery-status tick footer.
 */
public class MessageBubble extends JPanel {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    /** Delivery state — visual language ready ahead of the wire protocol (§3.7). */
    public enum Status { NONE, SENDING, SENT, DELIVERED, READ }

    private final boolean mine;

    public MessageBubble(String senderLabel, boolean mine, long timestamp,
                         Component content, Status status) {
        this.mine = mine;
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(8, 13, 7, 13));

        Color fg = mine ? Theme.textOnAccent() : Theme.textPrimary();
        Color metaColor = mine ? new Color(255, 255, 255, 200) : Theme.textSecondary();

        if (senderLabel != null) {
            JLabel name = new JLabel(senderLabel);
            name.setFont(Theme.font(Font.BOLD, 12));
            name.setForeground(mine ? fg : Theme.avatarColor(senderLabel).darker());
            name.setAlignmentX(LEFT_ALIGNMENT);
            add(name);
            add(Box.createVerticalStrut(2));
        }

        content.setMaximumSize(content.getPreferredSize());
        if (content instanceof JComponent jc) jc.setAlignmentX(LEFT_ALIGNMENT);
        add(content);

        JComponent footer = buildFooter(timestamp, status, metaColor, fg);
        footer.setAlignmentX(LEFT_ALIGNMENT);
        add(Box.createVerticalStrut(3));
        add(footer);
    }

    private JComponent buildFooter(long timestamp, Status status, Color metaColor, Color fg) {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        footer.setOpaque(false);
        JLabel time = new JLabel(Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault()).format(TIME));
        time.setFont(Theme.timestamp());
        time.setForeground(metaColor);
        footer.add(time);
        if (mine && status != Status.NONE) {
            JLabel tick = new JLabel(tickGlyph(status));
            tick.setFont(new Font(Theme.FONT_EMOJI, Font.PLAIN, 11));
            tick.setForeground(status == Status.READ ? Theme.textOnAccent() : metaColor);
            tick.setToolTipText(tickTooltip(status));
            tick.getAccessibleContext().setAccessibleName(tickTooltip(status));
            footer.add(tick);
        }
        return footer;
    }

    // Shape-based status so meaning isn't carried by colour alone (§7).
    private static String tickGlyph(Status s) {
        return switch (s) {
            case SENDING -> "🕓";
            case SENT -> "✓";
            case DELIVERED, READ -> "✓✓";
            default -> "";
        };
    }

    private static String tickTooltip(Status s) {
        return switch (s) {
            case SENDING -> "Đang gửi";
            case SENT -> "Đã gửi";
            case DELIVERED -> "Đã nhận";
            case READ -> "Đã xem";
            default -> "";
        };
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        int r = Theme.RADIUS_BUBBLE, tail = Theme.RADIUS_TAIL;

        // soft 1px drop shadow
        g2.setColor(new Color(0, 0, 0, 18));
        g2.fill(roundedAsym(1, 2, w - 2, h - 2, r, tail));

        g2.setColor(mine ? Theme.accent() : Theme.bubbleOther());
        Shape body = roundedAsym(0, 0, w - 2, h - 3, r, tail);
        g2.fill(body);

        if (!mine) { // subtle separation border on neutral bubbles
            g2.setColor(new Color(0, 0, 0, 20));
            g2.draw(body);
        }
        g2.dispose();
        super.paintComponent(g);
    }

    /** Rounded rect whose tail corner (nearest the sender) is reduced to {@code tail}. */
    private Shape roundedAsym(int x, int y, int w, int h, int r, int tail) {
        // bottom-right tail for mine, bottom-left tail for peer
        int blr = mine ? r : tail; // bottom-left radius
        int brr = mine ? tail : r; // bottom-right radius
        Path2D.Float p = new Path2D.Float();
        p.moveTo(x + r, y);
        p.lineTo(x + w - r, y);
        p.quadTo(x + w, y, x + w, y + r);
        p.lineTo(x + w, y + h - brr);
        p.quadTo(x + w, y + h, x + w - brr, y + h);
        p.lineTo(x + blr, y + h);
        p.quadTo(x, y + h, x, y + h - blr);
        p.lineTo(x, y + r);
        p.quadTo(x, y, x + r, y);
        p.closePath();
        return p;
    }

    // ---------------- factories ----------------

    public static MessageBubble text(String senderLabel, boolean mine, long ts,
                                     String body, int maxWidth, Status status) {
        Color fg = mine ? Theme.textOnAccent() : Theme.textPrimary();
        BubbleText bt = new BubbleText(body, 14, fg, maxWidth);
        return new MessageBubble(senderLabel, mine, ts, bt, status);
    }

    /** A distinct attachment card inside the bubble (file icon + name + size). */
    public static MessageBubble file(String senderLabel, boolean mine, long ts,
                                     String fileName, Path savedPath, String hint,
                                     Status status) {
        Color fg = mine ? Theme.textOnAccent() : Theme.textPrimary();
        Color sub = mine ? new Color(255, 255, 255, 200) : Theme.textSecondary();

        JPanel card = new JPanel(new BorderLayout(8, 0));
        card.setOpaque(false);
        card.setAlignmentX(LEFT_ALIGNMENT);

        JLabel icon = new JLabel(Icons.icon(Icons.FILE, 26, fg));
        card.add(icon, BorderLayout.WEST);

        JPanel meta = new JPanel();
        meta.setOpaque(false);
        meta.setLayout(new BoxLayout(meta, BoxLayout.Y_AXIS));
        JLabel nameLbl = new JLabel(fileName);
        nameLbl.setFont(Theme.font(Font.BOLD, 13));
        nameLbl.setForeground(fg);
        String size = readableSize(savedPath);
        JLabel sizeLbl = new JLabel(size == null ? hint : size + " · " + hint);
        sizeLbl.setFont(Theme.timestamp());
        sizeLbl.setForeground(sub);
        meta.add(nameLbl);
        meta.add(sizeLbl);
        card.add(meta, BorderLayout.CENTER);

        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setToolTipText("Bấm để mở");
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                openFile(card, savedPath);
            }
        });

        return new MessageBubble(senderLabel, mine, ts, card, status);
    }

    private static String readableSize(Path p) {
        try {
            if (p == null || !Files.exists(p)) return null;
            long bytes = Files.size(p);
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        } catch (Exception e) { return null; }
    }

    private static void openFile(Component parent, Path p) {
        if (p == null || !Files.exists(p)) {
            JOptionPane.showMessageDialog(parent, "File không còn ở: " + p,
                    "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            Desktop.getDesktop().open(new File(p.toString()));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent, "Không mở được file: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
}
