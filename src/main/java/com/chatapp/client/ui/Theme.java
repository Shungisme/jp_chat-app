package com.chatapp.client.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.prefs.Preferences;

/**
 * Centralized design-token layer — the Swing equivalent of CSS variables.
 *
 * <p>Holds the colour palette, spacing scale, corner radii, font ramp and motion
 * durations from the UI/UX plan, plus the one-time FlatLaf installation. Toggling
 * {@link #setDark(boolean)} re-skins the whole app from this single place.</p>
 */
public final class Theme {
    private Theme() {}

    private static final Preferences PREFS = Preferences.userRoot().node("com/chatapp/ui");
    private static boolean dark = PREFS.getBoolean("dark", false);

    // ---------------- spacing scale (8px rhythm) ----------------
    public static final int SP_1 = 4;
    public static final int SP_2 = 8;
    public static final int SP_3 = 12;
    public static final int SP_4 = 16;
    public static final int SP_6 = 24;
    public static final int SP_8 = 32;

    // ---------------- corner radii ----------------
    public static final int RADIUS_CONTROL = 8;
    public static final int RADIUS_CARD = 16;
    public static final int RADIUS_BUBBLE = 16;
    public static final int RADIUS_TAIL = 4;

    // ---------------- motion ----------------
    public static final int DURATION_FAST = 120;
    public static final int DURATION_MED = 200;

    // ---------------- fonts ----------------
    public static final String FONT = "Segoe UI";
    public static final String FONT_EMOJI = "Segoe UI Emoji";

    public static Font font(int style, int size) { return new Font(FONT, style, size); }
    public static Font title()        { return new Font(FONT, Font.BOLD, 17); }
    public static Font sectionHeader(){ return new Font(FONT, Font.BOLD, 12); }
    public static Font name()         { return new Font(FONT, Font.BOLD, 14); }
    public static Font body()         { return new Font(FONT, Font.PLAIN, 14); }
    public static Font meta()         { return new Font(FONT, Font.PLAIN, 12); }
    public static Font timestamp()    { return new Font(FONT, Font.PLAIN, 11); }

    // ---------------- palette ----------------
    // Each token resolves against the active light/dark theme.
    public static Color accent()        { return dark ? c(0x6366F1) : c(0x4F46E5); }
    public static Color accentHover()   { return dark ? c(0x7C7DF6) : c(0x4338CA); }
    public static Color accentSoft()    { return dark ? c(0x262A3B) : c(0xEEF2FF); }
    public static Color bgApp()         { return dark ? c(0x0F1117) : c(0xFFFFFF); }
    public static Color bgSidebar()     { return dark ? c(0x161922) : c(0xF7F8FA); }
    public static Color bubbleOther()   { return dark ? c(0x222634) : c(0xF1F3F5); }
    public static Color surfaceCard()   { return dark ? c(0x1B1F2A) : c(0xFFFFFF); }
    public static Color border()        { return dark ? c(0x2A2F3C) : c(0xE5E7EB); }
    public static Color textPrimary()   { return dark ? c(0xE5E7EB) : c(0x111827); }
    public static Color textSecondary() { return dark ? c(0x9CA3AF) : c(0x6B7280); }
    public static Color textOnAccent()  { return Color.WHITE; }
    public static Color online()        { return c(0x22C55E); }
    public static Color unread()        { return c(0xEF4444); }
    public static Color danger()        { return c(0xEF4444); }

    private static Color c(int rgb) { return new Color(rgb); }

    /** Deterministic, stable avatar/sender colour seeded by a name's hash. */
    public static Color avatarColor(String name) {
        int h = name == null ? 0 : name.hashCode();
        float hue = ((h % 360) + 360) % 360 / 360f;
        return Color.getHSBColor(hue, dark ? 0.45f : 0.55f, dark ? 0.70f : 0.78f);
    }

    public static String initials(String name) {
        if (name == null || name.isBlank()) return "?";
        String s = name.trim();
        String[] parts = s.split("\\s+");
        if (parts.length >= 2 && !parts[0].isEmpty() && !parts[1].isEmpty()) {
            return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
        }
        return s.substring(0, Math.min(2, s.length())).toUpperCase();
    }

    // ---------------- theme management ----------------
    public static boolean isDark() { return dark; }

    public static void setup() {
        applyLaf();
        tuneDefaults();
    }

    public static void setDark(boolean value) {
        if (dark == value) return;
        // Snapshot OLD token values BEFORE flipping the flag. Any component
        // whose foreground / background / fill matches an OLD value gets
        // rewritten to the NEW value of the same token — that way every
        // `setForeground(Theme.x())` and `new RoundedPanel(Theme.surfaceCard())`
        // call site stays correct across a toggle without us having to touch
        // each one to register a listener.
        Map<Color, Supplier<Color>> remap = snapshotTokens();
        dark = value;
        PREFS.putBoolean("dark", value);
        applyLaf();
        tuneDefaults();
        for (Window w : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(w);
            refreshThemedColors(w, remap);
            w.repaint();
        }
    }

    private static Map<Color, Supplier<Color>> snapshotTokens() {
        // Last put wins on color collisions (e.g. bgApp() == surfaceCard() ==
        // WHITE in light mode). Order so the more "primary" token wins so
        // ambiguous components land on the right side in the new theme.
        LinkedHashMap<Color, Supplier<Color>> m = new LinkedHashMap<>();
        m.put(accent(), Theme::accent);
        m.put(accentHover(), Theme::accentHover);
        m.put(accentSoft(), Theme::accentSoft);
        m.put(border(), Theme::border);
        m.put(textSecondary(), Theme::textSecondary);
        m.put(textPrimary(), Theme::textPrimary);
        m.put(bubbleOther(), Theme::bubbleOther);
        m.put(surfaceCard(), Theme::surfaceCard);
        m.put(bgSidebar(), Theme::bgSidebar);
        m.put(bgApp(), Theme::bgApp);
        return m;
    }

    private static void refreshThemedColors(Component c, Map<Color, Supplier<Color>> remap) {
        if (c instanceof JComponent jc) {
            Color fg = jc.getForeground();
            if (fg != null) {
                Supplier<Color> s = remap.get(fg);
                if (s != null) jc.setForeground(s.get());
            }
            Color bg = jc.getBackground();
            if (bg != null) {
                Supplier<Color> s = remap.get(bg);
                if (s != null) jc.setBackground(s.get());
            }
        }
        if (c instanceof UiKit.RoundedPanel rp) {
            Color fill = rp.getFill();
            if (fill != null) {
                Supplier<Color> s = remap.get(fill);
                if (s != null) rp.setFill(s.get());
            }
        }
        if (c instanceof BubbleText bt) {
            Color tc = bt.getColor();
            if (tc != null) {
                Supplier<Color> s = remap.get(tc);
                if (s != null) bt.setColor(s.get());
            }
        }
        if (c instanceof Container ct) {
            for (Component ch : ct.getComponents()) refreshThemedColors(ch, remap);
        }
    }

    public static void toggleDark() { setDark(!dark); }

    private static void applyLaf() {
        try {
            if (dark) FlatDarkLaf.setup();
            else FlatLightLaf.setup();
        } catch (Exception ignored) {}
    }

    /** Global UI properties: rounded controls, accent colour, comfortable insets. */
    private static void tuneDefaults() {
        UIManager.put("Component.arc", RADIUS_CONTROL);
        UIManager.put("Button.arc", RADIUS_CONTROL);
        UIManager.put("TextComponent.arc", RADIUS_CONTROL);
        UIManager.put("ProgressBar.arc", RADIUS_CONTROL);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("Component.focusColor", accent());
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Button.default.background", accent());
        UIManager.put("Component.accentColor", accent());
        UIManager.put("TabbedPane.selectedBackground", bgApp());
        UIManager.put("TabbedPane.showTabSeparators", false);
        UIManager.put("TabbedPane.tabHeight", 34);
        UIManager.put("defaultFont", body());
    }
}
