package com.chatapp.server;

import com.chatapp.model.Group;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GroupStore {
    private static final Path FILE = Path.of("data", "groups.txt");
    private final Map<String, Group> groupsById = new ConcurrentHashMap<>();

    public GroupStore() {
        load();
    }

    private synchronized void load() {
        try {
            if (!Files.exists(FILE)) return;
            boolean migrated = false;
            for (String line : Files.readAllLines(FILE, StandardCharsets.UTF_8)) {
                if (line == null || line.isBlank()) continue;
                String[] parts = line.split("\\|", 4);
                if (parts.length == 3) {
                    // Legacy format: name|owner|members → assign a new id
                    String id = UUID.randomUUID().toString();
                    Group g = new Group(id, parts[0].trim(), parts[1].trim());
                    addMembers(g, parts[2]);
                    groupsById.put(id, g);
                    migrated = true;
                } else if (parts.length == 4) {
                    String id = parts[0].trim();
                    if (id.isEmpty()) continue;
                    Group g = new Group(id, parts[1].trim(), parts[2].trim());
                    addMembers(g, parts[3]);
                    groupsById.put(id, g);
                }
            }
            if (migrated) save();
        } catch (IOException e) {
            System.err.println("[GroupStore] load failed: " + e.getMessage());
        }
    }

    private void addMembers(Group g, String raw) {
        if (raw == null || raw.isBlank()) return;
        for (String m : raw.split(",")) {
            String mt = m.trim();
            if (!mt.isEmpty()) g.add(mt);
        }
    }

    private synchronized void save() {
        try {
            Files.createDirectories(FILE.getParent());
            StringBuilder sb = new StringBuilder();
            for (Group g : groupsById.values()) {
                sb.append(g.getId()).append("|")
                  .append(g.getName()).append("|")
                  .append(g.getOwner()).append("|")
                  .append(String.join(",", g.getMembers()))
                  .append("\n");
            }
            Files.writeString(FILE, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[GroupStore] save failed: " + e.getMessage());
        }
    }

    // Creates a brand-new group with a freshly minted UUID. Multiple groups
    // can share the same display name.
    public Group create(String name, String owner, Collection<String> extraMembers) {
        if (name == null || name.isBlank() || owner == null) return null;
        String id = UUID.randomUUID().toString();
        Group g = new Group(id, name, owner);
        if (extraMembers != null) {
            for (String m : extraMembers) {
                if (m != null && !m.isBlank() && !m.equals(owner)) g.add(m);
            }
        }
        groupsById.put(id, g);
        save();
        return g;
    }

    public Group get(String id) { return id == null ? null : groupsById.get(id); }

    public boolean invite(String groupId, String username) {
        if (groupId == null || username == null || username.isBlank()) return false;
        Group g = groupsById.get(groupId);
        if (g != null && g.add(username)) {
            save();
            return true;
        }
        return false;
    }

    public boolean remove(String groupId, String username) {
        Group g = get(groupId);
        if (g == null) return false;
        boolean removed = g.remove(username);
        if (removed) {
            if (g.getMembers().isEmpty()) groupsById.remove(groupId);
            save();
        }
        return removed;
    }

    public boolean isEmpty(String groupId) {
        Group g = get(groupId);
        return g == null || g.getMembers().isEmpty();
    }

    public Map<String, Group> all() { return groupsById; }
}
