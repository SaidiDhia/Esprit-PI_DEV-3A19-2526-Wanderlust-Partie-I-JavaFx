package com.example.pi_dev.Entities.Users;

import java.util.UUID;

public class TFASecret {
    private UUID userId;
    private String secretKey;
    private String qrCode;

    public TFASecret() {}

    public TFASecret(UUID userId, String secretKey, String qrCode) {
        this.userId = userId;
        this.secretKey = secretKey;
        this.qrCode = qrCode;
    }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }
}
