package com.example.pi_dev.Entities.Messaging;

import java.time.LocalDateTime;

public class Message {

    // Enum for message status
    public enum Status {
        SENT, DELIVERED, READ, FAILED
    }

    // All fields
    private long id;
    private long conversationId;
    private String senderId;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime editedAt;

    // New fields for media
    private String messageType;  // TEXT, IMAGE, VIDEO, AUDIO, FILE
    private String fileUrl;
    private String thumbnailUrl;
    private long fileSize;
    private String fileName;
    private String mimeType;
    private Integer duration;     // For audio/video in seconds
    private Status status;

    // Default constructor
    public Message() {
        this.status = Status.SENT;
        this.messageType = "TEXT";
        this.createdAt = LocalDateTime.now();
    }

    // Constructor for text messages
    public Message(long conversationId, String senderId, String content) {
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.content = content;
        this.messageType = "TEXT";
        this.status = Status.SENT;
        this.createdAt = LocalDateTime.now();
    }

    // Constructor for media messages
    public Message(long conversationId, String senderId, String content, String messageType, String fileUrl) {
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.content = content;
        this.messageType = messageType;
        this.fileUrl = fileUrl;
        this.status = Status.SENT;
        this.createdAt = LocalDateTime.now();
    }

    // Full constructor
    public Message(long id, long conversationId, String senderId, String content,
                   LocalDateTime createdAt, String messageType, String fileUrl,
                   String thumbnailUrl, long fileSize, String fileName,
                   String mimeType, Integer duration, Status status) {
        this.id = id;
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.content = content;
        this.createdAt = createdAt;
        this.messageType = messageType;
        this.fileUrl = fileUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.fileSize = fileSize;
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.duration = duration;
        this.status = status;
    }

    // ========== Getters and Setters ==========

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getConversationId() { return conversationId; }
    public void setConversationId(long conversationId) { this.conversationId = conversationId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getEditedAt() { return editedAt; }
    public void setEditedAt(LocalDateTime editedAt) { this.editedAt = editedAt; }

    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    // ========== Helper Methods ==========

    public boolean isImage() {
        return "IMAGE".equals(messageType);
    }

    public boolean isVideo() {
        return "VIDEO".equals(messageType);
    }

    public boolean isAudio() {
        return "AUDIO".equals(messageType);
    }

    public boolean isFile() {
        return "FILE".equals(messageType);
    }

    public boolean isText() {
        return "TEXT".equals(messageType) || messageType == null;
    }

    public String getSenderName() {
        // This will be populated by UserRepository when needed
        return null;
    }

    public String getFormattedFileSize() {
        if (fileSize < 1024) return fileSize + " B";
        int exp = (int) (Math.log(fileSize) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.1f %sB", fileSize / Math.pow(1024, exp), pre);
    }

    @Override
    public String toString() {
        String prefix = "[" + createdAt.toLocalTime().withSecond(0) + "] User " + senderId + ": ";
        if (isImage()) {
            return prefix + "📷 Image" + (fileName != null ? " (" + fileName + ")" : "");
        } else if (isVideo()) {
            return prefix + "🎥 Video" + (fileName != null ? " (" + fileName + ")" : "");
        } else if (isAudio()) {
            return prefix + "🎵 Audio" + (duration != null ? " (" + duration + "s)" : "");
        } else if (isFile()) {
            return prefix + "📎 File" + (fileName != null ? " (" + fileName + ")" : "");
        } else {
            return prefix + content;
        }
    }
}