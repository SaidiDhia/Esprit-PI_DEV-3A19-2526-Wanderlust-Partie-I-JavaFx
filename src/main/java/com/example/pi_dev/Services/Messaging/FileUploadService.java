package com.example.pi_dev.Services.Messaging;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FileUploadService {

    private static final String UPLOAD_DIR = "uploads/";
    private static final String THUMBNAIL_DIR = "uploads/thumbnails/";

    public FileUploadService() {
        // Create directories if they don't exist
        new File(UPLOAD_DIR).mkdirs();
        new File(THUMBNAIL_DIR).mkdirs();
    }

    /**
     * Save uploaded file to disk and return the file path
     */
    public String saveFile(File sourceFile, String messageType) throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String originalFileName = sourceFile.getName();
        String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        String newFileName = timestamp + "_" + System.currentTimeMillis() + extension;

        Path targetPath = Paths.get(UPLOAD_DIR + newFileName);
        Files.copy(sourceFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        return targetPath.toString();
    }

    /**
     * Generate a thumbnail for images (simplified version)
     * In a real app, you'd use ImageIO to resize the image
     */
    public String generateThumbnail(String imagePath) throws IOException {
        // For now, just return the original path
        // In a real implementation, you'd create a smaller version
        return imagePath;
    }

    /**
     * Get file size in readable format
     */
    public String getFileSize(File file) {
        long bytes = file.length();
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}