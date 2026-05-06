package com.example.pi_dev.Controllers.Booking.Admin;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import java.io.IOException;

public class AdminDashboardController {

    @FXML
    private TabPane tabPane;
    @FXML
    private Tab rentingsTab;
    @FXML
    private Tab reservationsTab;
    @FXML
    private Tab allPlacesTab;

    @FXML
    public void initialize() {
        loadTab(rentingsTab, "/com/example/pi_dev/booking/views/admin/AdminRentings.fxml");
        loadTab(reservationsTab, "/com/example/pi_dev/booking/views/admin/AdminReservations.fxml");
        loadTab(allPlacesTab, "/com/example/pi_dev/booking/views/admin/AdminAllPlaces.fxml");
    }

    private void loadTab(Tab tab, String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node content = loader.load();
            tab.setContent(content);
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR,
                    "Cannot load tab: " + fxmlPath + "\n" + e.getMessage(), ButtonType.OK)
                    .showAndWait();
        }
    }
}
