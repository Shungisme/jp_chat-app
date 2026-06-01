package com.chatapp.client.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * The conversation thread: a vertically stacked panel of {@link MessageBubble}
 * components inside a scroll pane. Handles same-sender grouping, day dividers,
 * in-thread avatars, an empty state, a "jump to newest" pill, and an animated
 * typing indicator. This replaces the old single linear {@code JTextPane} log.
 */
public class ConversationView extends JPanel {
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMMM yyyy");
    private static final long GROUP_WINDOW_MS = 5 * 60 * 1000L;

    private final boolean group;          // group chat → always show sender names/avatars
    private final String emptyTitle;
    private final String emptyHint;

    private final JPanel messages = new JPanel();
    private final JScrollPane scroll;
    private final JLayeredPane layers = new JLayeredPane();
    private final JButton newPill = new JButton("↓ tin nhắn mới");
    private final List<BubbleText> bubbleTexts = new ArrayList<>();
    private TypingIndicator typing;
    private JComponent emptyState;

    private String lastSender;
    private long lastTs;
    private LocalDate lastDate;
    private boolean hasMessages;

    public ConversationView(boolean group, String emptyTitle, String emptyHint) {
        this.group = group;
        this.emptyTitle = emptyTitle;
        this.emptyHint = emptyHint;
        setLayout(new BorderLayout());
        setBackground(Theme.bgApp());

        messages.setLayout(new BoxLayout(messages, BoxLayout.Y_AXIS));
        messages.setOpaque(false);
        messages.setBorder(BorderFactory.createEmptyBorder(Theme.SP_3, Theme.SP_4, Theme.SP_3, Theme.SP_4));

        JPanel host = new JPanel(new BorderLayout());
        host.setOpaque(false);
        host.add(messages, BorderLayout.NORTH);

        scroll = new JScrollPane(host);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.bgApp());
        scroll.getVerticalScrollBar().setUnitIncrement(24);

        layers.setLayout(null);
        layers.add(scroll, Integer.valueOf(0));
        layers.add(newPill, Integer.valueOf(1));

        stylePill();
        newPill.setVisible(false);
        newPill.addActionListener(e -> scrollToBottom());

        layers.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { relayout(); }
        });
        add(layers, BorderLayout.CENTER);

        showEmptyState();
    }

    private void stylePill() {
        newPill.setFont(Theme.font(Font.BOLD, 12));
        newPill.setForeground(Theme.textOnAccent());
        newPill.setBackground(Theme.accent());
        newPill.setFocusable(false);
        newPill.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        newPill.putClientProperty("JButton.buttonType", "roundRect");
    }

    private void relayout() {
        int w = layers.getWidth(), h = layers.getHeight();
        scroll.setBounds(0, 0, w, h);
        Dimension ps = newPill.getPreferredSize();
        newPill.setBounds((w - ps.width) / 2, h - ps.height - 16, ps.width, ps.height);
        int cw = contentMaxWidth();
        for (BubbleText bt : bubbleTexts) bt.setMaxWidth(cw);
        messages.revalidate();
    }

    /** Bubbles wrap at ~72% of the thread width (§6). */
    private int contentMaxWidth() {
        int w = scroll.getViewport().getWidth();
        if (w <= 0) w = getWidth();
        return Math.max(160, (int) (w * 0.72) - 80);
    }

    // ---------------- public API ----------------

    public void addText(String sender, boolean mine, long ts, String body,
                        MessageBubble.Status status) {
        addRow(sender, mine, ts, body, null, null, null, status, false);
    }

    public void addFile(String sender, boolean mine, long ts, String fileName,
                        Path savedPath, String hint, MessageBubble.Status status) {
        addRow(sender, mine, ts, null, fileName, savedPath, hint, status, true);
    }

    public void setTyping(boolean on, String who) {
        ensureTypingRemoved();
        if (on) {
            typing = new TypingIndicator();
            messages.add(typing);
            messages.revalidate();
            scrollToBottom();
        }
        messages.repaint();
    }

    // ---------------- internals ----------------

    private void addRow(String sender, boolean mine, long ts, String body,
                        String fileName, Path savedPath, String hint,
                        MessageBubble.Status status, boolean isFile) {
        if (!hasMessages) { removeEmptyState(); hasMessages = true; }
        ensureTypingRemoved();

        boolean nearBottom = isNearBottom();
        maybeAddDayDivider(ts);

        boolean sameRun = sender != null && sender.equals(lastSender)
                && (ts - lastTs) <= GROUP_WINDOW_MS && lastDate != null
                && lastDate.equals(dateOf(ts));
        boolean showNameAvatar = !mine && (group ? true : !sameRun);
        String nameLabel = (group && !mine && !sameRun) ? sender : null;

        messages.add(Box.createVerticalStrut(sameRun ? 2 : Theme.SP_3));

        int cw = contentMaxWidth();
        MessageBubble bubble = isFile
                ? MessageBubble.file(nameLabel, mine, ts, fileName, savedPath, hint, status)
                : MessageBubble.text(nameLabel, mine, ts, body, cw, status);
        if (!isFile) trackBubbleText(bubble);

        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setAlignmentX(LEFT_ALIGNMENT);

        if (mine) {
            row.add(Box.createHorizontalGlue());
            row.add(bubble);
        } else {
            if (showNameAvatar && !sameRun) {
                row.add(Avatar.component(sender, 28, false));
            } else {
                row.add(Box.createRigidArea(new Dimension(28, 0)));
            }
            row.add(Box.createHorizontalStrut(Theme.SP_2));
            row.add(bubble);
            row.add(Box.createHorizontalGlue());
        }
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        messages.add(row);

        lastSender = sender;
        lastTs = ts;
        lastDate = dateOf(ts);

        messages.revalidate();
        messages.repaint();
        if (nearBottom) scrollToBottom();
        else showNewPill();
    }

    private void trackBubbleText(Container c) {
        for (Component child : c.getComponents()) {
            if (child instanceof BubbleText bt) bubbleTexts.add(bt);
            else if (child instanceof Container cc) trackBubbleText(cc);
        }
    }

    private void maybeAddDayDivider(long ts) {
        LocalDate d = dateOf(ts);
        if (d.equals(lastDate)) return;
        messages.add(Box.createVerticalStrut(Theme.SP_3));
        messages.add(new DayDivider(dayLabel(d)));
    }

    private static String dayLabel(LocalDate d) {
        LocalDate today = LocalDate.now();
        if (d.equals(today)) return "Hôm nay";
        if (d.equals(today.minusDays(1))) return "Hôm qua";
        return d.format(DAY);
    }

    private static LocalDate dateOf(long ts) {
        return Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private boolean isNearBottom() {
        JScrollBar b = scroll.getVerticalScrollBar();
        return b.getValue() + b.getVisibleAmount() >= b.getMaximum() - 80;
    }

    private void scrollToBottom() {
        newPill.setVisible(false);
        SwingUtilities.invokeLater(() -> {
            JScrollBar b = scroll.getVerticalScrollBar();
            b.setValue(b.getMaximum());
        });
    }

    private void showNewPill() {
        newPill.setVisible(true);
        relayout();
    }

    // ---------------- empty + typing ----------------

    private void showEmptyState() {
        emptyState = buildEmptyState();
        messages.add(emptyState);
    }

    private void removeEmptyState() {
        if (emptyState != null) { messages.remove(emptyState); emptyState = null; }
    }

    private JComponent buildEmptyState() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(48, 0, 0, 0));
        p.setAlignmentX(CENTER_ALIGNMENT);

        JLabel glyph = new JLabel("👋");
        glyph.setFont(new Font(Theme.FONT_EMOJI, Font.PLAIN, 48));
        glyph.setAlignmentX(CENTER_ALIGNMENT);
        JLabel title = new JLabel(emptyTitle);
        title.setFont(Theme.font(Font.BOLD, 15));
        title.setForeground(Theme.textPrimary());
        title.setAlignmentX(CENTER_ALIGNMENT);
        JLabel hint = new JLabel(emptyHint);
        hint.setFont(Theme.meta());
        hint.setForeground(Theme.textSecondary());
        hint.setAlignmentX(CENTER_ALIGNMENT);

        p.add(glyph);
        p.add(Box.createVerticalStrut(Theme.SP_3));
        p.add(title);
        p.add(Box.createVerticalStrut(Theme.SP_1));
        p.add(hint);
        return p;
    }

    private void ensureTypingRemoved() {
        if (typing != null) {
            typing.stop();
            messages.remove(typing);
            typing = null;
            messages.revalidate();
        }
    }

    /** Looping three-dot bubble shown while the peer is typing (§3.6). */
    private static class TypingIndicator extends JPanel {
        private final Timer timer;
        private int phase;

        TypingIndicator() {
            setOpaque(false);
            setAlignmentX(LEFT_ALIGNMENT);
            setPreferredSize(new Dimension(60, 34));
            setMaximumSize(new Dimension(60, 34));
            timer = new Timer(280, e -> { phase = (phase + 1) % 3; repaint(); });
            timer.start();
        }

        void stop() { timer.stop(); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Theme.bubbleOther());
            g2.fillRoundRect(0, 4, 56, 26, 16, 16);
            for (int i = 0; i < 3; i++) {
                int alpha = i == phase ? 220 : 90;
                g2.setColor(new Color(107, 114, 128, alpha));
                g2.fillOval(12 + i * 12, 14, 7, 7);
            }
            g2.dispose();
        }
    }
}
