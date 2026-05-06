package com.example.pi_dev.Controllers.Events;

import com.example.pi_dev.Services.Events.GoogleCalendarService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

import java.net.URL;
import java.util.ResourceBundle;

public class GoogleCalendarController implements Initializable {

    @FXML private Label statusLabel;
    @FXML private Button connectButton;
    @FXML private Button testButton;
    @FXML private Button syncButton;
    @FXML private ProgressBar progressBar;
    @FXML private VBox infoContainer;

    private GoogleCalendarService calendarService;
    private boolean isConnected = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        calendarService = new GoogleCalendarService();

        boolean initialized = calendarService.initialize();
        updateStatus(initialized);

        if (initialized) {
            testConnection();
        }
    }

    @FXML
    void connectToGoogle(ActionEvent event) {
        try {
            updateStatus(false);
            progressBar.setVisible(true);

            showAlert("🔑 Mode Simulation",
                    "Google Calendar est en mode simulation. En production, cette action ouvrirait la page d'authentification Google.");

            isConnected = true;
            updateStatus(true);

        } catch (Exception e) {
            System.err.println("Erreur lors de la connexion: " + e.getMessage());
            showAlert("❌ Erreur de connexion", "Impossible d'ouvrir la page d'authentification.");
        } finally {
            progressBar.setVisible(false);
        }
    }

    @FXML
    void testConnection(ActionEvent event) {
        testConnection();
    }

    private void testConnection() {
        try {
            progressBar.setVisible(true);
            statusLabel.setText("🔄 Test de connexion...");

            boolean connected = calendarService.testConnection();

            if (connected) {
                isConnected = true;
                updateStatus(true);
                showAlert("✅ Connexion réussie (simulation)", "Google Calendar est connecté en mode simulation.");
            } else {
                isConnected = false;
                updateStatus(false);
                showAlert("❌ Échec de connexion", "Impossible de se connecter à Google Calendar.");
            }

        } catch (Exception e) {
            System.err.println("Erreur lors du test: " + e.getMessage());
            isConnected = false;
            updateStatus(false);
        } finally {
            progressBar.setVisible(false);
        }
    }

    @FXML
    void syncReservations(ActionEvent event) {
        if (!isConnected) {
            showAlert("⚠️ Non connecté", "Veuillez d'abord vous connecter à Google Calendar.");
            return;
        }

        try {
            progressBar.setVisible(true);
            statusLabel.setText("🔄 Synchronisation...");

            int syncedCount = calendarService.syncAllReservations(
                    ReservationController.panier
            );

            if (syncedCount > 0) {
                showAlert("✅ Synchronisation réussie",
                        syncedCount + " réservations ont été synchronisées avec Google Calendar.");
            } else {
                showAlert("ℹ️ Synchronisation terminée",
                        "Aucune réservation à synchroniser.");
            }

        } catch (Exception e) {
            System.err.println("Erreur lors de la synchronisation: " + e.getMessage());
            showAlert("❌ Erreur de synchronisation", "Impossible de synchroniser les réservations.");
        } finally {
            progressBar.setVisible(false);
        }
    }

    @FXML
    void close(ActionEvent event) {
        Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
        stage.close();
    }

    private void updateStatus(boolean connected) {
        if (connected) {
            statusLabel.setText("📅 Google Calendar Connecté");
            statusLabel.setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;");
            connectButton.setText("🔄 Se reconnecter");
            syncButton.setDisable(false);

            infoContainer.getChildren().clear();
            Label infoLabel = new Label("✅ Google Calendar est connecté et prêt à synchroniser vos réservations.");
            infoLabel.setStyle("-fx-text-fill: #155724; -fx-font-style: italic;");
            infoContainer.getChildren().add(infoLabel);

        } else {
            statusLabel.setText("📅 Google Calendar Non Connecté");
            statusLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;");
            connectButton.setText("🔑 Se connecter");
            syncButton.setDisable(true);

            infoContainer.getChildren().clear();
            Label instructionLabel = new Label("📋 Pour connecter Google Calendar :");
            instructionLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

            Label step1 = new Label("1. Cliquez sur 'Se connecter'");
            Label step2 = new Label("2. Authentifiez-vous avec votre compte Google");
            Label step3 = new Label("3. Autorisez l'accès à Google Calendar");
            Label step4 = new Label("4. Revenez ici et testez la connexion");

            VBox stepsBox = new VBox(5);
            stepsBox.getChildren().addAll(step1, step2, step3, step4);
            stepsBox.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 10; -fx-background-radius: 5;");

            infoContainer.getChildren().addAll(instructionLabel, stepsBox);
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}