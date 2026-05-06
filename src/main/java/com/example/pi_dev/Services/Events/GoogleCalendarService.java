package com.example.pi_dev.Services.Events;

import com.example.pi_dev.Entities.Events.Reservation;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class GoogleCalendarService {
    private static final String APPLICATION_NAME = "Wanderlust Events";
    private boolean isInitialized = false;

    public boolean initialize() {
        try {
            this.isInitialized = true;
            System.out.println("📅 Google Calendar Service initialisé (mode simulation)");
            return true;

        } catch (Exception e) {
            System.err.println("Erreur lors de l'initialisation de Google Calendar: " + e.getMessage());
            this.isInitialized = false;
            return false;
        }
    }

    public boolean addReservationToCalendar(Reservation reservation) {
        if (!isInitialized) {
            System.err.println("Google Calendar n'est pas initialisé");
            return false;
        }

        try {
            System.out.println("📅 Ajout de l'événement au calendrier (mode simulation):");

            String title = String.format("🎫 Réservation Wanderlust - %s",
                    reservation.getEvent() != null && reservation.getEvent().getActivite() != null
                            ? reservation.getEvent().getActivite().getTitre()
                            : "Événement");
            System.out.println("   Titre: " + title);

            String description = createEventDescription(reservation);
            System.out.println("   Description: " + description.substring(0, Math.min(100, description.length())) + "...");

            if (reservation.getEvent() != null && reservation.getEvent().getLieu() != null) {
                System.out.println("   Lieu: " + reservation.getEvent().getLieu());
            }

            if (reservation.getEvent() != null && reservation.getEvent().getDateDebut() != null) {
                System.out.println("   Date: " + reservation.getEvent().getDateDebut().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")));
            }

            System.out.println("   Rappels: Email (24h avant), Popup (2h avant)");

            return true;

        } catch (Exception e) {
            System.err.println("Erreur lors de l'ajout au calendrier: " + e.getMessage());
            return false;
        }
    }

    private String createEventDescription(Reservation reservation) {
        StringBuilder description = new StringBuilder();
        description.append("🎫 DÉTAILS DE LA RÉSERVATION\n\n");

        description.append("📋 INFORMATIONS DE RÉSERVATION:\n");
        description.append("• ID Réservation: #").append(reservation.getId()).append("\n");
        description.append("• Nom: ").append(reservation.getNom()).append("\n");
        description.append("• Email: ").append(reservation.getEmail()).append("\n");
        description.append("• Téléphone: ").append(reservation.getTelephone()).append("\n");
        description.append("• Nombre de personnes: ").append(reservation.getNombrePersonnes()).append("\n");
        description.append("• Prix total: ").append(reservation.getPrixTotal()).append(" TND\n");
        description.append("• Statut: ").append(reservation.getStatut()).append("\n\n");

        if (reservation.getEvent() != null) {
            description.append("🎯 DÉTAILS DE L'ÉVÉNEMENT:\n");
            description.append("• Événement: ").append(reservation.getEvent().getActivite() != null
                    ? reservation.getEvent().getActivite().getTitre() : "Non spécifié").append("\n");
            description.append("• Lieu: ").append(reservation.getEvent().getLieu() != null
                    ? reservation.getEvent().getLieu() : "Non spécifié").append("\n");
            description.append("• Date: ").append(reservation.getEvent().getDateDebut() != null
                    ? reservation.getEvent().getDateDebut().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm"))
                    : "Non spécifiée").append("\n");
            description.append("• Capacité: ").append(reservation.getEvent().getCapaciteMax()).append(" personnes\n");
            description.append("• Places disponibles: ").append(reservation.getEvent().getPlacesDisponibles()).append("\n");

            if (reservation.getEvent().getDescription() != null && !reservation.getEvent().getDescription().trim().isEmpty()) {
                description.append("• Description: ").append(reservation.getEvent().getDescription()).append("\n");
            }

            if (reservation.getEvent().getMaterielsNecessaires() != null && !reservation.getEvent().getMaterielsNecessaires().trim().isEmpty()) {
                description.append("• Équipements requis: ").append(reservation.getEvent().getMaterielsNecessaires()).append("\n");
            }
        }

        description.append("\n🌍 Géré par Wanderlust Tunisie");
        description.append("\n📱 Contact: support@wanderlust-tunisie.com");

        return description.toString();
    }

    public boolean isAvailable() {
        return isInitialized;
    }

    public boolean setupReminders(Reservation reservation) {
        if (!isInitialized) {
            return false;
        }

        try {
            System.out.println("📧 Rappel email configuré: 1 jour avant l'événement");
            System.out.println("🔔 Rappel popup configuré: 2 heures avant l'événement");
            System.out.println("📱 Rappel mobile configuré: 30 minutes avant l'événement");

            return true;

        } catch (Exception e) {
            System.err.println("Erreur lors de la configuration des rappels: " + e.getMessage());
            return false;
        }
    }

    public int syncAllReservations(List<Reservation> reservations) {
        if (!isInitialized) {
            return 0;
        }

        int successCount = 0;
        System.out.println("🔄 Synchronisation de " + reservations.size() + " réservations avec Google Calendar...");

        for (Reservation reservation : reservations) {
            if (addReservationToCalendar(reservation)) {
                successCount++;
            }
        }

        System.out.println("✅ Synchronisation terminée: " + successCount + "/" + reservations.size() + " événements ajoutés");
        return successCount;
    }

    public boolean testConnection() {
        if (!isInitialized) {
            return false;
        }

        try {
            System.out.println("🔍 Test de connexion à Google Calendar...");
            System.out.println("✅ Connexion à Google Calendar simulée avec succès");
            return true;

        } catch (Exception e) {
            System.err.println("❌ Erreur de connexion: " + e.getMessage());
            return false;
        }
    }

    public String getAuthorizationUrl() {
        return "https://accounts.google.com/oauth/authorize?client_id=YOUR_CLIENT_ID&redirect_uri=YOUR_REDIRECT_URI&scope=https://www.googleapis.com/auth/calendar&response_type=code";
    }
}