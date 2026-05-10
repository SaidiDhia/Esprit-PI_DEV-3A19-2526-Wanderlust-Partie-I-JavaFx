package com.example.pi_dev.Controllers.Events;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;

public class ActiviteCardController {

    @FXML private Button catalog;
    @FXML private ImageView activiteImageView;
    @FXML private Label titreLabel;
    @FXML private Label typeLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label categorieLabel;

    private Object currentActivite;

    public void initialize() {
        // Plus besoin du service météo dans l'ActiviteCard
    }

    public void setActiviteData(Object activite) {
        this.currentActivite = activite;

        // Utiliser reflection pour accéder aux méthodes de l'activité
        try {
            // Récupérer les données via reflection avec les bonnes méthodes
            String titre = (String) activite.getClass().getMethod("getTitre").invoke(activite);
            String type = (String) activite.getClass().getMethod("getTypeActivite").invoke(activite);
            String description = (String) activite.getClass().getMethod("getDescription").invoke(activite);
            String imagePath = (String) activite.getClass().getMethod("getImage").invoke(activite);

            System.out.println("DEBUG ActiviteCard - Titre: " + titre);
            System.out.println("DEBUG ActiviteCard - Type: " + type);
            System.out.println("DEBUG ActiviteCard - Description: " + description);
            System.out.println("DEBUG ActiviteCard - Image: " + imagePath);

            // Remplir les champs avec les données de l'activité
            titreLabel.setText(titre);
            typeLabel.setText(type);
            descriptionLabel.setText(description);

            // Charger l'image si le chemin est valide
            if (imagePath != null && !imagePath.isEmpty()) {
                try {
                    File file = new File(imagePath);
                    if (file.exists()) {
                        Image image = new Image(file.toURI().toString());
                        activiteImageView.setImage(image);
                    } else {
                        // Image par défaut si le fichier n'existe pas
                        loadDefaultImage();
                    }
                } catch (Exception e) {
                    System.err.println("Erreur lors du chargement de l'image: " + e.getMessage());
                    loadDefaultImage();
                }
            } else {
                // Image par défaut si pas de chemin
                loadDefaultImage();
            }

            // Afficher la catégorie si disponible
            try {
                Object categorie = activite.getClass().getMethod("getCategorie").invoke(activite);
                if (categorie != null) {
                    categorieLabel.setText(categorie.toString());
                }
            } catch (Exception e) {
                // Pas de catégorie, ce n'est pas grave
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadDefaultImage() {
        try {
            Image defaultImage = new Image(getClass().getResourceAsStream("/com/example/pi_dev/images/wanderlust-logo.png"));
            if (defaultImage != null) {
                activiteImageView.setImage(defaultImage);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de l'image par défaut");
        }
    }

    @FXML
    void goToCatalogue(ActionEvent event) {
        fermerFenetre();
    }

    @FXML
    void reserver(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pi_dev/events/Reservation.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Réserver");
            stage.setScene(new Scene(root));
            stage.setWidth(800);
            stage.setHeight(600);
            stage.setMinWidth(700);
            stage.setMinHeight(500);
            stage.centerOnScreen();
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur lors de l'ouverture de l'interface de réservation");
        }
    }

    private void fermerFenetre() {
        Stage stage = (Stage) titreLabel.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(message);
        alert.showAndWait();
    }
}