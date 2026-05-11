package com.example.pi_dev.Controllers.Events;

import com.example.pi_dev.Services.Events.EventsGeminiService;
import com.example.pi_dev.Utils.Events.Mydatabase;
import com.example.pi_dev.Entities.Events.Activite;
import com.example.pi_dev.Session.Session;
import com.example.pi_dev.Utils.Events.CatalogueRefreshManager;
import com.example.pi_dev.Utils.Users.UserSession;
import com.example.pi_dev.enums.RoleEnum;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

public class EventController implements Initializable {

    @FXML private WebView mapWebView;
    @FXML private TextField lieuField;
    @FXML private DatePicker dateDebutPicker;
    @FXML private TextField heureDebutField;
    @FXML private DatePicker dateFinPicker;
    @FXML private TextField heureFinField;
    @FXML private DatePicker dateLimitePicker;
    @FXML private ComboBox<String> activiteCombo;
    @FXML private VBox activitesSelectionneesContainer;
    @FXML private TextField prixField;
    @FXML private TextField capaciteField;
    @FXML private TextField nomorgField;
    @FXML private TextField telephoneorgField;
    @FXML private TextField emailField;
    @FXML private VBox imageDropZone;
    @FXML private ScrollPane photosScrollPane;
    @FXML private HBox photosContainer;
    @FXML private TextField videoYoutubeField;
    @FXML private TextArea equipementField;
    @FXML private Button genererEquipementBtn;
    @FXML private CheckBox check1;
    @FXML private CheckBox check2;
    @FXML private CheckBox check3;
    @FXML private CheckBox check4;
    @FXML private Button adminDashboardButton;

    private Connection connection;
    private List<Object> activitesList = new ArrayList<>();
    private List<Object> activitesSelectionnees = new ArrayList<>();
    private List<byte[]> photosData = new ArrayList<>();
    private byte[] imagePrincipaleData;
    private static final String UPLOADS_DIR = "uploads/events/";
    private Timer geocodeTimer;

    private static final String MAP_HTML = "<!DOCTYPE html><html><head><meta charset='UTF-8'>" +
            "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>" +
            "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>" +
            "<style>body{margin:0;padding:0;}#map{width:100%;height:280px;}</style></head>" +
            "<body><div id='map'></div><script>" +
            "var map=L.map('map').setView([36.8,10.18],7);" +
            "L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{attribution:'© OpenStreetMap'}).addTo(map);" +
            "var marker;" +
            "function moveMap(lat,lng,label){if(marker)map.removeLayer(marker);" +
            "marker=L.marker([lat,lng]).addTo(map).bindPopup(label).openPopup();map.setView([lat,lng],13);}" +
            "</script></body></html>";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        connection = Mydatabase.getInstance().getConnextion();
        com.example.pi_dev.Utils.Events.DatabaseUtils.ensureSchemaCorrect(connection);
        loadActivites();
        createUploadsDirectory();
        updateAdminButtonVisibility();
        initMap();
        setupLieuListener();
    }


    private void initMap() {
        WebEngine engine = mapWebView.getEngine();
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                if (lieuField != null && !lieuField.getText().isEmpty()) {
                    geocodeAndMoveMap(lieuField.getText().trim());
                }
            }
        });
        engine.loadContent(MAP_HTML);
    }

    private void setupLieuListener() {
        lieuField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (geocodeTimer != null) geocodeTimer.cancel();
            if (newVal == null || newVal.trim().length() < 3) return;
            geocodeTimer = new Timer();
            geocodeTimer.schedule(new TimerTask() {
                @Override
                public void run() { geocodeAndMoveMap(newVal.trim()); }
            }, 800);
        });
    }

    private void geocodeAndMoveMap(String query) {
        new Thread(() -> {
            try {
                String encoded = URLEncoder.encode(query, "UTF-8");
                String url = "https://nominatim.openstreetmap.org/search?q=" + encoded + "&format=json&limit=1";
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent", "WanderlustJavaFX/1.0")
                        .build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                String body = resp.body();
                if (body.contains("\"lat\"")) {
                    int latIdx = body.indexOf("\"lat\":\"") + 7;
                    int latEnd = body.indexOf("\"", latIdx);
                    int lonIdx = body.indexOf("\"lon\":\"") + 7;
                    int lonEnd = body.indexOf("\"", lonIdx);
                    double lat = Double.parseDouble(body.substring(latIdx, latEnd));
                    double lon = Double.parseDouble(body.substring(lonIdx, lonEnd));
                    String jsLabel = query.replace("'", "\\'");
                    Platform.runLater(() -> {
                        try {
                            mapWebView.getEngine().executeScript("moveMap(" + lat + "," + lon + ",'" + jsLabel + "')");
                        } catch (Exception e) {
                            System.err.println("Map script error (EventController): " + e.getMessage());
                        }
                    });
                }
            } catch (Exception e) {
                System.err.println("Geocode error: " + e.getMessage());
            }
        }).start();
    }

    private void createUploadsDirectory() {
        try {
            Path path = Paths.get(UPLOADS_DIR);
            if (!Files.exists(path)) Files.createDirectories(path);
        } catch (IOException e) {
            System.err.println("Erreur création répertoire uploads: " + e.getMessage());
        }
    }

    private void loadActivites() {
        try {
            String sql = "SELECT id, titre, type_activite, description, image, status FROM activites";
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                String status = rs.getString("status");
                if (!"accepte".equalsIgnoreCase(status != null ? status.trim() : "")) continue;
                Activite a = new Activite();
                a.setId(rs.getInt("id"));
                a.setTitre(rs.getString("titre"));
                a.setTypeActivite(rs.getString("type_activite"));
                a.setDescription(rs.getString("description"));
                a.setImage(rs.getString("image"));
                activitesList.add(a);
                activiteCombo.getItems().add(a.getTitre());
            }
        } catch (SQLException e) {
            System.err.println("Erreur chargement activités: " + e.getMessage());
        }
    }

    @FXML
    void ajouterActivite(ActionEvent event) {
        String selected = activiteCombo.getValue();
        if (selected == null || selected.isEmpty()) {
            showAlert("Veuillez sélectionner une activité"); return;
        }
        for (Object a : activitesSelectionnees) {
            if (((Activite) a).getTitre().equals(selected)) {
                showAlert("Cette activité est déjà ajoutée"); return;
            }
        }
        for (Object a : activitesList) {
            if (((Activite) a).getTitre().equals(selected)) {
                activitesSelectionnees.add(a);
                updateActivitesDisplay();
                activiteCombo.setValue(null);
                return;
            }
        }
    }

    private void updateActivitesDisplay() {
        activitesSelectionneesContainer.getChildren().clear();
        if (activitesSelectionnees.isEmpty()) {
            Label lbl = new Label("Aucune activité sélectionnée");
            lbl.setStyle("-fx-text-fill: #94A3B8; -fx-font-style: italic; -fx-font-size: 13px;");
            activitesSelectionneesContainer.getChildren().add(lbl);
            return;
        }
        for (int i = 0; i < activitesSelectionnees.size(); i++) {
            Activite a = (Activite) activitesSelectionnees.get(i);
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-padding: 8 12; -fx-border-color: #E2E8F0; -fx-border-width: 1; -fx-border-radius: 8;");
            Label num = new Label("🌿");
            Label name = new Label(a.getTitre());
            name.setStyle("-fx-font-size: 13px; -fx-text-fill: #0F2C4F; -fx-font-weight: bold;");
            if (a.getTypeActivite() != null) {
                Label type = new Label("• " + a.getTypeActivite());
                type.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");
                row.getChildren().addAll(num, name, type);
            } else {
                row.getChildren().addAll(num, name);
            }
            Region spacer = new Region();
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
            final int idx = i;
            Button remove = new Button("✕");
            remove.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #EF4444; -fx-background-radius: 6; -fx-padding: 3 8; -fx-cursor: hand; -fx-font-size: 11px;");
            remove.setOnAction(e -> { activitesSelectionnees.remove(idx); updateActivitesDisplay(); });
            row.getChildren().addAll(spacer, remove);
            activitesSelectionneesContainer.getChildren().add(row);
        }
    }

    @FXML
    void genererEquipement(ActionEvent event) {
        if (activitesSelectionnees.isEmpty()) {
            showAlert("Veuillez sélectionner au moins une activité avant de générer les équipements.");
            return;
        }
        genererEquipementBtn.setDisable(true);
        genererEquipementBtn.setText("⏳ Génération en cours...");
        StringBuilder sb = new StringBuilder();
        for (Object obj : activitesSelectionnees) {
            Activite a = (Activite) obj;
            sb.append("- ").append(a.getTitre());
            if (a.getTypeActivite() != null) sb.append(" (").append(a.getTypeActivite()).append(")");
            sb.append("\n");
        }
        final String prompt = "Tu es un expert en organisation d'événements outdoor en Tunisie. " +
                "Génère une liste détaillée et pratique de matériels et équipements nécessaires " +
                "pour un événement comprenant ces activités :\n" + sb +
                "\nFournis une liste claire en français avec des emojis par catégorie. Sois concis et précis.";
        new Thread(() -> {
            EventsGeminiService gemini = new EventsGeminiService();
            String result = gemini.generateResponse(prompt);
            Platform.runLater(() -> {
                if (result != null && !result.isEmpty()) equipementField.setText(result);
                genererEquipementBtn.setDisable(false);
                genererEquipementBtn.setText("✨ Générer les équipements avec l'IA");
            });
        }).start();
    }

    @FXML
    void importerImages(MouseEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir des images");
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.webp", "*.gif"));
        List<File> files = fc.showOpenMultipleDialog(null);
        if (files == null || files.isEmpty()) return;
        for (File f : files) {
            try {
                byte[] bytes = Files.readAllBytes(f.toPath());
                if (imagePrincipaleData == null) {
                    imagePrincipaleData = bytes;
                } else {
                    photosData.add(bytes);
                }
                addImageThumbnail(bytes, f.getName());
            } catch (IOException e) {
                System.err.println("Erreur image: " + e.getMessage());
            }
        }
        photosScrollPane.setVisible(true);
        photosScrollPane.setManaged(true);
    }

    private void addImageThumbnail(byte[] bytes, String name) {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            Image img = new Image(bis, 90, 90, true, true);
            ImageView iv = new ImageView(img);
            iv.setFitWidth(90); iv.setFitHeight(90); iv.setPreserveRatio(true);
            iv.setStyle("-fx-border-radius: 8; -fx-background-radius: 8;");
            Label lbl = new Label(name.length() > 12 ? name.substring(0, 10) + "…" : name);
            lbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748B;");
            VBox box = new VBox(4, iv, lbl);
            box.setAlignment(Pos.CENTER);
            box.setStyle("-fx-background-color: white; -fx-border-color: #E2E8F0; -fx-border-radius: 8; -fx-border-width: 1; -fx-padding: 5;");
            photosContainer.getChildren().add(box);
        } catch (Exception e) {
            System.err.println("Miniature erreur: " + e.getMessage());
        }
    }

    @FXML
    void ajouterEvent(ActionEvent event) {
        try {
            if (!check1.isSelected() || !check2.isSelected() || !check3.isSelected() || !check4.isSelected()) {
                showAlert("Veuillez cocher toutes les cases de validation"); return;
            }
            if (activitesSelectionnees.isEmpty()) {
                showAlert("Veuillez ajouter au moins une activité"); return;
            }
            if (imagePrincipaleData == null) {
                showAlert("Veuillez sélectionner au moins une image"); return;
            }
            String lieu = lieuField.getText().trim();
            String nomEvent = nomorgField.getText().trim();
            String email = emailField.getText().trim();
            if (lieu.isEmpty() || nomEvent.isEmpty()) {
                showAlert("Le lieu et le nom de l'organisateur sont obligatoires"); return;
            }
            if (dateDebutPicker.getValue() == null) {
                showAlert("La date de début est obligatoire"); return;
            }
            double prix = 0;
            try { if (!prixField.getText().trim().isEmpty()) prix = Double.parseDouble(prixField.getText().trim()); }
            catch (NumberFormatException e) { showAlert("Prix invalide"); return; }
            int capacite = 0;
            try { if (!capaciteField.getText().trim().isEmpty()) capacite = Integer.parseInt(capaciteField.getText().trim()); }
            catch (NumberFormatException e) { showAlert("Capacité invalide"); return; }

            LocalDateTime dateDebut = buildDateTime(dateDebutPicker.getValue(), heureDebutField.getText());
            LocalDateTime dateFin = (dateFinPicker.getValue() != null) ? buildDateTime(dateFinPicker.getValue(), heureFinField.getText()) : dateDebut.plusHours(1);

            String primaryImagePath = savePrimaryImageToUploads();
            String youtubeUrl = videoYoutubeField.getText().trim();
            String equipement = equipementField.getText().trim();
            String description = "";

            // 1. Insert Event (One time)
            String sql = "INSERT INTO events (lieu, organisateur, email, telephone, description, " +
                    "materiels_necessaires, prix, capacite_max, places_disponibles, date_debut, date_fin, " +
                    "image, video_youtube, status, statut, date_limite_inscription, date_creation, date_modification, created_by_id) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            pstmt.setString(1, lieu);
            pstmt.setString(2, nomEvent);
            pstmt.setString(3, email);
            pstmt.setString(4, telephoneorgField.getText().trim());
            pstmt.setString(5, description);
            pstmt.setString(6, equipement);
            pstmt.setDouble(7, prix);
            pstmt.setInt(8, capacite);
            pstmt.setInt(9, capacite);
            pstmt.setTimestamp(10, Timestamp.valueOf(dateDebut));
            pstmt.setTimestamp(11, Timestamp.valueOf(dateFin));
            pstmt.setString(12, primaryImagePath != null ? primaryImagePath : "");
            pstmt.setString(13, youtubeUrl);
            pstmt.setString(14, "en_attente");
            pstmt.setString(15, "en_attente");

            Timestamp dateLimite;
            if (dateLimitePicker.getValue() != null) {
                dateLimite = Timestamp.valueOf(dateLimitePicker.getValue().atStartOfDay());
            } else {
                dateLimite = Timestamp.valueOf(dateDebut.minusDays(1));
            }
            pstmt.setTimestamp(16, dateLimite);
            pstmt.setTimestamp(17, new Timestamp(System.currentTimeMillis()));
            pstmt.setTimestamp(18, new Timestamp(System.currentTimeMillis()));
            pstmt.setString(19, Session.getCurrentUserId());
            
            pstmt.executeUpdate();

            ResultSet keys = pstmt.getGeneratedKeys();
            if (keys.next()) {
                int eventId = keys.getInt(1);

                // 2. Insert associations into events_activities
                for (Object obj : activitesSelectionnees) {
                    Activite activiteEntity = (Activite) obj;
                    PreparedStatement linkPstmt = connection.prepareStatement(
                        "INSERT IGNORE INTO events_activities (events_id, activities_id) VALUES (?, ?)");
                    linkPstmt.setInt(1, eventId);
                    linkPstmt.setInt(2, activiteEntity.getId());
                    linkPstmt.executeUpdate();
                }

                // 3. Insert additional photos
                for (byte[] photoData : photosData) {
                    String photoPath = savePhotoToUploads(photoData);
                    if (photoPath != null) {
                        PreparedStatement photoPstmt = connection.prepareStatement(
                            "INSERT INTO events_images (events_id, chemin_photo, description) VALUES (?, ?, ?)");
                        photoPstmt.setInt(1, eventId);
                        photoPstmt.setString(2, photoPath);
                        photoPstmt.setString(3, "Photo supplémentaire");
                        photoPstmt.executeUpdate();
                    }
                }
            }

            showAlert("Événement créé avec succès !");
            CatalogueRefreshManager.getInstance().requestRefresh();
            fermerFenetre();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur: " + e.getMessage());
        }
    }

    private LocalDateTime buildDateTime(java.time.LocalDate date, String heureText) {
        try {
            if (heureText != null && !heureText.trim().isEmpty()) {
                LocalTime time = LocalTime.parse(heureText.trim());
                return date.atTime(time);
            }
        } catch (DateTimeParseException ignored) {}
        return date.atStartOfDay();
    }

    private String savePrimaryImageToUploads() {
        return savePhotoToUploads(imagePrincipaleData);
    }

    private String savePhotoToUploads(byte[] data) {
        if (data == null || data.length == 0) return null;
        try {
            String ext = detectExt(data);
            String fileName = "event_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000) + "." + ext;
            Path target = Paths.get(UPLOADS_DIR, fileName);
            Files.write(target, data);
            return target.toString().replace("\\", "/");
        } catch (IOException e) {
            System.err.println("Erreur sauvegarde image: " + e.getMessage()); return null;
        }
    }

    private String detectExt(byte[] b) {
        if (b.length >= 4) {
            if ((b[0] & 0xFF) == 0x89 && b[1] == 0x50) return "png";
            if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8) return "jpg";
            if (b[0] == 'G' && b[1] == 'I' && b[2] == 'F') return "gif";
        }
        return "png";
    }

    private void updateAdminButtonVisibility() {
        if (adminDashboardButton != null) {
            boolean isAdmin = UserSession.getInstance().getCurrentUser() != null
                    && UserSession.getInstance().getCurrentUser().getRole() == RoleEnum.ADMIN;
            adminDashboardButton.setVisible(isAdmin);
            adminDashboardButton.setManaged(isAdmin);
        }
    }

    @FXML
    void ouvrirAdminApprovalDashboard(ActionEvent event) {
        if (UserSession.getInstance().getCurrentUser() == null
                || UserSession.getInstance().getCurrentUser().getRole() != RoleEnum.ADMIN) {
            showAlert("Accès réservé aux administrateurs."); return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pi_dev/events/ApprovalDashboard.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Validation");
            stage.setScene(new Scene(root));
            stage.setWidth(1200); stage.setHeight(800); stage.centerOnScreen(); stage.show();
        } catch (Exception e) { showAlert("Impossible d'ouvrir le tableau de validation."); }
    }

    @FXML
    void annulerEvent(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Annuler"); alert.setHeaderText("Êtes-vous sûr ?");
        alert.setContentText("Les données non sauvegardées seront perdues.");
        alert.showAndWait().ifPresent(r -> { if (r == ButtonType.OK) fermerFenetre(); });
    }

    private void fermerFenetre() {
        try { Stage stage = (Stage) nomorgField.getScene().getWindow(); stage.close(); }
        catch (Exception e) { System.err.println("Erreur fermeture: " + e.getMessage()); }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(message); alert.showAndWait();
    }
}
