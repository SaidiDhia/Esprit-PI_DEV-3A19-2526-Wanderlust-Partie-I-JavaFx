package com.example.pi_dev.Controllers.Events;

import com.example.pi_dev.Entities.Events.Event;
import com.example.pi_dev.Entities.Events.Activite;
import com.example.pi_dev.Session.Session;
import com.example.pi_dev.Utils.Events.Mydatabase;
import com.example.pi_dev.Utils.Events.CatalogueRefreshManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
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
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

public class modifierEventController implements Initializable {

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
    @FXML private HBox photosContainer;
    @FXML private TextField videoYoutubeField;
    @FXML private TextArea equipementField;
    @FXML private TextArea descriptionField;
    @FXML private ImageView imagePrincipaleView;
    @FXML private Label imageStatusLabel;
    @FXML private Button modifierEventButton;
    @FXML private Button annulerEventButton;
    @FXML private CheckBox check1;
    @FXML private CheckBox check2;
    @FXML private CheckBox check3;
    @FXML private CheckBox check4;
    @FXML private Button genererEquipementBtn;

    private Connection connection;
    private List<Object> activitesList = new ArrayList<>();
    private List<Object> activitesSelectionnees = new ArrayList<>();
    private Event currentEvent;
    private String currentEventOwnerId;
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
        initMap();
        setupLieuListener();
    }

    private void initMap() {
        if (mapWebView != null) {
            mapWebView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                    System.out.println("Map WebView loaded.");
                    if (lieuField != null && !lieuField.getText().isEmpty()) {
                        geocodeAndMoveMap(lieuField.getText().trim());
                    }
                }
            });
            mapWebView.getEngine().loadContent(MAP_HTML);
        }
    }

    private void setupLieuListener() {
        if (lieuField != null) {
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
    }

    private void createUploadsDirectory() {
        try {
            Path path = Paths.get(UPLOADS_DIR);
            if (!Files.exists(path)) Files.createDirectories(path);
        } catch (IOException e) {
            System.err.println("Erreur création répertoire uploads: " + e.getMessage());
        }
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
                        if (mapWebView != null) {
                            try {
                                mapWebView.getEngine().executeScript("moveMap(" + lat + "," + lon + ",'" + jsLabel + "')");
                            } catch (Exception e) {
                                System.err.println("Map script error (not ready yet): " + e.getMessage());
                            }
                        }
                    });
                }
            } catch (Exception e) {
                System.err.println("Geocode error: " + e.getMessage());
            }
        }).start();
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
        if (selected == null || selected.isEmpty()) return;
        for (Object a : activitesSelectionnees) {
            if (((Activite) a).getTitre().equals(selected)) return;
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
        for (int i = 0; i < activitesSelectionnees.size(); i++) {
            Activite a = (Activite) activitesSelectionnees.get(i);
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-padding: 8 12; -fx-border-color: #E2E8F0; -fx-border-width: 1; -fx-border-radius: 8;");
            Label num = new Label("🌿");
            Label name = new Label(a.getTitre());
            name.setStyle("-fx-font-size: 13px; -fx-text-fill: #0F2C4F; -fx-font-weight: bold;");
            Region spacer = new Region();
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
            final int idx = i;
            Button remove = new Button("✕");
            remove.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #EF4444; -fx-background-radius: 6; -fx-padding: 3 8; -fx-cursor: hand;");
            remove.setOnAction(e -> { activitesSelectionnees.remove(idx); updateActivitesDisplay(); });
            row.getChildren().addAll(num, name, spacer, remove);
            activitesSelectionneesContainer.getChildren().add(row);
        }
    }

    @FXML
    void genererEquipement(ActionEvent event) {
        if (activitesSelectionnees.isEmpty()) return;
        genererEquipementBtn.setDisable(true);
        genererEquipementBtn.setText("⏳ Génération...");
        StringBuilder sb = new StringBuilder();
        for (Object obj : activitesSelectionnees) {
            Activite a = (Activite) obj;
            sb.append("- ").append(a.getTitre()).append("\n");
        }
        final String prompt = "Génère une liste de matériels pour un événement avec ces activités :\n" + sb;
        new Thread(() -> {
            com.example.pi_dev.Services.Events.EventsGeminiService gemini = new com.example.pi_dev.Services.Events.EventsGeminiService();
            String result = gemini.generateResponse(prompt);
            Platform.runLater(() -> {
                if (result != null) equipementField.setText(result);
                genererEquipementBtn.setDisable(false);
                genererEquipementBtn.setText("✨ Générer avec l'IA");
            });
        }).start();
    }

    @FXML
    void importerImage(MouseEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir l'image principale");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.jpg", "*.png", "*.jpeg", "*.webp"));
        File f = fc.showOpenDialog(null);
        if (f == null) return;
        try {
            byte[] bytes = Files.readAllBytes(f.toPath());
            imagePrincipaleData = bytes;
            
            // Clear previous thumbnail if it was the principal one
            photosContainer.getChildren().clear(); 
            addImageThumbnail(bytes, f.getName());
        } catch (IOException e) { System.err.println("Img err: " + e.getMessage()); }
    }

    @FXML
    void ajouterPhoto(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Ajouter des photos");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.jpg", "*.png", "*.jpeg", "*.webp"));
        List<File> files = fc.showOpenMultipleDialog(null);
        if (files == null) return;
        for (File f : files) {
            try {
                byte[] bytes = Files.readAllBytes(f.toPath());
                photosData.add(bytes);
                addImageThumbnail(bytes, f.getName());
            } catch (IOException e) { System.err.println("Img err: " + e.getMessage()); }
        }
    }

    private void addImageThumbnail(byte[] bytes, String name) {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            Image img = new Image(bis, 90, 90, true, true);
            ImageView iv = new ImageView(img);
            iv.setFitWidth(90); iv.setFitHeight(90); iv.setPreserveRatio(true);
            VBox box = new VBox(5, iv, new Label(name.length() > 10 ? name.substring(0, 8) + "…" : name));
            box.setAlignment(Pos.CENTER);
            box.setStyle("-fx-background-color: white; -fx-border-color: #E2E8F0; -fx-padding: 5; -fx-border-radius: 5;");
            photosContainer.getChildren().add(box);
        } catch (Exception ignore) {}
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
                currentEvent.setDateLimiteInscription(rs.getTimestamp("date_limite_inscription") != null ? rs.getTimestamp("date_limite_inscription").toLocalDateTime() : null);
                currentEventOwnerId = rs.getString("created_by_id");

                // Load associated activities
                try (PreparedStatement psAct = connection.prepareStatement(
                        "SELECT a.* FROM activites a JOIN events_activities ea ON a.id = ea.activities_id WHERE ea.events_id = ?")) {
                    psAct.setInt(1, eventId);
                    ResultSet rsAct = psAct.executeQuery();
                    while (rsAct.next()) {
                        Activite a = new Activite();
                        a.setId(rsAct.getInt("id"));
                        a.setTitre(rsAct.getString("titre"));
                        activitesSelectionnees.add(a);
                    }
                    updateActivitesDisplay();
                } catch (SQLException ignore) {}

                setEventData(currentEvent);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setEventData(Event event) {
        this.currentEvent = event;
        if (nomorgField != null) nomorgField.setText(event.getOrganisateur() != null ? event.getOrganisateur() : "");
        if (lieuField != null) {
            lieuField.setText(event.getLieu() != null ? event.getLieu() : "");
            geocodeAndMoveMap(lieuField.getText());
        }
        if (emailField != null) emailField.setText(event.getEmail() != null ? event.getEmail() : "");
        if (telephoneorgField != null) telephoneorgField.setText(event.getTelephone() != null ? event.getTelephone().toString() : "");
        if (descriptionField != null) descriptionField.setText(event.getDescription() != null ? event.getDescription() : "");
        if (equipementField != null) equipementField.setText(event.getMaterielsNecessaires() != null ? event.getMaterielsNecessaires() : "");
        if (videoYoutubeField != null) videoYoutubeField.setText(event.getVideoYoutube() != null ? event.getVideoYoutube() : "");
        if (prixField != null) prixField.setText(event.getPrix() != null ? String.valueOf(event.getPrix()) : "0");
        if (capaciteField != null) capaciteField.setText(event.getCapaciteMax() != null ? String.valueOf(event.getCapaciteMax()) : "20");

        if (dateDebutPicker != null && event.getDateDebut() != null) {
            dateDebutPicker.setValue(event.getDateDebut().toLocalDate());
            if (heureDebutField != null) heureDebutField.setText(event.getDateDebut().toLocalTime().toString());
        }
        if (dateFinPicker != null && event.getDateFin() != null) {
            dateFinPicker.setValue(event.getDateFin().toLocalDate());
            if (heureFinField != null) heureFinField.setText(event.getDateFin().toLocalTime().toString());
        }
        if (dateLimitePicker != null && event.getDateLimiteInscription() != null) {
            dateLimitePicker.setValue(event.getDateLimiteInscription().toLocalDate());
        }
    }

    @FXML
    void modifierEvent(ActionEvent event) {
        try {
            if (!check1.isSelected() || !check2.isSelected() || !check3.isSelected() || !check4.isSelected()) {
                showAlert("Veuillez cocher toutes les cases de validation"); return;
            }
            if (activitesSelectionnees.isEmpty()) {
                showAlert("Veuillez ajouter au moins une activité"); return;
            }
            
            String nomEvent = nomorgField.getText().trim();
            String lieu = lieuField.getText().trim();
            String email = emailField.getText().trim();
            String equipement = equipementField.getText().trim();
            String videoYoutube = videoYoutubeField.getText().trim();

            if (nomEvent.isEmpty() || lieu.isEmpty()) {
                showAlert("Le nom et le lieu sont obligatoires"); return;
            }

            double prixValue = 0;
            try { prixValue = Double.parseDouble(prixField.getText()); } catch (Exception ignore) {}
            int capaciteValue = 20;
            try { capaciteValue = Integer.parseInt(capaciteField.getText()); } catch (Exception ignore) {}

            LocalDateTime dateDebut = buildDateTime(dateDebutPicker.getValue(), heureDebutField.getText());
            LocalDateTime dateFin = (dateFinPicker.getValue() != null) ? buildDateTime(dateFinPicker.getValue(), heureFinField.getText()) : dateDebut.plusHours(2);

            Timestamp dateLimite;
            if (dateLimitePicker.getValue() != null) {
                dateLimite = Timestamp.valueOf(dateLimitePicker.getValue().atStartOfDay());
            } else {
                dateLimite = Timestamp.valueOf(dateDebut.minusDays(1));
            }

            String primaryImagePath = currentEvent.getImage();
            if (imagePrincipaleData != null) primaryImagePath = savePhotoToUploads(imagePrincipaleData);

            String sql = "UPDATE events SET lieu = ?, organisateur = ?, email = ?, telephone = ?, description = ?, materiels_necessaires = ?, date_debut = ?, date_fin = ?, prix = ?, capacite_max = ?, statut = ?, date_modification = CURRENT_TIMESTAMP, video_youtube = ?, image = ?, date_limite_inscription = ? WHERE id = ?";

            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, lieu);
            pstmt.setString(2, nomEvent);
            pstmt.setString(3, email);
            pstmt.setString(4, telephoneorgField.getText().trim());
            pstmt.setString(5, descriptionField.getText());
            pstmt.setString(6, equipement);
            pstmt.setTimestamp(7, Timestamp.valueOf(dateDebut));
            pstmt.setTimestamp(8, Timestamp.valueOf(dateFin));
            pstmt.setDouble(9, prixValue);
            pstmt.setInt(10, capaciteValue);
            pstmt.setString(11, "EN_ATTENTE");
            pstmt.setString(12, videoYoutube);
            pstmt.setString(13, primaryImagePath);
            pstmt.setTimestamp(14, dateLimite);
            pstmt.setInt(15, currentEvent.getId());

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                // Update activities (join table)
                PreparedStatement delLink = connection.prepareStatement("DELETE FROM events_activities WHERE events_id = ?");
                delLink.setInt(1, currentEvent.getId());
                delLink.executeUpdate();

                for (Object obj : activitesSelectionnees) {
                    Activite a = (Activite) obj;
                    PreparedStatement insLink = connection.prepareStatement("INSERT INTO events_activities (events_id, activities_id) VALUES (?, ?)");
                    insLink.setInt(1, currentEvent.getId());
                    insLink.setInt(2, a.getId());
                    insLink.executeUpdate();
                }

                // Additional photos
                for (byte[] pData : photosData) {
                    String pPath = savePhotoToUploads(pData);
                    if (pPath != null) {
                        PreparedStatement photoPstmt = connection.prepareStatement("INSERT INTO events_images (events_id, chemin_photo, description) VALUES (?, ?, ?)");
                        photoPstmt.setInt(1, currentEvent.getId());
                        photoPstmt.setString(2, pPath);
                        photoPstmt.setString(3, "Photo supplémentaire (Modif)");
                        photoPstmt.executeUpdate();
                    }
                }

                showAlert("Événement mis à jour avec succès!");
                CatalogueRefreshManager.getInstance().requestRefresh();
                fermerFenetre();
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur modif: " + e.getMessage());
        }
    }

    private LocalDateTime buildDateTime(java.time.LocalDate date, String heureText) {
        if (date == null) return LocalDateTime.now();
        try {
            if (heureText != null && !heureText.trim().isEmpty()) {
                LocalTime time = LocalTime.parse(heureText.trim());
                return date.atTime(time);
            }
        } catch (Exception ignore) {}
        return date.atStartOfDay();
    }

    private String savePhotoToUploads(byte[] data) {
        if (data == null || data.length == 0) return null;
        try {
            String ext = detectExt(data);
            String fileName = "event_mod_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000) + "." + ext;
            Path target = Paths.get(UPLOADS_DIR, fileName);
            Files.write(target, data);
            return target.toString().replace("\\", "/");
        } catch (IOException e) { return null; }
    }

    private String detectExt(byte[] b) {
        if (b.length >= 4) {
            if ((b[0] & 0xFF) == 0x89 && b[1] == 0x50) return "png";
            if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8) return "jpg";
        }
        return "png";
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(message); alert.showAndWait();
    }

    private void fermerFenetre() {
        try {
            Stage stage = (Stage) nomorgField.getScene().getWindow();
            stage.close();
        } catch (Exception ignore) {}
    }

    @FXML
    void goToCatalogue(ActionEvent event) { fermerFenetre(); }

    @FXML
    void annulerEvent(ActionEvent event) { fermerFenetre(); }

    @FXML
    void supprimerEvent(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer"); alert.setHeaderText("Supprimer cet événement ?");
        alert.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    PreparedStatement ps = connection.prepareStatement("DELETE FROM events WHERE id = ?");
                    ps.setInt(1, currentEvent.getId());
                    ps.executeUpdate();
                    CatalogueRefreshManager.getInstance().requestRefresh();
                    fermerFenetre();
                } catch (SQLException e) { e.printStackTrace(); }
            }
        });
    }
}