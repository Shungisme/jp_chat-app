package com.chatapp.model;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

public class Group implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private String owner;
    private final Set<String> members = new LinkedHashSet<>();

    public Group() {}
    public Group(String name, String owner) {
        this.name = name;
        this.owner = owner;
        this.members.add(owner);
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public Set<String> getMembers() { return members; }

    public boolean add(String member) { return members.add(member); }
    public boolean remove(String member) { return members.remove(member); }
    public boolean contains(String member) { return members.contains(member); }
}
