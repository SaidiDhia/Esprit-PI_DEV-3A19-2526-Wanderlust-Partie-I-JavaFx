package com.example.pi_dev.Entities.Events;

import java.util.Arrays;
import java.util.List;

public class TypeActivite {
    private CategorieActivite categorie;
    private String nom;
    private String description;
    
    public TypeActivite(CategorieActivite categorie, String nom, String description) {
        this.categorie = categorie;
        this.nom = nom;
        this.description = description;
    }
    
    public CategorieActivite getCategorie() {
        return categorie;
    }
    
    public String getNom() {
        return nom;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return nom;
    }
    
    // Types prédéfinis par catégorie
    public static List<TypeActivite> getTypesByCategorie(CategorieActivite categorie) {
        switch (categorie) {
            case DESERT:
                return Arrays.asList(
                    new TypeActivite(CategorieActivite.DESERT, "Randonnée dans le Sahara", "🏜️"),
                    new TypeActivite(CategorieActivite.DESERT, "Trekking dans les dunes", "🥾"),
                    new TypeActivite(CategorieActivite.DESERT, "Quad et buggy", "🏎️"),
                    new TypeActivite(CategorieActivite.DESERT, "Moto cross désert", "🏍️"),
                    new TypeActivite(CategorieActivite.DESERT, "Balade à dos de dromadaire", "🐪"),
                    new TypeActivite(CategorieActivite.DESERT, "Nuit en campement saharien", "🏕️"),
                    new TypeActivite(CategorieActivite.DESERT, "Observation des étoiles", "⭐")
                );
            case MER:
                return Arrays.asList(
                    new TypeActivite(CategorieActivite.MER, "Jet ski", "🏄‍♂️"),
                    new TypeActivite(CategorieActivite.MER, "Parachute ascensionnel", "🪂"),
                    new TypeActivite(CategorieActivite.MER, "Paddle", "🛶"),
                    new TypeActivite(CategorieActivite.MER, "Kayak", "🛶"),
                    new TypeActivite(CategorieActivite.MER, "Planche à voile", "⛵"),
                    new TypeActivite(CategorieActivite.MER, "Plongée sous-marine", "🤿"),
                    new TypeActivite(CategorieActivite.MER, "Snorkeling", "🤽"),
                    new TypeActivite(CategorieActivite.MER, "Sortie en bateau", "⛵"),
                    new TypeActivite(CategorieActivite.MER, "Pêche touristique", "🎣")
                );
            case AERIEN:
                return Arrays.asList(
                    new TypeActivite(CategorieActivite.AERIEN, "Parachutisme", "🪂"),
                    new TypeActivite(CategorieActivite.AERIEN, "Parapente", "🪂"),
                    new TypeActivite(CategorieActivite.AERIEN, "Parachute ascensionnel (mer)", "🪂"),
                    new TypeActivite(CategorieActivite.AERIEN, "ULM (Ultra léger motorisé)", "🪂"),
                    new TypeActivite(CategorieActivite.AERIEN, "Montgolfière (occasionnellement dans le sud)", "🎈")
                );
            case NATURE:
                return Arrays.asList(
                    new TypeActivite(CategorieActivite.NATURE, "Randonnée en forêt", "🥾"),
                    new TypeActivite(CategorieActivite.NATURE, "Escalade", "🧗"),
                    new TypeActivite(CategorieActivite.NATURE, "Camping", "🏕️"),
                    new TypeActivite(CategorieActivite.NATURE, "VTT", "🚵"),
                    new TypeActivite(CategorieActivite.NATURE, "Spéléologie", "⛰️")
                );
            case CULTURE:
                return Arrays.asList(
                    new TypeActivite(CategorieActivite.CULTURE, "Visite des ksour de Tataouine", "🏛️"),
                    new TypeActivite(CategorieActivite.CULTURE, "Décors de films à Tozeur", "🎬"),
                    new TypeActivite(CategorieActivite.CULTURE, "Visite archéologique", "🏛️"),
                    new TypeActivite(CategorieActivite.CULTURE, "Festivals", "🎉"),
                    new TypeActivite(CategorieActivite.CULTURE, "Tourisme historique", "📚"),
                    new TypeActivite(CategorieActivite.CULTURE, "Photographie", "📷")
                );
            default:
                return Arrays.asList();
        }
    }
}
