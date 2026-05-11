package com.example.pi_dev.Controllers.Events;

import com.example.pi_dev.Entities.Events.Reservation;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import java.time.format.DateTimeFormatter;

public class ReservationDetailsController {

    @FXML private Label statusBadge;
    @FXML private Label lieuLabel;
    @FXML private Label dateLabel;
    @FXML private Label placesLabel;
    @FXML private Label nomLabel;
    @FXML private Label emailLabel;
    @FXML private Label telLabel;
    @FXML private Label paxLabel;
    @FXML private Label adultesLabel;
    @FXML private Label enfantsLabel;
    @FXML private Label prixTotalLabel;
    @FXML private Label prixDetailLabel;
    @FXML private Label demandesLabel;
    @FXML private Button ticketBtn;

    private Reservation reservation;

    public void setReservationData(Reservation res) {
        this.reservation = res;
        
        // Populate UI
        if (res.getStatut() != null) {
            String status = res.getStatut().name().toLowerCase();
            if (status.contains("accepte") || status.contains("confirme")) {
                statusBadge.setText("CONFIRMÉE");
                statusBadge.setStyle("-fx-background-color: #DCFCE7; -fx-text-fill: #166534; -fx-padding: 5 15; -fx-background-radius: 20; -fx-font-weight: bold;");
                ticketBtn.setDisable(false);
            } else if (status.contains("refuse") || status.contains("annule")) {
                statusBadge.setText("ANNULÉE");
                statusBadge.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B; -fx-padding: 5 15; -fx-background-radius: 20; -fx-font-weight: bold;");
                ticketBtn.setDisable(true);
            } else {
                statusBadge.setText("EN ATTENTE");
                statusBadge.setStyle("-fx-background-color: #FEF3C7; -fx-text-fill: #92400E; -fx-padding: 5 15; -fx-background-radius: 20; -fx-font-weight: bold;");
                ticketBtn.setDisable(true);
            }
        }

        if (res.getEvent() != null) {
            lieuLabel.setText(res.getEvent().getLieu() != null ? res.getEvent().getLieu() : "N/A");
            if (res.getEvent().getDateDebut() != null) {
                dateLabel.setText(res.getEvent().getDateDebut().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            } else {
                dateLabel.setText("N/A");
            }
            placesLabel.setText(String.valueOf(res.getEvent().getPlacesDisponibles()));
            
            double prixUnitaire = res.getEvent().getPrix() != null ? res.getEvent().getPrix().doubleValue() : 0.0;
            prixDetailLabel.setText(String.format("%.2f TND × %d", prixUnitaire, res.getNombrePersonnes()));
        }

        nomLabel.setText(res.getNomComplet() != null ? res.getNomComplet() : "N/A");
        emailLabel.setText(res.getEmail() != null ? res.getEmail() : "N/A");
        telLabel.setText(res.getTelephone() != null ? res.getTelephone() : "N/A");
        paxLabel.setText(String.valueOf(res.getNombrePersonnes()));
        prixTotalLabel.setText(String.format("%.2f TND", res.getPrixTotal() != null ? res.getPrixTotal() : 0.0));
        demandesLabel.setText(res.getDemandesSpeciales() != null && !res.getDemandesSpeciales().isEmpty() ? res.getDemandesSpeciales() : "none");
        
        // Adultes/Enfants logic (default to 0 as in screenshot if not tracked)
        adultesLabel.setText("0"); 
        enfantsLabel.setText("0");

        ticketBtn.setOnAction(e -> handleGetTicket());
    }

    private void handleGetTicket() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/example/pi_dev/events/TicketView.fxml"));
            javafx.scene.layout.AnchorPane root = loader.load();
            
            TicketViewController controller = loader.getController();
            controller.setTicketData(reservation);
            
            Stage stage = new Stage();
            stage.initStyle(javafx.stage.StageStyle.TRANSPARENT); // For rounded corners effect
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            stage.setScene(scene);
            stage.show();
        } catch (java.io.IOException e) {
            System.err.println("Erreur ouverture ticket: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}
