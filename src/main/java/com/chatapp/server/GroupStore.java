package com.chatapp.server;

import com.chatapp.model.Group;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GroupStore {
    private final Map<String, Group> groups = new ConcurrentHashMap<>();

    public Group create(String name, String owner) {
        if (name == null || name.isBlank() || owner == null) return null;
        return groups.computeIfAbsent(name, n -> new Group(n, owner));
    }

    public Group get(String name) { return name == null ? null : groups.get(name); }

    public boolean invite(String groupName, String username) {
        if (groupName == null || username == null || username.isBlank()) return false;
        Group g = groups.get(groupName);
        return g != null && g.add(username);
    }

    public boolean remove(String groupName, String username) {
        Group g = get(groupName);
        if (g == null) return false;
        boolean removed = g.remove(username);
        if (removed && g.getMembers().isEmpty()) groups.remove(groupName);
        return removed;
    }

    public boolean isEmpty(String groupName) {
        Group g = get(groupName);
        return g == null || g.getMembers().isEmpty();
    }

    public Map<String, Group> all() { return groups; }
}
