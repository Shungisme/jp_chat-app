package com.chatapp.client;

import java.awt.*;
import java.net.URL;

public class NotificationManager {
    private TrayIcon trayIcon;

    public boolean enable() {
        if (!SystemTray.isSupported()) return false;
        try {
            URL iconUrl = getClass().getResource("/icons/app.png");
            Image image = iconUrl != null
                    ? Toolkit.getDefaultToolkit().getImage(iconUrl)
                    : Toolkit.getDefaultToolkit().createImage(new byte[0]);
            trayIcon = new TrayIcon(image, "ChatApp");
            trayIcon.setImageAutoSize(true);
            SystemTray.getSystemTray().add(trayIcon);
            return true;
        } catch (AWTException e) {
            System.err.println("[Notify] failed to attach tray icon: " + e.getMessage());
            return false;
        }
    }

    public void notify(String from, String message) {
        if (trayIcon != null) {
            trayIcon.displayMessage("Tin nhắn mới từ " + from, message, TrayIcon.MessageType.INFO);
        }
    }

    public void dispose() {
        if (trayIcon != null) SystemTray.getSystemTray().remove(trayIcon);
    }
}
