package com.example.pi_dev.Entities.Events;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Event {

    private int id;
    private int idActivite; // FK vers Activite
    private String lieu;
    private String email;
    private String description;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private BigDecimal prix;
    private Integer capaciteMax;
    private Integer placesDisponibles;
    private String organisateur;
    private String materielsNecessaires;
    private String image; // Image principale (pour compatibilité)
    private String videoYoutube; // URL de la vidéo YouTube
    private List<String> photos; // Liste des photos supplémentaires
    private StatutEvent statut;
    private Timestamp dateCreation;
    private Timestamp dateModification;
    private Integer telephone;


    private Activite activite;



    public enum StatutEvent {
        A_VENIR,
        EN_COURS,
        ANNULE,
        TERMINE
    }

    public Event() {
        this.photos = new ArrayList<>(); // Initialiser la liste des photos
    }

    public Event(int idActivite, String lieu, String adresseComplete, String email, String description, LocalDateTime dateDebut,
                 LocalDateTime dateFin, double capaciteMax, int prix) {

        this.idActivite = idActivite;
        this.lieu = lieu;
        this.email = email;
        this.description = description;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.capaciteMax = (int) capaciteMax;
        this.placesDisponibles = (int) capaciteMax;
        this.prix = BigDecimal.valueOf(prix);
        this.statut = StatutEvent.A_VENIR;
        this.photos = new ArrayList<>(); // Initialiser la liste des photos
    }

    // Getters & Setters

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getIdActivite() { return idActivite; }
    public void setIdActivite(int idActivite) { this.idActivite = idActivite; }

    public String getLieu() { return lieu; }
    public void setLieu(String lieu) { this.lieu = lieu; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDateTime dateDebut) { this.dateDebut = dateDebut; }

    public LocalDateTime getDateFin() { return dateFin; }
    public void setDateFin(LocalDateTime dateFin) { this.dateFin = dateFin; }

    public BigDecimal getPrix() { return prix; }
    public void setPrix(BigDecimal prix) { this.prix = prix; }

    public Integer getCapaciteMax() { return capaciteMax; }
    public void setCapaciteMax(Integer capaciteMax) { this.capaciteMax = capaciteMax; }

    public Integer getPlacesDisponibles() { return placesDisponibles; }
    public void setPlacesDisponibles(Integer placesDisponibles) { this.placesDisponibles = placesDisponibles; }

    public String getOrganisateur() { return organisateur; }
    public void setOrganisateur(String organisateur) { this.organisateur = organisateur; }

    public String getMaterielsNecessaires() { return materielsNecessaires; }
    public void setMaterielsNecessaires(String materielsNecessaires) { this.materielsNecessaires = materielsNecessaires; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public String getVideoYoutube() { return videoYoutube; }
    public void setVideoYoutube(String videoYoutube) { this.videoYoutube = videoYoutube; }

    public List<String> getPhotos() { return photos; }
    public void setPhotos(List<String> photos) { this.photos = photos; }

    // Méthodes utilitaires pour gérer les photos
    public void addPhoto(String photoPath) {
        if (this.photos == null) {
            this.photos = new ArrayList<>();
        }
        this.photos.add(photoPath);
    }

    public void removePhoto(String photoPath) {
        if (this.photos != null) {
            this.photos.remove(photoPath);
        }
    }

    public void clearPhotos() {
        if (this.photos != null) {
            this.photos.clear();
        }
    }

    // Méthode pour extraire l'ID YouTube d'une URL
    public String getYoutubeVideoId() {
        if (videoYoutube == null || videoYoutube.isEmpty()) {
            return null;
        }
        
        // Extraire l'ID de la vidéo YouTube
        String pattern = "(?<=youtu.be/|watch\\?v=|/videos/|embed\\/)[^#\\&\\?]*";
        java.util.regex.Pattern compiledPattern = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher matcher = compiledPattern.matcher(videoYoutube);
        
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    // Méthode pour obtenir l'URL d'intégration YouTube
    public String getYoutubeEmbedUrl() {
        String videoId = getYoutubeVideoId();
        if (videoId != null) {
            return "https://www.youtube.com/embed/" + videoId;
        }
        return null;
    }

    public Integer getTelephone() {
        return telephone;
    }

    public void setTelephone(Integer telephone) {
        this.telephone = telephone;
    }

    public StatutEvent getStatut() { return statut; }
    public void setStatut(StatutEvent statut) { this.statut = statut; }

    public Timestamp getDateCreation() { return dateCreation; }
    public void setDateCreation(Timestamp dateCreation) { this.dateCreation = dateCreation; }

    public Timestamp getDateModification() { return dateModification; }
    public void setDateModification(Timestamp dateModification) { this.dateModification = dateModification; }

    public Activite getActivite() { return activite; }
    public void setActivite(Activite activite) { this.activite = activite; }

}
