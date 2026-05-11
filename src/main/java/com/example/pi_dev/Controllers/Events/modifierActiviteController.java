package com.example.pi_dev.Controllers.Events;

import com.example.pi_dev.Entities.Events.Activite;
import com.example.pi_dev.Entities.Events.CategorieActivite;
import com.example.pi_dev.Entities.Events.TypeActivite;
import com.example.pi_dev.Session.Session;
import com.example.pi_dev.Utils.Events.Mydatabase;
import com.example.pi_dev.Utils.Events.CatalogueRefreshManager;
import java.sql.Timestamp;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class modifierActiviteController {

    @FXML
    private TextField titreField;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private ComboBox<CategorieActivite> categorieCombo;
    @FXML
    private ComboBox<TypeActivite> typeactField;
    @FXML
    private TextField imageField1;
    @FXML
    private TextField ageMinField;
    @FXML
    private Button importImageButton;
    @FXML
    private Button modifierButton;
    @FXML
    private Button supprimerButton;
    @FXML
    private Button annulerButton;
    @FXML
    private Button enhanceDescBtn;
    @FXML
    private Label titleLabel;

    private Activite currentActivite;
    private String currentActiviteOwnerId;
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
        typeactField.setPromptText("Veuillez d'abord sélectionner une catégorie");
    }

    @FXML
    void importerImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image pour l'activité");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.bmp"));

        File selectedFile = fileChooser.showOpenDialog(new Stage());

        if (selectedFile != null) {
            selectedImagePath = selectedFile.getAbsolutePath();
            imageField1.setText(selectedFile.getName());
            showAlert("Image sélectionnée : " + selectedFile.getName());
        }
    }

    public void setActiviteData(Activite activite) {
        this.currentActivite = activite;

        System.out.println("DEBUG Modification - ID: " + activite.getId());
        System.out.println("DEBUG Modification - Titre: " + activite.getTitre());
        System.out.println("DEBUG Modification - Type: " + activite.getTypeActivite());
        System.out.println("DEBUG Modification - Description: " + activite.getDescription());
        System.out.println("DEBUG Modification - Image: " + activite.getImage());

        titreField.setText(activite.getTitre());
        descriptionArea.setText(activite.getDescription());
        imageField1.setText(activite.getImage() != null ? activite.getImage() : "");
        if (ageMinField != null) {
            Integer am = activite.getAgeMinimum();
            ageMinField.setText(am != null ? String.valueOf(am) : "");
        }

        if (activite.getCategorie() != null) {
            categorieCombo.setValue(activite.getCategorie());
            updateTypes();

            if (activite.getTypeActivite() != null) {
                for (TypeActivite type : TypeActivite.getTypesByCategorie(activite.getCategorie())) {
                    if (type.getNom().equals(activite.getTypeActivite())) {
                        typeactField.setValue(type);
                        break;
                    }
                }
            }
        }

        if (titleLabel != null) {
            titleLabel.setText("Modifier l'activité: " + activite.getTitre());
        }

        loadOwnerAndEnforce();
    }

    @FXML
    void enhanceDescription(ActionEvent event) {
        CategorieActivite categorie = categorieCombo.getValue();
        TypeActivite type = typeactField.getValue();
        String currentDesc = descriptionArea.getText().trim();

        if (categorie == null || type == null) {
            showAlert("Veuillez sélectionner une catégorie et un type d'activité avant d'améliorer la description.");
            return;
        }

        String promptBase = "Rédige une description attrayante et engageante pour une activité touristique de catégorie '" 
                + categorie.toString() + "' et de type '" + type.getNom() + "'. ";
        if (!currentDesc.isEmpty()) {
            promptBase += "Voici les détails initiaux à inclure et améliorer : " + currentDesc + ". ";
        }
        promptBase += "Garde la description concise, environ 3 à 4 phrases maximum. Ne pas inclure de texte introductif comme 'Voici une description', donne directement le texte final.";

        final String finalPrompt = promptBase;

        enhanceDescBtn.setDisable(true);
        enhanceDescBtn.setText("⏳ Génération...");

        new Thread(() -> {
            com.example.pi_dev.Services.Events.EventsGeminiService geminiService = new com.example.pi_dev.Services.Events.EventsGeminiService();
            String generatedDesc = geminiService.generateResponse(finalPrompt);
            
            javafx.application.Platform.runLater(() -> {
                if (generatedDesc != null && !generatedDesc.startsWith("Error")) {
                    descriptionArea.setText(generatedDesc.trim());
                } else {
                    showAlert("Erreur lors de la génération : " + generatedDesc);
                }
                enhanceDescBtn.setDisable(false);
                enhanceDescBtn.setText("✨ Améliorer avec l'IA");
            });
        }).start();
    }

    @FXML
    void modifier(ActionEvent event) {
        if (!isOwner()) {
            showAlert("Vous n'êtes pas autorisé à modifier cette activité");
            return;
        }
        String titre = titreField.getText().trim();
        String description = descriptionArea.getText().trim();
        TypeActivite type = typeactField.getValue();
        CategorieActivite categorie = categorieCombo.getValue();
        String imagePath = selectedImagePath.isEmpty() ? imageField1.getText().trim() : selectedImagePath;

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
            // Parse age minimum (optional)
            Integer ageMin = null;
            String ageText = (ageMinField != null) ? ageMinField.getText().trim() : "";
            if (!ageText.isEmpty()) {
                try {
                    int v = Integer.parseInt(ageText);
                    if (v < 0) {
                        showAlert("L'âge minimum ne peut pas être négatif");
                        ageMinField.requestFocus();
                        return;
                    }
                    ageMin = v;
                } catch (NumberFormatException nfe) {
                    showAlert("L'âge minimum doit être un nombre entier");
                    ageMinField.requestFocus();
                    return;
                }
            }

            String sql = "UPDATE activites SET titre = ?, description = ?, type_activite = ?, categorie = ?, image = ?, age_minimum = ?, status = ?, date_modification = ? WHERE id = ? AND created_by_id = ?";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, titre);
            pstmt.setString(2, description);
            pstmt.setString(3, type.getNom());
            pstmt.setString(4, categorie.toDbValue());
            pstmt.setString(5, imagePath);
            if (ageMin == null) {
                pstmt.setNull(6, java.sql.Types.INTEGER);
            } else {
                pstmt.setInt(6, ageMin);
            }
            pstmt.setString(7, "en_attente");
            pstmt.setTimestamp(8, new Timestamp(System.currentTimeMillis()));
            pstmt.setInt(9, currentActivite.getId());
            pstmt.setString(10, Session.getCurrentUserId());

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                showAlert("Activité modifiée avec succès");
                fermerFenetre();
            } else {
                showAlert("Erreur lors de la modification de l'activité");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur lors de la modification de l'activité");
        }
    }

    @FXML
    void supprimer(ActionEvent event) {
        try {
            if (!isOwner()) {
                showAlert("Vous n'êtes pas autorisé à supprimer cette activité");
                return;
            }

            String sql = "DELETE FROM activites WHERE id = ? AND created_by_id = ?";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, currentActivite.getId());
            pstmt.setString(2, Session.getCurrentUserId());

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                showAlert("Activité supprimée avec succès");
                fermerFenetre();
            } else {
                showAlert("Erreur lors de la suppression de l'activité");
            }

        } catch (SQLException e) {
            showAlert("Erreur lors de la suppression: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    void annuler(ActionEvent event) {
        fermerFenetre();
    }

    @FXML
    void goToCatalogue(ActionEvent event) {
        fermerFenetre();
    }

    private void fermerFenetre() {
        Stage stage = (Stage) titreField.getScene().getWindow();
        stage.close();

        CatalogueRefreshManager.getInstance().requestRefresh();
        refreshCatalogue();
    }

    private void refreshCatalogue() {
        try {
            for (javafx.stage.Window window : javafx.stage.Window.getWindows()) {
                if (window instanceof Stage) {
                    Stage stage = (Stage) window;
                    if (stage.getTitle() != null && stage.getTitle().contains("Catalogue")) {
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

    private void loadOwnerAndEnforce() {
        if (currentActivite == null) {
            return;
        }
        try {
            String sql = "SELECT created_by_id FROM activites WHERE id = ?";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, currentActivite.getId());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                currentActiviteOwnerId = rs.getString("created_by_id");
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du chargement de l'auteur: " + e.getMessage());
        }

        if (!isOwner()) {
            showAlert("Vous n'êtes pas autorisé à modifier cette activité");
            disableForm();
        }
    }

    private boolean isOwner() {
        String currentUserId = Session.getCurrentUserId();
        if (currentUserId == null || currentUserId.isBlank()) {
            return false;
        }
        if (currentActiviteOwnerId == null || currentActiviteOwnerId.isBlank()) {
            return false;
        }
        return currentUserId.equals(currentActiviteOwnerId);
    }

    private void disableForm() {
        if (titreField != null)
            titreField.setDisable(true);
        if (descriptionArea != null)
            descriptionArea.setDisable(true);
        if (categorieCombo != null)
            categorieCombo.setDisable(true);
        if (typeactField != null)
            typeactField.setDisable(true);
        if (imageField1 != null)
            imageField1.setDisable(true);
        if (importImageButton != null)
            importImageButton.setDisable(true);
        if (modifierButton != null)
            modifierButton.setDisable(true);
        if (supprimerButton != null)
            supprimerButton.setDisable(true);
        if (enhanceDescBtn != null)
            enhanceDescBtn.setDisable(true);
    }
}