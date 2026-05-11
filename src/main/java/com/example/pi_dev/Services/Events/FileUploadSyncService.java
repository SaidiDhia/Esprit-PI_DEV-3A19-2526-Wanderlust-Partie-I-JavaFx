package com.example.pi_dev.Services.Events;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.Map;

/**
 * FileUploadSyncService - Synchronizes event file uploads between Java and Symfony projects
 * Automatically uploads event images and promotional materials to Symfony when they are uploaded in Java
 * Also provides fallback to fetch files from Symfony directory if not found locally
 */
public class FileUploadSyncService {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final String symphonyApiUrl;
    private final String jwtToken;
    private final HttpClient httpClient;
    private static final int RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1000;
    
    // Local upload directories
    private static final String JAVA_UPLOAD_DIR = "uploads";
    private static final String SYMFONY_UPLOAD_DIR = "uploadssymfony";

    public FileUploadSyncService(String symphonyApiUrl, String jwtToken) {
        this.symphonyApiUrl = symphonyApiUrl;
        this.jwtToken = jwtToken;
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Upload event image to both local and Symfony storage
     * @param eventId Event ID
     * @param imageFile Image file to upload
     * @return true if successful, false otherwise
     */
    public boolean uploadEventImage(int eventId, File imageFile) {
        if (!imageFile.exists()) {
            System.err.println("Event image file not found: " + imageFile.getAbsolutePath());
            return false;
        }

        try {
            byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            return syncEventImageToSymfony(eventId, base64Image, imageFile.getName());
        } catch (Exception e) {
            System.err.println("Error uploading event image: " + e.getMessage());
            return false;
        }
    }

    /**
     * Upload multiple event images
     * @param eventId Event ID
     * @param imageFiles Array of image files
     * @return true if all successful, false otherwise
     */
    public boolean uploadEventImages(int eventId, File[] imageFiles) {
        boolean allSuccess = true;
        for (File imageFile : imageFiles) {
            if (!uploadEventImage(eventId, imageFile)) {
                allSuccess = false;
            }
        }
        return allSuccess;
    }

    /**
     * Upload activity image to both local and Symfony storage
     * @param activityId Activity ID
     * @param imageFile Image file to upload
     * @return true if successful, false otherwise
     */
    public boolean uploadActivityImage(int activityId, File imageFile) {
        if (!imageFile.exists()) {
            System.err.println("Activity image file not found: " + imageFile.getAbsolutePath());
            return false;
        }

        try {
            byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            return syncActivityImageToSymfony(activityId, base64Image, imageFile.getName());
        } catch (Exception e) {
            System.err.println("Error uploading activity image: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get file from local storage, or fetch from Symfony if not found
     * @param storedPath Path stored in database
     * @return File if found or fetched successfully, null otherwise
     */
    public File getFileWithFallback(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return null;
        }

        String normalized = storedPath.trim().replace('\\', '/');

        // Check local storage first
        File localFile = resolveLocalFile(normalized);
        if (localFile != null && localFile.exists()) {
            return localFile;
        }

        // Try to fetch from Symfony storage
        return fetchFromSymfonyStorage(normalized);
    }

    /**
     * Resolve local file path
     */
    private File resolveLocalFile(String storedPath) {
        String normalized = storedPath.trim().replace('\\', '/');

        if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
            return null;
        }

        if (normalized.startsWith("/uploads/")) {
            normalized = "uploads/" + normalized.substring("/uploads/".length());
        } else if (normalized.startsWith("uploads/")) {
            // already normalized
        } else {
            int uploadsIndex = normalized.indexOf("/uploads/");
            if (uploadsIndex >= 0) {
                normalized = "uploads/" + normalized.substring(uploadsIndex + "/uploads/".length());
            }
        }

        return new File(normalized);
    }

    /**
     * Fetch file from Symfony storage and save locally
     */
    private File fetchFromSymfonyStorage(String storedPath) {
        try {
            // Extract filename from path
            String fileName = storedPath.substring(storedPath.lastIndexOf("/") + 1);
            File symphonyFile = new File(SYMFONY_UPLOAD_DIR, fileName);

            if (symphonyFile.exists()) {
                // Copy from Symfony to Java uploads directory
                File javaFile = new File(JAVA_UPLOAD_DIR, fileName);
                Files.copy(symphonyFile.toPath(), javaFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                return javaFile;
            }
        } catch (Exception e) {
            System.err.println("Error fetching file from Symfony storage: " + e.getMessage());
        }

        return null;
    }

    /**
     * Sync event image to Symfony backend with retry logic
     */
    private boolean syncEventImageToSymfony(int eventId, String base64Image, String fileName) {
        return retrySync(() -> {
            String url = normalizeUrl(symphonyApiUrl) + "/api/upload/event-image";

            Map<String, Object> payload = Map.of(
                "eventId", eventId,
                "image", base64Image,
                "fileName", fileName
            );

            return sendToSymfony(url, payload);
        });
    }

    /**
     * Sync activity image to Symfony backend with retry logic
     */
    private boolean syncActivityImageToSymfony(int activityId, String base64Image, String fileName) {
        return retrySync(() -> {
            String url = normalizeUrl(symphonyApiUrl) + "/api/upload/activity-image";

            Map<String, Object> payload = Map.of(
                "activityId", activityId,
                "image", base64Image,
                "fileName", fileName
            );

            return sendToSymfony(url, payload);
        });
    }

    /**
     * Send request to Symfony API
     */
    private boolean sendToSymfony(String url, Map<String, Object> payload) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(java.time.Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + jwtToken)
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            boolean success = response.statusCode() >= 200 && response.statusCode() < 300;
            if (!success) {
                System.err.println("Symfony API error: " + response.statusCode() + " - " + response.body());
            }
            return success;
        } catch (Exception e) {
            System.err.println("Error communicating with Symfony: " + e.getMessage());
            return false;
        }
    }

    /**
     * Retry logic for network failures
     */
    private boolean retrySync(SyncOperation operation) {
        for (int attempt = 1; attempt <= RETRY_ATTEMPTS; attempt++) {
            try {
                if (operation.execute()) {
                    return true;
                }
            } catch (Exception e) {
                System.err.println("Sync attempt " + attempt + " failed: " + e.getMessage());
            }

            if (attempt < RETRY_ATTEMPTS) {
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }

        return false;
    }

    private String normalizeUrl(String url) {
        return url.replaceAll("/*$", "");
    }

    @FunctionalInterface
    private interface SyncOperation {
        boolean execute() throws Exception;
    }
}
