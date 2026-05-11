package com.example.pi_dev.Controllers.Events;

import com.example.pi_dev.Entities.Events.Event;
import com.example.pi_dev.Services.Events.EventsGeminiService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class EventSuggestionController {

    @FXML
    private TextField ageField;

    @FXML
    private TextField interestsField;

    @FXML
    private TextField locationField;

    @FXML
    private TextArea responseArea;

    @FXML
    private Button suggestButton;

    @FXML
    private ProgressIndicator loadingIndicator;

    @FXML
    private Label statusLabel;

    @FXML
    private VBox resultContainer;

    private List<Event> availableEvents;
    private EventsGeminiService geminiService;

    @FXML
    public void initialize() {
        geminiService = new EventsGeminiService();
        loadingIndicator.setVisible(false);
        loadingIndicator.setManaged(false);
        resultContainer.setVisible(false);
        resultContainer.setManaged(false);
    }

    public void setEventsList(List<Event> events) {
        this.availableEvents = events;
        System.out.println("DEBUG: Reçu " + (events != null ? events.size() : 0) + " événements pour la suggestion.");
    }

    @FXML
    private void getSuggestion() {
        if (availableEvents == null || availableEvents.isEmpty()) {
            responseArea.setText("Désolé, il n'y a actuellement aucun événement disponible dans le catalogue.");
            resultContainer.setVisible(true);
            resultContainer.setManaged(true);
            return;
        }

        String age = ageField.getText().trim();
        String interests = interestsField.getText().trim();
        String location = locationField.getText().trim();

        if (age.isEmpty() && interests.isEmpty() && location.isEmpty()) {
            statusLabel.setText("Veuillez remplir au moins un critère !");
            statusLabel.setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold;");
            return;
        }

        statusLabel.setText("Recherche de l'événement idéal...");
        statusLabel.setStyle("-fx-text-fill: #2563EB;");
        loadingIndicator.setVisible(true);
        loadingIndicator.setManaged(true);
        suggestButton.setDisable(true);
        resultContainer.setVisible(false);
        resultContainer.setManaged(false);

        // Build the prompt in background
        Thread aiThread = new Thread(() -> {
            try {
                StringBuilder eventsContext = new StringBuilder();
                eventsContext.append("CATALOGUE DES ÉVÉNEMENTS DISPONIBLES:\n");
                
                for (Event e : availableEvents) {
                    if (e.getPlacesDisponibles() != null && e.getPlacesDisponibles() > 0) {
                        eventsContext.append("- ID: ").append(e.getId())
                                .append(" | Titre/Org: ").append(e.getOrganisateur() != null ? e.getOrganisateur() : "Inconnu")
                                .append(" | Lieu: ").append(e.getLieu() != null ? e.getLieu() : "Inconnu")
                                .append(" | Date: ").append(e.getDateDebut() != null ? e.getDateDebut().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "Inconnue")
                                .append(" | Prix: ").append(e.getPrix() != null ? e.getPrix() + " TND" : "Gratuit")
                                .append(" | Description: ").append(e.getDescription() != null ? e.getDescription() : "Aucune")
                                .append("\n");
                    }
                }

                String prompt = "Tu es un assistant de voyage expert pour l'agence Wanderlust. " +
                        "Voici la liste stricte des événements actuellement disponibles (ne propose aucun autre événement qui n'est pas dans cette liste) :\n" +
                        eventsContext.toString() + "\n" +
                        "L'utilisateur qui te consulte a les caractéristiques suivantes :\n" +
                        (!age.isEmpty() ? "- Âge : " + age + " ans\n" : "") +
                        (!interests.isEmpty() ? "- Centres d'intérêt / Ce qu'il a envie de faire : " + interests + "\n" : "") +
                        (!location.isEmpty() ? "- Localisation souhaitée : près de " + location + "\n" : "") +
                        "\nMission : Analyse ces informations et recommande-lui UN ou DEUX événements maximum de notre catalogue qui correspondent le mieux à ses envies. " +
                        "Adopte un ton enthousiaste, accueillant et professionnel. " +
                        "Explique brièvement pourquoi tu penses que cet événement lui correspondrait bien (en te basant sur ses critères), " +
                        "et mentionne le lieu, la date et le prix. " +
                        "Si aucun événement ne correspond vraiment à ses critères (notamment la localisation), " +
                        "propose-lui l'événement le plus exceptionnel du moment tout en lui expliquant pourquoi il devrait faire une petite concession. " +
                        "N'utilise pas de formatage Markdown complexe, garde un texte clair et aéré.";

                String response = geminiService.generateResponse(prompt);

                Platform.runLater(() -> {
                    responseArea.setText(response);
                    resultContainer.setVisible(true);
                    resultContainer.setManaged(true);
                    loadingIndicator.setVisible(false);
                    loadingIndicator.setManaged(false);
                    suggestButton.setDisable(false);
                    statusLabel.setText("✨ Voici votre suggestion !");
                    statusLabel.setStyle("-fx-text-fill: #16A34A; -fx-font-weight: bold;");
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    responseArea.setText("Une erreur est survenue lors de la communication avec l'IA. Veuillez réessayer plus tard.\nErreur: " + e.getMessage());
                    resultContainer.setVisible(true);
                    resultContainer.setManaged(true);
                    loadingIndicator.setVisible(false);
                    loadingIndicator.setManaged(false);
                    suggestButton.setDisable(false);
                    statusLabel.setText("Erreur !");
                    statusLabel.setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold;");
                });
            }
        });
        
        aiThread.setDaemon(true);
        aiThread.start();
    }
}
