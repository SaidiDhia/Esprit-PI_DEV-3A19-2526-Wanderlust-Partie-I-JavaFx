package com.example.pi_dev.Controllers.Events;

import com.example.pi_dev.Services.Events.EventPhotoService;
import com.example.pi_dev.Services.Events.WeatherService;
import com.example.pi_dev.Services.Events.ReservationService;
import com.example.pi_dev.Services.Events.DynamicPricingEngine;
import com.example.pi_dev.Utils.Events.Mydatabase;
import com.example.pi_dev.Entities.Events.Event;
import com.example.pi_dev.Entities.Events.Activite;
import com.example.pi_dev.Entities.Events.Reservation;
import com.example.pi_dev.Entities.Events.EventPhoto;
import com.example.pi_dev.Session.Session;
import com.example.pi_dev.Utils.Users.UserSession;
import com.example.pi_dev.enums.RoleEnum;
import com.example.pi_dev.Utils.Events.CatalogueRefreshManager;
import javafx.scene.control.TextArea;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import javafx.concurrent.Worker;
import netscape.javascript.JSObject;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.net.URLEncoder;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.GridPane;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class catalogueController {

    private Connection connection;
    private List<Activite> activitesList;
    private List<Event> eventsList;
    private List<Reservation> reservationsList;
    private ReservationService reservationService;
    private WeatherService weatherService;
    private WeatherService.WeatherData currentWeather;

    @FXML
    private Label activitedesc;
    @FXML
    private ImageView activiteimg;
    @FXML
    private FlowPane flowActivites;
    @FXML
    private FlowPane flowEvents;
    @FXML
    private FlowPane flowReservations;
    @FXML
    private ScrollPane scrollPaneActivites;
    @FXML
    private ScrollPane scrollPaneEvents;
    @FXML
    private Tab tabActivites;
    @FXML
    private Tab tabMesReservations;
    @FXML
    private Tab tabEvents;
    @FXML
    private TextField txtRecherche;
    @FXML
    private VBox weatherWidgetContainer;
    @FXML
    private VBox weatherInfoContainer;
    @FXML
    private ComboBox<String> cityComboBox;
    @FXML
    private Button refreshWeatherButton;
    @FXML
    private Label activitetitre;
    @FXML
    private Label activitetype;
    @FXML
    private Label capaciteevent;
    @FXML
    private Label datedebut;
    @FXML
    private Label datefin;
    @FXML
    private VBox eventcard;
    @FXML
    private ImageView eventimg;
    @FXML
    private Button modevent;
    @FXML
    private Button modifieract;
    @FXML
    private Button orgactivite;
    @FXML
    private Button orgevent;
    @FXML
    private Label placesdispoevent;
    @FXML
    private Label prixevent;
    @FXML
    private Button recherche;
    @FXML
    private Button suppact;
    @FXML
    private Button suppevent;
    @FXML
    private Button adminDashboardButton;

    public void initialize() {
        initializeDatabase();
        com.example.pi_dev.Utils.Events.DatabaseUtils.ensureSchemaCorrect(connection);
        reservationService = new ReservationService();
        refreshData();
        updateAdminButtonVisibility();

        weatherService = new WeatherService();
        initializeCities();
        loadWeatherData();

        flowActivites.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((obs1, oldWindow, newWindow) -> {
                    if (newWindow != null) {
                        newWindow.focusedProperty().addListener((obs2, oldFocused, newFocused) -> {
                            if (newFocused) {
                                refreshData();
                            }
                        });
                    }
                });
            }
        });

        startRefreshListener();
    }

    private void initializeCities() {
        // ✅ Vérifier que cityComboBox n'est pas null
        if (cityComboBox == null)
            return;

        cityComboBox.getItems().addAll(
                "Tunis", "Sfax", "Sousse", "Kairouan", "Bizerte",
                "Gabès", "Ariana", "Nabeul", "Monastir", "Mahdia",
                "Kasserine", "Siliana", "Le Kef", "Jendouba", "Zaghouan",
                "Tozeur", "Kebili", "Tataouine", "Gafsa", "Médenine");

        cityComboBox.getSelectionModel().select("Tunis");
        cityComboBox.setOnAction(e -> loadWeatherData());
    }

    private void startRefreshListener() {
        Thread refreshThread = new Thread(() -> {
            long lastAutoRefresh = System.currentTimeMillis();
            while (true) {
                try {
                    Thread.sleep(1000); // Check every second for manual requests
                    
                    boolean manualRefresh = CatalogueRefreshManager.getInstance().isRefreshRequested();
                    boolean autoRefresh = (System.currentTimeMillis() - lastAutoRefresh) > 10000; // Auto refresh every 10s
                    
                    if (manualRefresh || autoRefresh) {
                        javafx.application.Platform.runLater(() -> {
                            refreshData();
                            if (manualRefresh) CatalogueRefreshManager.getInstance().resetRefresh();
                        });
                        if (autoRefresh) lastAutoRefresh = System.currentTimeMillis();
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        refreshThread.setDaemon(true);
        refreshThread.start();
    }

    public void refreshData() {
        loadActivites();
        loadEvents();
        loadReservations();
        displayActivites();
        displayEvents();
        displayReservations();
    }

    private void displayActivites() {
        flowActivites.getChildren().clear();
        for (Activite activite : activitesList) {
            // Only show activities that are explicitly accepted (visible to everyone only
            // after approval)
            if (!isPublishedActivity(activite)) {
                continue;
            }
            addActiviteCard(activite);
        }
    }

    private void displayEvents() {
        flowEvents.getChildren().clear();
        for (Event event : eventsList) {
            // Only show events that are approved
            if (!isPublishedEvent(event)) {
                continue;
            }
            addEventCard(event);
        }
    }

    private void loadReservations() {
        reservationsList = new ArrayList<>();
        String currentUserId = Session.getCurrentUserId();
        
        // Even if userId is null, we can try to fetch by email in the service
        if (reservationService == null) {
            return;
        }

        try {
            // Passing "" if null to the service which now handles email fallback
            reservationsList = reservationService.getReservationsByUser(currentUserId != null ? currentUserId : "");
        } catch (SQLException e) {
            System.err.println("Erreur chargement réservations: " + e.getMessage());
        }
    }

    private void displayReservations() {
        if (flowReservations == null) {
            return;
        }

        flowReservations.getChildren().clear();

        String currentUserId = Session.getCurrentUserId();
        if (currentUserId == null || currentUserId.trim().isEmpty()) {
            flowReservations.getChildren().add(createReservationInfoCard("Connectez-vous pour voir vos réservations."));
            return;
        }

        if (reservationsList == null || reservationsList.isEmpty()) {
            flowReservations.getChildren().add(createReservationInfoCard("Vous n'avez encore effectué aucune réservation."));
            return;
        }

        for (Reservation reservation : reservationsList) {
            addReservationCard(reservation);
        }
    }

    private void initializeDatabase() {
        try {
            connection = Mydatabase.getInstance().getConnextion();
        } catch (Exception e) {
            System.err.println("Database connection error: " + e.getMessage());
        }
    }


    private void loadActivites() {
        activitesList = new ArrayList<>();
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs;
            try {
                rs = stmt.executeQuery(
                        "SELECT id, titre, description, type_activite, image, status, created_by_id FROM activites");
                while (rs.next()) {
                    Activite activite = new Activite();
                    activite.setId(rs.getInt("id"));
                    activite.setTitre(rs.getString("titre"));
                    activite.setDescription(rs.getString("description"));
                    activite.setTypeActivite(rs.getString("type_activite"));
                    activite.setImage(rs.getString("image"));
                    activite.setStatus(rs.getString("status"));
                    activite.setCreatedById(rs.getString("created_by_id"));
                    activitesList.add(activite);
                }
            } catch (SQLException e) {
                rs = stmt.executeQuery("SELECT id, titre, description, image, created_by_id FROM activites");
                while (rs.next()) {
                    Activite activite = new Activite();
                    activite.setId(rs.getInt("id"));
                    activite.setTitre(rs.getString("titre"));
                    activite.setDescription(rs.getString("description"));
                    activite.setTypeActivite(null);
                    activite.setImage(rs.getString("image"));
                    activite.setCreatedById(rs.getString("created_by_id"));
                    // Default to pending to match Symfony enum and hide from catalogue until
                    // approved
                    activite.setStatus("en_attente");
                    activitesList.add(activite);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur chargement activités: " + e.getMessage());
        }
    }

    private void loadEvents() {
        eventsList = new ArrayList<>();
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT *, COALESCE(status, statut) AS effective_status FROM events");
            while (rs.next()) {
                Event event = new Event();
                int eventId = rs.getInt("id");
                event.setId(eventId);
                
                // Get activities for this event from join table
                try (PreparedStatement psAct = connection.prepareStatement(
                        "SELECT activities_id FROM events_activities WHERE events_id = ? LIMIT 1")) {
                    psAct.setInt(1, eventId);
                    try (ResultSet rsAct = psAct.executeQuery()) {
                        if (rsAct.next()) {
                            event.setIdActivite(rsAct.getInt("activities_id"));
                        }
                    }
                } catch (SQLException e) {
                    // Fallback to id_activite column if join table fetch fails
                    try { event.setIdActivite(rs.getInt("id_activite")); } catch (Exception ignore) {}
                }

                event.setDateDebut(
                        rs.getTimestamp("date_debut") != null ? rs.getTimestamp("date_debut").toLocalDateTime() : null);
                event.setDateFin(
                        rs.getTimestamp("date_fin") != null ? rs.getTimestamp("date_fin").toLocalDateTime() : null);
                event.setPrix(rs.getBigDecimal("prix"));
                event.setCapaciteMax(rs.getInt("capacite_max"));
                event.setPlacesDisponibles(rs.getInt("places_disponibles"));
                event.setOrganisateur(rs.getString("organisateur"));
                event.setMaterielsNecessaires(rs.getString("materiels_necessaires"));
                event.setStatut(parseStatutEvent(rs.getString("effective_status")));
                event.setDateCreation(rs.getTimestamp("date_creation"));
                event.setDateModification(rs.getTimestamp("date_modification"));
                try { event.setLieu(rs.getString("lieu")); } catch (Exception ignore) {}
                try { event.setDescription(rs.getString("description")); } catch (Exception ignore) {}
                try { event.setCreatedById(rs.getString("created_by_id")); } catch (Exception ignore) {}
                try { event.setEmail(rs.getString("email")); } catch (Exception ignore) {}
                try { event.setTelephone(rs.getInt("telephone")); } catch (Exception ignore) {}
                try { event.setVideoYoutube(rs.getString("video_youtube")); } catch (Exception ignore) {}
                try { event.setImage(rs.getString("image")); } catch (Exception ignore) {}
                
                eventsList.add(event);
            }
        } catch (SQLException e) {
            System.err.println("Erreur chargement événements: " + e.getMessage());
        }
    }

    private Event.StatutEvent parseStatutEvent(String statut) {
        if (statut == null || statut.trim().isEmpty()) {
            return Event.StatutEvent.EN_ATTENTE;
        }

        String normalized = statut.trim().toUpperCase();
        try {
            return Event.StatutEvent.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return Event.StatutEvent.EN_ATTENTE;
        }
    }

    private boolean isAdminUser() {
        return UserSession.getInstance().getCurrentUser() != null
                && UserSession.getInstance().getCurrentUser().getRole() == RoleEnum.ADMIN;
    }

    private void updateAdminButtonVisibility() {
        if (adminDashboardButton != null) {
            boolean isAdmin = isAdminUser();
            adminDashboardButton.setVisible(isAdmin);
            adminDashboardButton.setManaged(isAdmin);
        }
    }

    private boolean isPublishedActivity(Activite activite) {
        String status = activite.getStatus();
        if (status == null || status.trim().isEmpty()) {
            return false;
        }
        String normalized = status.trim().toLowerCase();
        return normalized.equals("accepte");
    }

    private boolean isPublishedEvent(Event event) {
        if (event.getStatut() == null) {
            return true;
        }
        return event.getStatut() == Event.StatutEvent.ACCEPTE;
    }

    private String normalizeReservationStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return "En attente";
        }

        String normalized = status.trim().toLowerCase();
        if (normalized.equals("accepte")) {
            return "Acceptée";
        }
        if (normalized.equals("refuse")) {
            return "Refusée";
        }
        if (normalized.equals("en_attente")) {
            return "En attente";
        }
        return status;
    }

    @FXML
    void ouvrirAdminApprovalDashboard(ActionEvent event) {
        if (!isAdminUser()) {
            showAlert("Accès réservé aux administrateurs.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pi_dev/events/ApprovalDashboard.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Validation des activités et événements");
            stage.setScene(new Scene(root));
            stage.setWidth(1200);
            stage.setHeight(800);
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Impossible d'ouvrir le tableau de validation.");
        }
    }

    @FXML
    void goToEventDetails(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pi_dev/events/EventDetails.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Détails de l'événement");
            stage.setScene(new Scene(root));
            stage.setWidth(1000);
            stage.setHeight(750);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleRecherche(ActionEvent event) {
        String searchText = txtRecherche.getText().toLowerCase();

        flowActivites.getChildren().clear();
        flowEvents.getChildren().clear();

        for (Activite activite : activitesList) {
            String titre = activite.getTitre() != null ? activite.getTitre().toLowerCase() : "";
            String type = activite.getTypeActivite() != null ? activite.getTypeActivite().toLowerCase() : "";
            String desc = activite.getDescription() != null ? activite.getDescription().toLowerCase() : "";

            if (titre.contains(searchText) || type.contains(searchText) || desc.contains(searchText)) {
                addActiviteCard(activite);
            }
        }

        for (Event eventItem : eventsList) {
            String organisateur = eventItem.getOrganisateur() != null ? eventItem.getOrganisateur().toLowerCase() : "";
            if (organisateur.contains(searchText)) {
                addEventCard(eventItem);
            }
        }
    }

    @FXML
    void ouvrirGestionActivites(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pi_dev/events/AjoutActivite.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Ajouter Activité");
            stage.setScene(new Scene(root));
            stage.setWidth(850);
            stage.setHeight(600);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    @FXML
    void ouvrirOrganisation(ActionEvent event) {
        try {
            System.out.println("Tentative d'ouverture de l'interface d'organisation d'événement...");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pi_dev/events/Event.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Organiser un événement");
            stage.setScene(new Scene(root));
            stage.setWidth(900);
            stage.setHeight(650);
            stage.centerOnScreen();
            stage.show();

            System.out.println("Interface d'organisation ouverte avec succès");

        } catch (IOException e) {
            System.err.println("Erreur lors de l'ouverture de Event.fxml: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur lors de l'ouverture de l'interface d'organisation d'événement");
        } catch (Exception e) {
            System.err.println("Erreur inattendue: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur inattendue lors de l'ouverture de l'interface");
        }
    }

    @FXML
    void ouvrirCarteEvents(ActionEvent event) {
        try {
            WebView webView = new WebView();
            WebEngine engine = webView.getEngine();
            
            JavaBridge bridge = new JavaBridge();

            engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == Worker.State.SUCCEEDED) {
                    JSObject window = (JSObject) engine.executeScript("window");
                    window.setMember("javaBridge", bridge);

                    JsonArray jsonArray = new JsonArray();
                    for (Event e : eventsList) {
                        if (!isPublishedEvent(e)) continue;
                        JsonObject obj = new JsonObject();
                        obj.addProperty("id", e.getId());
                        obj.addProperty("lieu", e.getLieu() != null ? e.getLieu() : "Lieu inconnu");
                        obj.addProperty("dateDebut", e.getDateDebut() != null ? e.getDateDebut().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "Date non définie");
                        obj.addProperty("places", e.getPlacesDisponibles() != null ? e.getPlacesDisponibles() : 0);
                        obj.addProperty("organisateur", e.getOrganisateur() != null ? e.getOrganisateur() : "Wanderlust");
                        obj.addProperty("prix", e.getPrix() != null ? e.getPrix() + " TND" : "Gratuit");
                        jsonArray.add(obj);
                    }
                    
                    String jsonStr = new Gson().toJson(jsonArray);
                    engine.executeScript("loadEventsData(" + jsonStr + ");");
                }
            });

            java.net.URL resource = getClass().getResource("/com/example/pi_dev/events/map.html");
            if (resource == null) {
                throw new IOException("Fichier map.html introuvable.");
            }
            engine.load(resource.toExternalForm());

            Stage stage = new Stage();
            stage.setTitle("Wanderlust - Carte des Événements");
            stage.setScene(new Scene(webView, 1000, 700));
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            System.err.println("Erreur chargement carte: " + e.getMessage());
            e.printStackTrace();
            showAlert("Impossible de charger la carte interactive.");
        }
    }

    public class JavaBridge {
        public void openEvent(int id) {
            javafx.application.Platform.runLater(() -> {
                for (Event e : eventsList) {
                    if (e.getId() == id) {
                        ouvrirDetailsEvent(e);
                        break;
                    }
                }
            });
        }
    }

    @FXML
    void ouvrirSuggestionIA(ActionEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/example/pi_dev/events/EventSuggestion.fxml"));
            javafx.scene.Parent root = loader.load();

            EventSuggestionController controller = loader.getController();
            
            // Pass only published events to the AI
            java.util.List<Event> publishedEvents = eventsList.stream().filter(this::isPublishedEvent).collect(java.util.stream.Collectors.toList());
            controller.setEventsList(publishedEvents);

            Stage stage = new Stage();
            stage.setTitle("Wanderlust - Assistant IA");
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            System.err.println("Erreur ouverture IA: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur lors de l'ouverture de l'assistant IA.");
        }
    }

    @FXML
    void supprimeract(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer une activité");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer cette activité ?");
        alert.showAndWait();
    }

    private void ouvrirModificationActivite(Activite activite) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pi_dev/events/modifsuppActivite.fxml"));
            Parent root = loader.load();
            modifierActiviteController controller = loader.getController();
            controller.setActiviteData(activite);
            Stage stage = new Stage();
            stage.setTitle("Modifier/Supprimer Activité: " + activite.getTitre());
            stage.setScene(new Scene(root));
            stage.setWidth(900);
            stage.setHeight(650);
            stage.centerOnScreen();
            stage.show();
            stage.setOnHidden(e -> refreshData());
        } catch (IOException e) {
            showAlert("Erreur lors de l'ouverture de l'interface de modification");
        }
    }

    private void supprimerActivite(Activite activite) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Supprimer l'activité");
        alert.setContentText("Voulez-vous vraiment supprimer \"" + activite.getTitre() + "\" ?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    PreparedStatement pstmt = connection.prepareStatement("DELETE FROM activites WHERE id = ?");
                    pstmt.setInt(1, activite.getId());
                    pstmt.executeUpdate();
                    showAlert("Activité supprimée avec succès");
                    refreshData();
                } catch (SQLException e) {
                    showAlert("Erreur lors de la suppression de l'activité");
                }
            }
        });
    }

    @FXML
    void supprimerevent(ActionEvent event) {
        showAlert("Utilisez le bouton supprimer sur la carte de l'événement concerné.");
    }

    private void supprimerEvent(Event eventItem) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Supprimer l'événement");
        alert.setContentText("Voulez-vous vraiment supprimer cet événement ?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    String currentUser = Session.getCurrentUserId();
                    if (currentUser == null || !currentUser.equals(eventItem.getCreatedById())) {
                        showAlert("Vous n'êtes pas autorisé à supprimer cet événement.");
                        return;
                    }

                    PreparedStatement pstmt = connection
                            .prepareStatement("DELETE FROM events WHERE id = ? AND created_by_id = ?");
                    pstmt.setInt(1, eventItem.getId());
                    pstmt.setString(2, currentUser);
                    int deleted = pstmt.executeUpdate();

                    if (deleted > 0) {
                        showAlert("Événement supprimé avec succès");
                        refreshData();
                    } else {
                        showAlert("Suppression refusée ou événement introuvable.");
                    }
                } catch (SQLException e) {
                    showAlert("Erreur lors de la suppression de l'événement");
                }
            }
        });
    }

    private void addActiviteCard(Activite activite) {
        VBox card = new VBox();
        card.setSpacing(12);
        card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #e2e8f0; -fx-border-width: 1; -fx-padding: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 4); -fx-cursor: hand;");
        card.setPrefWidth(220);

        ImageView imageView = new ImageView();
        imageView.setFitWidth(180);
        imageView.setFitHeight(120);
        imageView.setPreserveRatio(true);

        try {
            if (activite.getImage() != null && !activite.getImage().isEmpty()
                    && !activite.getImage().equals("default.jpg")) {
                File imageFile = new File(activite.getImage());
                if (imageFile.exists()) {
                    imageView.setImage(new Image(imageFile.toURI().toString()));
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur image: " + activite.getImage());
        }

        Label titleLabel = new Label(activite.getTitre());
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #0F2C4F;");
        titleLabel.setWrapText(true);

        card.getChildren().addAll(imageView, titleLabel);

        if (activite.getTypeActivite() != null && !activite.getTypeActivite().isEmpty()) {
            Label typeLabel = new Label("🎯 " + activite.getTypeActivite());
            typeLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #2D70B3; -fx-background-color: #EBF8FF; -fx-padding: 3 8; -fx-background-radius: 6;");
            card.getChildren().add(typeLabel);
        }

        Label descLabel = new Label(activite.getDescription() != null ? activite.getDescription() : "");
        descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");
        descLabel.setWrapText(true);
        descLabel.setMaxHeight(60);

        HBox buttonBox = new HBox(5);
        buttonBox.setAlignment(Pos.CENTER);

        String currentUser = Session.getCurrentUserId();
        if (currentUser != null && currentUser.equals(activite.getCreatedById())) {
            Button modifierButton = new Button("✏️");
            modifierButton.setStyle(
                    "-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 4; -fx-padding: 5 10; -fx-cursor: hand;");
            modifierButton.setOnAction(e -> ouvrirModificationActivite(activite));

            Button supprimerButton = new Button("🗑️");
            supprimerButton.setStyle(
                    "-fx-background-color: #f44336; -fx-text-fill: white; -fx-background-radius: 4; -fx-padding: 5 10; -fx-cursor: hand;");
            supprimerButton.setOnAction(e -> supprimerActivite(activite));

            buttonBox.getChildren().addAll(modifierButton, supprimerButton);
        }
        card.getChildren().addAll(descLabel, buttonBox);

        card.setOnMouseClicked(e -> ouvrirDetailsActivite(activite));
        flowActivites.getChildren().add(card);
    }

    private void ouvrirDetailsActivite(Activite activite) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pi_dev/events/ActiviteCard.fxml"));
            Parent root = loader.load();
            ActiviteCardController controller = loader.getController();
            controller.setActiviteData(activite);
            Stage stage = new Stage();
            stage.setTitle("Détails: " + activite.getTitre());
            stage.setScene(new Scene(root));
            stage.setWidth(800);
            stage.setHeight(700);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            showAlert("Erreur lors de l'ouverture des détails");
        }
    }

    private void addEventCard(Event event) {
        try {
            VBox card = new VBox();
            card.setSpacing(10);
            card.setStyle(
                    "-fx-border-color: #ccc; -fx-border-width: 1; -fx-padding: 15; -fx-background-color: white; -fx-background-radius: 10; -fx-border-radius: 10; -fx-cursor: default;");

            Label titleLabel = new Label(event.getOrganisateur() != null ? event.getOrganisateur() : "");
            titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1a5f3f;");

            Label dateDebutLabel = new Label("📅 Début: " +
                    (event.getDateDebut() != null
                            ? event.getDateDebut().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            : "Non défini"));
            dateDebutLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #333;");

            Label dateFinLabel = new Label("📅 Fin: " +
                    (event.getDateFin() != null ? event.getDateFin().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            : "Non défini"));
            dateFinLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #333;");

            DynamicPricingEngine.PricingResult pricing = DynamicPricingEngine.calculatePrice(event);
            
            VBox priceBox = new VBox(2);
            if (pricing.discountPercentage > 0) {
                Label oldPrix = new Label(String.format("%.2f TND", pricing.originalPrice));
                oldPrix.setStyle("-fx-font-size: 12px; -fx-text-fill: #94A3B8; -fx-strikethrough: true;");
                
                HBox newPriceRow = new HBox(8);
                newPriceRow.setAlignment(Pos.CENTER_LEFT);
                Label newPrix = new Label(String.format("%.2f TND", pricing.finalPrice));
                newPrix.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #DC2626;"); // Red color like screenshot
                
                Label badge = new Label(String.format("-%.0f%%", pricing.discountPercentage * 100));
                badge.setStyle("-fx-background-color: #EA580C; -fx-text-fill: white; -fx-padding: 2 6; -fx-background-radius: 10; -fx-font-size: 11px; -fx-font-weight: bold;");
                
                newPriceRow.getChildren().addAll(newPrix, badge);
                priceBox.getChildren().addAll(oldPrix, newPriceRow);
            } else {
                Label prixLabel = new Label("💰 Prix: " + (event.getPrix() != null ? event.getPrix() + " TND" : "0 TND"));
                prixLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2d7a2d;");
                priceBox.getChildren().add(prixLabel);
            }

            Label capaciteLabel = new Label("👥 Capacité: " + event.getCapaciteMax());
            capaciteLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

            Label placesLabel = new Label("🎫 Places: " + event.getPlacesDisponibles());
            placesLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

            HBox buttonBox = new HBox(6);
            buttonBox.setAlignment(Pos.CENTER);

            final Event eventFinal = event;

            // View button — always visible for everyone
            Button viewButton = new Button("👁️ Détails");
            viewButton.setStyle("-fx-background-color: #2D70B3; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8 16; -fx-font-size: 12px; -fx-cursor: hand; -fx-font-weight: bold;");
            viewButton.setOnAction(e -> ouvrirDetailsEvent(eventFinal));
            buttonBox.getChildren().add(viewButton);

            // Modifier / Supprimer — only for the creator
            String currentUser = Session.getCurrentUserId();
            if (currentUser != null && currentUser.equals(event.getCreatedById())) {
                Button modifierButton = new Button("✏️ Modifier");
                modifierButton.setStyle(
                        "-fx-background-color: #2196F3; -fx-text-fill: white; -fx-background-radius: 20; -fx-border-radius: 20; -fx-padding: 8 16; -fx-cursor: hand;");
                modifierButton.setOnAction(e -> ouvrirModificationEvent(eventFinal));

                Button supprimerButton = new Button("🗑️ Supprimer");
                supprimerButton.setStyle(
                        "-fx-background-color: #f44336; -fx-text-fill: white; -fx-background-radius: 20; -fx-border-radius: 20; -fx-padding: 8 16; -fx-cursor: hand;");
                supprimerButton.setOnAction(e -> supprimerEvent(eventFinal));

                buttonBox.getChildren().addAll(modifierButton, supprimerButton);
            }

            card.getChildren().addAll(titleLabel, dateDebutLabel, dateFinLabel, priceBox, capaciteLabel, placesLabel, buttonBox);
            // card.setOnMouseClicked(e -> ouvrirReservation(eventFinal)); // Removed action on card click
            flowEvents.getChildren().add(card);

        } catch (Exception e) {
            System.err.println("Erreur carte événement: " + e.getMessage());
        }
    }

    private void ouvrirDetailsEvent(Event event) {
        try {
            Stage dialog = new Stage();
            dialog.setTitle("Wanderlust - " + (event.getOrganisateur() != null ? event.getOrganisateur() : "Détails"));

            VBox mainContainer = new VBox(0);
            mainContainer.setStyle("-fx-background-color: #F8FAFC;");

            // --- HERO SECTION ---
            StackPane heroSection = new StackPane();
            heroSection.setPrefHeight(240);
            heroSection.setStyle("-fx-background-color: #1E293B;");

            // Hero Image
            if (event.getImage() != null && !event.getImage().isBlank()) {
                try {
                    String path = event.getImage();
                    if (!path.startsWith("http") && !path.startsWith("file")) {
                        path = new File(path).toURI().toString();
                    }
                    ImageView heroImg = new ImageView(new Image(path, 800, 400, true, true));
                    heroImg.setOpacity(0.6);
                    heroImg.setFitWidth(700);
                    heroImg.setPreserveRatio(true);
                    heroSection.getChildren().add(heroImg);
                } catch (Exception ignore) {}
            }

            // Hero Overlay Text
            VBox heroText = new VBox(10);
            heroText.setAlignment(Pos.BOTTOM_LEFT);
            heroText.setPadding(new Insets(0, 40, 30, 40));
            
            HBox titleRow = new HBox(15);
            titleRow.setAlignment(Pos.CENTER_LEFT);
            Label heroTitle = new Label(event.getOrganisateur() != null ? event.getOrganisateur().toUpperCase() : "AVENTURE WANDERLUST");
            heroTitle.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0, 0, 0);");
            
            Button shareBtn = new Button("🔗 PARTAGER");
            shareBtn.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-text-fill: white; -fx-background-radius: 20; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand;");
            
            VBox commentsList = new VBox(10); // Moved up to be accessible
            shareBtn.setOnAction(e -> ouvrirPartageDialog(event, dialog, commentsList));
            
            titleRow.getChildren().addAll(heroTitle, shareBtn);
            
            Label heroSubtitle = new Label("📍 " + (event.getLieu() != null ? event.getLieu() : "Lieu non défini"));
            heroSubtitle.setStyle("-fx-font-size: 16px; -fx-text-fill: #E2E8F0; -fx-font-weight: bold;");
            
            heroText.getChildren().addAll(titleRow, heroSubtitle);
            
            // Close Button
            Button closeBtn = new Button("✕");
            closeBtn.setStyle("-fx-background-color: rgba(0,0,0,0.3); -fx-text-fill: white; -fx-font-size: 20px; -fx-background-radius: 50; -fx-padding: 5 12; -fx-cursor: hand;");
            StackPane.setAlignment(closeBtn, Pos.TOP_RIGHT);
            StackPane.setMargin(closeBtn, new Insets(15));
            closeBtn.setOnAction(e -> dialog.close());
            
            heroSection.getChildren().addAll(heroText, closeBtn);

            // --- CONTENT SECTION ---
            VBox content = new VBox(25);
            content.setPadding(new Insets(30, 40, 40, 40));
            content.setStyle("-fx-background-color: transparent;");

            // ... (Quick Info remains) ...
            HBox quickInfo = new HBox(30);
            quickInfo.setAlignment(Pos.CENTER);
            quickInfo.getChildren().addAll(
                createStatCard("💰 PRIX", (event.getPrix() != null ? event.getPrix() + " TND" : "GRATUIT"), "#F0FDF4", "#166534"),
                createStatCard("👥 PLACES DISPONIBLES", (event.getPlacesDisponibles() != null ? event.getPlacesDisponibles() + " SUR " + event.getCapaciteMax() : "N/A"), "#EFF6FF", "#1E40AF")
            );

            // Detailed Info Section
            VBox detailBox = new VBox(20);
            detailBox.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 25; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 10, 0, 0, 4);");

            // Dates & Time
            VBox dateBox = new VBox(8);
            Label dateHead = new Label("📅 HORAIRES DE L'ÉVÉNEMENT");
            dateHead.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #64748B; -fx-letter-spacing: 1;");
            String debutStr = event.getDateDebut() != null ? event.getDateDebut().format(DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy 'à' HH:mm")) : "N/A";
            String finStr = event.getDateFin() != null ? event.getDateFin().format(DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy 'à' HH:mm")) : "N/A";
            Label dateFull = new Label("Du " + debutStr + "\nAu " + finStr);
            dateFull.setStyle("-fx-font-size: 15px; -fx-text-fill: #1E293B; -fx-line-spacing: 5;");
            dateBox.getChildren().addAll(dateHead, dateFull);

            // Contact & Equipment
            GridPane grid = new GridPane();
            grid.setHgap(40);
            grid.setVgap(25);
            VBox contactSection = createInfoSection("📞 CONTACT", 
                "✉️ " + (event.getEmail() != null ? event.getEmail() : "Non fourni"),
                "📱 " + (event.getTelephone() != null && event.getTelephone() != 0 ? event.getTelephone() : "Non fourni"));
            VBox equipSection = createInfoSection("🎒 ÉQUIPEMENTS", 
                (event.getMaterielsNecessaires() != null && !event.getMaterielsNecessaires().isBlank() 
                    ? event.getMaterielsNecessaires() : "Aucun matériel spécifique requis."));
            grid.add(contactSection, 0, 0);
            grid.add(equipSection, 1, 0);
            detailBox.getChildren().addAll(dateBox, new Separator(), grid);

            // Multimedia (Video, Activities & Gallery)
            VBox multimediaBox = new VBox(25);
            
            // Activities
            VBox activitySection = new VBox(10);
            Label activityHead = new Label("🎯 ACTIVITÉS ASSOCIÉES");
            activityHead.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #64748B;");
            activitySection.getChildren().add(activityHead);
            FlowPane activityChips = new FlowPane(10, 10);
            loadActivityChips(event, activityChips);
            activitySection.getChildren().add(activityChips);
            multimediaBox.getChildren().add(activitySection);

            // Video Link
            if (event.getVideoYoutube() != null && !event.getVideoYoutube().isBlank()) {
                HBox ytBox = new HBox(10);
                ytBox.setAlignment(Pos.CENTER_LEFT);
                ytBox.setStyle("-fx-background-color: #FEF2F2; -fx-padding: 15; -fx-background-radius: 10; -fx-cursor: hand;");
                Label ytLabel = new Label("🎬 VOIR LA VIDÉO DE PRÉSENTATION SUR YOUTUBE");
                ytLabel.setStyle("-fx-text-fill: #991B1B; -fx-font-weight: bold; -fx-font-size: 13px;");
                ytBox.getChildren().add(ytLabel);
                ytBox.setOnMouseClicked(e -> { try { java.awt.Desktop.getDesktop().browse(new java.net.URI(event.getVideoYoutube())); } catch (Exception ignored) {} });
                multimediaBox.getChildren().add(ytBox);
            }

            // Photo Album (Carousel)
            VBox albumBox = createCarousel(event);
            multimediaBox.getChildren().add(albumBox);

            // --- REVIEWS SECTION ---
            VBox reviewSection = new VBox(15);
            reviewSection.setStyle("-fx-background-color: #F1F5F9; -fx-padding: 20; -fx-background-radius: 15;");
            Label reviewTitle = new Label("💬 AVIS & COMMENTAIRES");
            reviewTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #475569;");
            loadComments(event.getId(), commentsList);
            reviewSection.getChildren().addAll(reviewTitle, commentsList);

            // Final Action Button
            Button reserverBtn = new Button("CONFIRMER MA RÉSERVATION");
            reserverBtn.setStyle("-fx-background-color: #2563EB; -fx-text-fill: white; -fx-background-radius: 12; -fx-padding: 18; -fx-font-size: 16px; -fx-font-weight: bold; -fx-cursor: hand;");
            reserverBtn.setMaxWidth(Double.MAX_VALUE);
            reserverBtn.setOnAction(e -> { dialog.close(); ouvrirReservation(event); });

            content.getChildren().addAll(quickInfo, detailBox, multimediaBox, reviewSection, reserverBtn);
            mainContainer.getChildren().addAll(heroSection, content);

            ScrollPane mainScroll = new ScrollPane(mainContainer);
            mainScroll.setFitToWidth(true);
            mainScroll.setStyle("-fx-background-color: #F8FAFC; -fx-border-color: transparent;");

            Scene scene = new Scene(mainScroll, 720, Math.min(900, javafx.stage.Screen.getPrimary().getVisualBounds().getHeight() - 50));
            dialog.setScene(scene);
            dialog.setResizable(true);
            dialog.centerOnScreen();
            dialog.show();

        } catch (Exception e) {
            System.err.println("Erreur design details: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur lors de l'affichage des détails.");
        }
    }

    private void ouvrirPartageDialog(Event event, Stage parent, VBox commentsContainerToRefresh) {
        Stage shareDialog = new Stage();
        shareDialog.initOwner(parent);
        shareDialog.setTitle("Partager l'événement");

        VBox layout = new VBox(15);
        layout.setStyle("-fx-padding: 20; -fx-background-color: white;");
        layout.setAlignment(Pos.CENTER);

        Label head = new Label("Partager avec un commentaire");
        head.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        TextArea commentArea = new TextArea();
        commentArea.setPromptText("Votre avis ou commentaire sur cet événement...");
        commentArea.setPrefRowCount(3);

        HBox networks = new HBox(10);
        networks.setAlignment(Pos.CENTER);
        Button fb = new Button("Facebook"); fb.setStyle("-fx-background-color: #1877F2; -fx-text-fill: white;");
        Button tw = new Button("Twitter"); tw.setStyle("-fx-background-color: #1DA1F2; -fx-text-fill: white;");
        networks.getChildren().addAll(fb, tw);

        Button submitBtn = new Button("PUBLIER MON AVIS");
        submitBtn.setStyle("-fx-background-color: #2563EB; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");
        
        fb.setOnAction(e -> {
            try {
                String url = "https://www.facebook.com/sharer/sharer.php?u=" + URLEncoder.encode("http://wanderlust.com/event/" + event.getId(), "UTF-8") + 
                             "&quote=" + URLEncoder.encode(commentArea.getText() + "\nRegardez cet événement Wanderlust !", "UTF-8");
                java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        tw.setOnAction(e -> {
            try {
                String url = "https://twitter.com/intent/tweet?text=" + URLEncoder.encode(commentArea.getText() + " #Wanderlust #Aventure", "UTF-8") + 
                             "&url=" + URLEncoder.encode("http://wanderlust.com/event/" + event.getId(), "UTF-8");
                java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        submitBtn.setOnAction(e -> {
            String comment = commentArea.getText().trim();
            if (!comment.isEmpty()) {
                saveComment(event.getId(), comment);
                
                // Refresh the UI
                if (commentsContainerToRefresh != null) {
                    commentsContainerToRefresh.getChildren().clear();
                    loadComments(event.getId(), commentsContainerToRefresh);
                }
                
                shareDialog.close();
                showAlert("Merci ! Votre avis a été publié.");
            }
        });

        layout.getChildren().addAll(head, commentArea, networks, submitBtn);
        shareDialog.setScene(new Scene(layout, 400, 300));
        shareDialog.show();
    }

    private void saveComment(int eventId, String comment) {
        System.out.println("DEBUG: Sauvegarde du commentaire pour l'événement ID: " + eventId);
        try {
            String sql = "INSERT INTO event_reviews (events_id, user_name, comment, date_creation) VALUES (?, ?, ?, CURRENT_TIMESTAMP)";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, eventId);
            pstmt.setString(2, "Utilisateur"); // On pourrait utiliser Session.getCurrentUserName()
            pstmt.setString(3, comment);
            int affected = pstmt.executeUpdate();
            System.out.println("DEBUG: Commentaire sauvegardé, lignes affectées: " + affected);
        } catch (SQLException e) {
            System.err.println("Erreur sauvegarde avis: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadComments(int eventId, VBox container) {
        System.out.println("DEBUG: Chargement des commentaires pour l'événement ID: " + eventId);
        try {
            String sql = "SELECT user_name, comment, date_creation FROM event_reviews WHERE events_id = ? ORDER BY date_creation DESC";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, eventId);
            ResultSet rs = pstmt.executeQuery();
            boolean hasComments = false;
            container.getChildren().clear(); // Clear existing children to avoid duplicates
            
            while (rs.next()) {
                hasComments = true;
                VBox card = new VBox(5);
                card.setStyle("-fx-background-color: white; -fx-padding: 10; -fx-background-radius: 8; -fx-border-color: #E2E8F0; -fx-border-radius: 8;");
                
                Label user = new Label("👤 " + rs.getString("user_name"));
                user.setStyle("-fx-font-weight: bold; -fx-text-fill: #1E293B;");
                
                Label text = new Label(rs.getString("comment"));
                text.setWrapText(true);
                text.setStyle("-fx-text-fill: #475569;");
                
                Timestamp ts = rs.getTimestamp("date_creation");
                String dateStr = (ts != null) ? ts.toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "Date inconnue";
                Label date = new Label(dateStr);
                date.setStyle("-fx-font-size: 10px; -fx-text-fill: #94A3B8;");
                
                card.getChildren().addAll(user, text, date);
                container.getChildren().add(card);
            }
            if (!hasComments) {
                Label noMsg = new Label("Aucun avis pour le moment. Soyez le premier à partager !");
                noMsg.setStyle("-fx-text-fill: #94A3B8; -fx-font-style: italic;");
                container.getChildren().add(noMsg);
            }
            System.out.println("DEBUG: Nombre de commentaires chargés: " + (hasComments ? "plusieurs" : "zéro"));
        } catch (SQLException e) {
            System.err.println("Erreur chargement avis: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadActivityChips(Event event, FlowPane container) {
        try {
            String sql = "SELECT a.titre FROM activites a JOIN events_activities ea ON a.id = ea.activities_id WHERE ea.events_id = ?";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, event.getId());
            ResultSet rs = ps.executeQuery();
            boolean found = false;
            while (rs.next()) {
                found = true;
                Label chip = new Label(rs.getString("titre"));
                chip.setStyle("-fx-background-color: #F3E8FF; -fx-text-fill: #6B21A8; -fx-padding: 8 15; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 12px;");
                container.getChildren().add(chip);
            }
            if (!found) {
                Label noAct = new Label("Aucune activité associée.");
                noAct.setStyle("-fx-text-fill: #94A3B8; -fx-font-style: italic;");
                container.getChildren().add(noAct);
            }
        } catch (SQLException ignore) {}
    }

    private VBox createCarousel(Event event) {
        VBox albumBox = new VBox(15);
        albumBox.setAlignment(Pos.CENTER);
        Label albumTitle = new Label("📸 ALBUM PHOTOS");
        albumTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #64748B;");
        
        List<String> allImagePaths = new ArrayList<>();
        if (event.getImage() != null && !event.getImage().isBlank()) allImagePaths.add(event.getImage());
        try {
            EventPhotoService eps = new EventPhotoService();
            List<EventPhoto> photos = eps.getPhotosByEvent(event.getId());
            for (EventPhoto p : photos) allImagePaths.add(p.getCheminPhoto());
        } catch (Exception ignore) {}

        if (allImagePaths.isEmpty()) {
            Label noPhoto = new Label("Aucun visuel disponible.");
            noPhoto.setStyle("-fx-text-fill: #94A3B8; -fx-font-style: italic;");
            albumBox.getChildren().addAll(albumTitle, noPhoto);
        } else {
            StackPane imageContainer = new StackPane();
            imageContainer.setPrefSize(500, 350);
            imageContainer.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-border-color: #E2E8F0; -fx-border-radius: 15; -fx-padding: 10;");
            ImageView carouselImageView = new ImageView();
            carouselImageView.setFitWidth(480); carouselImageView.setFitHeight(330); carouselImageView.setPreserveRatio(true);
            imageContainer.getChildren().add(carouselImageView);
            HBox controls = new HBox(20); controls.setAlignment(Pos.CENTER);
            Button prevBtn = new Button("◀"); prevBtn.setStyle("-fx-background-color: #BFDBFE; -fx-text-fill: #1E40AF; -fx-background-radius: 30; -fx-pref-width: 50; -fx-pref-height: 50; -fx-font-weight: bold; -fx-cursor: hand;");
            Label counterLabel = new Label(); counterLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #64748B;");
            Button nextBtn = new Button("▶"); nextBtn.setStyle("-fx-background-color: #BFDBFE; -fx-text-fill: #1E40AF; -fx-background-radius: 30; -fx-pref-width: 50; -fx-pref-height: 50; -fx-font-weight: bold; -fx-cursor: hand;");
            final int[] currentIndex = {0};
            Runnable updateCarousel = () -> {
                String path = allImagePaths.get(currentIndex[0]);
                try {
                    if (!path.startsWith("http") && !path.startsWith("file")) {
                        File f = new File(path); if (!f.exists()) f = new File(System.getProperty("user.dir") + "/" + path);
                        path = f.toURI().toString();
                    }
                    carouselImageView.setImage(new Image(path, 600, 400, true, true, true));
                    counterLabel.setText((currentIndex[0] + 1) + " / " + allImagePaths.size());
                } catch (Exception e) {}
            };
            prevBtn.setOnAction(e -> { currentIndex[0] = (currentIndex[0] - 1 + allImagePaths.size()) % allImagePaths.size(); updateCarousel.run(); });
            nextBtn.setOnAction(e -> { currentIndex[0] = (currentIndex[0] + 1) % allImagePaths.size(); updateCarousel.run(); });
            updateCarousel.run();
            controls.getChildren().addAll(prevBtn, counterLabel, nextBtn);
            albumBox.getChildren().addAll(albumTitle, imageContainer, controls);
        }
        return albumBox;
    }

    private VBox createStatCard(String title, String value, String bgColor, String textColor) {
        VBox card = new VBox(5);
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(180);
        card.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 12; -fx-padding: 15;");
        
        Label t = new Label(title);
        t.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + textColor + "; -fx-opacity: 0.8;");
        
        Label v = new Label(value);
        v.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + textColor + ";");
        
        card.getChildren().addAll(t, v);
        return card;
    }

    private VBox createInfoSection(String title, String... lines) {
        VBox section = new VBox(8);
        Label head = new Label(title);
        head.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #64748B;");
        section.getChildren().add(head);
        for (String line : lines) {
            Label l = new Label(line);
            l.setStyle("-fx-font-size: 14px; -fx-text-fill: #1E293B;");
            l.setWrapText(true);
            section.getChildren().add(l);
        }
        return section;
    }

    private String getEventActivityTitle(Event event) {
        if (event.getIdActivite() <= 0) return "DIVERS";
        try {
            String actSql = "SELECT titre FROM activites WHERE id = ?";
            PreparedStatement actPs = connection.prepareStatement(actSql);
            actPs.setInt(1, event.getIdActivite());
            ResultSet actRs = actPs.executeQuery();
            if (actRs.next()) return actRs.getString("titre").toUpperCase();
        } catch (SQLException ignore) {}
        return "ACTIVITÉ";
    }

    private void addDetailRow(GridPane grid, int row, String label, String value) {
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #475569;");
        Label val = new Label(value != null ? value : "N/A");
        val.setStyle("-fx-text-fill: #1E293B;");
        grid.add(lbl, 0, row);
        grid.add(val, 1, row);
    }

    private void addThumbnail(HBox container, String path) {
        if (path == null || path.isBlank()) return;
        try {
            String finalPath = path;
            if (!finalPath.startsWith("http") && !finalPath.startsWith("file")) {
                // Try absolute path first
                File f = new File(finalPath);
                if (!f.exists()) {
                    // Try relative to project root
                    f = new File(System.getProperty("user.dir") + "/" + finalPath);
                }
                finalPath = f.toURI().toString();
            }
            Image img = new Image(finalPath, 200, 150, true, true, true);
            img.errorProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) System.err.println("Error loading image: " + path);
            });
            
            ImageView iv = new ImageView(img);
            iv.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 8, 0, 0, 3); -fx-background-radius: 8;");
            
            // Add click to enlarge? Maybe later.
            container.getChildren().add(iv);
        } catch (Exception e) {
            System.err.println("Failed to add thumbnail for path: " + path + " -> " + e.getMessage());
        }
    }

    private void addReservationCard(Reservation reservation) {
        VBox card = new VBox();
        card.setSpacing(12);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 4); -fx-padding: 20;");
        card.setPrefWidth(320);

        // Header with status badge
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(10);

        Label titleLabel = new Label(reservation.getEvent() != null && reservation.getEvent().getOrganisateur() != null
                ? reservation.getEvent().getOrganisateur()
                : "Réservation #" + reservation.getId());
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");
        titleLabel.setWrapText(true);
        HBox.setHgrow(titleLabel, javafx.scene.layout.Priority.ALWAYS);

        String status = reservation.getStatut() != null ? reservation.getStatut().name().toLowerCase() : "en_attente";
        Label statusBadge = new Label(normalizeReservationStatus(status));
        String badgeStyle = "-fx-padding: 4 10; -fx-background-radius: 20; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: white;";
        
        if (status.equals("accepte")) {
            badgeStyle += "-fx-background-color: #10B981;"; // Emerald 500
        } else if (status.equals("refuse")) {
            badgeStyle += "-fx-background-color: #EF4444;"; // Red 500
        } else {
            badgeStyle += "-fx-background-color: #F59E0B;"; // Amber 500
        }
        statusBadge.setStyle(badgeStyle);

        header.getChildren().addAll(titleLabel, statusBadge);

        // Info lines
        VBox infoBox = new VBox(8);
        
        HBox lieuBox = new HBox(8);
        lieuBox.setAlignment(Pos.CENTER_LEFT);
        Label lieuIcon = new Label("📍");
        Label lieuText = new Label(reservation.getEvent() != null && reservation.getEvent().getLieu() != null ? reservation.getEvent().getLieu() : "Lieu non défini");
        lieuText.setStyle("-fx-text-fill: #64748B; -fx-font-size: 13px;");
        lieuBox.getChildren().addAll(lieuIcon, lieuText);

        HBox dateBox = new HBox(8);
        dateBox.setAlignment(Pos.CENTER_LEFT);
        Label dateIcon = new Label("📅");
        String dateStr = "Non défini";
        if (reservation.getEvent() != null && reservation.getEvent().getDateDebut() != null) {
            dateStr = reservation.getEvent().getDateDebut().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        }
        Label dateText = new Label(dateStr);
        dateText.setStyle("-fx-text-fill: #64748B; -fx-font-size: 13px;");
        dateBox.getChildren().addAll(dateIcon, dateText);

        HBox paxBox = new HBox(8);
        paxBox.setAlignment(Pos.CENTER_LEFT);
        Label paxIcon = new Label("👥");
        Label paxText = new Label(reservation.getNombrePersonnes() + " participant(s)");
        paxText.setStyle("-fx-text-fill: #64748B; -fx-font-size: 13px;");
        paxBox.getChildren().addAll(paxIcon, paxText);

        infoBox.getChildren().addAll(lieuBox, dateBox, paxBox);

        Separator sep = new Separator();
        sep.setStyle("-fx-padding: 5 0;");

        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_LEFT);
        Label totalTitle = new Label("Total payé");
        totalTitle.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px;");
        HBox.setHgrow(totalTitle, javafx.scene.layout.Priority.ALWAYS);

        Label totalValue = new Label(String.format("%.2f TND", reservation.getPrixTotal() != null ? reservation.getPrixTotal() : 0.0));
        totalValue.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        Button viewBtn = new Button("Détails");
        viewBtn.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-padding: 5 15; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-weight: bold;");
        viewBtn.setOnAction(e -> showReservationDetails(reservation));

        footer.getChildren().addAll(totalTitle, totalValue, viewBtn);
        HBox.setMargin(viewBtn, new javafx.geometry.Insets(0, 0, 0, 10));

        card.getChildren().addAll(header, infoBox, sep, footer);
        flowReservations.getChildren().add(card);
    }

    private void showReservationDetails(Reservation reservation) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pi_dev/events/ReservationDetails.fxml"));
            AnchorPane root = loader.load();
            
            ReservationDetailsController controller = loader.getController();
            controller.setReservationData(reservation);
            
            Stage stage = new Stage();
            stage.setTitle("Détails de la réservation");
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setScene(new javafx.scene.Scene(root));
            stage.show();
        } catch (IOException e) {
            System.err.println("Erreur ouverture détails réservation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private VBox createReservationInfoCard(String text) {
        VBox card = new VBox();
        card.setSpacing(10);
        card.setStyle(
                "-fx-border-color: #cbd5e1; -fx-border-width: 1; -fx-padding: 15; -fx-background-color: white; -fx-background-radius: 10; -fx-border-radius: 10;");
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569;");
        label.setWrapText(true);
        card.getChildren().add(label);
        return card;
    }

    private void ouvrirReservation(Event event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pi_dev/events/Reservation.fxml"));
            Parent root = loader.load();
            ReservationController controller = loader.getController();
            controller.loadEvent(event.getId());
            Stage stage = new Stage();
            stage.setTitle("Réservation: " + event.getOrganisateur());
            stage.setScene(new Scene(root));
            stage.setWidth(900);
            stage.setHeight(700);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur lors de l'ouverture de la réservation: " + e.getMessage());
        }
    }

    private void ouvrirModificationEvent(Event event) {
        try {
            System.out.println("DEBUG: Ouverture modification pour l'événement ID: " + event.getId());

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pi_dev/events/modifierEvent.fxml"));
            if (loader.getLocation() == null) {
                System.err.println("ERREUR: Fichier modifierEvent.fxml introuvable");
                showAlert("Fichier de modification d'événement introuvable");
                return;
            }

            Parent root = loader.load();
            modifierEventController controller = loader.getController();

            if (controller == null) {
                System.err.println("ERREUR: Controller non trouvé dans le FXML");
                showAlert("Controller de modification non trouvé");
                return;
            }

            controller.setEventId(event.getId());
            Stage stage = new Stage();
            stage.setTitle("Modifier l'événement: " + event.getOrganisateur());
            stage.setScene(new Scene(root));
            stage.setWidth(900);
            stage.setHeight(700);
            stage.centerOnScreen();
            stage.show();
            stage.setOnHidden(e -> refreshData());

            System.out.println("DEBUG: Fenêtre de modification ouverte avec succès");

        } catch (IOException e) {
            System.err.println("ERREUR IOException lors de l'ouverture de la modification: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur lors de l'ouverture de la modification: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showAlert(String message) {
        showAlert("Information", message);
    }

    private void loadWeatherData() {
        String selectedCity = (cityComboBox != null && cityComboBox.getSelectionModel().getSelectedItem() != null)
                ? cityComboBox.getSelectionModel().getSelectedItem()
                : "Tunis";

        weatherService.getCurrentWeather(selectedCity, new WeatherService.WeatherCallback() {
            @Override
            public void onWeatherReceived(WeatherService.WeatherData weather) {
                currentWeather = weather;
                javafx.application.Platform.runLater(() -> updateWeatherDisplay(weather, selectedCity));
            }

            @Override
            public void onError(String error) {
                currentWeather = null;
                System.err.println("Erreur météo: " + error);
                javafx.application.Platform.runLater(() -> showWeatherError(error, selectedCity));
            }
        });
    }

    @FXML
    void refreshWeather(ActionEvent event) {
        loadWeatherData();
    }

    private void updateWeatherDisplay(WeatherService.WeatherData weather, String city) {
        if (weatherInfoContainer == null)
            return;

        weatherInfoContainer.getChildren().clear();
        VBox weatherWidget = weatherService.createWeatherWidget(weather);
        weatherInfoContainer.getChildren().add(weatherWidget);
    }

    private void showWeatherError(String error, String city) {
        if (weatherInfoContainer == null)
            return;

        weatherInfoContainer.getChildren().clear();
        VBox errorWidget = new VBox(10);
        errorWidget.setStyle("-fx-background-color: #FEE2E2; -fx-background-radius: 8; -fx-padding: 15;");
        Label errorLabel = new Label("❌ Erreur météo pour " + city + ": " + error);
        errorLabel.setStyle("-fx-text-fill: #991B1B; -fx-font-size: 12px;");
        errorWidget.getChildren().add(errorLabel);
        weatherInfoContainer.getChildren().add(errorWidget);
    }

    public boolean isEventCompatibleWithWeather(String activityType, LocalDate eventDate) {
        if (currentWeather == null || activityType == null)
            return true;

        String condition = currentWeather.getCondition();

        if (condition.contains("rain")) {
            if (activityType.toLowerCase().contains("plong") ||
                    activityType.toLowerCase().contains("bateau") ||
                    activityType.toLowerCase().contains("kayak") ||
                    activityType.toLowerCase().contains("skydiving"))
                return false;
        }

        if (currentWeather.getWindSpeed() > 10) {
            if (activityType.toLowerCase().contains("parapente") ||
                    activityType.toLowerCase().contains("montgolfière") ||
                    activityType.toLowerCase().contains("deltaplane"))
                return false;
        }

        if (condition.contains("storm")) {
            if (activityType.toLowerCase().contains("plong") ||
                    activityType.toLowerCase().contains("escalade") ||
                    activityType.toLowerCase().contains("randonnée"))
                return false;
        }

        if (currentWeather.getTemperature() > 40) {
            if (activityType.toLowerCase().contains("trekking") ||
                    activityType.toLowerCase().contains("randonnée") ||
                    activityType.toLowerCase().contains("vélo"))
                return false;
        }

        return true;
    }

    public void showWeatherWarningForEvent(String activityType, LocalDate eventDate) {
        if (currentWeather == null || isEventCompatibleWithWeather(activityType, eventDate))
            return;

        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("⚠️ Alerte Météo");
        alert.setHeaderText("Conditions météo défavorables pour cet événement");

        String condition = currentWeather.getCondition();
        String riskMessage = "";

        if (condition.contains("rain")) {
            riskMessage = "Pluie détectée - Activités nautiques déconseillées";
        } else if (currentWeather.getWindSpeed() > 10) {
            riskMessage = "Vent fort (" + currentWeather.getWindSpeedDisplay()
                    + ") - Activités aériennes déconseillées";
        } else if (condition.contains("storm")) {
            riskMessage = "Orage détecté - Activités en extérieur déconseillées";
        } else if (currentWeather.getTemperature() > 40) {
            riskMessage = "Chaleur extrême (" + currentWeather.getTemperatureDisplay() + ") - Risque de déshydratation";
        }

        String content = "📅 Date: " + eventDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")) + "\n" +
                "🌤️ Météo: " + currentWeather.getDescription() + "\n" +
                "🌡️ Température: " + currentWeather.getTemperatureDisplay() + "\n" +
                "💨 Vent: " + currentWeather.getWindSpeedDisplay() + "\n\n" +
                "⚠️ " + riskMessage + "\n\nActivité: " + activityType + "\n\nVoulez-vous continuer ?";

        alert.setContentText(content);
        alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.NO) {
                System.out.println("Événement annulé - conditions météo défavorables");
            }
        });
    }
}