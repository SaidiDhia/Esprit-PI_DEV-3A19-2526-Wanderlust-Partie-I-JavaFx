package com.example.pi_dev.Controllers.Booking.Front;

import com.example.pi_dev.Entities.Booking.Booking;
import com.example.pi_dev.Entities.Booking.Place;
import com.example.pi_dev.Services.Booking.BookingService;
import com.example.pi_dev.Services.Booking.ReceiptService;
import com.example.pi_dev.Utils.Booking.Session;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class BookingDialogController {

    @FXML
    private Label placeTitleLabel;
    @FXML
    private Label placeAddressLabel;
    @FXML
    private Label placeCapacityLabel;
    @FXML
    private Label placeMaxGuestsLabel;
    @FXML
    private Label placePriceLabel;
    @FXML
    private DatePicker startDatePicker;
    @FXML
    private DatePicker endDatePicker;
    @FXML
    private Spinner<Integer> guestsSpinner;
    @FXML
    private Label totalPriceLabel;
    @FXML
    private Label errorLabel;
    @FXML
    private ComboBox<String> statusCombo;

    private Place currentPlace;
    private Booking editingBooking;
    private final BookingService bookingService = new BookingService();
    private final ReceiptService receiptService = new ReceiptService();

    public void setPlace(Place place) {
        this.currentPlace = place;
        placeTitleLabel.setText(place.getTitle());
        placeAddressLabel.setText(place.getAddress() + ", " + place.getCity());
        placeCapacityLabel.setText(String.valueOf(place.getCapacity()));
        placeMaxGuestsLabel.setText(String.valueOf(place.getMaxGuests()));
        placePriceLabel.setText(String.format("$%.2f / day", place.getPricePerDay()));

        SpinnerValueFactory<Integer> factory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1,
                place.getMaxGuests(), 1);
        guestsSpinner.setValueFactory(factory);

        if (statusCombo != null) {
            statusCombo.getItems().setAll("PENDING");
            statusCombo.setValue("PENDING");
            statusCombo.setDisable(true);
        }

        startDatePicker.valueProperty().addListener((obs, o, n) -> recalcTotal());
        endDatePicker.valueProperty().addListener((obs, o, n) -> recalcTotal());
    }

    public void setEditingBooking(Booking b, Place place) {
        this.editingBooking = b;
        setPlace(place);
        startDatePicker.setValue(b.getStartDate());
        endDatePicker.setValue(b.getEndDate());
        if (guestsSpinner.getValueFactory() != null)
            guestsSpinner.getValueFactory().setValue(b.getGuestsCount());
        recalcTotal();
    }

    private void recalcTotal() {
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();
        if (start != null && end != null && end.isAfter(start) && currentPlace != null) {
            long nights = ChronoUnit.DAYS.between(start, end);
            totalPriceLabel.setText(String.format("$%.2f", nights * currentPlace.getPricePerDay()));
        } else {
            totalPriceLabel.setText("$0.00");
        }
    }

    @FXML
    private void handleConfirm() {
        errorLabel.setText("");

        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();
        int guests = guestsSpinner.getValue();

        if (start == null) {
            errorLabel.setText("Please select a start date.");
            return;
        }
        if (end == null) {
            errorLabel.setText("Please select an end date.");
            return;
        }
        if (!start.isBefore(end)) {
            errorLabel.setText("End date must be after start date.");
            return;
        }
        if (guests > currentPlace.getMaxGuests()) {
            errorLabel.setText("Guests exceed maximum allowed (" + currentPlace.getMaxGuests() + ").");
            return;
        }

        long nights = ChronoUnit.DAYS.between(start, end);
        double total = nights * currentPlace.getPricePerDay();

        if (editingBooking != null) {
            editingBooking.setStartDate(start);
            editingBooking.setEndDate(end);
            editingBooking.setGuestsCount(guests);
            editingBooking.setTotalPrice(total);
            try {
                bookingService.modifierBooking(editingBooking);
                offerReceipt(editingBooking);
                closeDialog();
            } catch (RuntimeException ex) {
                errorLabel.setText("These dates are not available.");
            }
        } else {
            boolean available = bookingService.isAvailable(currentPlace.getId(), start, end);
            if (!available) {
                errorLabel.setText("These dates are not available.");
                return;
            }

            Booking b = new Booking();
            b.setPlaceId(currentPlace.getId());
            b.setUserId(Session.currentUserId);
            b.setStartDate(start);
            b.setEndDate(end);
            b.setGuestsCount(guests);
            b.setTotalPrice(total);
            b.setStatus(Booking.Status.PENDING);
            bookingService.ajouterBooking(b);

            // Offer receipt download immediately after booking
            offerReceipt(b);
            closeDialog();
        }
    }

    /**
     * Asks the user if they want to download the booking receipt and opens it.
     */
    private void offerReceipt(Booking booking) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Booking saved! Would you like to download your receipt?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Download Receipt");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                downloadReceipt(booking);
            }
        });
    }

    private void downloadReceipt(Booking booking) {
        try {
            Path receiptFile = receiptService.generateReceipt(booking, currentPlace);
            // Open in default browser so user can print/save as PDF
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(receiptFile.toFile().toURI());
            } else {
                new Alert(Alert.AlertType.INFORMATION,
                        "Receipt saved to:\n" + receiptFile.toAbsolutePath(),
                        ButtonType.OK).showAndWait();
            }
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Could not generate receipt: " + e.getMessage(), ButtonType.OK)
                    .showAndWait();
        }
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) placeTitleLabel.getScene().getWindow();
        stage.close();
    }
}
