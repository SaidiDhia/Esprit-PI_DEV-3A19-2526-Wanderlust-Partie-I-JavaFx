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

    // TFA Advanced Fields
    private String tfaSecret;
    private String faceReferenceImage;

    // Security Fields
    private LocalDateTime lastLogin;
    private Integer failedAttempts;
    private Double accountRiskLevel;
    private Integer loginCount;

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
        this.failedAttempts = 0;
        this.accountRiskLevel = 0.0;
        this.loginCount = 0;
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

    // TFA Advanced Getters and Setters
    public String getTfaSecret() { return tfaSecret; }
    public void setTfaSecret(String tfaSecret) { this.tfaSecret = tfaSecret; }

    public String getFaceReferenceImage() { return faceReferenceImage; }
    public void setFaceReferenceImage(String faceReferenceImage) { this.faceReferenceImage = faceReferenceImage; }

    // Security Getters and Setters
    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }

    public Integer getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(Integer failedAttempts) { this.failedAttempts = failedAttempts; }

    public Double getAccountRiskLevel() { return accountRiskLevel; }
    public void setAccountRiskLevel(Double accountRiskLevel) { this.accountRiskLevel = accountRiskLevel; }

    public Integer getLoginCount() { return loginCount; }
    public void setLoginCount(Integer loginCount) { this.loginCount = loginCount; }
}
