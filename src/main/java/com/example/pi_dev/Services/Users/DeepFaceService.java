package com.example.pi_dev.Services.Users;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Service to communicate with the DeepFace Python API.
 * The API endpoint `/verify` accepts two images and returns a JSON payload
 * containing `{"verified": true}` if the faces in both images match.
 */
public class DeepFaceService {

    private static final String DEFAULT_BASE_URL = "http://localhost:5000";
    private final String baseUrl;

    public DeepFaceService() {
        String url = System.getenv("DEEPFACE_API_URL");
        this.baseUrl = (url != null && !url.isEmpty()) ? url.replaceAll("/$", "") : DEFAULT_BASE_URL;
    }

    public DeepFaceService(String baseUrl) {
        this.baseUrl = (baseUrl != null && !baseUrl.isEmpty()) ? baseUrl.replaceAll("/$", "") : DEFAULT_BASE_URL;
    }

    /**
     * Sends the configured 2FA photo and the login captured photo to the DeepFace API for comparison.
     *
     * @param storedImagePath The filesystem path of the photo saved during 2FA configuration.
     * @param capturedImageBytes The byte array of the photo just taken during login.
     * @return true if the DeepFace API confirms the faces match.
     */
    public boolean verify(String storedImagePath, byte[] capturedImageBytes) {
        // 1. Basic Validation
        if (storedImagePath == null || capturedImageBytes == null || capturedImageBytes.length == 0) {
            return false;
        }

        File storedFile = new File(storedImagePath);
        storedFile = storedFile.isAbsolute() ? storedFile : storedFile.getAbsoluteFile();
        if (!storedFile.exists()) {
            System.err.println("DeepFace: configured 2FA face image not found: " + storedFile.getAbsolutePath());
            return false;
        }

        try {
            // 2. Setup HTTP Request to DeepFace /verify endpoint
            String boundary = "----DeepFaceBoundary" + System.currentTimeMillis();
            URL url = new URL(baseUrl + "/verify");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000); // 15s connect timeout
            conn.setReadTimeout(60000);    // 60s processing timeout for DeepFace
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try (OutputStream out = conn.getOutputStream()) {
                byte[] crlf = "\r\n".getBytes(StandardCharsets.UTF_8);

                // 3. Attach Image 1 (img1): The reference photo configured during 2FA setup
                out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
                out.write("Content-Disposition: form-data; name=\"img1\"; filename=\"img1.png\"\r\n".getBytes(StandardCharsets.UTF_8));
                out.write("Content-Type: image/png\r\n\r\n".getBytes(StandardCharsets.UTF_8));
                Files.copy(storedFile.toPath(), out); // Read stored configuration file into request
                out.write(crlf);

                // 4. Attach Image 2 (img2): The live snapshot taken just now at login
                out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
                out.write("Content-Disposition: form-data; name=\"img2\"; filename=\"img2.png\"\r\n".getBytes(StandardCharsets.UTF_8));
                out.write("Content-Type: image/png\r\n\r\n".getBytes(StandardCharsets.UTF_8));
                out.write(capturedImageBytes); // Write captured bytes into request
                out.write(crlf);
                
                // End the multipart request
                out.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            }

            // 5. Check API Response status
            int code = conn.getResponseCode();
            if (code != 200) {
                System.err.println("DeepFace: API returned " + code + " (expected 200)");
                return false;
            }

            // 6. Parse JSON Response
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }
            String body = response.toString();
            
            // Expected JSON: {"verified": true, "distance": 0.xx, "model": "VGG-Face", ...}
            // We do a simple string check to see if verified is true.
            return body.contains("\"verified\":true") || body.contains("\"verified\": true");

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
