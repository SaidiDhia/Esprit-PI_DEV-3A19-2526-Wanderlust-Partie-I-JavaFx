package com.example.pi_dev.common.models;

import java.time.LocalDateTime;

public class ActivityLog {
    private int id;
    private String module;
    private String action;
    private String userId;
    private String userName;
    private String userAvatar;
    private String targetType;
    private String targetId;
    private String targetName;
    private String targetImage;
    private String content;
    private String destination;
    private String metadata;
    private LocalDateTime createdAt;

    public ActivityLog(int id, String module, String action, String userId, String userName, String userAvatar,
                      String targetType, String targetId, String targetName, String targetImage, String content,
                      String destination, String metadata, LocalDateTime createdAt) {
        this.id = id;
        this.module = module;
        this.action = action;
        this.userId = userId;
        this.userName = userName;
        this.userAvatar = userAvatar;
        this.targetType = targetType;
        this.targetId = targetId;
        this.targetName = targetName;
        this.targetImage = targetImage;
        this.content = content;
        this.destination = destination;
        this.metadata = metadata;
        this.createdAt = createdAt;
    }

    public ActivityLog(int id, String userEmail, String action, String details, LocalDateTime timestamp) {
        this(id, "GENERAL", action, null, userEmail, null, null, null, null, null, details, null, null, timestamp);
    }

    public int getId() { return id; }
    public String getModule() { return module; }
    public String getAction() { return action; }
    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getUserAvatar() { return userAvatar; }
    public String getTargetType() { return targetType; }
    public String getTargetId() { return targetId; }
    public String getTargetName() { return targetName; }
    public String getTargetImage() { return targetImage; }
    public String getContent() { return content; }
    public String getDestination() { return destination; }
    public String getMetadata() { return metadata; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // Legacy compatibility accessors used by the JavaFX UI
    public String getUserEmail() { return userName; }
    public String getDetails() { return content; }
    public LocalDateTime getTimestamp() { return createdAt; }
}
