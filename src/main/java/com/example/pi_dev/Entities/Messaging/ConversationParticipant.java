package com.example.pi_dev.Entities.Messaging;

import java.time.LocalDateTime;

public class ConversationParticipant {
    private long id;
    private long conversationId;
    private String userId;
    private String fullName;      // Field for user's full name
    private String email;          // Field for user's email
    private String role; // CREATOR, ADMIN, MEMBER
    private LocalDateTime joinedAt;
    private String addedBy;
    private boolean isActive;

    // Default constructor
    public ConversationParticipant() {}

    // Constructor with fields
    public ConversationParticipant(long conversationId, String userId, String role, String addedBy) {
        this.conversationId = conversationId;
        this.userId = userId;
        this.role = role;
        this.addedBy = addedBy;
        this.joinedAt = LocalDateTime.now();
        this.isActive = true;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getConversationId() { return conversationId; }
    public void setConversationId(long conversationId) { this.conversationId = conversationId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }

    public String getAddedBy() { return addedBy; }
    public void setAddedBy(String addedBy) { this.addedBy = addedBy; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    /**
     * Returns the display name for the participant
     * Uses full name if available, otherwise falls back to user ID
     */
    public String getDisplayName() {
        return (fullName != null && !fullName.isEmpty()) ? fullName : "User " + userId;
    }

    @Override
    public String toString() {
        return getDisplayName() + " (" + role + ")";
    }
}