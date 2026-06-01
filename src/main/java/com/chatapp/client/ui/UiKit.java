package com.chatapp.client.ui;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.geom.Ellipse2D;

/** Small factory of shared, theme-aware widgets used across the redesigned screens. */
public final class UiKit {
    private UiKit() {}

    /** A rounded, focus-aware surface panel (input containers, cards). */
    public static class RoundedPanel extends JPanel {
        private final int radius;
        private boolean focused;
        private Color fill;

        public RoundedPanel(int radius, Color fill) {
            this(radius, fill, new BorderLayout());
        }

        public RoundedPanel(int radius, Color fill, LayoutManager lm) {
            super(lm);
            this.radius = radius;
            this.fill = fill;
            setOpaque(false);
        }

        public void setFocused(boolean f) { this.focused = f; repaint(); }
        public void setFill(Color c) { this.fill = c; repaint(); }
        public Color getFill() { return fill; }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, w - 1, h - 1, radius, radius);
            g2.setColor(focused ? Theme.accent() : Theme.border());
            g2.setStroke(new BasicStroke(focused ? 1.6f : 1f));
            g2.drawRoundRect(0, 0, w - 1, h - 1, radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Circular accent send button with a paper-plane glyph; greys out when disabled. */
    public static class CircleButton extends JButton {
        private final int size;

        public CircleButton(String glyph, int size) {
            super(glyph);
            this.size = size;
            setFont(new Font(Theme.FONT_EMOJI, Font.PLAIN, Math.round(size * 0.42f)));
            setForeground(Theme.textOnAccent());
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override public Dimension getPreferredSize() { return new Dimension(size, size); }
        @Override public Dimension getMaximumSize() { return getPreferredSize(); }
        @Override public Dimension getMinimumSize() { return getPreferredSize(); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color base = isEnabled() ? (getModel().isPressed() ? Theme.accentHover() : Theme.accent())
                    : Theme.border();
            g2.setColor(base);
            int inset = isEnabled() && getModel().isPressed() ? 1 : 0; // press-scale
            g2.fill(new Ellipse2D.Float(inset, inset, size - 1 - inset * 2, size - 1 - inset * 2));
            if (getText() == null || getText().isEmpty()) {
                paintPaperPlane(g2);
                g2.dispose();
                return;
            }
            g2.dispose();
            setForeground(isEnabled() ? Theme.textOnAccent() : Theme.textSecondary());
            super.paintComponent(g);
        }

        /** A crisp, font-independent paper-plane glyph for the send action. */
        private void paintPaperPlane(Graphics2D g2) {
            g2.setColor(isEnabled() ? Theme.textOnAccent() : Theme.textSecondary());
            double c = size / 2.0;
            double s = size * 0.30;
            int[] xs = {(int) (c - s), (int) (c + s), (int) (c - s)};
            int[] ys = {(int) (c - s), (int) c, (int) (c + s)};
            g2.fillPolygon(xs, ys, 3);
            g2.setColor(isEnabled() ? new Color(255, 255, 255, 90) : Theme.border());
            g2.fillPolygon(new int[]{(int) (c - s), (int) (c + s), (int) c},
                    new int[]{(int) (c + s), (int) c, (int) c}, 3);
        }
    }

    /** A flat, quiet icon button with a hover tint and an accessible name. */
    public static JButton iconButton(String glyph, String tooltip) {
        JButton b = new JButton(glyph);
        b.setFont(new Font(Theme.FONT_EMOJI, Font.PLAIN, 16));
        b.setToolTipText(tooltip);
        b.setFocusable(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setOpaque(false);
        b.setForeground(Theme.textSecondary());
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        b.getAccessibleContext().setAccessibleName(tooltip);
        return b;
    }

    /** A muted, letter-spaced section header ("TRỰC TUYẾN"). */
    public static JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text.toUpperCase());
        l.setFont(Theme.sectionHeader());
        l.setForeground(Theme.textSecondary());
        l.setBorder(BorderFactory.createEmptyBorder(Theme.SP_3, Theme.SP_3, Theme.SP_1, Theme.SP_3));
        return l;
    }

    public static Border pad(int t, int l, int b, int r) {
        return BorderFactory.createEmptyBorder(t, l, b, r);
    }
}
