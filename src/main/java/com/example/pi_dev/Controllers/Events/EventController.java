package com.example.pi_dev.Controllers.Events;

import com.example.pi_dev.Services.Events.WeatherService;
import com.example.pi_dev.Utils.Events.Mydatabase;
import com.example.pi_dev.Entities.Events.Activite;

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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class EventController implements Initializable {

    @FXML private ComboBox<String> activiteCombo;
    @FXML private VBox activitesSelectionneesContainer;
    @FXML private TextField nomorgField;
    @FXML private TextField lieuField;
    @FXML private TextField emailField;
    @FXML private DatePicker dateDebutPicker;
    @FXML private DatePicker dateFinPicker;
    @FXML private TextField prixField;
    @FXML private TextField capaciteField;
    @FXML private TextField telephoneorgField;
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
    private WeatherService.WeatherData currentWeather;
    private List<Object> activitesList = new ArrayList<>();
    private List<Object> activitesSelectionnees = new ArrayList<>();
    private Object currentActivite;
    private List<byte[]> photosData = new ArrayList<>();
    private byte[] imagePrincipaleData;

    private static final String UPLOADS_DIR = "uploads/events/";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        connection = Mydatabase.getInstance().getConnextion();
        weatherService = new WeatherService();
        loadWeatherData();
        loadActivites();
        createUploadsDirectory();
    }

    private void createUploadsDirectory() {
        try {
            Path path = Paths.get(UPLOADS_DIR);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                System.out.println("Répertoire uploads créé: " + path.toAbsolutePath());
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

                Object activite = createActiviteObject(id, titre, type, description, imagePath);
                if (activite != null) {
                    activitesList.add(activite);
                    activiteCombo.getItems().add(titre);
                    System.out.println("Activité ajoutée au combo: " + titre);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du chargement des activités: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Object createActiviteObject(int id, String titre, String type, String description, String imagePath) {
        try {
            Activite activite = new Activite();
            activite.setId(id);
            activite.setTitre(titre);
            activite.setTypeActivite(type);
            activite.setDescription(description);
            activite.setImage(imagePath);
            return activite;
        } catch (Exception e) {
            System.err.println("Erreur création Activite: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @FXML
    void ajouterEvent(ActionEvent event) {
        try {
            if (!check1.isSelected() || !check2.isSelected() || !check3.isSelected() || !check4.isSelected()) {
                showAlert("Veuillez cocher toutes les cases de validation avant de soumettre l'événement");
                return;
            }

            if (activitesSelectionnees.isEmpty()) {
                showAlert("Veuillez ajouter au moins une activité pour cet événement");
                return;
            }

            if (imagePrincipaleData == null || imagePrincipaleData.length == 0) {
                showAlert("Veuillez sélectionner une image principale pour l'événement");
                return;
            }

            String nomEvent = nomorgField.getText().trim();
            String lieu = lieuField.getText().trim();
            String email = emailField.getText().trim();
            String description = descriptionField.getText().trim();
            String equipement = equipementField.getText().trim();
            String youtubeUrl = videoYoutubeField != null ? videoYoutubeField.getText().trim() : "";
            if (!youtubeUrl.isEmpty() && !isValidYouTubeUrl(youtubeUrl)) {
                showAlert("L'URL YouTube n'est pas valide");
                return;
            }
            String prixText = prixField.getText().trim();
            String capaciteText = capaciteField.getText().trim();

            if (nomEvent.isEmpty()) {
                showAlert("Le nom de l'événement est obligatoire");
                return;
            }

            if (lieu.isEmpty()) {
                showAlert("Le lieu de l'événement est obligatoire");
                return;
            }

            if (email.isEmpty()) {
                showAlert("L'email de l'événement est obligatoire");
                return;
            }

            if (dateDebutPicker.getValue() == null) {
                showAlert("La date de début est obligatoire");
                return;
            }

            double prixValue = 0.0;
            if (!prixText.isEmpty()) {
                try {
                    prixValue = Double.parseDouble(prixText);
                    if (prixValue < 0) {
                        showAlert("Le prix ne peut pas être négatif");
                        return;
                    }
                } catch (NumberFormatException e) {
                    showAlert("Le prix doit être un nombre valide");
                    return;
                }
            }

            int capaciteValue = 0;
            if (!capaciteText.isEmpty()) {
                try {
                    capaciteValue = Integer.parseInt(capaciteText);
                    if (capaciteValue <= 0) {
                        showAlert("La capacité doit être supérieure à 0");
                        return;
                    }
                } catch (NumberFormatException e) {
                    showAlert("La capacité doit être un nombre valide");
                    return;
                }
            }

            for (Object activite : activitesSelectionnees) {
                Activite activiteEntity = (Activite) activite;
                int activiteId = activiteEntity.getId();

                String sql = "INSERT INTO events (id_activite, lieu, organisateur, email, description, materiels_necessaires, prix, capacite_max, places_disponibles, date_debut, date_fin, image, video_youtube, statut, date_creation, date_modification) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

                pstmt.setInt(1, activiteId);
                pstmt.setString(2, lieu);
                pstmt.setString(3, nomEvent);
                pstmt.setString(4, email);
                pstmt.setString(5, description);
                pstmt.setString(6, equipement);
                pstmt.setDouble(7, prixValue);
                pstmt.setInt(8, capaciteValue);
                pstmt.setInt(9, capaciteValue);
                pstmt.setDate(10, dateDebutPicker.getValue() != null ? java.sql.Date.valueOf(dateDebutPicker.getValue()) : null);
                pstmt.setDate(11, dateFinPicker.getValue() != null ? java.sql.Date.valueOf(dateFinPicker.getValue()) : null);
                // Save primary image to disk and persist its relative path instead of raw bytes
                String primaryImagePath = savePrimaryImageToUploads();
                pstmt.setString(12, primaryImagePath != null ? primaryImagePath : "");
                pstmt.setString(13, videoYoutubeField != null ? videoYoutubeField.getText().trim() : "");
                pstmt.setString(14, "A_VENIR");
                pstmt.setTimestamp(15, new Timestamp(System.currentTimeMillis()));
                pstmt.setTimestamp(16, new Timestamp(System.currentTimeMillis()));

                int rowsAffected = pstmt.executeUpdate();

                int eventId = -1;
                ResultSet generatedKeys = pstmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    eventId = generatedKeys.getInt(1);
                }

                if (eventId != -1 && !photosData.isEmpty()) {
                    for (byte[] photoData : photosData) {
                        String photoSql = "INSERT INTO event_photos (id_event, photo_data, description) VALUES (?, ?, ?)";
                        PreparedStatement photoPstmt = connection.prepareStatement(photoSql);
                        photoPstmt.setInt(1, eventId);
                        photoPstmt.setBytes(2, photoData);
                        photoPstmt.setString(3, "Photo supplémentaire");
                        photoPstmt.executeUpdate();
                    }
                }

                if (rowsAffected <= 0) {
                    showAlert("Erreur lors de la création de l'événement pour une des activités");
                    return;
                }
            }

            showAlert("Événement créé avec succès pour " + activitesSelectionnees.size() + " activité(s) !");
            clearFields();

            CatalogueRefreshManager.getInstance().requestRefresh();
            fermerFenetre();

        } catch (Exception e) {
            System.err.println("ERREUR lors de la création de l'événement:");
            e.printStackTrace();
            showAlert("Erreur lors de la création de l'événement: " + e.getMessage());
        }
    }

    private boolean isValidYouTubeUrl(String youtubeUrl) {
        if (youtubeUrl == null || youtubeUrl.trim().isEmpty()) {
            return true;
        }

        String url = youtubeUrl.trim().toLowerCase();

        return url.contains("youtube.com/watch") ||
                url.contains("youtu.be/") ||
                url.contains("youtube.com/embed/") ||
                url.contains("youtube.com/shorts/") ||
                url.contains("m.youtube.com/watch") ||
                url.contains("youtube.com/v/");
    }

    private void clearFields() {
        nomorgField.clear();
        lieuField.clear();
        emailField.clear();
        descriptionField.clear();
        equipementField.clear();
        prixField.clear();
        capaciteField.clear();
        videoYoutubeField.clear();
        dateDebutPicker.setValue(null);
        dateFinPicker.setValue(null);
        activiteCombo.setValue(null);
        activitesSelectionnees.clear();
        updateActivitesSelectionneesDisplay();
        imagePrincipaleData = null;
        if (imagePrincipaleView != null) {
            imagePrincipaleView.setImage(null);
        }
        if (imageStatusLabel != null) {
            imageStatusLabel.setText("Aucune image sélectionnée");
        }
        photosData.clear();
        if (photosContainer != null) {
            photosContainer.getChildren().clear();
        }
        check1.setSelected(false);
        check2.setSelected(false);
        check3.setSelected(false);
        check4.setSelected(false);
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void fermerFenetre() {
        Stage stage = (Stage) nomorgField.getScene().getWindow();
        stage.close();
    }

    private void loadWeatherData() {
        weatherService.getCurrentWeather("Tunis", new WeatherService.WeatherCallback() {
            @Override
            public void onWeatherReceived(WeatherService.WeatherData weather) {
                currentWeather = weather;
                System.out.println("Météo chargée: " + weather.getDescription() + " (" + weather.getTemperatureDisplay() + ")");
            }

            @Override
            public void onError(String error) {
                System.err.println("Erreur lors du chargement météo: " + error);
                currentWeather = null;
            }
        });
    }

    private void updateActivitesSelectionneesDisplay() {
        activitesSelectionneesContainer.getChildren().clear();

        if (activitesSelectionnees.isEmpty()) {
            Label emptyLabel = new Label("Aucune activité sélectionnée");
            emptyLabel.setStyle("-fx-text-fill: #666; -fx-font-style: italic;");
            activitesSelectionneesContainer.getChildren().add(emptyLabel);
            return;
        }

        for (int i = 0; i < activitesSelectionnees.size(); i++) {
            Object activite = activitesSelectionnees.get(i);
            try {
                Activite activiteEntity = (Activite) activite;
                String titre = activiteEntity.getTitre();

                HBox activiteBox = new HBox(10);
                activiteBox.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-radius: 5; -fx-padding: 8;");
                activiteBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                Label label = new Label((i + 1) + ". " + titre);
                label.setStyle("-fx-text-fill: #333; -fx-font-size: 12px;");

                final int index = i;
                Button removeButton = new Button("❌");
                removeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #999; -fx-border-color: transparent; -fx-cursor: hand; -fx-padding: 2;");
                removeButton.setOnAction(e -> removeActivite(index));

                activiteBox.getChildren().addAll(label, new javafx.scene.layout.Region(), removeButton);
                activitesSelectionneesContainer.getChildren().add(activiteBox);

            } catch (Exception e) {
                System.err.println("Erreur lors de l'affichage de l'activité: " + e.getMessage());
                continue;
            }
        }
    }

    private void removeActivite(int index) {
        if (index >= 0 && index < activitesSelectionnees.size()) {
            Object removed = activitesSelectionnees.remove(index);
            updateActivitesSelectionneesDisplay();

            try {
                Activite activiteEntity = (Activite) removed;
                String titre = activiteEntity.getTitre();
                showAlert("Activité '" + titre + "' retirée");
            } catch (Exception e) {
                showAlert("Activité retirée");
            }
        }
    }

    @FXML
    void ajouterActivite(ActionEvent event) {
        String selectedActiviteText = activiteCombo.getValue();

        if (selectedActiviteText == null || selectedActiviteText.isEmpty()) {
            showAlert("Veuillez d'abord sélectionner une activité");
            return;
        }

        for (Object activite : activitesSelectionnees) {
            try {
                Activite activiteEntity = (Activite) activite;
                String titre = activiteEntity.getTitre();
                if (titre.equals(selectedActiviteText)) {
                    showAlert("Cette activité est déjà sélectionnée");
                    return;
                }
            } catch (Exception e) {
                continue;
            }
        }

        for (Object activite : activitesList) {
            try {
                Activite activiteEntity = (Activite) activite;
                String titre = activiteEntity.getTitre();
                if (titre.equals(selectedActiviteText)) {
                    activitesSelectionnees.add(activite);
                    updateActivitesSelectionneesDisplay();
                    activiteCombo.setValue(null);
                    showAlert("Activité ajoutée avec succès !");
                    return;
                }
            } catch (Exception e) {
                continue;
            }
        }

        showAlert("Activité non trouvée");
    }

    @FXML
    void importerImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image principale pour l'événement");

        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
                "Fichiers images (*.jpg, *.jpeg, *.png, *.gif)", "*.jpg", "*.jpeg", "*.png", "*.gif");
        fileChooser.getExtensionFilters().add(extFilter);

        File selectedFile = fileChooser.showOpenDialog(null);

        if (selectedFile != null) {
            try {
                byte[] imageBytes = Files.readAllBytes(selectedFile.toPath());
                imagePrincipaleData = imageBytes;

                Image image = new Image(selectedFile.toURI().toString());
                if (imagePrincipaleView != null) {
                    imagePrincipaleView.setImage(image);
                }

                if (imageStatusLabel != null) {
                    imageStatusLabel.setText("Image sélectionnée: " + selectedFile.getName());
                }

                System.out.println("Image principale chargée en mémoire: " + selectedFile.getName() + " (" + imageBytes.length + " bytes)");

            } catch (IOException e) {
                System.err.println("Erreur lors du chargement de l'image: " + e.getMessage());
                showAlert("Erreur lors du chargement de l'image");
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
                    byte[] photoBytes = Files.readAllBytes(selectedFile.toPath());
                    photosData.add(photoBytes);

                    createPhotoThumbnail(photoBytes, selectedFile);

                    System.out.println("Photo supplémentaire chargée en mémoire: " + selectedFile.getName() + " (" + photoBytes.length + " bytes)");

                } catch (IOException e) {
                    System.err.println("Erreur lors du chargement de la photo: " + e.getMessage());
                }
            }
        }
    }

    private String savePrimaryImageToUploads() {
        try {
            if (imagePrincipaleData == null || imagePrincipaleData.length == 0) return null;
            String ext = detectImageExtension(imagePrincipaleData);
            String fileName = "event_" + System.currentTimeMillis() + (ext != null ? ("." + ext) : ".png");
            Path target = Paths.get(UPLOADS_DIR, fileName);
            Files.write(target, imagePrincipaleData);
            return target.toString().replace("\\", "/");
        } catch (IOException e) {
            System.err.println("Erreur sauvegarde image principale: " + e.getMessage());
            return null;
        }
    }

    private String detectImageExtension(byte[] bytes) {
        if (bytes.length >= 8) {
            // PNG header: 89 50 4E 47 0D 0A 1A 0A
            if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) return "png";
            // JPEG header: FF D8
            if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) return "jpg";
            // GIF header: GIF87a or GIF89a
            if (bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F') return "gif";
        }
        return "png";
    }

    private void createPhotoThumbnail(byte[] photoBytes, File originalFile) {
        try {
            HBox photoContainer = new HBox(5);
            photoContainer.setStyle("-fx-background-color: #f0f0f0; -fx-border-radius: 5; -fx-padding: 5;");

            ByteArrayInputStream inputStream = new ByteArrayInputStream(photoBytes);
            Image image = new Image(inputStream, 80, 80, true, true);
            inputStream.close();

            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(80);
            imageView.setFitHeight(80);
            imageView.setPreserveRatio(true);
            imageView.setStyle("-fx-border-radius: 5; -fx-background-radius: 5;");

            Label nameLabel = new Label(originalFile.getName());
            nameLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");
            nameLabel.setPrefWidth(80);
            nameLabel.setWrapText(true);

            Button removeButton = new Button("❌");
            removeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: red; -fx-cursor: hand; -fx-font-size: 12px;");
            removeButton.setOnAction(e -> {
                photosContainer.getChildren().remove(photoContainer);
                photosData.remove(photoBytes);
                System.out.println("Photo supprimée: " + originalFile.getName());
            });

            VBox imageBox = new VBox(2);
            imageBox.getChildren().addAll(imageView, nameLabel);

            photoContainer.getChildren().addAll(imageBox, removeButton);
            photosContainer.getChildren().add(photoContainer);

        } catch (Exception e) {
            System.err.println("Erreur lors de la création de la miniature: " + e.getMessage());
        }
    }

    @FXML
    void annulerEvent(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Annuler la création");
        alert.setHeaderText("Êtes-vous sûr de vouloir annuler ?");
        alert.setContentText("Toutes les informations non sauvegardées seront perdues.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                fermerFenetre();
            }
        });
    }
}
