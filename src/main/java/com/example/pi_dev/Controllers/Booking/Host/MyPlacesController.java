package com.example.pi_dev.Controllers.Booking.Host;

import com.example.pi_dev.Entities.Booking.Place;
import com.example.pi_dev.Services.Booking.PlaceService;
import com.example.pi_dev.Utils.Booking.Session;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MyPlacesController {

    @FXML
    private FlowPane placesFlowPane;

    private final PlaceService placeService = new PlaceService();

    @FXML
    public void initialize() {
        loadData();
    }

    private void loadData() {
        if (Session.currentUserId == null) return;
        placesFlowPane.getChildren().clear();
        List<Place> places = placeService.findByHost(Session.currentUserId);
        for (Place p : places) {
            placesFlowPane.getChildren().add(createPlaceCard(p));
        }
    }

    private VBox createPlaceCard(Place p) {
        VBox card = new VBox(12);
        card.setPrefWidth(280);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2); " +
                "-fx-border-color: #E5E7EB; -fx-border-radius: 12; -fx-border-width: 1;");
        card.setPadding(new Insets(16));

        // Image
        StackPane imageHolder = new StackPane();
        imageHolder.setPrefHeight(150);
        imageHolder.setStyle("-fx-background-color: #F3F4F6; -fx-background-radius: 8;");
        
        ImageView imageView = new ImageView();
        imageView.setFitWidth(248);
        imageView.setFitHeight(150);
        imageView.setPreserveRatio(true);
        
        if (p.getImageUrl() != null && !p.getImageUrl().isEmpty()) {
            loadImage(imageView, p.getImageUrl());
        }
        imageHolder.getChildren().add(imageView);

        // Content
        VBox content = new VBox(4);
        Label title = new Label(p.getTitle());
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        title.setWrapText(true);

        Label location = new Label("📍 " + p.getCity());
        location.setStyle("-fx-font-size: 13px; -fx-text-fill: #6B7280;");

        Label price = new Label(String.format("$%.2f / night", p.getPricePerDay()));
        price.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2563EB;");

        // Status Badge
        Label statusBadge = new Label(p.getStatus().name());
        String badgeColor = switch (p.getStatus()) {
            case APPROVED -> "#DCFCE7";
            case DENIED -> "#FEE2E2";
            case PENDING -> "#FEF9C3";
            default -> "#F3F4F6";
        };
        String textColor = switch (p.getStatus()) {
            case APPROVED -> "#166534";
            case DENIED -> "#991B1B";
            case PENDING -> "#854D0E";
            default -> "#374151";
        };
        statusBadge.setStyle("-fx-background-color: " + badgeColor + "; -fx-text-fill: " + textColor + "; " +
                "-fx-padding: 4 8; -fx-background-radius: 4; -fx-font-size: 11px; -fx-font-weight: bold;");

        content.getChildren().addAll(title, location, price, statusBadge);

        // Actions
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button editBtn = new Button("Edit");
        editBtn.setStyle("-fx-background-color: #2563EB; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-size: 12px; -fx-cursor: hand;");
        editBtn.setOnAction(e -> handleEdit(p));

        Button deleteBtn = new Button("Delete");
        deleteBtn.setStyle("-fx-background-color: #DC2626; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-size: 12px; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> handleDelete(p));

        actions.getChildren().addAll(editBtn, deleteBtn);
        card.getChildren().addAll(imageHolder, content, actions);
        return card;
    }

    private void loadImage(ImageView imageView, String url) {
        try {
            if (url.startsWith("file:") || url.startsWith("http:") || url.startsWith("https:")) {
                imageView.setImage(new Image(url));
            } else {
                File file = new File(url);
                if (file.exists()) {
                    imageView.setImage(new Image(file.toURI().toString()));
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load image: " + url);
        }
    }

    private void handleEdit(Place place) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pi_dev/booking/views/host/PlaceForm.fxml"));
            Node view = loader.load();
            PlaceFormController ctrl = loader.getController();
            ctrl.setEditingPlace(place);

            StackPane parent = (StackPane) placesFlowPane.getScene().lookup("#contentPane");
            if (parent != null) {
                parent.getChildren().setAll(view);
            }
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Cannot open edit form: " + e.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    private void handleDelete(Place place) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete \"" + place.getTitle() + "\"?");
        if (confirm.showAndWait().orElse(null) == ButtonType.OK) {
            placeService.supprimerPlace(place.getId());
            loadData();
        }
    }
}
