package com.example.pi_dev.Controllers.Events;

import com.example.pi_dev.Entities.Events.Reservation;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.text.DecimalFormat;

public class PanierController {

    @FXML private VBox panierContainer;
    @FXML private HBox panierVideBox;
    @FXML private Text totalLabel;
    @FXML private Button retourCatalogueButton;
    @FXML private Button viderPanierButton;
    @FXML private Button validerPanierButton;

    private DecimalFormat df = new DecimalFormat("#0.00");

    public void initialize() {
        afficherPanier();
    }

    private void afficherPanier() {
        ObservableList<Reservation> panier = ReservationController.panier;
        System.out.println("=== DEBUG: afficherPanier() appelé ===");
        System.out.println("Nombre de réservations dans le panier: " + panier.size());

        panierContainer.getChildren().clear();

        if (panier.isEmpty()) {
            System.out.println("DEBUG: Panier vide - affichage du message vide");
            panierVideBox.setVisible(true);
            totalLabel.setText("0 TND");
            validerPanierButton.setDisable(true);
        } else {
            System.out.println("DEBUG: Panier non vide - affichage des réservations");
            panierVideBox.setVisible(false);
            validerPanierButton.setDisable(false);

            double total = 0;
            for (Reservation reservation : panier) {
                System.out.println("DEBUG: Création de la carte pour réservation #" + reservation.getId() + " - " + reservation.getNomComplet());
                VBox reservationCard = createReservationCard(reservation);
                panierContainer.getChildren().add(reservationCard);
                total += reservation.getPrixUnitaire() * reservation.getNombrePersonnes();
            }
            System.out.println("DEBUG: Total calculé: " + total + " TND");
            totalLabel.setText(df.format(total) + " TND");
        }
    }

    private VBox createReservationCard(Reservation reservation) {
        VBox card = new VBox();
        card.setSpacing(10);
        card.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-width: 1; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 15;");
        card.setPrefWidth(900);

        HBox headerBox = new HBox();
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setSpacing(15);

        Text titreText = new Text("Réservation #" + reservation.getId());
        titreText.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-fill: #1a5f3f;");

        Text nomText = new Text(reservation.getNomComplet());
        nomText.setStyle("-fx-font-size: 16px; -fx-fill: #333;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Button supprimerButton = new Button("🗑️");
        supprimerButton.setStyle("-fx-background-color: #ff6b6b; -fx-text-fill: white; -fx-background-radius: 15; -fx-border-radius: 15; -fx-padding: 5 10; -fx-font-size: 12px; -fx-cursor: hand;");
        supprimerButton.setOnAction(e -> supprimerReservation(reservation));

        headerBox.getChildren().addAll(titreText, spacer, nomText, supprimerButton);

        VBox contentBox = new VBox();
        contentBox.setSpacing(8);

        Text emailText = new Text("📧 " + reservation.getEmail());
        emailText.setStyle("-fx-font-size: 14px; -fx-fill: #666;");

        Text telephoneText = new Text("📱 " + reservation.getTelephone());
        telephoneText.setStyle("-fx-font-size: 14px; -fx-fill: #666;");

        Text personnesText = new Text("👥 " + reservation.getNombrePersonnes() + " personne(s)");
        personnesText.setStyle("-fx-font-size: 14px; -fx-fill: #666;");

        double sousTotal = reservation.getPrixUnitaire() * reservation.getNombrePersonnes();
        Text prixText = new Text("💰 " + df.format(reservation.getPrixUnitaire()) + " TND × " + reservation.getNombrePersonnes() + " = " + df.format(sousTotal) + " TND");
        prixText.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-fill: #2d7a2d;");

        contentBox.getChildren().addAll(emailText, telephoneText, personnesText, prixText);

        card.getChildren().addAll(headerBox, contentBox);

        return card;
    }

    private void supprimerReservation(Reservation reservation) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation de suppression");
        confirmation.setHeaderText("Supprimer cette réservation?");
        confirmation.setContentText("Voulez-vous vraiment supprimer la réservation de " + reservation.getNomComplet() + " ?");

        if (confirmation.showAndWait().get() == ButtonType.OK) {
            ReservationController.panier.remove(reservation);
            afficherPanier();
        }
    }

    @FXML
    void viderPanier(ActionEvent event) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation de vidage");
        confirmation.setHeaderText("Vider le panier?");
        confirmation.setContentText("Voulez-vous vraiment supprimer toutes les réservations du panier?");

        if (confirmation.showAndWait().get() == ButtonType.OK) {
            ReservationController.panier.clear();
            afficherPanier();
        }
    }

    @FXML
    void validerPanier(ActionEvent event) {
        if (ReservationController.panier.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Panier vide");
            alert.setHeaderText("Impossible de valider");
            alert.setContentText("Votre panier est vide. Ajoutez des réservations avant de valider.");
            alert.showAndWait();
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation de validation");
        confirmation.setHeaderText("Valider le panier?");
        confirmation.setContentText("Confirmez la réservation de " + ReservationController.panier.size() + " événement(s) pour un total de " + totalLabel.getText());

        if (confirmation.showAndWait().get() == ButtonType.OK) {
            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Réservations validées");
            success.setHeaderText("🎉 Réservations confirmées!");
            success.setContentText("Vos " + ReservationController.panier.size() + " réservation(s) ont été validées avec succès.\n\nUn email de confirmation vous sera envoyé.");
            success.showAndWait();

            ReservationController.panier.clear();
            afficherPanier();
        }
    }

    @FXML
    void goToCatalogue(ActionEvent event) {
        fermerFenetre();
    }

    private void fermerFenetre() {
        Stage stage = (Stage) totalLabel.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText("Une erreur est survenue");
        alert.setContentText(message);
        alert.showAndWait();
    }
}