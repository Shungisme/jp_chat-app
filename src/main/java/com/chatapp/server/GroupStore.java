package com.chatapp.server;

import com.chatapp.model.Group;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GroupStore {
    private final Map<String, Group> groups = new ConcurrentHashMap<>();

    public Group create(String name, String owner) {
        return groups.computeIfAbsent(name, n -> new Group(n, owner));
    }

    public Group get(String name) { return groups.get(name); }

    public boolean invite(String groupName, String username) {
        Group g = groups.get(groupName);
        return g != null && g.add(username);
    }

    public boolean remove(String groupName, String username) {
        Group g = groups.get(groupName);
        return g != null && g.remove(username);
    }

    public Map<String, Group> all() { return groups; }
}
