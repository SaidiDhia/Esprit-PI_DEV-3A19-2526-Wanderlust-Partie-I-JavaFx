package com.example.pi_dev.Controllers.Booking.Admin;

import com.example.pi_dev.Entities.Booking.Place;
import com.example.pi_dev.Services.Booking.PlaceService;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.io.File;
import java.util.List;

public class AdminAllPlacesController {

    @FXML
    private FlowPane placesFlowPane;

    private final PlaceService placeService = new PlaceService();

    @FXML
    public void initialize() {
        loadData();
    }

    private void loadData() {
        placesFlowPane.getChildren().clear();
        List<Place> all = placeService.afficherPlaces();
        for (Place p : all) {
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

        // Image Placeholder or Actual Image
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

        Button approveBtn = new Button("Approve");
        approveBtn.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-size: 12px; -fx-cursor: hand;");
        approveBtn.setOnAction(e -> {
            placeService.updateStatus(p.getId(), Place.Status.APPROVED);
            loadData();
        });
        approveBtn.setVisible(p.getStatus() != Place.Status.APPROVED);
        approveBtn.setManaged(p.getStatus() != Place.Status.APPROVED);

        Button denyBtn = new Button("Deny");
        denyBtn.setStyle("-fx-background-color: #d97706; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-size: 12px; -fx-cursor: hand;");
        denyBtn.setOnAction(e -> {
            placeService.updateStatus(p.getId(), Place.Status.DENIED);
            loadData();
        });
        denyBtn.setVisible(p.getStatus() != Place.Status.DENIED);
        denyBtn.setManaged(p.getStatus() != Place.Status.DENIED);

        Button deleteBtn = new Button("Delete");
        deleteBtn.setStyle("-fx-background-color: #DC2626; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-size: 12px; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete \"" + p.getTitle() + "\"?");
            if (confirm.showAndWait().orElse(null) == ButtonType.OK) {
                placeService.supprimerPlace(p.getId());
                loadData();
            }
        });

        actions.getChildren().addAll(approveBtn, denyBtn, deleteBtn);

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
}
