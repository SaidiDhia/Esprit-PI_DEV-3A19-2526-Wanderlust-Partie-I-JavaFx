package com.example.pi_dev.Entities.Users;

import com.example.pi_dev.enums.RoleEnum;
import com.example.pi_dev.enums.TFAMethod;

import java.time.LocalDateTime;
import java.util.UUID;

public class User {
    private UUID userId;
    private String email;
    private String passwordHash;
    private String fullName;
    private String phoneNumber;
    private Boolean isActive;
    private RoleEnum role;
    private TFAMethod tfaMethod;
    private LocalDateTime createdAt;
    private String profilePicture;

    public User() {}

    public User(UUID userId, String email, String passwordHash, String fullName, String phoneNumber, Boolean isActive, RoleEnum role, TFAMethod tfaMethod, LocalDateTime createdAt, String profilePicture) {
        this.userId = userId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.isActive = isActive;
        this.role = role;
        this.tfaMethod = tfaMethod;
        this.createdAt = createdAt;
        this.profilePicture = profilePicture;
    }

    // Getters and Setters
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public RoleEnum getRole() { return role; }
    public void setRole(RoleEnum role) { this.role = role; }

    public TFAMethod getTfaMethod() { return tfaMethod; }
    public void setTfaMethod(TFAMethod tfaMethod) { this.tfaMethod = tfaMethod; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getProfilePicture() { return profilePicture; }
    public void setProfilePicture(String profilePicture) { this.profilePicture = profilePicture; }
}
