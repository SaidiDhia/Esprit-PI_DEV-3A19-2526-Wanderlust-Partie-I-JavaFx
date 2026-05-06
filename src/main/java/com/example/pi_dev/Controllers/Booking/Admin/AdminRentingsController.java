package com.example.pi_dev.Controllers.Booking.Admin;

import com.example.pi_dev.Entities.Booking.Place;
import com.example.pi_dev.Services.Booking.PlaceService;
import com.example.pi_dev.enums.RoleEnum;
import com.example.pi_dev.Entities.Users.User;
import com.example.pi_dev.Repositories.Users.UserRepository;
import com.example.pi_dev.Utils.Users.UserSession;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class AdminRentingsController {

    @FXML
    private FlowPane placesFlowPane;

    private final PlaceService placeService = new PlaceService();
    private final UserRepository userRepository = new UserRepository();

    @FXML
    public void initialize() {
        loadData();
    }

    private void loadData() {
        placesFlowPane.getChildren().clear();
        List<Place> pending = placeService.findPending();
        for (Place p : pending) {
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

        Label hostIdLabel = new Label("Host ID: " + p.getHostId());
        hostIdLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3AF;");

        content.getChildren().addAll(title, location, price, hostIdLabel);

        // Actions
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button approveBtn = new Button("Approve");
        approveBtn.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-size: 12px; -fx-cursor: hand;");
        approveBtn.setOnAction(e -> {
            placeService.updateStatus(p.getId(), Place.Status.APPROVED);

            // Update user role to HOST when place is approved, unless they are already ADMIN or HOST
            try {
                UUID hostId = UUID.fromString(p.getHostId());
                userRepository.findById(hostId).ifPresent(user -> {
                    if (user.getRole() != RoleEnum.ADMIN && user.getRole() != RoleEnum.HOST) {
                        try {
                            userRepository.updateRole(hostId, RoleEnum.HOST);

                            // If the user being approved is the current logged-in user,
                            // update their role in memory so the UI refreshes
                            User currentUser = UserSession.getInstance().getCurrentUser();
                            if (currentUser != null && currentUser.getUserId().equals(hostId)) {
                                currentUser.setRole(RoleEnum.HOST);
                                com.example.pi_dev.Utils.Booking.Session.update();
                            }
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }
                    }
                });
            } catch (SQLException | IllegalArgumentException ex) {
                ex.printStackTrace();
            }

            loadData();
        });

        Button denyBtn = new Button("Deny");
        denyBtn.setStyle("-fx-background-color: #DC2626; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-size: 12px; -fx-cursor: hand;");
        denyBtn.setOnAction(e -> {
            placeService.updateStatus(p.getId(), Place.Status.DENIED);
            loadData();
        });

        actions.getChildren().addAll(approveBtn, denyBtn);
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
