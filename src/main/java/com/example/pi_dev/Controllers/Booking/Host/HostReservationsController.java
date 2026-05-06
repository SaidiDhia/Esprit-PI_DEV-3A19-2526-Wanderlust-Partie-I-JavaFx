package com.example.pi_dev.Controllers.Booking.Host;

import com.example.pi_dev.Entities.Booking.Booking;
import com.example.pi_dev.Services.Booking.BookingService;
import com.example.pi_dev.Utils.Booking.Session;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

public class HostReservationsController {

    @FXML
    private FlowPane bookingsFlowPane;

    private final BookingService bookingService = new BookingService();

    @FXML
    public void initialize() {
        loadData();
    }

    private void loadData() {
        if (Session.currentUserId == null) return;
        bookingsFlowPane.getChildren().clear();
        List<BookingService.BookingView> views = bookingService.findByHostWithTitle(Session.currentUserId);
        for (BookingService.BookingView view : views) {
            bookingsFlowPane.getChildren().add(createBookingCard(view));
        }
    }

    private VBox createBookingCard(BookingService.BookingView bv) {
        VBox card = new VBox(12);
        card.setPrefWidth(300);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2); " +
                "-fx-border-color: #E5E7EB; -fx-border-radius: 12; -fx-border-width: 1;");
        card.setPadding(new Insets(16));

        VBox content = new VBox(6);
        Label property = new Label(bv.placeTitle);
        property.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        property.setWrapText(true);

        Label dates = new Label("📅 " + bv.booking.getStartDate() + " to " + bv.booking.getEndDate());
        dates.setStyle("-fx-font-size: 13px; -fx-text-fill: #4B5563;");

        Label guests = new Label("👥 Guests: " + bv.booking.getGuestsCount());
        guests.setStyle("-fx-font-size: 13px; -fx-text-fill: #4B5563;");

        Label price = new Label(String.format("Total: $%.2f", bv.booking.getTotalPrice()));
        price.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #059669;");

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

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        boolean isPending = bv.booking.getStatus() == Booking.Status.PENDING;

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

        actions.getChildren().addAll(confirmBtn, rejectBtn);
        card.getChildren().addAll(content, actions);
        return card;
    }
}
