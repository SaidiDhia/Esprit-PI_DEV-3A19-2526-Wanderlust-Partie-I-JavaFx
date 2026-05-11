package com.example.pi_dev.Services.Users;

import com.example.pi_dev.enums.TFAMethod;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Enhanced TwoFactorService - Supports multiple 2FA methods
 * - Google Authenticator (TOTP)
 * - Email verification codes
 * - SMS verification codes
 * - WhatsApp verification codes
 * - Face recognition verification
 */
public class TwoFactorService {

    private final GoogleAuthenticator gAuth;
    private final IEmailService emailService;
    private final SmsService smsService;
    private final IFaceVerificationService faceVerificationService;
    private final SecureRandom secureRandom;

    // Session management for verification codes
    private final Map<String, CodeSession> activeSessions = new HashMap<>();
    private static final int CODE_VALIDITY_SECONDS = 600; // 10 minutes
    private static final int CODE_LENGTH = 6;

    public TwoFactorService() {
        this(new EmailService(), new SmsServiceImpl(), new FaceVerificationServiceImpl());
    }

    public TwoFactorService(IEmailService emailService, SmsService smsService,
                           IFaceVerificationService faceVerificationService) {
        GoogleAuthenticatorConfig config = new GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder()
                .setTimeStepSizeInMillis(30000)
                .setWindowSize(3)
                .setCodeDigits(6)
                .build();
        this.gAuth = new GoogleAuthenticator(config);
        this.emailService = emailService;
        this.smsService = smsService;
        this.faceVerificationService = faceVerificationService;
        this.secureRandom = new SecureRandom();
    }

    // ==================== GOOGLE AUTHENTICATOR ====================

    public String generateSecretKey() {
        final GoogleAuthenticatorKey key = gAuth.createCredentials();
        return key.getKey();
    }

    public boolean validateCode(String secretKey, int code) {
        return gAuth.authorize(secretKey, code);
    }

    public byte[] generateQRCodeImage(String barcodeText) throws WriterException, IOException {
        MultiFormatWriter barcodeWriter = new MultiFormatWriter();
        BitMatrix bitMatrix = barcodeWriter.encode(barcodeText, BarcodeFormat.QR_CODE, 200, 200);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "png", baos);
        return baos.toByteArray();
    }

    public String getGoogleAuthenticatorBarCode(String secretKey, String accountName, String issuer) {
        return "otpauth://totp/" + issuer + ":" + accountName + "?secret=" + secretKey + "&issuer=" + issuer;
    }

    // ==================== EMAIL VERIFICATION ====================

    /**
     * Issue and send verification code via email
     */
    public boolean issueEmailCode(String email, String userName) {
        String code = generateVerificationCode();
        String sessionKey = "email_" + email;

        // Save to session
        activeSessions.put(sessionKey, new CodeSession(code, Instant.now().getEpochSecond()));

        // Send via email
        return emailService != null && emailService.sendVerificationCode(email, userName, code);
    }

    /**
     * Verify email code
     */
    public boolean verifyEmailCode(String email, String code) {
        String sessionKey = "email_" + email;
        CodeSession session = activeSessions.get(sessionKey);

        if (session == null || !session.isValid()) {
            activeSessions.remove(sessionKey);
            return false;
        }

        boolean valid = session.code.equals(code);
        if (valid) {
            activeSessions.remove(sessionKey);
        }
        return valid;
    }

    // ==================== SMS VERIFICATION ====================

    /**
     * Issue and send verification code via SMS
     */
    public boolean issueSmsCode(String phoneNumber) {
        String code = generateVerificationCode();
        String sessionKey = "sms_" + phoneNumber;

        activeSessions.put(sessionKey, new CodeSession(code, Instant.now().getEpochSecond()));

        return smsService != null && smsService.sendVerificationCode(phoneNumber, code);
    }

    /**
     * Verify SMS code
     */
    public boolean verifySmsCode(String phoneNumber, String code) {
        String sessionKey = "sms_" + phoneNumber;
        CodeSession session = activeSessions.get(sessionKey);

        if (session == null || !session.isValid()) {
            activeSessions.remove(sessionKey);
            return false;
        }

        boolean valid = session.code.equals(code);
        if (valid) {
            activeSessions.remove(sessionKey);
        }
        return valid;
    }

    // ==================== WHATSAPP VERIFICATION ====================

    /**
     * Issue and send verification code via WhatsApp
     */
    public boolean issueWhatsAppCode(String phoneNumber) {
        String code = generateVerificationCode();
        String sessionKey = "whatsapp_" + phoneNumber;

        activeSessions.put(sessionKey, new CodeSession(code, Instant.now().getEpochSecond()));

        return smsService != null && smsService.sendWhatsAppCode(phoneNumber, code);
    }

    /**
     * Verify WhatsApp code
     */
    public boolean verifyWhatsAppCode(String phoneNumber, String code) {
        String sessionKey = "whatsapp_" + phoneNumber;
        CodeSession session = activeSessions.get(sessionKey);

        if (session == null || !session.isValid()) {
            activeSessions.remove(sessionKey);
            return false;
        }

        boolean valid = session.code.equals(code);
        if (valid) {
            activeSessions.remove(sessionKey);
        }
        return valid;
    }

    // ==================== FACE VERIFICATION ====================

    /**
     * Verify face image against stored reference
     */
    public boolean verifyFace(String selfieBase64, String referencePath) {
        return faceVerificationService != null &&
               faceVerificationService.verifyBase64SelfieAgainstReference(selfieBase64, referencePath);
    }

    /**
     * Store reference face image for future verification
     */
    public boolean storeReferenceFace(String faceImagePath) {
        return faceVerificationService != null &&
               faceVerificationService.validateFaceImage(faceImagePath);
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Generate random verification code
     */
    private String generateVerificationCode() {
        int code = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(code);
    }

    /**
     * Check if code can be resent (rate limiting)
     */
    public boolean canResendCode(String identifier, long minSecondsBetween) {
        String sessionKey = "resend_" + identifier;
        Object lastSentTimeObj = activeSessions.get(sessionKey);

        if (lastSentTimeObj == null) {
            return true;
        }

        // Extract the timestamp from CodeSession
        if (lastSentTimeObj instanceof CodeSession) {
            CodeSession session = (CodeSession) lastSentTimeObj;
            return (Instant.now().getEpochSecond() - session.issuedAt) >= minSecondsBetween;
        }

        return true;
    }

    /**
     * Mark resend time
     */
    public void markResendTime(String identifier) {
        String sessionKey = "resend_" + identifier;
        activeSessions.put(sessionKey, new CodeSession(null, Instant.now().getEpochSecond()));
    }

    /**
     * Get TFA method display name
     */
    public String getTfaMethodName(TFAMethod method) {
        return method != null ? method.getDisplayName() : "None";
    }

    // ==================== INNER CLASSES ====================

    /**
     * Code session holder for expiration tracking
     */
    public static class CodeSession {
        public String code;
        public long issuedAt;

        public CodeSession(String code, long issuedAt) {
            this.code = code;
            this.issuedAt = issuedAt;
        }

        public boolean isValid() {
            long elapsedSeconds = Instant.now().getEpochSecond() - issuedAt;
            return elapsedSeconds < CODE_VALIDITY_SECONDS;
        }
    }
}
