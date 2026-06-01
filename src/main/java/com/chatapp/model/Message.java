package com.chatapp.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        LOGIN, REGISTER, CHAT, GROUP_CHAT, FILE, USER_LIST,
        GROUP_CREATE, GROUP_INVITE, GROUP_LIST, LOGOUT, KICKED, ACK, ERROR,
        GROUP_FILE, GROUP_VOICE, GROUP_VIDEO,
        GROUP_VOICE_JOIN, GROUP_VOICE_LEAVE, GROUP_VOICE_ROOM,
        GROUP_VOICE_START, GROUP_VOICE_END,
        GROUP_VIDEO_JOIN, GROUP_VIDEO_LEAVE,
        GROUP_VIDEO_START, GROUP_VIDEO_END,
        GROUP_QUERY, GROUP_INFO, GROUP_REMOVE, GROUP_REMOVED,
        VOICE_INVITE, VOICE_ACCEPT, VOICE_REJECT, VOICE, VOICE_END,
        VIDEO_INVITE, VIDEO_ACCEPT, VIDEO_REJECT, VIDEO, VIDEO_END
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
