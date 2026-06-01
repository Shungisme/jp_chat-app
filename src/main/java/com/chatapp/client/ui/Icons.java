package com.chatapp.client.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.RoundRectangle2D;

/**
 * Monochrome vector icons painted in code (§2.5). Drawing the glyphs ourselves
 * — instead of relying on emoji codepoints — guarantees they always appear,
 * stay pixel-aligned, and tint on hover, regardless of the host's emoji fonts.
 */
public final class Icons {
    private Icons() {}

    /** Paints a single icon into a w×h box with the given stroke colour. */
    public interface Painter { void paint(Graphics2D g, int w, int h, Color c); }

    // ---------------- named icons ----------------

    public static final Painter CLOSE = (g, w, h, c) -> {
        float a = w * 0.30f;
        stroke(g, c, w * 0.11f);
        g.drawLine((int) a, (int) a, (int) (w - a), (int) (h - a));
        g.drawLine((int) (w - a), (int) a, (int) a, (int) (h - a));
    };

    public static final Painter MIC = (g, w, h, c) -> {
        stroke(g, c, w * 0.085f);
        float cw = w * 0.26f, ch = h * 0.42f;
        float cx = (w - cw) / 2f, cy = h * 0.14f;
        g.draw(new RoundRectangle2D.Float(cx, cy, cw, ch, cw, cw));
        // cradle arc
        float ar = w * 0.30f;
        g.draw(new Arc2D.Float(w / 2f - ar, h * 0.30f, ar * 2, ar * 2, 200, 140, Arc2D.OPEN));
        // stem + base
        g.drawLine(w / 2, (int) (h * 0.78f), w / 2, (int) (h * 0.90f));
        g.drawLine((int) (w * 0.34f), (int) (h * 0.90f), (int) (w * 0.66f), (int) (h * 0.90f));
    };

    public static final Painter VIDEO = (g, w, h, c) -> {
        stroke(g, c, w * 0.085f);
        float bx = w * 0.16f, by = h * 0.30f, bw = w * 0.46f, bh = h * 0.40f;
        g.draw(new RoundRectangle2D.Float(bx, by, bw, bh, w * 0.12f, w * 0.12f));
        GeneralPath lens = new GeneralPath();
        lens.moveTo(bx + bw + w * 0.04f, by + bh / 2);
        lens.lineTo(w * 0.84f, by);
        lens.lineTo(w * 0.84f, by + bh);
        lens.closePath();
        g.fill(lens);
    };

    public static final Painter CLOCK = (g, w, h, c) -> {
        stroke(g, c, w * 0.085f);
        float in = w * 0.18f;
        g.draw(new Ellipse2D.Float(in, in, w - in * 2, h - in * 2));
        g.drawLine(w / 2, h / 2, w / 2, (int) (h * 0.30f));        // minute
        g.drawLine(w / 2, h / 2, (int) (w * 0.66f), (int) (h * 0.56f)); // hour
    };

    public static final Painter SMILEY = (g, w, h, c) -> {
        stroke(g, c, w * 0.085f);
        float in = w * 0.18f;
        g.draw(new Ellipse2D.Float(in, in, w - in * 2, h - in * 2));
        float eye = w * 0.07f;
        g.fill(new Ellipse2D.Float(w * 0.36f - eye, h * 0.40f - eye, eye * 2, eye * 2));
        g.fill(new Ellipse2D.Float(w * 0.64f - eye, h * 0.40f - eye, eye * 2, eye * 2));
        g.draw(new Arc2D.Float(w * 0.30f, h * 0.40f, w * 0.40f, h * 0.30f, 200, 140, Arc2D.OPEN));
    };

    public static final Painter ATTACH = (g, w, h, c) -> {
        stroke(g, c, w * 0.085f);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.rotate(Math.toRadians(45), w / 2.0, h / 2.0);
        float cw = w * 0.26f, ch = h * 0.66f;
        g2.draw(new RoundRectangle2D.Float((w - cw) / 2f, (h - ch) / 2f, cw, ch, cw, cw));
        g2.drawLine((int) (w / 2f), (int) ((h - ch) / 2f + ch * 0.18f),
                (int) (w / 2f), (int) ((h + ch) / 2f - ch * 0.30f));
        g2.dispose();
    };

    public static final Painter MOON = (g, w, h, c) -> {
        float r = w * 0.34f;
        Area moon = new Area(new Ellipse2D.Float(w / 2f - r, h / 2f - r, r * 2, r * 2));
        moon.subtract(new Area(new Ellipse2D.Float(w / 2f - r + w * 0.16f, h / 2f - r - w * 0.04f, r * 2, r * 2)));
        g.setColor(c);
        g.fill(moon);
    };

    public static final Painter SUN = (g, w, h, c) -> {
        stroke(g, c, w * 0.08f);
        float r = w * 0.18f;
        g.fill(new Ellipse2D.Float(w / 2f - r, h / 2f - r, r * 2, r * 2));
        for (int i = 0; i < 8; i++) {
            double ang = Math.PI / 4 * i;
            float r1 = w * 0.30f, r2 = w * 0.42f;
            g.drawLine((int) (w / 2 + Math.cos(ang) * r1), (int) (h / 2 + Math.sin(ang) * r1),
                    (int) (w / 2 + Math.cos(ang) * r2), (int) (h / 2 + Math.sin(ang) * r2));
        }
    };

    public static final Painter SEARCH = (g, w, h, c) -> {
        stroke(g, c, w * 0.09f);
        float d = w * 0.46f, x = w * 0.20f, y = h * 0.20f;
        g.draw(new Ellipse2D.Float(x, y, d, d));
        g.drawLine((int) (x + d * 0.92f), (int) (y + d * 0.92f), (int) (w * 0.82f), (int) (h * 0.82f));
    };

    public static final Painter GROUP = (g, w, h, c) -> {
        stroke(g, c, w * 0.075f);
        float r = w * 0.16f;
        // two heads
        g.draw(new Ellipse2D.Float(w * 0.30f - r, h * 0.30f - r, r * 2, r * 2));
        g.draw(new Ellipse2D.Float(w * 0.66f - r, h * 0.34f - r, r * 1.7f, r * 1.7f));
        // two shoulders (arcs)
        g.draw(new Arc2D.Float(w * 0.10f, h * 0.52f, w * 0.40f, h * 0.40f, 0, 180, Arc2D.OPEN));
        g.draw(new Arc2D.Float(w * 0.48f, h * 0.56f, w * 0.40f, h * 0.36f, 0, 180, Arc2D.OPEN));
    };

    public static final Painter ADD = (g, w, h, c) -> {
        stroke(g, c, w * 0.10f);
        g.drawLine(w / 2, (int) (h * 0.26f), w / 2, (int) (h * 0.74f));
        g.drawLine((int) (w * 0.26f), h / 2, (int) (w * 0.74f), h / 2);
    };

    public static final Painter FILE = (g, w, h, c) -> {
        stroke(g, c, w * 0.075f);
        float x = w * 0.26f, y = h * 0.14f, pw = w * 0.48f, ph = h * 0.72f;
        float fold = w * 0.16f;
        GeneralPath page = new GeneralPath();
        page.moveTo(x, y);
        page.lineTo(x + pw - fold, y);
        page.lineTo(x + pw, y + fold);
        page.lineTo(x + pw, y + ph);
        page.lineTo(x, y + ph);
        page.closePath();
        g.draw(page);
        // folded corner
        g.drawLine((int) (x + pw - fold), (int) y, (int) (x + pw - fold), (int) (y + fold));
        g.drawLine((int) (x + pw - fold), (int) (y + fold), (int) (x + pw), (int) (y + fold));
        // text lines
        g.drawLine((int) (x + pw * 0.18f), (int) (y + ph * 0.48f), (int) (x + pw * 0.82f), (int) (y + ph * 0.48f));
        g.drawLine((int) (x + pw * 0.18f), (int) (y + ph * 0.68f), (int) (x + pw * 0.82f), (int) (y + ph * 0.68f));
    };

    private static void stroke(Graphics2D g, Color c, float width) {
        g.setColor(c);
        g.setStroke(new BasicStroke(Math.max(1.2f, width), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    }

    // ---------------- factories ----------------

    public static Icon icon(Painter p, int size, Color color) {
        return new Icon() {
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.translate(x, y);
                p.paint(g2, size, size, color);
                g2.dispose();
            }
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }
        };
    }

    /** A flat, square icon button with hover tint and a ≥32px hit target (§7). */
    public static VectorIconButton button(Painter p, String tooltip) {
        return new VectorIconButton(p, tooltip, 18);
    }
}
