package com.example.pi_dev.Entities.Messaging;

import java.time.LocalDateTime;

public class Reaction {
    private long id;
    private long messageId;
    private String userId;
    private String reaction; // 👍 ❤️ 😂 etc.
    private LocalDateTime createdAt;
    private String userFullName; // For display

    // Empty constructor
    public Reaction() {}

    // Constructor without id (for insert)
    public Reaction(long messageId, String userId, String reaction, LocalDateTime createdAt, String userFullName) {
        this.messageId = messageId;
        this.userId = userId;
        this.reaction = reaction;
        this.createdAt = createdAt;
        this.userFullName = userFullName;
    }

    // Full constructor
    public Reaction(long id, long messageId, String userId, String reaction, LocalDateTime createdAt, String userFullName) {
        this.id = id;
        this.messageId = messageId;
        this.userId = userId;
        this.reaction = reaction;
        this.createdAt = createdAt;
        this.userFullName = userFullName;
    }

    // Getters & Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getMessageId() { return messageId; }
    public void setMessageId(long messageId) { this.messageId = messageId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getReaction() { return reaction; }
    public void setReaction(String reaction) { this.reaction = reaction; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getUserFullName() { return userFullName; }
    public void setUserFullName(String userFullName) { this.userFullName = userFullName; }

    // toString for debugging
    @Override
    public String toString() {
        return "Reaction{" +
                "id=" + id +
                ", messageId=" + messageId +
                ", userId='" + userId + '\'' +
                ", reaction='" + reaction + '\'' +
                ", createdAt=" + createdAt +
                ", userFullName='" + userFullName + '\'' +
                '}';
    }
}