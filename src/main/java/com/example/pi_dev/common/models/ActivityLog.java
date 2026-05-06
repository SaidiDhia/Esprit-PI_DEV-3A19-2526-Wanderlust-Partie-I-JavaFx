package com.example.pi_dev.common.models;

import java.time.LocalDateTime;

public class ActivityLog {
    private int id;
    private String userEmail;
    private String action;
    private String details;
    private LocalDateTime timestamp;

    public ActivityLog(int id, String userEmail, String action, String details, LocalDateTime timestamp) {
        this.id = id;
        this.userEmail = userEmail;
        this.action = action;
        this.details = details;
        this.timestamp = timestamp;
    }

    public int getId() { return id; }
    public String getUserEmail() { return userEmail; }
    public String getAction() { return action; }
    public String getDetails() { return details; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
