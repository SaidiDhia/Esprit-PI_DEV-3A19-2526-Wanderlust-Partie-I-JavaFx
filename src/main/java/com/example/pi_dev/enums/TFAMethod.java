package com.example.pi_dev.enums;

public enum TFAMethod {
    NONE("None"),
    EMAIL("Email"),
    SMS("SMS"),
    WHATSAPP("WhatsApp"),
    QR("Google Authenticator"),
    GOOGLE_AUTHENTICATOR("Google Authenticator"),
    FACE_ID("Face Recognition");

    private final String displayName;

    TFAMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
