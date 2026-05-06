package com.example.pi_dev.Services.Users;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TwoFactorServiceTest {

    private final TwoFactorService twoFactorService = new TwoFactorService();

    @Test
    void generateSecretKey() {
        String secretKey = twoFactorService.generateSecretKey();
        assertNotNull(secretKey, "Secret key should not be null");
        assertFalse(secretKey.isEmpty(), "Secret key should not be empty");
    }

    @Test
    void validateCode() {
        String secretKey = twoFactorService.generateSecretKey();
        // We can't easily test valid code without knowing the current time and secret
        // but we can test that an invalid code fails
        assertFalse(twoFactorService.validateCode(secretKey, 123456), "Invalid code should fail");
    }

    @Test
    void generateQRCodeImage() throws Exception {
        String barcodeText = "otpauth://totp/PI_DEV_APP:test@example.com?secret=JBSWY3DPEHPK3PXP&issuer=PI_DEV_APP";
        byte[] qrCode = twoFactorService.generateQRCodeImage(barcodeText);
        assertNotNull(qrCode, "QR code should not be null");
        assertTrue(qrCode.length > 0, "QR code should not be empty");
    }

    @Test
    void getGoogleAuthenticatorBarCode() {
        String secretKey = "SECRET_KEY";
        String accountName = "test@example.com";
        String issuer = "PI_DEV_APP";
        String barcode = twoFactorService.getGoogleAuthenticatorBarCode(secretKey, accountName, issuer);
        
        assertNotNull(barcode, "Barcode should not be null");
        assertTrue(barcode.contains("otpauth://totp/"), "Barcode should follow TOTP format");
        assertTrue(barcode.contains("secret=" + secretKey), "Barcode should contain secret key");
        assertTrue(barcode.contains("issuer=" + issuer), "Barcode should contain issuer");
    }
}