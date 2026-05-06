package com.example.pi_dev.Controllers.Booking.Front;

import com.example.pi_dev.Entities.Booking.Place;
import com.example.pi_dev.Services.Booking.PlaceImageService;
import com.example.pi_dev.Services.Booking.PlaceService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

import java.util.List;
import java.util.stream.Collectors;

public class PlaceBrowseController {

    @FXML
    private TextField searchField;
    @FXML
    private FlowPane cardsPane;
    @FXML
    private Label resultCountLabel;

    private final PlaceService placeService = new PlaceService();
    private final PlaceImageService placeImageService = new PlaceImageService();
    private List<Place> allPlaces;

    @FXML
    public void initialize() {
        loadPlaces();
        searchField.textProperty().addListener((obs, old, val) -> filterPlaces(val));
    }

    private void loadPlaces() {
        allPlaces = placeService.findApproved();
        renderCards(allPlaces);
    }

    @FXML
    private void handleSearch() {
        filterPlaces(searchField.getText());
    }

    private void filterPlaces(String query) {
        if (query == null || query.isBlank()) {
            renderCards(allPlaces);
            return;
        }
        String q = query.toLowerCase();
        List<Place> filtered = allPlaces.stream()
                .filter(p -> p.getTitle().toLowerCase().contains(q)
                        || p.getCity().toLowerCase().contains(q)
                        || p.getCategory().toLowerCase().contains(q)
                        || p.getDescription().toLowerCase().contains(q))
                .collect(Collectors.toList());
        renderCards(filtered);
    }

    private void renderCards(List<Place> places) {
        cardsPane.getChildren().clear();
        if (resultCountLabel != null) {
            resultCountLabel.setText(places.size() + " properties found");
        }
        for (Place p : places) {
            cardsPane.getChildren().add(buildCard(p));
        }
    }

    private VBox buildCard(Place place) {
        VBox card = new VBox(0);
        card.getStyleClass().add("place-card");
        card.setPrefWidth(300);
        card.setMaxWidth(300);

        // ── Image: primary from place_images, fallback to place.imageUrl ──
        ImageView imageView = new ImageView();
        imageView.setFitWidth(300);
        imageView.setFitHeight(180);
        imageView.setPreserveRatio(false);
        imageView.getStyleClass().add("card-image");

        String imageUrl = null;
        try {
            imageUrl = placeImageService.getPrimaryImageUrl(place.getId());
        } catch (Exception e) {
            System.err.println("WARN getPrimaryImageUrl placeId=" + place.getId() + ": " + e.getMessage());
        }

        if (imageUrl == null || imageUrl.isBlank()) {
            imageUrl = place.getImageUrl();
        }

        System.out.println("DEBUG card placeId=" + place.getId() + " imageUrl=" + imageUrl);

        loadImageIntoView(imageView, imageUrl);

        // Chips row
        HBox chips = new HBox(8);
        chips.setPadding(new Insets(10, 12, 4, 12));
        Label catChip = new Label(place.getCategory());
        catChip.getStyleClass().add("chip");
        Label priceChip = new Label(String.format("$%.0f/day", place.getPricePerDay()));
        priceChip.getStyleClass().addAll("chip", "chip-price");
        chips.getChildren().addAll(catChip, priceChip);

        // Title
        Label title = new Label(place.getTitle());
        title.getStyleClass().add("card-title");
        title.setWrapText(true);
        VBox.setMargin(title, new Insets(4, 12, 0, 12));

        // Description
        Label desc = new Label(place.getDescription());
        desc.getStyleClass().add("card-desc");
        desc.setWrapText(true);
        desc.setMaxHeight(40);
        VBox.setMargin(desc, new Insets(4, 12, 0, 12));

        // Address
        Label addr = new Label("📍 " + place.getAddress() + ", " + place.getCity());
        addr.getStyleClass().add("card-meta");
        addr.setWrapText(true);
        VBox.setMargin(addr, new Insets(4, 12, 0, 12));

        // Capacity row
        HBox meta = new HBox(16);
        meta.setPadding(new Insets(4, 12, 8, 12));
        Label cap = new Label("🏠 " + place.getCapacity() + " rooms");
        Label guests = new Label("👥 " + place.getMaxGuests() + " guests");
        cap.getStyleClass().add("card-meta");
        guests.getStyleClass().add("card-meta");
        meta.getChildren().addAll(cap, guests);

        // Buttons
        HBox btnRow = new HBox(8);
        btnRow.setPadding(new Insets(0, 12, 14, 12));
        Button detailsBtn = new Button("View Details");
        detailsBtn.getStyleClass().addAll("btn", "btn-outline");
        detailsBtn.setOnAction(e -> openDetails(place));
        Button bookBtn = new Button("Book Now");
        bookBtn.getStyleClass().addAll("btn", "btn-primary");
        bookBtn.setOnAction(e -> openBookingDialog(place));
        HBox.setHgrow(detailsBtn, Priority.ALWAYS);
        HBox.setHgrow(bookBtn, Priority.ALWAYS);
        detailsBtn.setMaxWidth(Double.MAX_VALUE);
        bookBtn.setMaxWidth(Double.MAX_VALUE);
        btnRow.getChildren().addAll(detailsBtn, bookBtn);

        card.getChildren().addAll(imageView, chips, title, desc, addr, meta, btnRow);
        return card;
    }

    private void openDetails(Place place) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pi_dev/booking/views/front/PlaceDetails.fxml"));
            Node view = loader.load();
            PlaceDetailsController ctrl = loader.getController();
            ctrl.setPlace(place);
            StackPane parent = (StackPane) cardsPane.getScene().lookup("#contentPane");
            if (parent != null) {
                parent.getChildren().setAll(view);
            }
        } catch (IOException e) {
            showError("Cannot open details: " + e.getMessage());
        }
    }

    private void openBookingDialog(Place place) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pi_dev/booking/views/front/BookingDialog.fxml"));
            Node root = loader.load();
            BookingDialogController ctrl = loader.getController();
            ctrl.setPlace(place);

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Book: " + place.getTitle());
            dialog.setScene(new Scene((javafx.scene.Parent) root));
            dialog.showAndWait();
        } catch (IOException e) {
            showError("Cannot open booking dialog: " + e.getMessage());
        }
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }

    /**
     * Loads an image into an ImageView.
     * For local files (file:// URIs or absolute paths): uses FileInputStream to
     * bypass
     * JavaFX URI encoding issues on Windows (spaces, special chars in paths).
     * For http:// URLs: standard Image loading.
     */
    private void loadImageIntoView(ImageView iv, String url) {
        if (url == null || url.isBlank())
            return;
        try {
            if (url.startsWith("http://") || url.startsWith("https://")) {
                // Remote URL — load normally
                Image img = new Image(url, true);
                img.errorProperty().addListener((obs, o, isErr) -> {
                    if (isErr)
                        System.err.println("❌ Remote image error: " + url + " — " + img.getException());
                });
                iv.setImage(img);

            } else {
                // Local file — convert file:// URI or plain path → File → FileInputStream
                java.io.File file;
                if (url.startsWith("file:")) {
                    // Parse the file:// URI to get the actual file path
                    file = new java.io.File(java.net.URI.create(url));
                } else {
                    // Legacy absolute path (Windows: C:\... or Unix: /home/...)
                    file = new java.io.File(url);
                }

                System.out.println("DEBUG loading local file: " + file.getAbsolutePath() + " exists=" + file.exists());

                if (file.exists()) {
                    try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
                        iv.setImage(new Image(fis));
                    }
                } else {
                    System.err.println("❌ File not found: " + file.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            System.err.println("❌ loadImageIntoView error for url=" + url + " : " + e.getMessage());
        }
    }
}
