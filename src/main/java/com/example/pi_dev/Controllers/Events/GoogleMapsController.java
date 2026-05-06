package com.example.pi_dev.Controllers.Events;

import com.example.pi_dev.Services.Events.GoogleMapsService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

public class GoogleMapsController {
    @FXML private Label titleLabel;
    @FXML private TextField searchField;
    @FXML private TextField locationField;
    @FXML private ComboBox<String> placeTypeComboBox;
    @FXML private Slider radiusSlider;
    @FXML private Label radiusLabel;
    @FXML private Button searchButton;
    @FXML private Button testConnectionButton;
    @FXML private Button getDirectionsButton;
    @FXML private Button showMapButton;
    @FXML private ProgressBar progressBar;
    @FXML private Label statusLabel;
    @FXML private VBox resultsContainer;
    @FXML private ImageView mapImageView;
    @FXML private ScrollPane scrollPane;

    private GoogleMapsService mapsService;
    private boolean isConnected = false;

    @FXML
    public void initialize() {
        mapsService = new GoogleMapsService();

        setupUI();

        mapsService.initialize();

        updateStatus(false);
    }

    private void setupUI() {
        titleLabel.setText("🗺️ Google Maps - Localisation d'Événements");

        placeTypeComboBox.getItems().addAll(
                "Restaurant",
                "Hotel",
                "Parking",
                "Centre Commercial",
                "Pharmacie",
                "Banque",
                "Station Service"
        );
        placeTypeComboBox.getSelectionModel().selectFirst();

        radiusSlider.setMin(100);
        radiusSlider.setMax(5000);
        radiusSlider.setValue(1000);
        radiusSlider.setShowTickLabels(true);
        radiusSlider.setShowTickMarks(true);
        radiusSlider.setMajorTickUnit(1000);
        radiusSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            radiusLabel.setText(String.format("Rayon: %.0f m", newVal));
        });

        radiusLabel.setText("Rayon: 1000 m");
        progressBar.setVisible(false);

        mapImageView.setFitWidth(600);
        mapImageView.setFitHeight(400);
        mapImageView.setPreserveRatio(true);
    }

    @FXML
    void searchLocation(ActionEvent event) {
        String location = searchField.getText().trim();
        if (location.isEmpty()) {
            showAlert("⚠️ Recherche vide", "Veuillez entrer une adresse ou un lieu à rechercher.");
            return;
        }

        performSearch(location);
    }

    @FXML
    void findNearbyPlaces(ActionEvent event) {
        String location = locationField.getText().trim();
        String placeType = placeTypeComboBox.getValue();
        int radius = (int) radiusSlider.getValue();

        if (location.isEmpty()) {
            showAlert("⚠️ Localisation requise", "Veuillez entrer une localisation de base.");
            return;
        }

        performNearbySearch(location, placeType, radius);
    }

    @FXML
    void testConnection(ActionEvent event) {
        testMapsConnection();
    }

    @FXML
    void getDirections(ActionEvent event) {
        String origin = searchField.getText().trim();
        String destination = locationField.getText().trim();

        if (origin.isEmpty() || destination.isEmpty()) {
            showAlert("⚠️ Informations manquantes",
                    "Veuillez entrer un point de départ et une destination.");
            return;
        }

        openDirections(origin, destination);
    }

    @FXML
    void showMap(ActionEvent event) {
        String location = searchField.getText().trim();
        if (location.isEmpty()) {
            showAlert("⚠️ Localisation requise", "Veuillez entrer une adresse à afficher sur la carte.");
            return;
        }

        try {
            updateStatus("🗺️ Test avec carte statique...");

            String testUrl = "https://maps.googleapis.com/maps/api/staticmap?center=36.8065,10.1815&zoom=15&size=600x400&markers=color:red|36.8065,10.1815&key=AIzaSyDWvi3ZewbLDWLkkPlFtNg1iV7hcbdHyE4";

            System.out.println("DEBUG: Test URL: " + testUrl);

            Image testImage = new Image(testUrl, true);

            testImage.errorProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && newVal) {
                    System.err.println("❌ Erreur avec l'URL de test");
                    Platform.runLater(() -> {
                        updateStatus("❌ Erreur de chargement de la carte de test");
                        showAlert("❌ Erreur", "Impossible de charger la carte de test.");
                    });
                } else {
                    System.out.println("✅ Carte de test chargée avec succès");
                    Platform.runLater(() -> {
                        mapImageView.setImage(testImage);
                        mapImageView.setVisible(true);
                        updateStatus("✅ Carte de test affichée");
                    });
                }
            });

            mapImageView.setImage(testImage);
            mapImageView.setVisible(true);

        } catch (Exception e) {
            System.err.println("❌ Erreur lors du test: " + e.getMessage());
            updateStatus("❌ Erreur de test");
            showAlert("❌ Erreur", "Erreur lors du test de la carte: " + e.getMessage());
        }

        showStaticMap(location);
    }

    private void performSearch(String location) {
        updateStatus("🔍 Recherche en cours...");
        progressBar.setVisible(true);

        Task<GoogleMapsService.Coordinates> searchTask = new Task<>() {
            @Override
            protected GoogleMapsService.Coordinates call() throws Exception {
                return mapsService.getCoordinates(location).get();
            }

            @Override
            protected void succeeded() {
                GoogleMapsService.Coordinates coords = getValue();
                Platform.runLater(() -> {
                    progressBar.setVisible(false);

                    if (coords != null) {
                        isConnected = true;
                        updateStatus("✅ Localisation trouvée: " + coords);
                        displayLocationResult(location, coords);

                        locationField.setText(location);

                        showStaticMap(location);
                    } else {
                        updateStatus("❌ Localisation non trouvée");
                        showAlert("❌ Erreur", "Impossible de trouver cette localisation.");
                    }
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    updateStatus("❌ Erreur de recherche");
                    showAlert("❌ Erreur", "Une erreur est survenue lors de la recherche.");
                });
            }
        };

        new Thread(searchTask).start();
    }

    private void performNearbySearch(String location, String placeType, int radius) {
        updateStatus("🔍 Recherche de lieux à proximité...");
        progressBar.setVisible(true);

        Task<GoogleMapsService.NearbyPlacesResult> searchTask = new Task<>() {
            @Override
            protected GoogleMapsService.NearbyPlacesResult call() throws Exception {
                return mapsService.findNearbyPlaces(location, placeType, radius).get();
            }

            @Override
            protected void succeeded() {
                GoogleMapsService.NearbyPlacesResult result = getValue();
                Platform.runLater(() -> {
                    progressBar.setVisible(false);

                    if (result != null && !result.getPlaces().isEmpty()) {
                        updateStatus("✅ " + result.getPlaces().size() + " lieux trouvés");
                        displayNearbyResults(result);
                    } else {
                        updateStatus("❌ Aucun lieu trouvé");
                        showAlert("ℹ️ Aucun résultat", "Aucun lieu de ce type n'a été trouvé à proximité.");
                    }
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    updateStatus("❌ Erreur de recherche");
                    showAlert("❌ Erreur", "Une erreur est survenue lors de la recherche.");
                });
            }
        };

        new Thread(searchTask).start();
    }

    private void displayLocationResult(String location, GoogleMapsService.Coordinates coords) {
        resultsContainer.getChildren().clear();

        VBox resultBox = new VBox(10);
        resultBox.setPadding(new Insets(15));
        resultBox.setStyle("-fx-background-color: #e8f5e8; -fx-background-radius: 8;");

        Label titleLabel = new Label("📍 Localisation trouvée");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2e7d32;");

        Label locationLabel = new Label("📍 " + location);
        locationLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        Label coordsLabel = new Label("🗺️ Coordonnées: " + coords);
        coordsLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_LEFT);

        Button mapBtn = new Button("🗺️ Afficher la carte");
        mapBtn.setStyle("-fx-background-color: #4285f4; -fx-text-fill: white; -fx-background-radius: 5;");
        mapBtn.setOnAction(e -> showStaticMap(location));

        Button directionsBtn = new Button("🧭 Itinéraire");
        directionsBtn.setStyle("-fx-background-color: #34a853; -fx-text-fill: white; -fx-background-radius: 5;");
        directionsBtn.setOnAction(e -> openDirections("Ma position", location));

        buttonBox.getChildren().addAll(mapBtn, directionsBtn);

        resultBox.getChildren().addAll(titleLabel, locationLabel, coordsLabel, buttonBox);
        resultsContainer.getChildren().add(resultBox);
    }

    private void displayNearbyResults(GoogleMapsService.NearbyPlacesResult result) {
        resultsContainer.getChildren().clear();

        VBox resultBox = new VBox(10);
        resultBox.setPadding(new Insets(15));
        resultBox.setStyle("-fx-background-color: #fff3e0; -fx-background-radius: 8;");

        Label titleLabel = new Label("🏪 Lieux à proximité");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #f57c00;");

        resultBox.getChildren().add(titleLabel);

        for (GoogleMapsService.NearbyPlacesResult.Place place : result.getPlaces()) {
            VBox placeBox = new VBox(5);
            placeBox.setPadding(new Insets(10));
            placeBox.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 5;");

            Label nameLabel = new Label("📍 " + place.getName());
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

            Label infoLabel = new Label(String.format("⭐ %.1f • 🚶 %.1f km", place.getRating(), place.getDistance()));
            infoLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");

            placeBox.getChildren().addAll(nameLabel, infoLabel);
            resultBox.getChildren().add(placeBox);
        }

        resultsContainer.getChildren().add(resultBox);
    }

    private void showStaticMap(String location) {
        try {
            updateStatus("🗺️ Génération de la carte...");

            System.out.println("DEBUG: Tentative de génération de carte pour: " + location);

            GoogleMapsService.Coordinates coords = mapsService.simulateCoordinates(location);
            if (coords != null) {
                String mapUrl = String.format(
                        "https://via.placeholder.com/600x400/1e3a8a/ffffff?text=Carte+de+%s\\nLat: %.4f\\nLng: %.4f\\nTunisie",
                        location.replace(" ", "+"),
                        coords.getLatitude(),
                        coords.getLongitude()
                );

                System.out.println("DEBUG: URL de carte avec coordonnées: " + mapUrl);

                Image mapImage = new Image(mapUrl, true);

                mapImageView.setImage(mapImage);
                mapImageView.setVisible(true);
                updateStatus("✅ Carte avec coordonnées affichée");

            } else {
                System.err.println("❌ Coordonnées non trouvées pour: " + location);
                showDefaultMap();
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'affichage de la carte: " + e.getMessage());
            e.printStackTrace();
            showDefaultMap();
        }
    }

    private void showDefaultMap() {
        try {
            updateStatus("🗺️ Affichage de la carte par défaut...");

            GoogleMapsService.Coordinates coords = mapsService.simulateCoordinates("Tunis");
            String infoText;
            if (coords != null) {
                infoText = String.format("Carte Indisponible\\nCoordonnées: %.4f, %.4f\\nLocalisation: Tunisie",
                        coords.getLatitude(), coords.getLongitude());
            } else {
                infoText = "Carte Indisponible\\nLocalisation: Tunisie";
            }

            String defaultMapUrl = String.format(
                    "https://via.placeholder.com/600x400/4285f4/ffffff?text=%s",
                    java.net.URLEncoder.encode(infoText, "UTF-8").replace("+", "%20")
            );

            System.out.println("DEBUG: URL carte par défaut: " + defaultMapUrl);

            Image defaultImage = new Image(defaultMapUrl, true);
            mapImageView.setImage(defaultImage);
            mapImageView.setVisible(true);

            updateStatus("ℹ️ Carte par défaut affichée");

            Platform.runLater(() -> {
                showAlert("ℹ️ Carte limitée", "Les services de cartes en ligne ne sont pas disponibles. Affichage des informations de localisation.");
            });

        } catch (Exception e) {
            System.err.println("❌ Erreur même avec la carte par défaut: " + e.getMessage());
            updateStatus("❌ Carte indisponible");

            Platform.runLater(() -> {
                showAlert("❌ Erreur", "Impossible d'afficher une carte.");
            });
        }
    }

    private void openDirections(String origin, String destination) {
        try {
            String directionsUrl = mapsService.getDirectionsUrl(origin, destination);

            if (directionsUrl != null) {
                java.awt.Desktop.getDesktop().browse(java.net.URI.create(directionsUrl));

                updateStatus("🧭 Itinéraire ouvert dans le navigateur");
            } else {
                showAlert("❌ Erreur", "Impossible de générer l'itinéraire.");
            }

        } catch (Exception e) {
            System.err.println("Erreur lors de l'ouverture de l'itinéraire: " + e.getMessage());
            showAlert("❌ Erreur", "Impossible d'ouvrir l'itinéraire.");
        }
    }

    private void testMapsConnection() {
        updateStatus("🔄 Test de connexion...");
        progressBar.setVisible(true);

        Task<Boolean> testTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                return mapsService.testConnection();
            }

            @Override
            protected void succeeded() {
                boolean connected = getValue();
                Platform.runLater(() -> {
                    progressBar.setVisible(false);

                    if (connected) {
                        isConnected = true;
                        if (mapsService.isAvailable()) {
                            updateStatus("✅ Google Maps connecté (API réelle)");
                            showAlert("✅ Connexion réussie", "Google Maps est connecté avec l'API réelle.");
                        } else {
                            updateStatus("✅ Google Maps connecté (simulation)");
                            showAlert("✅ Connexion réussie", "Google Maps est connecté en mode simulation.");
                        }
                    } else {
                        isConnected = false;
                        updateStatus("❌ Échec de connexion");
                        showAlert("❌ Échec", "Impossible de se connecter à Google Maps.");
                    }
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    updateStatus("❌ Erreur de test");
                    showAlert("❌ Erreur", "Une erreur est survenue lors du test de connexion.");
                });
            }
        };

        new Thread(testTask).start();
    }

    private void updateStatus(String message) {
        statusLabel.setText(message);

        if (message.contains("✅")) {
            statusLabel.setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;");
        } else if (message.contains("❌")) {
            statusLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;");
        } else if (message.contains("🔄") || message.contains("🔍")) {
            statusLabel.setStyle("-fx-text-fill: #ffc107; -fx-font-weight: bold;");
        } else {
            statusLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-weight: normal;");
        }
    }

    private void updateStatus(boolean connected) {
        if (connected) {
            statusLabel.setText("🗺️ Google Maps Connecté");
            statusLabel.setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;");
            isConnected = true;
        } else {
            statusLabel.setText("🗺️ Google Maps Non Connecté");
            statusLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;");
            isConnected = false;
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    void close(ActionEvent event) {
        Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
        stage.close();
    }
}