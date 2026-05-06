package com.example.pi_dev.Controllers.Events;

import com.example.pi_dev.Services.Events.WeatherService;
import com.example.pi_dev.Utils.Events.Mydatabase;
import com.example.pi_dev.Entities.Events.Event;
import com.example.pi_dev.Entities.Events.Activite;
import com.example.pi_dev.Session.Session;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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
    private ScrollPane scrollPaneActivites;
    @FXML
    private ScrollPane scrollPaneEvents;
    @FXML
    private Tab tabActivites;
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

    public void initialize() {
        initializeDatabase();
        refreshData();

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
            while (true) {
                try {
                    Thread.sleep(1000);
                    if (CatalogueRefreshManager.getInstance().isRefreshRequested()) {
                        javafx.application.Platform.runLater(() -> {
                            refreshData();
                            CatalogueRefreshManager.getInstance().resetRefresh();
                        });
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
        displayActivites();
        displayEvents();
    }

    private void displayActivites() {
        flowActivites.getChildren().clear();
        for (Activite activite : activitesList) {
            addActiviteCard(activite);
        }
    }

    private void displayEvents() {
        flowEvents.getChildren().clear();
        for (Event event : eventsList) {
            addEventCard(event);
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
                        "SELECT id, titre, description, type_activite, image, created_by_id FROM activites");
                while (rs.next()) {
                    Activite activite = new Activite();
                    activite.setId(rs.getInt("id"));
                    activite.setTitre(rs.getString("titre"));
                    activite.setDescription(rs.getString("description"));
                    activite.setTypeActivite(rs.getString("type_activite"));
                    activite.setImage(rs.getString("image"));
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
            ResultSet rs = stmt.executeQuery("SELECT * FROM events");
            while (rs.next()) {
                Event event = new Event();
                event.setId(rs.getInt("id"));
                event.setIdActivite(rs.getInt("id_activite"));
                event.setDateDebut(
                        rs.getTimestamp("date_debut") != null ? rs.getTimestamp("date_debut").toLocalDateTime() : null);
                event.setDateFin(
                        rs.getTimestamp("date_fin") != null ? rs.getTimestamp("date_fin").toLocalDateTime() : null);
                event.setPrix(rs.getBigDecimal("prix"));
                event.setCapaciteMax(rs.getInt("capacite_max"));
                event.setPlacesDisponibles(rs.getInt("places_disponibles"));
                event.setOrganisateur(rs.getString("organisateur"));
                event.setMaterielsNecessaires(rs.getString("materiels_necessaires"));
                event.setStatut(parseStatutEvent(rs.getString("statut")));
                event.setDateCreation(rs.getTimestamp("date_creation"));
                event.setDateModification(rs.getTimestamp("date_modification"));
                try {
                    event.setCreatedById(rs.getString("created_by_id"));
                } catch (Exception ignore) {
                }
                eventsList.add(event);
            }
        } catch (SQLException e) {
            System.err.println("Erreur chargement événements: " + e.getMessage());
        }
    }

    private Event.StatutEvent parseStatutEvent(String statut) {
        if (statut == null || statut.trim().isEmpty()) {
            return Event.StatutEvent.A_VENIR;
        }

        String normalized = statut.trim().toUpperCase();
        try {
            return Event.StatutEvent.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return Event.StatutEvent.A_VENIR;
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
    void ouvrirGoogleCalendar(ActionEvent event) {
        try {
            System.out.println("Ouverture de l'interface Google Calendar...");
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pi_dev/events/googleCalendar.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Google Calendar - Configuration");
            stage.setScene(new Scene(root));
            stage.setWidth(500);
            stage.setHeight(600);
            stage.centerOnScreen();
            stage.show();

            System.out.println("Interface Google Calendar ouverte avec succès");

        } catch (Exception e) {
            System.err.println("Erreur lors de l'ouverture de Google Calendar: " + e.getMessage());
            showAlert("Erreur", "Impossible d'ouvrir l'interface Google Calendar.");
        }
    }

    @FXML
    void ouvrirGoogleMaps(ActionEvent event) {
        try {
            System.out.println("Ouverture de l'interface Google Maps...");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pi_dev/events/googleMaps.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Google Maps - Localisation d'Événements");
            stage.setScene(new Scene(root));
            stage.setWidth(850);
            stage.setHeight(750);
            stage.centerOnScreen();
            stage.show();

            System.out.println("Interface Google Maps ouverte avec succès");

        } catch (Exception e) {
            System.err.println("Erreur lors de l'ouverture de Google Maps: " + e.getMessage());
            showAlert("Erreur", "Impossible d'ouvrir l'interface Google Maps.");
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
        card.setSpacing(10);
        card.setStyle(
                "-fx-border-color: #ccc; -fx-border-width: 1; -fx-padding: 10; -fx-background-color: white; -fx-background-radius: 8; -fx-border-radius: 8; -fx-cursor: hand;");
        card.setPrefWidth(200);

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
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1a5f3f;");
        titleLabel.setWrapText(true);

        card.getChildren().addAll(imageView, titleLabel);

        if (activite.getTypeActivite() != null && !activite.getTypeActivite().isEmpty()) {
            Label typeLabel = new Label("Type: " + activite.getTypeActivite());
            typeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
            card.getChildren().add(typeLabel);
        }

        Label descLabel = new Label(activite.getDescription() != null ? activite.getDescription() : "");
        descLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");
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
                    "-fx-border-color: #ccc; -fx-border-width: 1; -fx-padding: 15; -fx-background-color: white; -fx-background-radius: 10; -fx-border-radius: 10; -fx-cursor: hand;");

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

            Label prixLabel = new Label("💰 Prix: " + (event.getPrix() != null ? event.getPrix() + " TND" : "0 TND"));
            prixLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2d7a2d;");

            Label capaciteLabel = new Label("👥 Capacité: " + event.getCapaciteMax());
            capaciteLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

            Label placesLabel = new Label("🎫 Places: " + event.getPlacesDisponibles());
            placesLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

            HBox buttonBox = new HBox(5);
            buttonBox.setAlignment(Pos.CENTER);

            String currentUser = Session.getCurrentUserId();
            final Event eventFinal = event;
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
            card.getChildren().addAll(titleLabel, dateDebutLabel, dateFinLabel, prixLabel, capaciteLabel, placesLabel,
                    buttonBox);

            card.setOnMouseClicked(e -> ouvrirReservation(eventFinal));

            flowEvents.getChildren().add(card);

        } catch (Exception e) {
            System.err.println("Erreur carte événement: " + e.getMessage());
        }
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
            showAlert("Erreur lors de l'ouverture de la réservation");
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