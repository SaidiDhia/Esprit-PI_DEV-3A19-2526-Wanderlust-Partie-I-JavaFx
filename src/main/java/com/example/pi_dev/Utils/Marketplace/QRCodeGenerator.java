package com.example.pi_dev.Utils.Marketplace;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class QRCodeGenerator {

    /**
     * Generates a QR code as a byte array (PNG format)
     * @param content - the text to encode in the QR code
     * @param width   - width in pixels
     * @param height  - height in pixels
     * @return PNG image as byte array
     */
    public static byte[] generateQRCode(String content, int width, int height)
            throws WriterException, IOException {

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

        return outputStream.toByteArray();
    }
}
