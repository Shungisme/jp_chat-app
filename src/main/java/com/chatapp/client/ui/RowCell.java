package com.chatapp.client.ui;

import javax.swing.*;
import java.awt.*;

/**
 * A rich left-rail row: circular avatar with a green presence dot, name,
 * muted preview line and an accent unread-count pill, with hover and
 * accent-bar selected states. Configured per-row by the list renderers and
 * painted as a single component (Swing cell-renderer "stamp" pattern).
 */
public class RowCell extends JPanel {
    private String name = "";
    private String preview = "";
    private boolean online;
    private boolean selected;
    private boolean hovered;
    private int unread;

    public RowCell() {
        setOpaque(true);
        setPreferredSize(new Dimension(0, 64));
    }

    public void configure(String name, String preview, boolean online,
                          int unread, boolean selected, boolean hovered) {
        this.name = name;
        this.preview = preview;
        this.online = online;
        this.unread = unread;
        this.selected = selected;
        this.hovered = hovered;
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();

        // background: selected > hover > base
        Color bg = Theme.bgSidebar();
        if (selected) bg = Theme.accentSoft();
        else if (hovered) bg = blend(Theme.bgSidebar(), Theme.accentSoft(), 0.5f);
        g2.setColor(bg);
        g2.fillRect(0, 0, w, h);

        if (selected) { // leading accent bar
            g2.setColor(Theme.accent());
            g2.fillRect(0, 8, 3, h - 16);
        }

        int avSize = 40;
        int avX = Theme.SP_3, avY = (h - avSize) / 2;
        Avatar.paint(g2, name, avX, avY, avSize, online);

        boolean bold = unread > 0;
        int textX = avX + avSize + Theme.SP_3;

        // right column: unread pill
        int rightLimit = w - Theme.SP_3;
        if (unread > 0) {
            String badge = unread > 9 ? "9+" : String.valueOf(unread);
            g2.setFont(Theme.font(Font.BOLD, 11));
            FontMetrics bfm = g2.getFontMetrics();
            int bw = Math.max(20, bfm.stringWidth(badge) + 12);
            int bh = 20;
            int bx = w - Theme.SP_3 - bw;
            int by = (h - bh) / 2;
            g2.setColor(Theme.unread());
            g2.fillRoundRect(bx, by, bw, bh, bh, bh);
            g2.setColor(Color.WHITE);
            g2.drawString(badge, bx + (bw - bfm.stringWidth(badge)) / 2,
                    by + bfm.getAscent() + (bh - bfm.getHeight()) / 2);
            rightLimit = bx - Theme.SP_2;
        }

        // name
        g2.setColor(Theme.textPrimary());
        g2.setFont(Theme.font(bold ? Font.BOLD : Font.PLAIN, 14));
        FontMetrics nfm = g2.getFontMetrics();
        int nameBaseline = avY + 18;
        g2.drawString(ellipsize(g2, name, rightLimit - textX), textX, nameBaseline);

        // preview
        if (preview != null && !preview.isEmpty()) {
            g2.setColor(Theme.textSecondary());
            g2.setFont(Theme.font(bold ? Font.BOLD : Font.PLAIN, 12));
            g2.drawString(ellipsize(g2, preview, rightLimit - textX),
                    textX, nameBaseline + nfm.getHeight() - 2);
        }
        g2.dispose();
    }

    private static String ellipsize(Graphics2D g, String s, int maxW) {
        if (s == null) return "";
        FontMetrics fm = g.getFontMetrics();
        if (fm.stringWidth(s) <= maxW) return s;
        String ell = "…";
        int ew = fm.stringWidth(ell);
        StringBuilder sb = new StringBuilder();
        int w = 0;
        for (int i = 0; i < s.length(); i++) {
            int cw = fm.charWidth(s.charAt(i));
            if (w + cw + ew > maxW) break;
            sb.append(s.charAt(i));
            w += cw;
        }
        return sb + ell;
    }

    private static Color blend(Color a, Color b, float t) {
        return new Color(
                Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }
}
