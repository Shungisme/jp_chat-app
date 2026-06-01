package com.chatapp.client.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * A square button that paints a {@link Icons.Painter} vector icon itself, with a
 * subtle rounded hover background and pressed/hover tinting. Because nothing is
 * rendered from a font, the icon can never come out as a missing-glyph box and is
 * always centered in a ≥32px hit target.
 */
public class VectorIconButton extends JButton {
    private final Icons.Painter painter;
    private final int iconSize;
    private final int pad;
    private Color hoverColor;
    private boolean active;

    public VectorIconButton(Icons.Painter painter, String tooltip, int iconSize) {
        this.painter = painter;
        this.iconSize = iconSize;
        this.pad = 7;
        this.hoverColor = Theme.textPrimary();
        setToolTipText(tooltip);
        getAccessibleContext().setAccessibleName(tooltip);
        setFocusable(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(BorderFactory.createEmptyBorder(pad, pad, pad, pad));
    }

    /** Tint shown on hover (e.g. red for a destructive close button). */
    public VectorIconButton hover(Color c) { this.hoverColor = c; return this; }

    /** Toggle a persistent "lit" state — used for on/off icon toggles like mic. */
    public void setActive(boolean on) { if (this.active != on) { this.active = on; repaint(); } }
    public boolean isActive() { return active; }

    @Override public Dimension getPreferredSize() {
        int s = iconSize + pad * 2;
        return new Dimension(s, s);
    }
    @Override public Dimension getMaximumSize() { return getPreferredSize(); }
    @Override public Dimension getMinimumSize() { return getPreferredSize(); }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        boolean rollover = getModel().isRollover();
        boolean pressed = getModel().isPressed();

        if (rollover || pressed || active) {
            g2.setColor(Theme.accentSoft());
            g2.fill(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 10, 10));
        }
        Color c = !isEnabled() ? Theme.border()
                : pressed ? Theme.accent()
                : active ? Theme.accent()
                : rollover ? hoverColor
                : Theme.textSecondary();
        g2.translate((w - iconSize) / 2, (h - iconSize) / 2);
        painter.paint(g2, iconSize, iconSize, c);
        g2.dispose();
    }
}
