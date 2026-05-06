package com.example.pi_dev.Controllers.Marketplace;

import com.example.pi_dev.Entities.Marketplace.Facture;
import com.example.pi_dev.Services.Marketplace.FactureService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import com.example.pi_dev.Test.Marketplace.MainFx;

import java.text.SimpleDateFormat;
import java.util.List;

import static com.example.pi_dev.Services.Marketplace.ProductService.CURRENT_USER_ID;

/**
 * Controller for Facture List Page
 */
public class FactureListController {

    @FXML private VBox facturesContainer;
    @FXML private VBox emptyState;

    private FactureService factureService = new FactureService();




    @FXML
    public void initialize() {
        loadFactures();
    }

    /**
     * Load all factures for current user
     */
    private void loadFactures() {
        facturesContainer.getChildren().clear();

        List<Facture> factures = factureService.getFacturesByUser(CURRENT_USER_ID);

        if (factures.isEmpty()) {
            emptyState.setVisible(true);
            emptyState.setManaged(true);
            facturesContainer.setVisible(false);
            return;
        }

        emptyState.setVisible(false);
        emptyState.setManaged(false);
        facturesContainer.setVisible(true);

        for (Facture facture : factures) {
            HBox factureCard = createFactureCard(facture);
            facturesContainer.getChildren().add(factureCard);
        }
    }

    /**
     * Create facture card
     */
    private HBox createFactureCard(Facture facture) {
        HBox card = new HBox(20);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: white; " +
                     "-fx-background-radius: 12; " +
                     "-fx-border-color: #E0E0E0; " +
                     "-fx-border-radius: 12; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 3);");

        // Icon
        Label icon = new Label("🧾");
        icon.setStyle("-fx-font-size: 36px;");

        // Facture Info
        VBox infoBox = new VBox(8);
        infoBox.setAlignment(Pos.CENTER_LEFT);

        Label invoiceLabel = new Label("Invoice #" + facture.getId());
        invoiceLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm");
        Label dateLabel = new Label("📅 " + sdf.format(facture.getDate()));
        dateLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 13px;");

        String status = facture.getDeliveryStatus();
        String statusText;
        String statusColor;

        switch (status) {
            case "confirmed" -> { statusText = "✅ Confirmed";  statusColor = "#4CAF50"; }
            case "cancelled" -> { statusText = "❌ Cancelled";  statusColor = "#F44336"; }
            default          -> { statusText = "⏳ Pending";    statusColor = "#FF9800"; }
        }

        Label statusLabel = new Label(statusText);
        statusLabel.setStyle("-fx-text-fill: " + statusColor + "; -fx-font-weight: bold; -fx-font-size: 12px;");
        //Label statusLabel = new Label("✓ Completed");
        //statusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold; -fx-font-size: 12px;");

        infoBox.getChildren().addAll(invoiceLabel, dateLabel, statusLabel);

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Total Amount
        VBox totalBox = new VBox(5);
        totalBox.setAlignment(Pos.CENTER_RIGHT);

        Label totalLabel = new Label(String.format("%.2f DT", facture.getTotal()));
        totalLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 20px; -fx-text-fill: #2E7D32;");

        Label totalText = new Label("Total");
        totalText.setStyle("-fx-text-fill: #999; -fx-font-size: 11px;");

        totalBox.getChildren().addAll(totalLabel, totalText);

        // Details Button
        Button detailsBtn = new Button("View Details");
        detailsBtn.getStyleClass().add("btn-details");
        detailsBtn.setStyle("-fx-background-color: linear-gradient(to right, #00796B, #43A047); " +
                           "-fx-text-fill: white; " +
                           "-fx-background-radius: 20; " +
                           "-fx-padding: 10 25; " +
                           "-fx-font-weight: bold; " +
                           "-fx-cursor: hand;");

        detailsBtn.setOnAction(e -> openFactureDetails(facture.getId()));

        card.getChildren().addAll(icon, infoBox, spacer, totalBox, detailsBtn);

        // Hover effect
        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-background-color: #F5F5F5; " +
                         "-fx-background-radius: 12; " +
                         "-fx-border-color: #00796B; " +
                         "-fx-border-width: 2; " +
                         "-fx-border-radius: 12; " +
                         "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 15, 0, 0, 5);");
        });

        card.setOnMouseExited(e -> {
            card.setStyle("-fx-background-color: white; " +
                         "-fx-background-radius: 12; " +
                         "-fx-border-color: #E0E0E0; " +
                         "-fx-border-radius: 12; " +
                         "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 3);");
        });

        return card;
    }

    /**
     * Open facture details page
     */
    private void openFactureDetails(int factureId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pi_dev/marketplace/fxml/FactureDetails.fxml"));
            Parent root = loader.load();

            FactureDetailsController controller = loader.getController();
            controller.loadFactureDetails(factureId);

            MainFx.setCenter(root); // <- swap center instead of new stage

        } catch (Exception e) {
            System.err.println("Error loading facture details: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Refresh factures list
     */
    @FXML
    private void refresh() {
        loadFactures();
    }

    /**
     * Go back to previous page
     */
    @FXML
    private void goBack() {
        MainFx.setCenter("/com/example/pi_dev/marketplace/fxml/BuyerHome.fxml");
    }


    /**
     * Navigate to products page
     */
    @FXML
    private void goToProducts() {
        goBack();
    }
}
