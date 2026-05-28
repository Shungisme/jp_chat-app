package com.chatapp.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        LOGIN, REGISTER, CHAT, GROUP_CHAT, FILE, USER_LIST,
        GROUP_CREATE, GROUP_INVITE, LOGOUT, ACK, ERROR,
        VOICE, VOICE_END, VIDEO, VIDEO_END
    }

    private Type type;
    private String sender;
    private String target;
    private String content;
    private long timestamp;

    public Message() {}

    public Message(Type type, String sender, String target, String content) {
        this.type = type;
        this.sender = sender;
        this.target = target;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "[" + type + "] " + sender + " -> " + target + ": " + content;
    }
}
