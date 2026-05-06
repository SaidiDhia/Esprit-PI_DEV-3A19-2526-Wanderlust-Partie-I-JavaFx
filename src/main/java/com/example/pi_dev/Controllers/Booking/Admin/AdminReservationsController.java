package com.example.pi_dev.Controllers.Booking.Admin;

import com.example.pi_dev.Entities.Booking.Booking;
import com.example.pi_dev.Services.Booking.BookingService;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;
import java.util.Optional;

public class AdminReservationsController {

    @FXML
    private FlowPane bookingsFlowPane;

    private final BookingService bookingService = new BookingService();

    @FXML
    public void initialize() {
        loadData();
    }

    private void loadData() {
        bookingsFlowPane.getChildren().clear();
        List<BookingService.BookingView> views = bookingService.findAllWithTitle();
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

        Booking.Status status = bv.booking.getStatus();
        boolean isPending = status == Booking.Status.PENDING;
        boolean isConfirmed = status == Booking.Status.CONFIRMED;

        Button confirmBtn = new Button("Confirm");
        confirmBtn.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-size: 12px; -fx-cursor: hand;");
        confirmBtn.setOnAction(e -> {
            bookingService.updateStatus(bv.booking.getId(), Booking.Status.CONFIRMED);
            loadData();
        });
        confirmBtn.setVisible(isPending);
        confirmBtn.setManaged(isPending);

        Button rejectBtn = new Button("Reject");
        rejectBtn.setStyle("-fx-background-color: #DC2626; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-size: 12px; -fx-cursor: hand;");
        rejectBtn.setOnAction(e -> {
            bookingService.updateStatus(bv.booking.getId(), Booking.Status.REJECTED);
            loadData();
        });
        rejectBtn.setVisible(isPending);
        rejectBtn.setManaged(isPending);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #F59E0B; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-size: 12px; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> handleAdminCancel(bv));
        boolean canAdminCancel = isPending || isConfirmed;
        cancelBtn.setVisible(canAdminCancel);
        cancelBtn.setManaged(canAdminCancel);

        actions.getChildren().addAll(confirmBtn, rejectBtn, cancelBtn);

        card.getChildren().addAll(content, actions);
        return card;
    }

    private void handleAdminCancel(BookingService.BookingView bv) {
        TextInputDialog reasonDialog = new TextInputDialog();
        reasonDialog.setTitle("Admin Cancellation");
        reasonDialog.setHeaderText("Cancel reservation #" + bv.booking.getId());
        reasonDialog.setContentText("Reason (optional):");

        Optional<String> result = reasonDialog.showAndWait();
        if (result.isPresent()) {
            String reason = result.get().trim().isEmpty() ? null : result.get().trim();
            bookingService.cancelByAdmin(bv.booking.getId(), reason);
            loadData();
        }
    }
}
