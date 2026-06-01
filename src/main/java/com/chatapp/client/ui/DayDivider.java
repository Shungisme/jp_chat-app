package com.chatapp.client.ui;

import javax.swing.*;
import java.awt.*;

/** A centered, pill-styled date separator inserted when the day changes. */
public class DayDivider extends JPanel {
    private final String label;

    public DayDivider(String label) {
        this.label = label;
        setOpaque(false);
        setAlignmentX(CENTER_ALIGNMENT);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        setPreferredSize(new Dimension(10, 30));
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setFont(Theme.timestamp());
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(label);
        int w = getWidth(), h = getHeight();
        int padX = 10;
        int pillW = tw + padX * 2;
        int x = (w - pillW) / 2;
        int y = (h - 20) / 2;

        // hairlines either side
        g2.setColor(Theme.border());
        g2.drawLine(16, h / 2, x - 8, h / 2);
        g2.drawLine(x + pillW + 8, h / 2, w - 16, h / 2);

        g2.setColor(Theme.bgSidebar());
        g2.fillRoundRect(x, y, pillW, 20, 12, 12);
        g2.setColor(Theme.textSecondary());
        g2.drawString(label, x + padX, y + fm.getAscent() + (20 - fm.getHeight()) / 2);
        g2.dispose();
    }
}
