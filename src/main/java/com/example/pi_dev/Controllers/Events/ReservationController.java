package com.example.pi_dev.Controllers.Events;

import com.example.pi_dev.Entities.Events.Event;
import com.example.pi_dev.Entities.Events.Reservation;
import com.example.pi_dev.Services.Events.EventService;
import com.example.pi_dev.Services.Events.ReservationService;
import com.example.pi_dev.Services.Events.EmailService;
import com.example.pi_dev.Services.Events.DynamicPricingEngine;
import com.example.pi_dev.Utils.Events.Mydatabase;
import com.example.pi_dev.Session.Session;

import java.util.regex.Pattern;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;

public class ReservationController {

    @FXML
    private Label Eventlieu;
    @FXML
    private Label datedebut;
    @FXML
    private Label personCountLabel;
    @FXML
    private Label prixlabel;
    // Media fields removed as they are no longer in the FXML

    @FXML
    private TextField idEventField;
    @FXML
    private TextField nomField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField telephoneField;
    @FXML
    private TextArea demandesp;
    @FXML
    private Button reserverButton;

    @FXML
    private Label nbAdultesLabel;
    @FXML
    private Label nbEnfantsLabel;
    @FXML
    private Label totalLabel;
    @FXML
    private Label nbPersonnesTotalLabel;
    @FXML
    private Label oldPrixlabel;

    private double discountRate = 0.0;
    private double basePricePerPerson = 0.0;

    private int adultCount = 1;
    private int childCount = 0;

    private EventService eventService = new EventService();
    private ReservationService reservationService = new ReservationService();

    private Event currentEvent;
    private List<javafx.scene.Node> allMediaNodes = new ArrayList<>();
    private int currentImageIndex = 0;

    public static ObservableList<Reservation> panier = FXCollections.observableArrayList();


    private EmailService emailService;

    public void initialize() {
        System.out.println("✅ ReservationController initialisé");

        emailService = new EmailService();
        boolean emailInitialized = emailService.initialize();

        if (emailInitialized) {
            System.out.println("📧 Email Service prêt");
        } else {
            System.out.println("⚠️ Email Service non disponible");
        }
        
        updateParticipantsDisplay();
    }

    public void loadEvent(int idEvent) {
        try {
            currentEvent = eventService.findById(idEvent);
            
            if (currentEvent != null) {
                System.out.println("DEBUG: Événement chargé - ID: " + currentEvent.getId());
                
                if (Eventlieu != null) {
                    Eventlieu.setText(currentEvent.getLieu() != null ? currentEvent.getLieu() : "Lieu non spécifié");
                }

                if (datedebut != null) {
                    if (currentEvent.getDateDebut() != null) {
                        datedebut.setText(currentEvent.getDateDebut()
                                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                    } else {
                        datedebut.setText("Date non définie");
                    }
                }

                if (personCountLabel != null) {
                    int placesRestantes = currentEvent.getPlacesDisponibles();
                    personCountLabel.setText(placesRestantes + " places disponibles");
                }

                if (demandesp != null) {
                    if (currentEvent.getMaterielsNecessaires() != null
                            && !currentEvent.getMaterielsNecessaires().trim().isEmpty()) {
                        demandesp.setPromptText(
                                "Régimes alimentaires, besoins spécifiques...\n\n📋 Matériels requis :\n"
                                        + currentEvent.getMaterielsNecessaires());
                    } else {
                        demandesp.setPromptText("Régimes alimentaires, besoins spécifiques...");
                    }
                }

                DynamicPricingEngine.PricingResult pricing = DynamicPricingEngine.calculatePrice(currentEvent);
                this.basePricePerPerson = pricing.originalPrice;
                this.discountRate = pricing.discountPercentage;

                if (discountRate > 0) {
                    if (oldPrixlabel != null) {
                        oldPrixlabel.setText(String.format("%.2f TND", basePricePerPerson));
                        oldPrixlabel.setVisible(true);
                        oldPrixlabel.setManaged(true);
                    }
                } else {
                    if (oldPrixlabel != null) {
                        oldPrixlabel.setVisible(false);
                        oldPrixlabel.setManaged(false);
                    }
                }

                if (prixlabel != null) {
                    prixlabel.setText(String.format("%.2f TND", pricing.finalPrice));
                }

                updateParticipantsDisplay();

                // Check if reservation should be disabled
                if (reserverButton != null) {
                    boolean isClosed = false;
                    if (currentEvent.getPlacesDisponibles() <= 0) {
                        isClosed = true;
                        reserverButton.setText("ÉVÉNEMENT COMPLET");
                    } else if (currentEvent.getDateDebut() != null && currentEvent.getDateDebut().isBefore(LocalDateTime.now())) {
                        isClosed = true;
                        reserverButton.setText("ÉVÉNEMENT PASSÉ");
                    }
                    
                    if (isClosed) {
                        reserverButton.setDisable(true);
                        reserverButton.setStyle("-fx-background-color: #94A3B8; -fx-text-fill: white; -fx-background-radius: 12; -fx-padding: 18; -fx-font-size: 16px; -fx-font-weight: bold;");
                    } else {
                        reserverButton.setDisable(false);
                        reserverButton.setText("CONFIRMER LA RÉSERVATION");
                        reserverButton.setStyle("-fx-background-color: #2563EB; -fx-text-fill: white; -fx-background-radius: 12; -fx-padding: 18; -fx-font-size: 16px; -fx-font-weight: bold; -fx-cursor: hand;");
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Legacy media methods removed

    private boolean validateInput() {
        if (currentEvent == null) {
            showAlert("Veuillez d'abord sélectionner un événement");
            return false;
        }

        if (nomField.getText().isEmpty()) {
            showAlert("Le nom est obligatoire");
            return false;
        }

        if (!Pattern.matches("^[A-Za-z ]+$", nomField.getText())) {
            showAlert("Nom invalide");
            return false;
        }

        if (!Pattern.matches("^[A-Za-z0-9+_.-]+@(.+)$", emailField.getText())) {
            showAlert("Email invalide");
            return false;
        }

        if (!Pattern.matches("^[0-9]{8,15}$", telephoneField.getText())) {
            showAlert("Téléphone invalide");
            return false;
        }

        int nombre = adultCount + childCount;

        if (currentEvent.getPlacesDisponibles() <= 0) {
            showAlert("Cet événement est complet.");
            return false;
        }

        if (currentEvent.getDateDebut() != null && currentEvent.getDateDebut().isBefore(LocalDateTime.now())) {
            showAlert("Cet événement est déjà passé.");
            return false;
        }

        if (nombre > currentEvent.getPlacesDisponibles()) {
            showAlert("Pas assez de places disponibles");
            return false;
        }

        return true;
    }

    @FXML
    void reserver(ActionEvent event) {
        if (!validateInput()) {
            return;
        }

        try {
            if (currentEvent == null || currentEvent.getId() == 0) {
                showAlert("Aucun événement sélectionné. Veuillez sélectionner un événement avant de réserver.");
                return;
            }

            if (!eventExists(currentEvent.getId())) {
                showAlert(
                        "L'événement sélectionné n'est plus disponible. Veuillez rafraîchir la liste des événements et réessayer.");
                return;
            }

            Reservation reservation = new Reservation();
            reservation.setIdEvent(currentEvent.getId());
            String currentUserId = Session.getCurrentUserId();
            System.out.println("DEBUG: Tentative de réservation pour l'utilisateur ID: " + currentUserId);
            reservation.setUserId(currentUserId);

            reservation.setNom(nomField.getText());
            reservation.setEmail(emailField.getText());
            reservation.setTelephone(telephoneField.getText());

            reservation.setNombrePersonnes(adultCount + childCount);
            reservation.setDemandesSpeciales(
                    demandesp.getText() != null && !demandesp.getText().trim().isEmpty() ? demandesp.getText().trim()
                            : "");
            reservation.setEvent(currentEvent);

            // Check if event date has passed
            if (currentEvent.getDateDebut() != null
                    && currentEvent.getDateDebut().isBefore(java.time.LocalDateTime.now())) {
                showAlert("La date de cet événement est déjà passée. Vous ne pouvez pas réserver.");
                return;
            }

            reservation.setStatut(Reservation.StatutReservation.EN_ATTENTE);

            int nbPersonnes = adultCount + childCount;
            double baseTotal = basePricePerPerson * nbPersonnes;
            double prixTotal = baseTotal - (baseTotal * discountRate);
            reservation.setPrixTotal(prixTotal);

            reservationService.add(reservation);

            panier.add(reservation);

            showAlert("Réservation effectuée avec succès !\nRetrouvez-la dans l'onglet 'Mes Réservations'.");

            com.example.pi_dev.Utils.Events.CatalogueRefreshManager.getInstance().requestRefresh();
            
            fermerFenetre();

        } catch (SQLException e) {
            System.err.println("Erreur lors de la réservation: " + e.getMessage());

            if (e.getMessage().contains("foreign key constraint fails")) {
                showAlert(
                        "Erreur : L'événement sélectionné n'est plus disponible. Veuillez rafraîchir la liste des événements et réessayer.");
                fermerFenetre();
            } else {
                showAlert("Erreur lors de la réservation: " + e.getMessage());
            }
        } catch (NumberFormatException e) {
            showAlert("Veuillez entrer un nombre valide pour le nombre de personnes");
        }
    }

    private void afficherQRCode(Reservation reservation) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pi_dev/events/QRCode.fxml"));
            Parent root = loader.load();

            QRCodeController qrController = loader.getController();
            qrController.setReservation(reservation);

            Stage stage = new Stage();
            stage.setTitle("QR Code de Réservation");
            stage.setScene(new Scene(root));
            stage.setWidth(500);
            stage.setHeight(600);
            stage.centerOnScreen();
            stage.setResizable(false);
            stage.show();

        } catch (IOException e) {
            System.err.println("Erreur lors de l'ouverture de l'interface QR Code: " + e.getMessage());
            showAlert("Erreur lors de l'ouverture du QR Code");
        }
    }

    @FXML
    void annulerRES(ActionEvent event) {
        goToCatalogue(event);
    }

    @FXML
    void goToCatalogue(ActionEvent event) {
        fermerFenetre();
    }

    @FXML
    void goToPanier(ActionEvent event) {
        fermerFenetre();
    }

    @FXML
    void nombrePersonnesChanged(ActionEvent event) {
        updateParticipantsDisplay();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void clearFields() {
        nomField.clear();
        emailField.clear();
        telephoneField.clear();
        adultCount = 1;
        childCount = 0;
        updateParticipantsDisplay();
        demandesp.clear();
    }

    @FXML
    void incrementAdults(ActionEvent event) {
        adultCount++;
        updateParticipantsDisplay();
    }

    @FXML
    void decrementAdults(ActionEvent event) {
        if (adultCount > 1) {
            adultCount--;
            updateParticipantsDisplay();
        }
    }

    @FXML
    void incrementChildren(ActionEvent event) {
        childCount++;
        updateParticipantsDisplay();
    }

    @FXML
    void decrementChildren(ActionEvent event) {
        if (childCount > 0) {
            childCount--;
            updateParticipantsDisplay();
        }
    }

    private void updateParticipantsDisplay() {
        if (nbAdultesLabel != null) nbAdultesLabel.setText(String.valueOf(adultCount));
        if (nbEnfantsLabel != null) nbEnfantsLabel.setText(String.valueOf(childCount));
        
        int totalPersons = adultCount + childCount;
        if (nbPersonnesTotalLabel != null) {
            nbPersonnesTotalLabel.setText(totalPersons + (totalPersons > 1 ? " personnes" : " personne"));
        }
        
        if (currentEvent != null) {
            double total = basePricePerPerson * totalPersons;
            double discountedTotal = total - (total * discountRate);
            if (totalLabel != null) {
                if (discountRate > 0) {
                    totalLabel.setText(String.format("%.2f TND (au lieu de %.2f)", discountedTotal, total));
                } else {
                    totalLabel.setText(String.format("%.2f TND", discountedTotal));
                }
            }
            if (prixlabel != null) prixlabel.setText(String.format("%.2f TND", discountedTotal));
        }
    }



    private void fermerFenetre() {
        if (reserverButton != null && reserverButton.getScene() != null) {
            Stage stage = (Stage) reserverButton.getScene().getWindow();
            stage.close();
        }
    }

    private boolean eventExists(int eventId) {
        try {
            String sql = "SELECT COUNT(*) FROM events WHERE id = ?";
            java.sql.PreparedStatement ps = Mydatabase.getInstance().getConnextion().prepareStatement(sql);
            ps.setInt(1, eventId);
            java.sql.ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int count = rs.getInt(1);
                return count > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}