package com.example.pi_dev.Controllers.Booking;

import com.example.pi_dev.Utils.Booking.Session;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class AppController {

    @FXML
    private StackPane contentPane;

    @FXML
    private Button btnListYourPlace;
    @FXML
    private Button btnMyBookings;
    @FXML
    private Button btnMyPlaces;
    @FXML
    private Button btnReservations;
    @FXML
    private Button btnAdmin;

    @FXML
    public void initialize() {
        updateNavbarVisibility();
        loadView("/com/example/pi_dev/booking/views/front/PlaceBrowse.fxml");
    }

    private void updateNavbarVisibility() {
        boolean isAdmin = Session.isAdmin();
        boolean isHost = Session.isHost();

        // Admin strictly sees Admin button
        btnAdmin.setVisible(isAdmin);
        btnAdmin.setManaged(isAdmin);

        // Host strictly sees My Places and Reservations
        // Admins should NOT see these in the main booking navbar according to user request
        btnMyPlaces.setVisible(isHost);
        btnMyPlaces.setManaged(isHost);

        btnReservations.setVisible(isHost);
        btnReservations.setManaged(isHost);

        // All logged-in users can see these
        btnListYourPlace.setVisible(true);
        btnMyBookings.setVisible(true);
    }

    @FXML
    private void handleHome() {
        loadView("/com/example/pi_dev/booking/views/front/PlaceBrowse.fxml");
    }

    @FXML
    private void handleListYourPlace() {
        loadView("/com/example/pi_dev/booking/views/host/PlaceForm.fxml");
    }

    @FXML
    private void handleMyBookings() {
        loadView("/com/example/pi_dev/booking/views/front/MyBookings.fxml");
    }

    @FXML
    private void handleMyPlaces() {
        loadView("/com/example/pi_dev/booking/views/host/MyPlaces.fxml");
    }

    @FXML
    private void handleReservations() {
        loadView("/com/example/pi_dev/booking/views/host/HostReservations.fxml");
    }

    @FXML
    private void handleAdminDashboard() {
        loadView("/com/example/pi_dev/booking/views/admin/AdminDashboard.fxml");
    }

    public void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();
            contentPane.getChildren().setAll(view);
        } catch (IOException e) {
            throw new RuntimeException("Cannot load view: " + fxmlPath, e);
        }
    }

    public StackPane getContentPane() {
        return contentPane;
    }
}
