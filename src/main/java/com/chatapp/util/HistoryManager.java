package com.chatapp.util;

import com.chatapp.model.Message;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class HistoryManager {
    private static final Path BASE = Path.of("data", "history");

    // Scoped per local user so two client instances sharing a working directory
    // do not append to the same file and double-log every message.
    private static Path fileFor(String me, String peer) {
        String pair = me.compareTo(peer) < 0 ? me + "__" + peer : peer + "__" + me;
        return BASE.resolve(me).resolve(pair + ".log");
    }

    public static synchronized void append(String me, String peer, Message msg) {
        try {
            Path f = fileFor(me, peer);
            Files.createDirectories(f.getParent());
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
            List<String> raw = Files.readAllLines(f, StandardCharsets.UTF_8);
            List<String> valid = new ArrayList<>();
            for (String line : raw) {
                if (line == null || line.isBlank()) continue;
                // skip lines that fail the expected "ts|sender|type|content" shape
                if (line.split("\\|", 4).length < 4) {
                    System.err.println("[History] skipping malformed line: " + line);
                    continue;
                }
                valid.add(line);
            }
            return valid;
        } catch (IOException e) {
            System.err.println("[History] load failed (corrupted?): " + e.getMessage());
            return List.of();
        }
    }

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

    // ---------------- group history ----------------
    // Keyed by groupId (UUID) so two groups with the same display name keep
    // separate logs.

    private static Path groupFileFor(String me, String groupId) {
        return BASE.resolve(me).resolve("group__" + groupId + ".log");
    }

    public static synchronized void appendGroup(String me, String groupId, Message msg) {
        try {
            Path f = groupFileFor(me, groupId);
            Files.createDirectories(f.getParent());
            String line = msg.getTimestamp() + "|" + msg.getSender() + "|"
                    + msg.getType() + "|" + safe(msg.getContent()) + "\n";
            Files.writeString(f, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("[History] group append failed: " + e.getMessage());
        }
    }

    public static List<String> loadGroup(String me, String groupId) {
        try {
            Path f = groupFileFor(me, groupId);
            if (!Files.exists(f)) return List.of();
            List<String> raw = Files.readAllLines(f, StandardCharsets.UTF_8);
            List<String> valid = new ArrayList<>();
            for (String line : raw) {
                if (line == null || line.isBlank()) continue;
                if (line.split("\\|", 4).length < 4) continue;
                valid.add(line);
            }
            return valid;
        } catch (IOException e) {
            System.err.println("[History] group load failed: " + e.getMessage());
            return List.of();
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s.replace("\n", "\\n");
    }
}
