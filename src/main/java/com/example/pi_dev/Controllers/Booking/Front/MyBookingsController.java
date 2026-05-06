package com.example.pi_dev.Controllers.Booking.Front;

import com.example.pi_dev.Entities.Booking.Booking;
import com.example.pi_dev.Entities.Booking.Place;
import com.example.pi_dev.Services.Booking.BookingService;
import com.example.pi_dev.Services.Booking.PlaceService;
import com.example.pi_dev.Services.Booking.ReceiptService;
import com.example.pi_dev.Utils.Booking.Session;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class MyBookingsController {

    @FXML
    private FlowPane bookingsFlowPane;

    private final BookingService bookingService = new BookingService();
    private final PlaceService placeService = new PlaceService();
    private final ReceiptService receiptService = new ReceiptService();

    @FXML
    public void initialize() {
        loadData();
    }

    private void loadData() {
        bookingsFlowPane.getChildren().clear();
        List<BookingService.BookingView> views = bookingService.findByUserWithTitle(Session.currentUserId);
        for (BookingService.BookingView view : views) {
            bookingsFlowPane.getChildren().add(createBookingCard(view));
        }
    }

    private VBox createBookingCard(BookingService.BookingView bv) {
        VBox card = new VBox(12);
        card.setPrefWidth(320);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2); " +
                "-fx-border-color: #E5E7EB; -fx-border-radius: 12; -fx-border-width: 1;");
        card.setPadding(new Insets(16));

        // Image
        StackPane imageHolder = new StackPane();
        imageHolder.setPrefHeight(150);
        imageHolder.setStyle("-fx-background-color: #F3F4F6; -fx-background-radius: 8;");
        
        ImageView imageView = new ImageView();
        imageView.setFitWidth(288);
        imageView.setFitHeight(150);
        imageView.setPreserveRatio(true);
        
        Place place = placeService.getPlaceById(bv.booking.getPlaceId());
        if (place != null && place.getImageUrl() != null && !place.getImageUrl().isEmpty()) {
            loadImage(imageView, place.getImageUrl());
        }
        imageHolder.getChildren().add(imageView);

        // Content
        VBox content = new VBox(6);
        Label property = new Label(bv.placeTitle);
        property.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        property.setWrapText(true);

        Label dates = new Label("📅 " + bv.booking.getStartDate() + " → " + bv.booking.getEndDate());
        dates.setStyle("-fx-font-size: 13px; -fx-text-fill: #4B5563;");

        Label guests = new Label("👥 Guests: " + bv.booking.getGuestsCount());
        guests.setStyle("-fx-font-size: 13px; -fx-text-fill: #4B5563;");

        Label price = new Label(String.format("Total: $%.2f", bv.booking.getTotalPrice()));
        price.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #059669;");

        // Status Badge
        Label statusBadge = new Label(bv.booking.getStatus().name());
        String badgeColor = switch (bv.booking.getStatus()) {
            case CONFIRMED -> "#DCFCE7";
            case REJECTED, CANCELLED -> "#FEE2E2";
            case PENDING -> "#FEF9C3";
            default -> "#F3F4F6";
        };
        String textColor = switch (bv.booking.getStatus()) {
            case CONFIRMED -> "#166534";
            case REJECTED, CANCELLED -> "#991B1B";
            case PENDING -> "#854D0E";
            default -> "#374151";
        };
        statusBadge.setStyle("-fx-background-color: " + badgeColor + "; -fx-text-fill: " + textColor + "; " +
                "-fx-padding: 4 8; -fx-background-radius: 4; -fx-font-size: 11px; -fx-font-weight: bold;");

        content.getChildren().addAll(property, dates, guests, price, statusBadge);

        // Actions
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button receiptBtn = new Button("Receipt");
        receiptBtn.setStyle("-fx-background-color: #0EA5E9; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-size: 11px; -fx-cursor: hand;");
        receiptBtn.setOnAction(e -> handleDownloadReceipt(bv));

        Booking.Status status = bv.booking.getStatus();
        boolean isPending = status == Booking.Status.PENDING;
        boolean isConfirmed = status == Booking.Status.CONFIRMED;

        Button editBtn = new Button("Edit");
        editBtn.setStyle("-fx-background-color: #2563EB; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-size: 11px; -fx-cursor: hand;");
        editBtn.setDisable(!isPending);
        editBtn.setOnAction(e -> handleEdit(bv));

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #F59E0B; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-size: 11px; -fx-cursor: hand;");
        cancelBtn.setVisible(isPending || isConfirmed);
        cancelBtn.setManaged(isPending || isConfirmed);
        cancelBtn.setOnAction(e -> handleCancel(bv));

        actions.getChildren().addAll(receiptBtn, editBtn, cancelBtn);

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

    private void handleDownloadReceipt(BookingService.BookingView bv) {
        try {
            Place place = placeService.getPlaceById(bv.booking.getPlaceId());
            Path receiptFile = receiptService.generateReceipt(bv.booking, place);
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(receiptFile.toFile().toURI());
            } else {
                new Alert(Alert.AlertType.INFORMATION,
                        "Receipt saved:\n" + receiptFile.toAbsolutePath(), ButtonType.OK).showAndWait();
            }
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Could not generate receipt: " + e.getMessage(), ButtonType.OK)
                    .showAndWait();
        }
    }

    private void handleEdit(BookingService.BookingView bv) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pi_dev/booking/views/front/BookingDialog.fxml"));
            Node root = loader.load();
            BookingDialogController ctrl = loader.getController();
            Place place = placeService.getPlaceById(bv.booking.getPlaceId());
            ctrl.setEditingBooking(bv.booking, place);

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Edit Booking");
            dialog.setScene(new Scene((javafx.scene.Parent) root));
            dialog.showAndWait();
            loadData();
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Cannot open edit dialog: " + e.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    private void handleCancel(BookingService.BookingView bv) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Cancel this booking?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                bookingService.updateStatus(bv.booking.getId(), Booking.Status.CANCELLED);
                loadData();
            }
        });
    }

    private void handleDelete(BookingService.BookingView bv) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete this booking?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                bookingService.supprimerBooking(bv.booking.getId());
                loadData();
            }
        });
    }
}
