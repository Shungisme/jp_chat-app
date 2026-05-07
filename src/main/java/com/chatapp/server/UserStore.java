package com.chatapp.server;

import com.chatapp.model.User;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserStore {
    private static final Path FILE = Path.of("data", "users.txt");
    private final Map<String, User> users = new ConcurrentHashMap<>();

    public UserStore() {
        load();
    }

    public synchronized boolean register(String username, String password) {
        if (users.containsKey(username)) return false;
        users.put(username, new User(username, hash(password)));
        save();
        return true;
    }

    public synchronized boolean authenticate(String username, String password) {
        User u = users.get(username);
        return u != null && u.getPasswordHash().equals(hash(password));
    }

    private void load() {
        try {
            if (!Files.exists(FILE)) return;
            for (String line : Files.readAllLines(FILE, StandardCharsets.UTF_8)) {
                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    users.put(parts[0], new User(parts[0], parts[1]));
                }
            }
        } catch (IOException e) {
            System.err.println("[UserStore] load failed: " + e.getMessage());
        }
    }

    private void save() {
        try {
            Files.createDirectories(FILE.getParent());
            StringBuilder sb = new StringBuilder();
            for (User u : users.values()) {
                sb.append(u.getUsername()).append(":").append(u.getPasswordHash()).append("\n");
            }
            Files.writeString(FILE, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[UserStore] save failed: " + e.getMessage());
        }
    }

    private String hash(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(password.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
