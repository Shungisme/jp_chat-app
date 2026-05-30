package com.chatapp.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

// Persistent list of saved servers on the client side.
// Format: one server per line — "name|host|port".
public class ServerConfig {
    private static final Path FILE = Path.of("data", "servers.txt");

    public static final class Entry {
        public final String name;
        public final String host;
        public final int port;

        public Entry(String name, String host, int port) {
            this.name = name;
            this.host = host;
            this.port = port;
        }

        public String label() { return name + " (" + host + ":" + port + ")"; }
        @Override public String toString() { return label(); }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Entry e)) return false;
            return port == e.port && Objects.equals(host, e.host) && Objects.equals(name, e.name);
        }
        @Override public int hashCode() { return Objects.hash(name, host, port); }
    }

    public static synchronized List<Entry> load() {
        List<Entry> out = new ArrayList<>();
        try {
            if (!Files.exists(FILE)) {
                out.add(new Entry("Local", "127.0.0.1", 9999));
                save(out);
                return out;
            }
            for (String line : Files.readAllLines(FILE, StandardCharsets.UTF_8)) {
                if (line == null || line.isBlank()) continue;
                String[] parts = line.split("\\|", 3);
                if (parts.length != 3) continue;
                try {
                    out.add(new Entry(parts[0].trim(), parts[1].trim(), Integer.parseInt(parts[2].trim())));
                } catch (NumberFormatException ignored) {}
            }
        } catch (IOException e) {
            System.err.println("[ServerConfig] load failed: " + e.getMessage());
        }
        if (out.isEmpty()) out.add(new Entry("Local", "127.0.0.1", 9999));
        return out;
    }

    public static synchronized void save(List<Entry> entries) {
        try {
            Files.createDirectories(FILE.getParent());
            StringBuilder sb = new StringBuilder();
            for (Entry e : entries) {
                sb.append(e.name).append("|").append(e.host).append("|").append(e.port).append("\n");
            }
            Files.writeString(FILE, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[ServerConfig] save failed: " + e.getMessage());
        }
    }

    public static synchronized void add(Entry entry) {
        List<Entry> all = load();
        all.add(entry);
        save(all);
    }

    public static synchronized void update(int index, Entry entry) {
        List<Entry> all = load();
        if (index < 0 || index >= all.size()) return;
        all.set(index, entry);
        save(all);
    }

    public static synchronized void delete(int index) {
        List<Entry> all = load();
        if (index < 0 || index >= all.size()) return;
        all.remove(index);
        if (all.isEmpty()) all.add(new Entry("Local", "127.0.0.1", 9999));
        save(all);
    }

    public static List<Entry> unmodifiable(List<Entry> entries) {
        return Collections.unmodifiableList(entries);
    }
}
