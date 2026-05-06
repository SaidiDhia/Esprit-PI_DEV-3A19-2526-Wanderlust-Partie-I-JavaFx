package com.example.pi_dev.Controllers.Events;

import com.example.pi_dev.Entities.Events.CategorieActivite;
import com.example.pi_dev.Entities.Events.TypeActivite;
import com.example.pi_dev.Session.Session;
import com.example.pi_dev.Utils.Events.Mydatabase;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public class AjoutActiviteController {

    @FXML private TextField titreField;
    @FXML private TextArea descriptionArea;
    @FXML private ComboBox<CategorieActivite> categorieCombo;
    @FXML private ComboBox<TypeActivite> typeactField;
    @FXML private TextField imageField1;
    @FXML private Button importImageButton;
    @FXML private Button ajouterButton;
    @FXML private Button annulerButton;

    private Connection connection;
    private String selectedImagePath = "";

    public void initialize() {
        initializeDatabase();
        initializeCategories();
        initializeTypes();
    }

    private void initializeDatabase() {
        try {
            connection = Mydatabase.getInstance().getConnextion();
        } catch (Exception e) {
            System.err.println("Database connection error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initializeCategories() {
        categorieCombo.getItems().addAll(CategorieActivite.values());
        categorieCombo.setOnAction(e -> updateTypes());
    }

    private void updateTypes() {
        CategorieActivite selectedCategorie = categorieCombo.getValue();
        if (selectedCategorie != null) {
            typeactField.getItems().clear();
            typeactField.getItems().addAll(TypeActivite.getTypesByCategorie(selectedCategorie));
        }
    }

    private void initializeTypes() {
        // Initialiser avec une liste vide
        typeactField.setPromptText("Veuillez d'abord sélectionner une catégorie");
    }

    @FXML
    void importerImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image pour l'activité");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.bmp")
        );

        File selectedFile = fileChooser.showOpenDialog(new Stage());

        if (selectedFile != null) {
            selectedImagePath = selectedFile.getAbsolutePath();
            imageField1.setText(selectedFile.getName());
            showAlert("Image sélectionnée : " + selectedFile.getName());
        }
    }

    @FXML
    void ajouter(ActionEvent event) {
        String titre = titreField.getText().trim();
        String description = descriptionArea.getText().trim();
        TypeActivite type = typeactField.getValue();
        CategorieActivite categorie = categorieCombo.getValue();
        String imagePath = selectedImagePath.isEmpty() ? "default.jpg" : selectedImagePath;

        // Validation des champs
        if (titre.isEmpty()) {
            showAlert("Le titre est obligatoire");
            titreField.requestFocus();
            return;
        }

        if (titre.length() < 3 || titre.length() > 100) {
            showAlert("Le titre doit contenir entre 3 et 100 caractères");
            titreField.requestFocus();
            return;
        }

        if (description.isEmpty()) {
            showAlert("La description est obligatoire");
            descriptionArea.requestFocus();
            return;
        }

        if (description.length() < 10 || description.length() > 500) {
            showAlert("La description doit contenir entre 10 et 500 caractères");
            descriptionArea.requestFocus();
            return;
        }

        if (categorie == null) {
            showAlert("La catégorie est obligatoire");
            categorieCombo.requestFocus();
            return;
        }

        if (type == null) {
            showAlert("Le type d'activité est obligatoire");
            typeactField.requestFocus();
            return;
        }

        try {
            String sql = "INSERT INTO activites (titre, description, type_activite, categorie, image, status, created_by_id, date_creation, date_modification) VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, titre);
            pstmt.setString(2, description);
            pstmt.setString(3, type.getNom());
            pstmt.setString(4, categorie.toDbValue());
            pstmt.setString(5, imagePath);
            pstmt.setString(6, "accepte");
            String currentUserId = Session.getCurrentUserId();
            if (currentUserId == null || currentUserId.isBlank()) {
                pstmt.setNull(7, Types.CHAR);
            } else {
                pstmt.setString(7, currentUserId);
            }
            pstmt.executeUpdate();

            showAlert("Activité ajoutée avec succès");
            fermerFenetre();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur lors de l'ajout de l'activité");
        }
    }

    @FXML
    void goToCatalogue(ActionEvent event) {
        fermerFenetre();
    }

    @FXML
    void annuler(ActionEvent event) {
        fermerFenetre();
    }

    private void fermerFenetre() {
        Stage stage = (Stage) titreField.getScene().getWindow();
        stage.close();

        // Rafraîchir le catalogue automatiquement
        CatalogueRefreshManager.getInstance().requestRefresh();
        refreshCatalogue();
    }

    private void refreshCatalogue() {
        try {
            // Trouver toutes les fenêtres ouvertes et rafraîchir les catalogues
            for (javafx.stage.Window window : javafx.stage.Window.getWindows()) {
                if (window instanceof Stage) {
                    Stage stage = (Stage) window;
                    if (stage.getTitle() != null && stage.getTitle().contains("Catalogue")) {
                        // Envoyer un événement de rafraîchissement
                        stage.requestFocus();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(message);
        alert.showAndWait();
    }
}