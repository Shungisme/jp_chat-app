package com.chatapp.server;

import com.chatapp.model.Group;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GroupStore {
    private static final Path FILE = Path.of("data", "groups.txt");
    private final Map<String, Group> groups = new ConcurrentHashMap<>();

    public GroupStore() {
        load();
    }

    private synchronized void load() {
        try {
            if (!Files.exists(FILE)) return;
            for (String line : Files.readAllLines(FILE, StandardCharsets.UTF_8)) {
                if (line == null || line.isBlank()) continue;
                String[] parts = line.split("\\|", 3);
                if (parts.length < 2) continue;
                String name = parts[0].trim();
                String owner = parts[1].trim();
                if (name.isEmpty() || owner.isEmpty()) continue;
                Group g = new Group(name, owner);
                if (parts.length == 3 && !parts[2].isBlank()) {
                    for (String m : parts[2].split(",")) {
                        String mt = m.trim();
                        if (!mt.isEmpty()) g.add(mt);
                    }
                }
                groups.put(name, g);
            }
        } catch (IOException e) {
            System.err.println("[GroupStore] load failed: " + e.getMessage());
        }
    }

    private synchronized void save() {
        try {
            Files.createDirectories(FILE.getParent());
            StringBuilder sb = new StringBuilder();
            for (Group g : groups.values()) {
                sb.append(g.getName()).append("|").append(g.getOwner()).append("|");
                sb.append(String.join(",", g.getMembers()));
                sb.append("\n");
            }
            Files.writeString(FILE, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[GroupStore] save failed: " + e.getMessage());
        }
    }

    public Group create(String name, String owner) {
        if (name == null || name.isBlank() || owner == null) return null;
        Group g = groups.computeIfAbsent(name, n -> new Group(n, owner));
        save();
        return g;
    }

    public Group get(String name) { return name == null ? null : groups.get(name); }

    public boolean invite(String groupName, String username) {
        if (groupName == null || username == null || username.isBlank()) return false;
        Group g = groups.get(groupName);
        if (g != null && g.add(username)) {
            save();
            return true;
        }
        return false;
    }

    public boolean remove(String groupName, String username) {
        Group g = get(groupName);
        if (g == null) return false;
        boolean removed = g.remove(username);
        if (removed) {
            if (g.getMembers().isEmpty()) groups.remove(groupName);
            save();
        }
        return removed;
    }

    public boolean isEmpty(String groupName) {
        Group g = get(groupName);
        return g == null || g.getMembers().isEmpty();
    }

    public Map<String, Group> all() { return groups; }
}
