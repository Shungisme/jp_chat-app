package com.chatapp.util;

import com.chatapp.model.Message;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class HistoryManager {
    private static final Path BASE = Path.of("data", "history");

    private static Path fileFor(String me, String peer) {
        String pair = me.compareTo(peer) < 0 ? me + "__" + peer : peer + "__" + me;
        return BASE.resolve(pair + ".log");
    }

    public static synchronized void append(String me, String peer, Message msg) {
        try {
            Files.createDirectories(BASE);
            Path f = fileFor(me, peer);
            String line = msg.getTimestamp() + "|" + msg.getSender() + "|"
                    + msg.getType() + "|" + safe(msg.getContent()) + "\n";
            Files.writeString(f, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("[History] append failed: " + e.getMessage());
        }
    }

    public static List<String> load(String me, String peer) {
        try {
            Path f = fileFor(me, peer);
            if (!Files.exists(f)) return List.of();
            return new ArrayList<>(Files.readAllLines(f, StandardCharsets.UTF_8));
        } catch (IOException e) {
            return List.of();
        }
    }

    // Deletes the message at the given zero-based line index.
    public static synchronized boolean deleteAt(String me, String peer, int index) {
        try {
            Path f = fileFor(me, peer);
            if (!Files.exists(f)) return false;
            List<String> lines = new ArrayList<>(Files.readAllLines(f, StandardCharsets.UTF_8));
            if (index < 0 || index >= lines.size()) return false;
            lines.remove(index);
            Files.write(f, lines, StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static synchronized void clear(String me, String peer) {
        try {
            Files.deleteIfExists(fileFor(me, peer));
        } catch (IOException ignored) {}
    }

    private static String safe(String s) {
        return s == null ? "" : s.replace("\n", "\\n");
    }
}
