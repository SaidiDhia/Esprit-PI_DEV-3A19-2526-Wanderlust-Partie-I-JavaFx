package com.example.pi_dev.Controllers.Booking.Host;

import com.example.pi_dev.Entities.Booking.GeoPoint;
import com.example.pi_dev.Entities.Booking.Place;
import com.example.pi_dev.Services.Booking.GeoService;
import com.example.pi_dev.Services.Booking.PlaceService;
import com.example.pi_dev.Utils.Booking.Session;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.stage.FileChooser;

public class PlaceFormController {

    @FXML
    private TextField titleField;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private TextField priceField;
    @FXML
    private TextField capacityField;
    @FXML
    private TextField maxGuestsField;
    @FXML
    private TextField addressField;
    @FXML
    private TextField cityField;
    @FXML
    private TextField categoryField;
    @FXML
    private TextField imageUrlField;
    @FXML
    private Label errorLabel;
    @FXML
    private Label successLabel;
    @FXML
    private Button submitBtn;
    @FXML
    private Label geoStatusLabel;
    @FXML
    private FlowPane photosPreview;
    @FXML
    private Label photoCountLabel;

    private Place editingPlace;
    private Double pendingLat;
    private Double pendingLng;

    private final PlaceService placeService = new PlaceService();
    private final GeoService geoService = new GeoService();

    @FXML
    public void initialize() {
        if (errorLabel != null)
            errorLabel.setText("");
        if (successLabel != null)
            successLabel.setText("");
        if (geoStatusLabel != null)
            geoStatusLabel.setText("");
    }

    // Call this to pre-fill form for editing
    public void setEditingPlace(Place p) {
        this.editingPlace = p;
        this.pendingLat = p.getLat();
        this.pendingLng = p.getLng();
        titleField.setText(p.getTitle());
        descriptionArea.setText(p.getDescription());
        priceField.setText(String.valueOf(p.getPricePerDay()));
        capacityField.setText(String.valueOf(p.getCapacity()));
        maxGuestsField.setText(String.valueOf(p.getMaxGuests()));
        addressField.setText(p.getAddress());
        cityField.setText(p.getCity());
        categoryField.setText(p.getCategory());
        if (imageUrlField != null) {
            imageUrlField.setText(p.getImageUrl() != null ? p.getImageUrl() : "");
        }
        if (submitBtn != null)
            submitBtn.setText("Update Listing");
        if (geoStatusLabel != null && p.getLat() != null) {
            geoStatusLabel.setText("✅ Adresse validée (lat=" + p.getLat() + ")");
            geoStatusLabel.setStyle("-fx-text-fill: #16A34A;");
        }
    }

    @FXML
    private void handleValidateAddress() {
        if (geoStatusLabel != null) {
            geoStatusLabel.setText("⏳ Geocoding...");
            geoStatusLabel.setStyle("-fx-text-fill: #6B7280;");
        }

        String address = addressField.getText().trim();
        String city = cityField.getText().trim();

        if (address.isEmpty() || city.isEmpty()) {
            if (geoStatusLabel != null) {
                geoStatusLabel.setText("❌ Please fill Address and City first.");
                geoStatusLabel.setStyle("-fx-text-fill: #DC2626;");
            }
            return;
        }

        // Run geocoding in background thread to avoid blocking UI
        new Thread(() -> {
            GeoPoint point = geoService.geocode(address, city);
            Platform.runLater(() -> {
                if (point != null) {
                    pendingLat = point.getLat();
                    pendingLng = point.getLng();
                    if (geoStatusLabel != null) {
                        geoStatusLabel.setText("✅ Adresse validée");
                        geoStatusLabel.setStyle("-fx-text-fill: #16A34A;");
                    }
                } else {
                    pendingLat = null;
                    pendingLng = null;
                    if (geoStatusLabel != null) {
                        geoStatusLabel.setText("❌ Adresse introuvable");
                        geoStatusLabel.setStyle("-fx-text-fill: #DC2626;");
                    }
                }
            });
        }).start();
    }

    @FXML
    private void handleAddPhotos() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        java.util.List<java.io.File> files = chooser.showOpenMultipleDialog(null);
        if (files != null && !files.isEmpty()) {
            if (photoCountLabel != null) {
                photoCountLabel.setText(files.size() + " photo(s) selected");
            }
            if (photosPreview != null) {
                photosPreview.getChildren().clear();
                for (java.io.File f : files) {
                    ImageView iv = new ImageView(new Image(f.toURI().toString(), 120, 90, true, true));
                    iv.setFitWidth(120);
                    iv.setFitHeight(90);
                    photosPreview.getChildren().add(iv);
                }
            }
            if (!files.isEmpty() && imageUrlField != null) {
                imageUrlField.setText(files.get(0).toURI().toString());
            }
        }
    }

    @FXML
    private void handleSubmit() {
        if (errorLabel != null)
            errorLabel.setText("");
        if (successLabel != null)
            successLabel.setText("");

        String title = titleField.getText().trim();
        String desc = descriptionArea.getText().trim();
        String priceStr = priceField.getText().trim();
        String capStr = capacityField.getText().trim();
        String maxGStr = maxGuestsField.getText().trim();
        String address = addressField.getText().trim();
        String city = cityField.getText().trim();
        String category = categoryField.getText().trim();
        String imageUrl = imageUrlField != null ? imageUrlField.getText().trim() : "";

        if (title.isEmpty() || desc.isEmpty() || priceStr.isEmpty() || capStr.isEmpty()
                || maxGStr.isEmpty() || address.isEmpty() || city.isEmpty() || category.isEmpty()) {
            if (errorLabel != null)
                errorLabel.setText("All fields are required.");
            return;
        }

        double price;
        int capacity, maxGuests;
        try {
            price = Double.parseDouble(priceStr);
            capacity = Integer.parseInt(capStr);
            maxGuests = Integer.parseInt(maxGStr);
        } catch (NumberFormatException e) {
            if (errorLabel != null)
                errorLabel.setText("Price, Capacity and Max Guests must be numbers.");
            return;
        }

        if (editingPlace != null) {
            // Update mode
            editingPlace.setTitle(title);
            editingPlace.setDescription(desc);
            editingPlace.setPricePerDay(price);
            editingPlace.setCapacity(capacity);
            editingPlace.setMaxGuests(maxGuests);
            editingPlace.setAddress(address);
            editingPlace.setCity(city);
            editingPlace.setCategory(category);
            editingPlace.setImageUrl(imageUrl);
            editingPlace.setLat(pendingLat);
            editingPlace.setLng(pendingLng);
            placeService.modifierPlace(editingPlace);
            if (successLabel != null)
                successLabel.setText("Place updated successfully!");
        } else {
            // Create mode
            Place p = new Place();
            p.setHostId(Session.currentUserId);
            p.setTitle(title);
            p.setDescription(desc);
            p.setPricePerDay(price);
            p.setCapacity(capacity);
            p.setMaxGuests(maxGuests);
            p.setAddress(address);
            p.setCity(city);
            p.setCategory(category);
            p.setImageUrl(imageUrl);
            p.setStatus(Place.Status.PENDING);
            p.setLat(pendingLat);
            p.setLng(pendingLng);
            placeService.ajouterPlace(p);
            if (successLabel != null)
                successLabel.setText("Place submitted for approval!");
            clearForm();
        }
    }

    @FXML
    private void handleCancel() {
        clearForm();
        editingPlace = null;
        pendingLat = null;
        pendingLng = null;
    }

    private void clearForm() {
        titleField.clear();
        descriptionArea.clear();
        priceField.clear();
        capacityField.clear();
        maxGuestsField.clear();
        addressField.clear();
        cityField.clear();
        categoryField.clear();
        if (imageUrlField != null) {
            imageUrlField.clear();
        }
        if (errorLabel != null)
            errorLabel.setText("");
        if (successLabel != null)
            successLabel.setText("");
        if (geoStatusLabel != null)
            geoStatusLabel.setText("");
        pendingLat = null;
        pendingLng = null;
    }
}
