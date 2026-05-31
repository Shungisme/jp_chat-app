package com.chatapp.client;

import java.util.Objects;

// Display item for the "Nhóm của bạn" list. toString returns just the
// name so the JList shows what the user expects, while id stays unique.
public record GroupEntry(String id, String name) {
    @Override public String toString() { return name; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GroupEntry e)) return false;
        return Objects.equals(id, e.id);
    }

    @Override public int hashCode() { return Objects.hash(id); }
}
