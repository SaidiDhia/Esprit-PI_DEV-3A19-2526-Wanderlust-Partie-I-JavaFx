package com.example.pi_dev.Services.Events;

import com.example.pi_dev.Entities.Events.Event;
import com.example.pi_dev.Entities.Events.Reservation;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

public class EmailService {
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final String EMAIL_FROM = "wanderlusttunisie582@gmail.com";
    private static final String EMAIL_PASSWORD = "nwes wdeh bwet ctsz";

    private GoogleMapsService mapsService;
    private boolean isInitialized = false;

    public EmailService() {
        this.mapsService = new GoogleMapsService();
        this.mapsService.initialize();
    }

    public boolean initialize() {
        try {
            System.out.println("📧 Email Service initialisé (mode réel)");
            this.isInitialized = true;
            return true;

        } catch (Exception e) {
            System.err.println("Erreur lors de l'initialisation du service email: " + e.getMessage());
            this.isInitialized = false;
            return false;
        }
    }

    public boolean sendReservationConfirmation(Reservation reservation) {
        if (!isInitialized) {
            System.err.println("Email Service n'est pas initialisé");
            return false;
        }

        try {
            String emailContent = generateReservationEmailContent(reservation);
            System.out.println("📧 Préparation de l'email de confirmation...");
            System.out.println("   Destinataire: " + reservation.getEmail());
            System.out.println("   Sujet: Confirmation de réservation - Wanderlust Events");

            Properties props = new Properties();
            props.put("mail.smtp.host", SMTP_HOST);
            props.put("mail.smtp.port", SMTP_PORT);
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

            Session session = Session.getInstance(props, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(EMAIL_FROM, EMAIL_PASSWORD);
                }
            });

            try {
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(EMAIL_FROM));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(reservation.getEmail()));
                message.setSubject("Confirmation de réservation - Wanderlust Events");

                message.setContent(emailContent, "text/html");

                Transport.send(message);

                System.out.println("✅ Email envoyé avec succès à " + reservation.getEmail());
                return true;

            } catch (MessagingException e) {
                System.err.println("Erreur lors de l'envoi de l'email: " + e.getMessage());
                e.printStackTrace();

                System.out.println("\n" + "=".repeat(60));
                System.out.println("CONTENU DE L'EMAIL (mode secours):");
                System.out.println("=".repeat(60));
                System.out.println(emailContent);
                System.out.println("=".repeat(60));

                return false;
            }

        } catch (Exception e) {
            System.err.println("Erreur lors de la préparation de l'email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private String generateReservationEmailContent(Reservation reservation) {
        StringBuilder content = new StringBuilder();

        content.append("<!DOCTYPE html>")
                .append("<html><head>")
                .append("<meta charset='UTF-8'>")
                .append("<title>Confirmation de réservation - Wanderlust Events</title>")
                .append("<style>")
                .append("body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f4f4f4; }")
                .append(".container { max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 10px; box-shadow: 0 0 10px rgba(0,0,0,0.1); }")
                .append(".header { text-align: center; margin-bottom: 30px; }")
                .append(".logo { font-size: 24px; color: #1a5f3f; font-weight: bold; }")
                .append(".title { color: #333; margin-bottom: 20px; }")
                .append(".info-box { background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 10px 0; }")
                .append(".info-label { font-weight: bold; color: #1a5f3f; }")
                .append(".info-value { color: #555; }")
                .append(".maps-button { display: inline-block; background-color: #34a853; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; margin: 20px 0; }")
                .append(".footer { text-align: center; margin-top: 30px; color: #666; font-size: 12px; }")
                .append("</style>")
                .append("</head><body>");

        content.append("<div class='container'>");

        content.append("<div class='header'>")
                .append("<div class='logo'>🌍 Wanderlust Events</div>")
                .append("<h1 class='title'>🎫 Confirmation de Réservation</h1>")
                .append("</div>");

        content.append("<p>Bonjour <strong>").append(reservation.getNom()).append("</strong>,</p>")
                .append("<p>Merci pour votre réservation ! Nous sommes ravis de vous accueillir à notre événement.</p>");

        content.append("<h2>📋 Détails de votre réservation</h2>");

        content.append("<div class='info-box'>")
                .append("<p><span class='info-label'>Numéro de réservation:</span> <span class='info-value'>#").append(reservation.getId()).append("</span></p>")
                .append("<p><span class='info-label'>Nom:</span> <span class='info-value'>").append(reservation.getNom()).append("</span></p>")
                .append("<p><span class='info-label'>Email:</span> <span class='info-value'>").append(reservation.getEmail()).append("</span></p>")
                .append("<p><span class='info-label'>Téléphone:</span> <span class='info-value'>").append(reservation.getTelephone()).append("</span></p>")
                .append("<p><span class='info-label'>Nombre de personnes:</span> <span class='info-value'>").append(reservation.getNombrePersonnes()).append("</span></p>")
                .append("<p><span class='info-label'>Prix total:</span> <span class='info-value'>").append(reservation.getPrixTotal()).append(" TND</span></p>");

        if (reservation.getDemandesSpeciales() != null && !reservation.getDemandesSpeciales().trim().isEmpty()) {
            content.append("<p><span class='info-label'>📝 Demandes spéciales:</span> <span class='info-value'>").append(reservation.getDemandesSpeciales()).append("</span></p>");
        }

        content.append("</div>");

        if (reservation.getEvent() != null) {
            Event event = reservation.getEvent();
            content.append("<h2>🎯 Détails de l'événement</h2>");

            content.append("<div class='info-box'>");

            if (event.getActivite() != null) {
                content.append("<p><span class='info-label'>Événement:</span> <span class='info-value'>").append(event.getActivite().getTitre()).append("</span></p>");
            }

            if (event.getLieu() != null && !event.getLieu().trim().isEmpty()) {
                content.append("<p><span class='info-label'>Lieu:</span> <span class='info-value'>").append(event.getLieu()).append("</span></p>");

                String mapsLink = generateMapsLink(event.getLieu());
                content.append("<p><span class='info-label'>🗺️ Itinéraire:</span> <a href='").append(mapsLink).append("' class='maps-button'>Voir sur Google Maps</a></p>");
            }

            if (event.getDateDebut() != null) {
                content.append("<p><span class='info-label'>Date de début:</span> <span class='info-value'>").append(event.getDateDebut().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm"))).append("</span></p>");
            }

            if (event.getDateFin() != null) {
                content.append("<p><span class='info-label'>Date de fin:</span> <span class='info-value'>").append(event.getDateFin().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm"))).append("</span></p>");
            }

            content.append("<p><span class='info-label'>Capacité:</span> <span class='info-value'>").append(event.getCapaciteMax()).append(" personnes</span></p>")
                    .append("<p><span class='info-label'>Places disponibles:</span> <span class='info-value'>").append(event.getPlacesDisponibles()).append("</span></p>");

            if (event.getDescription() != null && !event.getDescription().trim().isEmpty()) {
                content.append("<p><span class='info-label'>Description:</span> <span class='info-value'>").append(event.getDescription()).append("</span></p>");
            }

            if (event.getMaterielsNecessaires() != null && !event.getMaterielsNecessaires().trim().isEmpty()) {
                content.append("<p><span class='info-label'>Équipements requis:</span> <span class='info-value'>").append(event.getMaterielsNecessaires()).append("</span></p>");
            }

            content.append("</div>");
        }

        content.append("<h2>📝 Instructions importantes</h2>")
                .append("<div class='info-box'>")
                .append("<ul>")
                .append("<li>Veuillez arriver 15 minutes avant le début de l'événement</li>")
                .append("<li>Présentez ce email à l'entrée pour confirmer votre réservation</li>")
                .append("<li>N'oubliez pas d'apporter les équipements nécessaires</li>")
                .append("<li>En cas d'annulation, contactez-nous 48h à l'avance</li>")
                .append("</ul>")
                .append("</div>");

        content.append("<div class='footer'>")
                .append("<p>🌍 Wanderlust Tunisie</p>")
                .append("<p>📧 Contact: support@wanderlust-tunisie.com</p>")
                .append("<p>📱 Téléphone: +216 XX XXX XXX</p>")
                .append("<p><em>Cet email a été généré automatiquement. Merci de ne pas répondre.</em></p>")
                .append("</div>");

        content.append("</div></body></html>");

        return content.toString();
    }

    private String generateMapsLink(String location) {
        try {
            var coordsFuture = mapsService.getCoordinates(location);
            var coords = coordsFuture.get();

            if (coords != null) {
                return String.format(
                        "https://www.google.com/maps/search/?api=1&query=%.6f,%.6f",
                        coords.getLatitude(),
                        coords.getLongitude()
                );
            } else {
                return "https://www.google.com/maps/search/?api=1&query=" +
                        java.net.URLEncoder.encode(location, "UTF-8");
            }

        } catch (Exception e) {
            System.err.println("Erreur lors de la génération du lien Maps: " + e.getMessage());
            try {
                return "https://www.google.com/maps/search/?api=1&query=" +
                        java.net.URLEncoder.encode(location, "UTF-8");
            } catch (Exception ex) {
                return "https://www.google.com/maps";
            }
        }
    }

    public boolean sendReminderEmail(Reservation reservation) {
        if (!isInitialized) {
            return false;
        }

        try {
            System.out.println("📧 Envoi de l'email de rappel (mode simulation)");
            System.out.println("   Destinataire: " + reservation.getEmail());
            System.out.println("   Sujet: Rappel - Votre événement bientôt !");

            System.out.println("✅ Email de rappel envoyé avec succès");
            return true;

        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi du rappel: " + e.getMessage());
            return false;
        }
    }

    public boolean testConnection() {
        if (!isInitialized) {
            return false;
        }

        try {
            System.out.println("📧 Test de connexion au service email...");
            System.out.println("✅ Service email connecté (mode simulation)");
            return true;

        } catch (Exception e) {
            System.err.println("❌ Erreur de connexion email: " + e.getMessage());
            return false;
        }
    }

    public boolean isAvailable() {
        return isInitialized;
    }
}