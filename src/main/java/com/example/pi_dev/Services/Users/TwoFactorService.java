package com.example.pi_dev.Services.Users;

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

public class TwoFactorService {

    private final GoogleAuthenticator gAuth;

    public TwoFactorService() {
        GoogleAuthenticatorConfig config = new GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder()
                .setTimeStepSizeInMillis(30000)
                .setWindowSize(3) // Allow 3 steps (3 * 30sec = 1.5 min) variance
                .setCodeDigits(6)
                .build();
        this.gAuth = new GoogleAuthenticator(config);
    }

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
        // Format: otpauth://totp/Issuer:AccountName?secret=SecretKey&issuer=Issuer
        return "otpauth://totp/" + issuer + ":" + accountName + "?secret=" + secretKey + "&issuer=" + issuer;
    }
}
