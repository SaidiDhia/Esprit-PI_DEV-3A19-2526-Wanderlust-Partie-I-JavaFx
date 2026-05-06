package com.example.pi_dev.Entities.Messaging;

import java.time.LocalDateTime;

public class Conversation {

    private long id;
    private String type;          // PERSONAL / GROUP
    private String contextType;   // EVENT / POST / STORE
    private long contextId;
    private LocalDateTime createdAt;
    private String name;
    private String createdBy;      // New field
    private Long lastMessageId;     // New field
    private LocalDateTime lastActivity; // New field
    private boolean isArchived;      // New field
    private boolean isPinned;        // New field
    private boolean muteNotifications; // New field

    public Conversation() {}

    public Conversation(long id, String type, String contextType, long contextId, LocalDateTime createdAt) {
        this.id = id;
        this.type = type;
        this.contextType = contextType;
        this.contextId = contextId;
        this.createdAt = createdAt;
    }

    // Getters & Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getContextType() { return contextType; }
    public void setContextType(String contextType) { this.contextType = contextType; }

    public long getContextId() { return contextId; }
    public void setContextId(long contextId) { this.contextId = contextId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Long getLastMessageId() { return lastMessageId; }
    public void setLastMessageId(Long lastMessageId) { this.lastMessageId = lastMessageId; }

    public LocalDateTime getLastActivity() { return lastActivity; }
    public void setLastActivity(LocalDateTime lastActivity) { this.lastActivity = lastActivity; }

    public boolean isArchived() { return isArchived; }
    public void setArchived(boolean archived) { isArchived = archived; }

    public boolean isPinned() { return isPinned; }
    public void setPinned(boolean pinned) { isPinned = pinned; }

    public boolean isMuteNotifications() { return muteNotifications; }
    public void setMuteNotifications(boolean muteNotifications) { this.muteNotifications = muteNotifications; }

    @Override
    public String toString() {
        return type + " - " + (name != null ? name : "Conversation " + id);
    }
}