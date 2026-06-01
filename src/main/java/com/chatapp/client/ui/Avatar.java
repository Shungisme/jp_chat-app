package com.chatapp.client.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;

/**
 * Reusable circular initials-avatar with an optional green presence dot.
 *
 * <p>Used identically in the left rail, thread headers and in-thread bubbles.
 * Colour is deterministic per name (see {@link Theme#avatarColor}) so each person
 * keeps a stable identity colour that doubles as their sender colour in groups.</p>
 */
public final class Avatar {
    private Avatar() {}

    /** Paint a circular avatar with centered initials at (x,y) of the given size. */
    public static void paint(Graphics2D g, String name, int x, int y, int size, boolean online) {
        Object oldAA = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(Theme.avatarColor(name));
        g.fill(new Ellipse2D.Float(x, y, size, size));

        // 1px subtle ring for separation on busy backgrounds
        g.setColor(new Color(0, 0, 0, 28));
        g.draw(new Ellipse2D.Float(x + 0.5f, y + 0.5f, size - 1f, size - 1f));

        String text = Theme.initials(name);
        g.setColor(Color.WHITE);
        g.setFont(Theme.font(Font.BOLD, Math.round(size * 0.42f)));
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(text);
        int tx = x + (size - tw) / 2;
        int ty = y + (size - fm.getHeight()) / 2 + fm.getAscent();
        g.drawString(text, tx, ty);

        if (online) {
            int d = Math.max(8, Math.round(size * 0.28f));
            int dx = x + size - d;
            int dy = y + size - d;
            g.setColor(Theme.bgApp());
            g.fill(new Ellipse2D.Float(dx - 2, dy - 2, d + 4, d + 4)); // white ring
            g.setColor(Theme.online());
            g.fill(new Ellipse2D.Float(dx, dy, d, d));
        }

        if (oldAA != null) g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
    }

    /** A fixed-size avatar component (handy for headers and bubbles). */
    public static JComponent component(String name, int size, boolean online) {
        JComponent comp = new JComponent() {
            @Override protected void paintComponent(Graphics g) {
                Avatar.paint((Graphics2D) g, name, 0, 0, size, online);
            }
            @Override public Dimension getPreferredSize() { return new Dimension(size, size); }
            @Override public Dimension getMaximumSize() { return getPreferredSize(); }
            @Override public Dimension getMinimumSize() { return getPreferredSize(); }
        };
        comp.setOpaque(false);
        comp.setToolTipText(name);
        comp.setAlignmentY(Component.TOP_ALIGNMENT);
        return comp;
    }

    /** An {@link Icon} variant for renderers / labels. */
    public static Icon icon(String name, int size, boolean online) {
        return new Icon() {
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                paint((Graphics2D) g.create(), name, x, y, size, online);
            }
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }
        };
    }
}
