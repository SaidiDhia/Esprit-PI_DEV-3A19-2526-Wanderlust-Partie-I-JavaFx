package com.example.pi_dev.Entities.Users;

import java.time.LocalDateTime;
import java.util.UUID;

public class PasswordResetToken {
    private UUID tokenId;
    private UUID userId;
    private String token;
    private LocalDateTime expiresAt;

    public PasswordResetToken() {}

    public PasswordResetToken(UUID tokenId, UUID userId, String token, LocalDateTime expiresAt) {
        this.tokenId = tokenId;
        this.userId = userId;
        this.token = token;
        this.expiresAt = expiresAt;
    }

    public UUID getTokenId() { return tokenId; }
    public void setTokenId(UUID tokenId) { this.tokenId = tokenId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
