package com.chatapp.client.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;

/**
 * Word-wrapping text component that lays out its own glyphs so a message body
 * can wrap to a max width, report a correct preferred height, and switch between
 * {@code Segoe UI} and {@code Segoe UI Emoji} per codepoint — preserving the
 * existing per-codepoint emoji rendering inside the new bubble components.
 */
public class BubbleText extends JComponent {
    private static final FontRenderContext FRC = new FontRenderContext(null, true, true);

    private final String text;
    private final int fontSize;
    private Color color;
    private int maxWidth;

    public BubbleText(String text, int fontSize, Color color, int maxWidth) {
        this.text = (text == null || text.isEmpty()) ? " " : text;
        this.fontSize = fontSize;
        this.color = color;
        this.maxWidth = maxWidth;
        setOpaque(false);
        setToolTipText(null);
    }

    @Override public javax.accessibility.AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new javax.swing.JComponent.AccessibleJComponent() {};
            accessibleContext.setAccessibleName(text);
        }
        return accessibleContext;
    }

    public void setColor(Color c) { this.color = c; repaint(); }
    public Color getColor() { return color; }

    public void setMaxWidth(int w) {
        if (w != maxWidth) { maxWidth = Math.max(40, w); revalidate(); repaint(); }
    }

    private static boolean isEmojiCodePoint(int cp) {
        return (cp >= 0x2600 && cp <= 0x27BF) || cp >= 0x1F000;
    }

    /** Build an attributed paragraph with per-codepoint font + uniform colour. */
    private AttributedString attributed(String paragraph) {
        AttributedString as = new AttributedString(paragraph);
        as.addAttribute(TextAttribute.FOREGROUND, color);
        Font textFont = Theme.font(Font.PLAIN, fontSize);
        Font emojiFont = new Font(Theme.FONT_EMOJI, Font.PLAIN, fontSize);
        as.addAttribute(TextAttribute.FONT, textFont);
        int i = 0;
        while (i < paragraph.length()) {
            int cp = paragraph.codePointAt(i);
            int n = Character.charCount(cp);
            if (isEmojiCodePoint(cp)) {
                as.addAttribute(TextAttribute.FONT, emojiFont, i, i + n);
            }
            i += n;
        }
        return as;
    }

    /** Lay out into TextLayouts; returns total {width,height}. If g != null, paints. */
    private Dimension layout(Graphics2D g) {
        int wrap = Math.max(40, maxWidth);
        float y = 0;
        float maxLine = 0;
        for (String paragraph : text.split("\n", -1)) {
            if (paragraph.isEmpty()) {
                Font f = Theme.font(Font.PLAIN, fontSize);
                y += f.getLineMetrics(" ", FRC).getHeight();
                continue;
            }
            AttributedCharacterIterator it = attributed(paragraph).getIterator();
            LineBreakMeasurer m = new LineBreakMeasurer(it, g != null ? g.getFontRenderContext() : FRC);
            while (m.getPosition() < it.getEndIndex()) {
                TextLayout tl = m.nextLayout(wrap);
                float ascent = tl.getAscent();
                float lineH = ascent + tl.getDescent() + tl.getLeading();
                maxLine = Math.max(maxLine, tl.getAdvance());
                if (g != null) tl.draw(g, 0, y + ascent);
                y += lineH;
            }
        }
        return new Dimension((int) Math.ceil(maxLine), (int) Math.ceil(y));
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        layout(g2);
        g2.dispose();
    }

    @Override public Dimension getPreferredSize() { return layout(null); }
    @Override public Dimension getMinimumSize() { return getPreferredSize(); }
    @Override public Dimension getMaximumSize() {
        Dimension d = getPreferredSize();
        return new Dimension(maxWidth, d.height);
    }
}
