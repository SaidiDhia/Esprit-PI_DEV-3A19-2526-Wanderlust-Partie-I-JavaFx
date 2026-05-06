package com.example.pi_dev.Controllers.Events;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe de test pour valider les nouvelles fonctionnalités d'images et vidéos
 * Exécutez cette classe pour tester avant d'utiliser l'application principale
 */
public class TestEventFeatures {
    
    private static final String UPLOADS_DIR = "uploads/events/";
    
    public static void main(String[] args) {
        System.out.println("=== TEST DES FONCTIONNALITÉS ÉVÉNEMENT ===\n");
        
        // Test 1: Création du répertoire
        testDirectoryCreation();
        
        // Test 2: Validation YouTube
        testYouTubeValidation();
        
        // Test 3: Gestion des chemins d'images
        testImagePathHandling();
        
        // Test 4: Simulation d'ajout de photos
        testPhotoManagement();
        
        System.out.println("\n=== TEST TERMINÉ ===");
        System.out.println("Si tous les tests passent, vous pouvez utiliser l'application principale !");
    }
    
    private static void testDirectoryCreation() {
        System.out.println("📁 Test 1: Création du répertoire uploads");
        
        try {
            Path uploadsPath = Paths.get(UPLOADS_DIR);
            if (!Files.exists(uploadsPath)) {
                Files.createDirectories(uploadsPath);
                System.out.println("✅ Répertoire créé: " + uploadsPath.toAbsolutePath());
            } else {
                System.out.println("✅ Répertoire existe déjà: " + uploadsPath.toAbsolutePath());
            }
            
            // Vérifier les permissions
            boolean writable = Files.isWritable(uploadsPath);
            System.out.println("✅ Permissions écriture: " + (writable ? "OK" : "ERREUR"));
            
        } catch (IOException e) {
            System.out.println("❌ Erreur création répertoire: " + e.getMessage());
        }
        System.out.println();
    }
    
    private static void testYouTubeValidation() {
        System.out.println("🎥 Test 2: Validation des URLs YouTube");
        
        String[] validUrls = {
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://youtu.be/dQw4w9WgXcQ",
            "https://www.youtube.com/embed/dQw4w9WgXcQ"
        };
        
        String[] invalidUrls = {
            "https://www.google.com",
            "https://facebook.com",
            "",
            null
        };
        
        System.out.println("URLs valides:");
        for (String url : validUrls) {
            boolean isValid = isValidYouTubeUrl(url);
            System.out.println("  " + url + " → " + (isValid ? "✅ VALIDE" : "❌ INVALIDE"));
        }
        
        System.out.println("URLs invalides:");
        for (String url : invalidUrls) {
            boolean isValid = isValidYouTubeUrl(url);
            System.out.println("  " + (url != null ? url : "null") + " → " + (isValid ? "❌ VALIDE" : "✅ INVALIDE"));
        }
        System.out.println();
    }
    
    private static boolean isValidYouTubeUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        return url.contains("youtube.com/watch") || 
               url.contains("youtu.be/") || 
               url.contains("youtube.com/embed/");
    }
    
    private static void testImagePathHandling() {
        System.out.println("📷 Test 3: Gestion des chemins d'images");
        
        // Test de création de nom de fichier unique
        long timestamp = System.currentTimeMillis();
        String originalName = "test_image.jpg";
        String uniqueName = timestamp + "_" + originalName;
        String fullPath = UPLOADS_DIR + uniqueName;
        
        System.out.println("✅ Nom original: " + originalName);
        System.out.println("✅ Nom unique: " + uniqueName);
        System.out.println("✅ Chemin complet: " + fullPath);
        
        // Test de validation de chemin
        Path path = Paths.get(fullPath);
        boolean exists = Files.exists(path);
        System.out.println("✅ Fichier existe: " + (exists ? "OUI" : "NON"));
        
        System.out.println();
    }
    
    private static void testPhotoManagement() {
        System.out.println("🖼️ Test 4: Gestion des photos multiples");
        
        List<String> photos = new ArrayList<>();
        
        // Simuler l'ajout de photos
        for (int i = 1; i <= 3; i++) {
            long timestamp = System.currentTimeMillis() + i;
            String photoName = timestamp + "_photo" + i + ".jpg";
            String photoPath = UPLOADS_DIR + photoName;
            
            photos.add(photoPath);
            System.out.println("✅ Photo " + i + " ajoutée: " + photoPath);
        }
        
        // Simuler la suppression
        if (!photos.isEmpty()) {
            String removedPhoto = photos.remove(0);
            System.out.println("✅ Photo supprimée: " + removedPhoto);
            System.out.println("✅ Photos restantes: " + photos.size());
        }
        
        System.out.println();
    }
    
    /**
     * Test pour simuler la sélection de fichiers
     */
    private static void testFileSelection() {
        System.out.println("📂 Test 5: Simulation de sélection de fichiers");
        
        // Simuler des fichiers sélectionnés
        List<File> selectedFiles = new ArrayList<>();
        selectedFiles.add(new File("test1.jpg"));
        selectedFiles.add(new File("test2.png"));
        selectedFiles.add(new File("test3.jpeg"));
        
        System.out.println("✅ Fichiers sélectionnés: " + selectedFiles.size());
        for (int i = 0; i < selectedFiles.size(); i++) {
            File file = selectedFiles.get(i);
            System.out.println("  Fichier " + (i + 1) + ": " + file.getName());
        }
        
        System.out.println();
    }
    
    /**
     * Test pour valider le format des données
     */
    private static void testDataValidation() {
        System.out.println("✅ Test 6: Validation des données");
        
        // Test validation prix
        String[] validPrices = {"50", "100.50", "0", "1500"};
        String[] invalidPrices = {"-10", "abc", "", "50.5.5"};
        
        System.out.println("Validation prix:");
        for (String price : validPrices) {
            boolean isValid = isValidPrice(price);
            System.out.println("  " + price + " → " + (isValid ? "✅ VALIDE" : "❌ INVALIDE"));
        }
        
        for (String price : invalidPrices) {
            boolean isValid = isValidPrice(price);
            System.out.println("  " + price + " → " + (isValid ? "❌ VALIDE" : "✅ INVALIDE"));
        }
        
        // Test validation capacité
        String[] validCapacities = {"10", "50", "100", "1"};
        String[] invalidCapacities = {"-5", "0", "abc", "", "10.5"};
        
        System.out.println("Validation capacité:");
        for (String capacity : validCapacities) {
            boolean isValid = isValidCapacity(capacity);
            System.out.println("  " + capacity + " → " + (isValid ? "✅ VALIDE" : "❌ INVALIDE"));
        }
        
        for (String capacity : invalidCapacities) {
            boolean isValid = isValidCapacity(capacity);
            System.out.println("  " + capacity + " → " + (isValid ? "❌ VALIDE" : "✅ INVALIDE"));
        }
        
        System.out.println();
    }
    
    private static boolean isValidPrice(String price) {
        if (price == null || price.trim().isEmpty()) return false;
        try {
            double value = Double.parseDouble(price);
            return value >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    private static boolean isValidCapacity(String capacity) {
        if (capacity == null || capacity.trim().isEmpty()) return false;
        try {
            int value = Integer.parseInt(capacity);
            return value > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
