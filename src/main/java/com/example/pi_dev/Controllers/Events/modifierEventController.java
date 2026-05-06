package com.example.pi_dev.Controllers.Events;

import com.example.pi_dev.Entities.Events.Event;
import com.example.pi_dev.Entities.Events.Activite;
import com.example.pi_dev.Services.Events.WeatherService;
import com.example.pi_dev.Utils.Events.Mydatabase;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class modifierEventController implements Initializable {

    @FXML private ComboBox<String> activiteCombo;
    @FXML private VBox activitesSelectionneesContainer;
    @FXML private TextField nomorgField;
    @FXML private TextField lieuField;
    @FXML private TextField emailField;
    @FXML private TextField telephoneorgField;
    @FXML private DatePicker dateDebutPicker;
    @FXML private DatePicker dateFinPicker;
    @FXML private TextField prixField;
    @FXML private TextField capaciteField;
    @FXML private TextArea equipementField;
    @FXML private TextArea descriptionField;
    @FXML private Label imageStatusLabel;
    @FXML private Button importerImageButton;
    @FXML private ImageView imagePrincipaleView;
    @FXML private HBox photosContainer;
    @FXML private Button ajouterPhotoButton;
    @FXML private TextField videoYoutubeField;
    @FXML private CheckBox check1;
    @FXML private CheckBox check2;
    @FXML private CheckBox check3;
    @FXML private CheckBox check4;

    private Connection connection;
    private WeatherService weatherService;
    private List<Object> activitesList = new ArrayList<>();
    private Event currentEvent;
    private List<String> photosPaths = new ArrayList<>();
    private String imagePrincipalePath;
    private static final String UPLOADS_DIR = "uploads/events/";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        connection = Mydatabase.getInstance().getConnextion();
        weatherService = new WeatherService();
        loadActivites();
        createDirectoriesIfNotExists();
    }

    private void createDirectoriesIfNotExists() {
        try {
            Path uploadsPath = Paths.get(UPLOADS_DIR);
            if (!Files.exists(uploadsPath)) {
                Files.createDirectories(uploadsPath);
                System.out.println("Répertoire uploads créé: " + uploadsPath.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la création du répertoire uploads: " + e.getMessage());
        }
    }

    private void loadActivites() {
        try {
            String sql = "SELECT id, titre, type_activite, description, image FROM activites";
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {
                int id = rs.getInt("id");
                String titre = rs.getString("titre");
                String type = rs.getString("type_activite");
                String description = rs.getString("description");
                String imagePath = rs.getString("image");

                Activite activite = new Activite();
                activite.setId(id);
                activite.setTitre(titre);
                activite.setTypeActivite(type);
                activite.setDescription(description);
                activite.setImage(imagePath);

                activitesList.add(activite);
                activiteCombo.getItems().add(titre);
                System.out.println("Activité ajoutée au combo: " + titre);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du chargement des activités: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void setEventId(int eventId) {
        try {
            String sql = "SELECT * FROM events WHERE id = ?";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, eventId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                currentEvent = new Event();
                currentEvent.setId(rs.getInt("id"));
                currentEvent.setIdActivite(rs.getInt("id_activite"));
                currentEvent.setLieu(rs.getString("lieu"));
                currentEvent.setOrganisateur(rs.getString("organisateur"));
                currentEvent.setEmail(rs.getString("email"));
                currentEvent.setTelephone(rs.getInt("telephone"));
                currentEvent.setDescription(rs.getString("description"));
                currentEvent.setMaterielsNecessaires(rs.getString("materiels_necessaires"));
                currentEvent.setPrix(rs.getBigDecimal("prix"));
                currentEvent.setCapaciteMax(rs.getInt("capacite_max"));
                currentEvent.setDateDebut(rs.getTimestamp("date_debut") != null ? rs.getTimestamp("date_debut").toLocalDateTime() : null);
                currentEvent.setDateFin(rs.getTimestamp("date_fin") != null ? rs.getTimestamp("date_fin").toLocalDateTime() : null);
                currentEvent.setImage(rs.getString("image"));
                currentEvent.setVideoYoutube(rs.getString("video_youtube"));

                setEventData(currentEvent);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du chargement de l'événement: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void setEventData(Event event) {
        this.currentEvent = event;

        try {
            System.out.println("DEBUG: Chargement de l'événement ID: " + event.getId());

            if (nomorgField != null) {
                nomorgField.setText(event.getOrganisateur() != null ? event.getOrganisateur() : "");
            }
            if (lieuField != null) {
                lieuField.setText(event.getLieu() != null ? event.getLieu() : "");
            }
            if (emailField != null) {
                emailField.setText(event.getEmail() != null ? event.getEmail() : "");
                System.out.println("DEBUG: Email field rempli avec: " + event.getEmail());
            }
            if (telephoneorgField != null) {
                telephoneorgField.setText(event.getTelephone() != null ? event.getTelephone().toString() : "");
                System.out.println("DEBUG: Téléphone field rempli avec: " + event.getTelephone());
            }
            if (descriptionField != null) {
                descriptionField.setText(event.getDescription() != null ? event.getDescription() : "");
            }
            if (equipementField != null) {
                equipementField.setText(event.getMaterielsNecessaires() != null ? event.getMaterielsNecessaires() : "");
            }
            if (videoYoutubeField != null) {
                videoYoutubeField.setText(event.getVideoYoutube() != null ? event.getVideoYoutube() : "");
            }
            if (prixField != null) {
                prixField.setText(event.getPrix() != null ? String.valueOf(event.getPrix()) : "0");
            }
            if (capaciteField != null) {
                capaciteField.setText(event.getCapaciteMax() != null ? String.valueOf(event.getCapaciteMax()) : "20");
            }

            if (dateDebutPicker != null && event.getDateDebut() != null) {
                dateDebutPicker.setValue(event.getDateDebut().toLocalDate());
            }
            if (dateFinPicker != null && event.getDateFin() != null) {
                dateFinPicker.setValue(event.getDateFin().toLocalDate());
            }

            if (imagePrincipaleView != null && event.getImage() != null && !event.getImage().trim().isEmpty()) {
                try {
                    File imageFile = new File(event.getImage());
                    if (imageFile.exists()) {
                        Image image = new Image(imageFile.toURI().toString());
                        imagePrincipaleView.setImage(image);
                        imagePrincipalePath = event.getImage();
                        if (imageStatusLabel != null) {
                            imageStatusLabel.setText("Image actuelle: " + imageFile.getName());
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Erreur lors du chargement de l'image: " + e.getMessage());
                }
            }

            if (activiteCombo != null && event.getIdActivite() > 0) {
                for (int i = 0; i < activitesList.size(); i++) {
                    Activite activite = (Activite) activitesList.get(i);
                    if (activite.getId() == event.getIdActivite()) {
                        activiteCombo.getSelectionModel().select(i);
                        System.out.println("DEBUG: Activité sélectionnée: " + activite.getTitre());
                        break;
                    }
                }
            }

            System.out.println("DEBUG: Événement chargé avec succès");

        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération des données de l'événement: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    void modifierEvent(ActionEvent event) {
        try {
            if (!check1.isSelected() || !check2.isSelected() || !check3.isSelected() || !check4.isSelected()) {
                showAlert("Veuillez cocher toutes les cases de validation avant de soumettre");
                return;
            }

            String nomEvent = nomorgField.getText().trim();
            String lieu = lieuField.getText().trim();
            String email = emailField.getText().trim();
            String description = descriptionField.getText().trim();
            String equipement = equipementField.getText().trim();
            String videoYoutube = videoYoutubeField.getText().trim();

            if (nomEvent.isEmpty()) {
                showAlert("Le nom de l'événement est obligatoire");
                return;
            }

            if (lieu.isEmpty()) {
                showAlert("Le lieu de l'événement est obligatoire");
                return;
            }

            if (description.isEmpty()) {
                showAlert("La description de l'événement est obligatoire");
                return;
            }

            if (dateDebutPicker.getValue() == null) {
                showAlert("La date de début est obligatoire");
                return;
            }

            if (dateFinPicker.getValue() == null) {
                showAlert("La date de fin est obligatoire");
                return;
            }

            if (dateFinPicker.getValue().isBefore(dateDebutPicker.getValue())) {
                showAlert("La date de fin doit être après la date de début");
                return;
            }

            double prixValue = 0;
            try {
                String prix = prixField.getText().trim();
                if (!prix.isEmpty()) {
                    prixValue = Double.parseDouble(prix);
                    if (prixValue < 0) {
                        showAlert("Le prix ne peut pas être négatif");
                        return;
                    }
                }
            } catch (NumberFormatException e) {
                showAlert("Veuillez entrer un prix valide");
                return;
            }

            int capaciteValue = 20;
            try {
                String capacite = capaciteField.getText().trim();
                if (!capacite.isEmpty()) {
                    capaciteValue = Integer.parseInt(capacite);
                    if (capaciteValue <= 0) {
                        showAlert("La capacité doit être supérieure à 0");
                        return;
                    }
                }
            } catch (NumberFormatException e) {
                showAlert("Veuillez entrer une capacité valide");
                return;
            }

            int activiteId = getSelectedActiviteId();

            String sql = "UPDATE events SET id_activite = ?, lieu = ?, organisateur = ?, email = ?, telephone = ?, description = ?, materiels_necessaires = ?, date_debut = ?, date_fin = ?, prix = ?, capacite_max = ?, places_disponibles = ?, statut = ?, date_modification = CURRENT_TIMESTAMP, video_youtube = ?, image = ? WHERE id = ?";

            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, activiteId);
            pstmt.setString(2, lieu);
            pstmt.setString(3, nomEvent);
            pstmt.setString(4, email);
            pstmt.setString(5, telephoneorgField.getText().trim());
            pstmt.setString(6, description);
            pstmt.setString(7, equipement);
            pstmt.setTimestamp(8, dateDebutPicker.getValue() != null ? java.sql.Timestamp.valueOf(dateDebutPicker.getValue().atStartOfDay()) : null);
            pstmt.setTimestamp(9, dateFinPicker.getValue() != null ? java.sql.Timestamp.valueOf(dateFinPicker.getValue().atStartOfDay()) : null);
            pstmt.setDouble(10, prixValue);
            pstmt.setInt(11, capaciteValue);
            pstmt.setInt(12, capaciteValue);
            pstmt.setString(13, "A_VENIR");
            pstmt.setString(14, videoYoutube);
            pstmt.setString(15, imagePrincipalePath != null ? imagePrincipalePath : (currentEvent != null ? currentEvent.getImage() : ""));
            pstmt.setInt(16, currentEvent != null ? currentEvent.getId() : 0);

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                showAlert("Événement modifié avec succès!");

                CatalogueRefreshManager.getInstance().requestRefresh();
                fermerFenetre();
            } else {
                showAlert("Erreur lors de la modification de l'événement");
            }

        } catch (Exception e) {
            System.err.println("ERREUR lors de la modification de l'événement:");
            e.printStackTrace();
            showAlert("Erreur lors de la modification de l'événement: " + e.getMessage());
        }
    }

    private int getSelectedActiviteId() {
        String selectedTitre = activiteCombo.getValue();
        if (selectedTitre != null && !selectedTitre.trim().isEmpty()) {
            try {
                for (Object activiteObj : activitesList) {
                    if (activiteObj instanceof Activite) {
                        Activite currentActivite = (Activite) activiteObj;
                        if (selectedTitre.equals(currentActivite.getTitre())) {
                            return currentActivite.getId();
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Erreur lors de la récupération de l'ID de l'activité: " + e.getMessage());
            }
        }
        return 1;
    }

    @FXML
    void importerImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image pour l'événement");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            try {
                Image image = new Image(selectedFile.toURI().toString());
                imagePrincipaleView.setImage(image);
                imagePrincipalePath = selectedFile.getAbsolutePath();

                imageStatusLabel.setText("Nouvelle image: " + selectedFile.getName());

                System.out.println("Image sélectionnée: " + imagePrincipalePath);

            } catch (Exception e) {
                showAlert("Erreur lors du chargement de l'image: " + e.getMessage());
            }
        }
    }

    @FXML
    void ajouterPhoto(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Ajouter des photos supplémentaires");

        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
                "Fichiers images (*.jpg, *.jpeg, *.png, *.gif)", "*.jpg", "*.jpeg", "*.png", "*.gif");
        fileChooser.getExtensionFilters().add(extFilter);

        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(null);

        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            for (File selectedFile : selectedFiles) {
                try {
                    String fileName = System.currentTimeMillis() + "_" + selectedFile.getName();
                    Path targetPath = Paths.get(UPLOADS_DIR + fileName);
                    Files.copy(selectedFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

                    String photoPath = UPLOADS_DIR + fileName;
                    photosPaths.add(photoPath);

                    createPhotoThumbnail(photoPath, selectedFile);

                    System.out.println("Photo supplémentaire ajoutée: " + photoPath);

                } catch (IOException e) {
                    System.err.println("Erreur lors de la copie de la photo: " + e.getMessage());
                }
            }
        }
    }

    private void createPhotoThumbnail(String photoPath, File originalFile) {
        try {
            HBox container = new HBox(5);
            container.setStyle("-fx-background-color: #f0f0f0; -fx-border-radius: 5; -fx-padding: 5;");
            ImageView thumb = new ImageView(new Image(originalFile.toURI().toString()));
            thumb.setFitHeight(60); thumb.setFitWidth(60); thumb.setPreserveRatio(true);
            Button del = new Button("❌");
            del.setStyle("-fx-background-color: transparent; -fx-border: none; -fx-cursor: hand;");
            del.setOnAction(e -> { photosContainer.getChildren().remove(container); photosPaths.remove(photoPath); });
            container.getChildren().addAll(thumb, del);
            if (photosContainer != null) photosContainer.getChildren().add(container);
        } catch (Exception e) {
            System.err.println("Erreur vignette: " + e.getMessage());
        }
    }

    @FXML
    void goToCatalogue(ActionEvent event) {
        fermerFenetre();
    }

    @FXML
    void annulerEvent(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Annuler la modification");
        alert.setHeaderText("Êtes-vous sûr de vouloir annuler ?");
        alert.setContentText("Toutes les modifications non sauvegardées seront perdues.");
        alert.showAndWait().ifPresent(r -> { if (r == ButtonType.OK) fermerFenetre(); });
    }

    @FXML
    void supprimerEvent(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer l'événement");
        alert.setHeaderText("Êtes-vous sûr de vouloir supprimer cet événement ?");
        alert.setContentText("Cette action est irréversible et supprimera définitivement l'événement.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK && currentEvent != null) {
                try {
                    String sql = "DELETE FROM events WHERE id = ?";
                    PreparedStatement pstmt = connection.prepareStatement(sql);
                    pstmt.setInt(1, currentEvent.getId());
                    int rowsAffected = pstmt.executeUpdate();

                    if (rowsAffected > 0) {
                        showAlert("Événement supprimé avec succès!");
                        CatalogueRefreshManager.getInstance().requestRefresh();
                        fermerFenetre();
                    } else {
                        showAlert("Erreur lors de la suppression de l'événement");
                    }
                } catch (SQLException e) {
                    System.err.println("Erreur lors de la suppression: " + e.getMessage());
                    showAlert("Erreur lors de la suppression: " + e.getMessage());
                }
            }
        });
    }

    private void fermerFenetre() {
        Stage stage = (Stage) nomorgField.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}