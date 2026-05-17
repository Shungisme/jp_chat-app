package com.chatapp.util;

import com.chatapp.model.Message;

import javax.swing.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public class FileTransfer {
    public static final int CHUNK_SIZE = 4096;

    public static Message buildFileMessage(String sender, String target, Path file) throws IOException {
        try (InputStream is = Files.newInputStream(file);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[CHUNK_SIZE];
            int n;
            while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
            String b64 = Base64.getEncoder().encodeToString(bos.toByteArray());
            String content = file.getFileName().toString() + "|" + b64;
            return new Message(Message.Type.FILE, sender, target, content);
        }
    }

    public static Path saveIncoming(Message msg, Path dir) throws IOException {
        Files.createDirectories(dir);
        String[] parts = msg.getContent().split("\\|", 2);
        if (parts.length != 2) throw new IOException("malformed FILE message");
        Path out = dir.resolve(parts[0]);
        try (OutputStream os = Files.newOutputStream(out)) {
            os.write(Base64.getDecoder().decode(parts[1]));
        }
        return out;
    }

    public static JDialog progressDialog(java.awt.Frame parent, String title) {
        JDialog d = new JDialog(parent, title, false);
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setStringPainted(true);
        d.add(bar);
        d.pack();
        d.setSize(280, 80);
        d.setLocationRelativeTo(parent);
        d.putClientProperty("progressBar", bar);
        return d;
    }

    public static void setProgress(JDialog dialog, int percent) {
        JProgressBar bar = (JProgressBar) dialog.getClientProperty("progressBar");
        if (bar != null) bar.setValue(percent);
    }
}
