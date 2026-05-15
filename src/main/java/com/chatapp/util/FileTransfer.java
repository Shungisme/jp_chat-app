package com.chatapp.util;

import com.chatapp.model.Message;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public class FileTransfer {
    public static final int CHUNK_SIZE = 4096;

    // Encodes the entire file content as base64 in the message content.
    // For large files this should be chunked; kept simple for the demo.
    public static Message buildFileMessage(String sender, String target, Path file) throws IOException {
        byte[] data = Files.readAllBytes(file);
        String b64 = Base64.getEncoder().encodeToString(data);
        String content = file.getFileName().toString() + "|" + b64;
        return new Message(Message.Type.FILE, sender, target, content);
    }

    public static Path saveIncoming(Message msg, Path dir) throws IOException {
        Files.createDirectories(dir);
        String[] parts = msg.getContent().split("\\|", 2);
        if (parts.length != 2) throw new IOException("malformed FILE message");
        Path out = dir.resolve(parts[0]);
        Files.write(out, Base64.getDecoder().decode(parts[1]));
        return out;
    }
}
