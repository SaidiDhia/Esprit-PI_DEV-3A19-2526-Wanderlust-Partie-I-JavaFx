package com.example.pi_dev.Entities.Events;

public class Reservation {

    private int id;
    private int idEvent; // FK vers Event
    private String nomComplet;
    private String email;
    private String telephone;
    private Integer nombrePersonnes;
    private Double prixTotal;
    private java.time.LocalDateTime dateReservation;
    private String demandesSpeciales;
    private StatutReservation statut;
    private java.sql.Timestamp dateCreation;
    private java.sql.Timestamp dateModification;

    // Pour affichage
    private Event event;

    public enum StatutReservation {
        EN_ATTENTE,
        CONFIRMEE,
        ANNULEE,
        TERMINEE
    }

    public Reservation() {
        this.nombrePersonnes = 1;
        this.statut = StatutReservation.EN_ATTENTE;
    }

    public Reservation(int idEvent, String nomComplet, String email,
                       String telephone, Integer nombrePersonnes) {
        this();
        this.idEvent = idEvent;
        this.nomComplet = nomComplet;
        this.email = email;
        this.telephone = telephone;
        this.nombrePersonnes = nombrePersonnes;
    }

    // Getters & Setters

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getIdEvent() { return idEvent; }
    public void setIdEvent(int idEvent) { this.idEvent = idEvent; }

    public String getNomComplet() { return nomComplet; }
    public void setNomComplet(String nomComplet) { this.nomComplet = nomComplet; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public Integer getNombrePersonnes() { return nombrePersonnes; }
    public void setNombrePersonnes(Integer nombrePersonnes) { this.nombrePersonnes = nombrePersonnes; }

    public Double getPrixTotal() { return prixTotal; }
    public void setPrixTotal(Double prixTotal) { this.prixTotal = prixTotal; }

    public java.time.LocalDateTime getDateReservation() { return dateReservation; }
    public void setDateReservation(java.time.LocalDateTime dateReservation) { this.dateReservation = dateReservation; }

    public String getDemandesSpeciales() { return demandesSpeciales; }
    public void setDemandesSpeciales(String demandesSpeciales) { this.demandesSpeciales = demandesSpeciales; }

    public StatutReservation getStatut() { return statut; }
    public void setStatut(StatutReservation statut) { this.statut = statut; }

    public java.sql.Timestamp getDateCreation() { return dateCreation; }
    public void setDateCreation(java.sql.Timestamp dateCreation) { this.dateCreation = dateCreation; }

    public java.sql.Timestamp getDateModification() { return dateModification; }
    public void setDateModification(java.sql.Timestamp dateModification) { this.dateModification = dateModification; }

    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }

    // Alias pour compatibilité avec le controller
    public String getNom() { return nomComplet; }
    public void setNom(String nom) { this.nomComplet = nom; }

    public Double getPrixUnitaire() {
        if (event != null && event.getPrix() != null) {
            return event.getPrix().doubleValue();
        }
        return 0.0;
    }
}
