package com.example.pi_dev.Entities.Events;

import java.sql.Timestamp;

public class EventPhoto {
    
    private int id;
    private int idEvent; // FK vers Event
    private String cheminPhoto; // Chemin vers le fichier photo
    private String description; // Description optionnelle de la photo
    private Timestamp dateCreation;
    
    public EventPhoto() {}
    
    public EventPhoto(int idEvent, String cheminPhoto, String description) {
        this.idEvent = idEvent;
        this.cheminPhoto = cheminPhoto;
        this.description = description;
        this.dateCreation = new Timestamp(System.currentTimeMillis());
    }
    
    // Getters & Setters
    
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getIdEvent() { return idEvent; }
    public void setIdEvent(int idEvent) { this.idEvent = idEvent; }
    
    public String getCheminPhoto() { return cheminPhoto; }
    public void setCheminPhoto(String cheminPhoto) { this.cheminPhoto = cheminPhoto; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Timestamp getDateCreation() { return dateCreation; }
    public void setDateCreation(Timestamp dateCreation) { this.dateCreation = dateCreation; }
}
