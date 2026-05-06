package com.example.pi_dev.Entities.Events;

import java.text.Normalizer;
import java.util.Locale;

public enum CategorieActivite {
    DESERT("Désert", "🏜️"),
    MER("Mer", "🌊"),
    AERIEN("Aérien", "🪂"),
    NATURE("Nature", "🌳"),
    CULTURE("Culture", "🏛️");

    private final String nom;
    private final String icone;

    CategorieActivite(String nom, String icone) {
        this.nom = nom;
        this.icone = icone;
    }

    public String getNom() {
        return nom;
    }

    public String getIcone() {
        return icone;
    }

    public String toDbValue() {
        return switch (this) {
            case DESERT -> "desert";
            case MER -> "Mer";
            case AERIEN -> "Aérien";
            case NATURE -> "nature";
            case CULTURE -> "Culture";
        };
    }

    public static CategorieActivite fromDbValue(String dbValue) {
        if (dbValue == null || dbValue.isBlank()) {
            return NATURE;
        }

        String normalized = normalize(dbValue);
        return switch (normalized) {
            case "desert" -> DESERT;
            case "mer" -> MER;
            case "aerien" -> AERIEN;
            case "nature" -> NATURE;
            case "culture" -> CULTURE;
            default -> NATURE;
        };
    }

    private static String normalize(String value) {
        String trimmed = value.trim().toLowerCase(Locale.ROOT);
        String noAccents = Normalizer.normalize(trimmed, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return noAccents;
    }

    @Override
    public String toString() {
        return nom;
    }
}
