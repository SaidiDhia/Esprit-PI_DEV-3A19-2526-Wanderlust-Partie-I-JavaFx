package com.example.pi_dev.Controllers.Marketplace;

import com.example.pi_dev.Entities.Marketplace.DeliveryAddress;
import com.example.pi_dev.Entities.Marketplace.Facture;
import com.example.pi_dev.Entities.Marketplace.FactureProduct;
import com.example.pi_dev.Services.Marketplace.DeliveryAddressService;
import com.example.pi_dev.Services.Marketplace.FactureService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import com.example.pi_dev.Test.Marketplace.MainFx;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.Collectors;

import static com.example.pi_dev.Services.Marketplace.ProductService.CURRENT_USER_ID;

/**
 * Orders Management — seller sees all orders for their products.
 * Can confirm cash delivery or cancel any order.
 */
public class OrdersManagementController {

    @FXML private VBox      ordersContainer;
    @FXML private VBox      emptyState;
    @FXML private Label     statPending;
    @FXML private Label     statConfirmed;
    @FXML private Label     statCancelled;
    @FXML private ComboBox<String> statusFilter;

    private FactureService         factureService = new FactureService();
    private DeliveryAddressService daService      = new DeliveryAddressService();
    private SimpleDateFormat       sdf            = new SimpleDateFormat("dd/MM/yyyy  HH:mm");
    private List<Facture>          allOrders;

    @FXML
    public void initialize() {
        statusFilter.setItems(FXCollections.observableArrayList(
            "All Orders", "⏳ Pending", "✅ Confirmed", "❌ Cancelled"));
        statusFilter.setValue("All Orders");
        statusFilter.setOnAction(e -> applyFilter());
        loadOrders();
    }

    @FXML
    public void loadOrders() {
        allOrders = factureService.getFacturesByProductOwner(CURRENT_USER_ID);
        updateStats();
        applyFilter();
    }

    private void applyFilter() {
        String filter = statusFilter.getValue();
        List<Facture> filtered = allOrders.stream().filter(f -> {
            if (filter == null || filter.equals("All Orders")) return true;
            if (filter.contains("Pending"))   return "pending".equals(f.getDeliveryStatus());
            if (filter.contains("Confirmed")) return "confirmed".equals(f.getDeliveryStatus());
            if (filter.contains("Cancelled")) return "cancelled".equals(f.getDeliveryStatus());
            return true;
        }).collect(Collectors.toList());

        renderOrders(filtered);
    }

    private void updateStats() {
        long pending   = allOrders.stream().filter(f -> "pending".equals(f.getDeliveryStatus())).count();
        long confirmed = allOrders.stream().filter(f -> "confirmed".equals(f.getDeliveryStatus())).count();
        long cancelled = allOrders.stream().filter(f -> "cancelled".equals(f.getDeliveryStatus())).count();
        statPending.setText("⏳ Pending: "   + pending);
        statConfirmed.setText("✅ Confirmed: " + confirmed);
        statCancelled.setText("❌ Cancelled: " + cancelled);
    }

    private void renderOrders(List<Facture> orders) {
        ordersContainer.getChildren().clear();

        if (orders.isEmpty()) {
            emptyState.setVisible(true);
            emptyState.setManaged(true);
            ordersContainer.getChildren().add(emptyState);
            return;
        }

        emptyState.setVisible(false);
        emptyState.setManaged(false);

        for (Facture f : orders) {
            DeliveryAddress da       = daService.getByFactureId(f.getId());
            List<FactureProduct> fps = factureService.getFactureProducts(f.getId());
            ordersContainer.getChildren().add(buildOrderCard(f, da, fps));
        }
    }

    // ─── Build order card ─────────────────────────────────────────────────────

    private VBox buildOrderCard(Facture f, DeliveryAddress da, List<FactureProduct> fps) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(18));
        card.setStyle("-fx-background-color:white;-fx-background-radius:12;" +
            "-fx-border-color:" + borderColor(f.getDeliveryStatus()) + ";" +
            "-fx-border-radius:12;-fx-border-width:2;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),8,0,0,2);");

        // ── Header row ──────────────────────────────────────────────────────
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox orderInfo = new VBox(3);
        Label orderId = new Label("Order #" + f.getId());
        orderId.setStyle("-fx-font-weight:bold;-fx-font-size:15px;");
        Label orderDate = new Label("📅 " + sdf.format(f.getDate()));
        orderDate.setStyle("-fx-text-fill:#777;-fx-font-size:12px;");
        orderInfo.getChildren().addAll(orderId, orderDate);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Payment badge
        Label payBadge = new Label(
            "online".equals(f.getPaymentMethod()) ? "💳 Online" : "💵 Cash");
        payBadge.setStyle("-fx-background-color:" +
            ("online".equals(f.getPaymentMethod()) ? "#E3F2FD" : "#FFF8E1") + ";" +
            "-fx-text-fill:" +
            ("online".equals(f.getPaymentMethod()) ? "#1565C0" : "#E65100") + ";" +
            "-fx-background-radius:20;-fx-padding:4 12;-fx-font-weight:bold;-fx-font-size:12px;");

        // Status badge
        Label statusBadge = new Label(statusIcon(f.getDeliveryStatus()) + "  " +
            f.getDeliveryStatus().toUpperCase());
        statusBadge.setStyle("-fx-background-color:" + statusBg(f.getDeliveryStatus()) + ";" +
            "-fx-text-fill:" + statusFg(f.getDeliveryStatus()) + ";" +
            "-fx-background-radius:20;-fx-padding:4 14;-fx-font-weight:bold;-fx-font-size:12px;");

        // Total
        Label total = new Label(String.format("%.3f TND", f.getTotal()));
        total.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:#2E7D32;");

        header.getChildren().addAll(orderInfo, spacer, payBadge, statusBadge, total);

        Separator sep1 = new Separator();

        // ── Customer info row ────────────────────────────────────────────────
        HBox customerRow = new HBox(30);
        customerRow.setAlignment(Pos.CENTER_LEFT);

        if (da != null) {
            customerRow.getChildren().addAll(
                infoChip("👤", "Customer",  da.getFullName()),
                infoChip("📞", "Phone",     da.getPhone()),
                infoChip("🏠", "Address",   da.getAddress() + ", " + da.getCity() +
                    (da.getPostalCode().isEmpty() ? "" : " " + da.getPostalCode())),
                infoChip("📝", "Notes",     da.getNotes().isEmpty() ? "—" : da.getNotes())
            );
        } else {
            Label noAddr = new Label("No delivery address saved");
            noAddr.setStyle("-fx-text-fill:#AAA;-fx-font-size:12px;");
            customerRow.getChildren().add(noAddr);
        }

        Separator sep2 = new Separator();

        // ── Products row ─────────────────────────────────────────────────────
        HBox productsRow = new HBox(8);
        productsRow.setAlignment(Pos.CENTER_LEFT);
        Label prodLabel = new Label("🛍 Items:");
        prodLabel.setStyle("-fx-font-weight:bold;-fx-font-size:13px;");
        productsRow.getChildren().add(prodLabel);
        for (FactureProduct fp : fps) {
            Label chip = new Label(fp.getProductTitle() + " ×" + fp.getQuantity());
            chip.setStyle("-fx-background-color:#F5F5F5;-fx-background-radius:15;" +
                "-fx-border-color:#E0E0E0;-fx-border-radius:15;-fx-padding:4 10;-fx-font-size:12px;");
            productsRow.getChildren().add(chip);
        }

        // ── Action buttons ────────────────────────────────────────────────────
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        if ("pending".equals(f.getDeliveryStatus())) {
            if ("cash".equals(f.getPaymentMethod())) {
                Button confirmBtn = new Button("✅  Confirm Delivery");
                confirmBtn.setStyle("-fx-background-color:#2E7D32;-fx-text-fill:white;" +
                    "-fx-background-radius:8;-fx-padding:9 20;-fx-font-weight:bold;-fx-cursor:hand;");
                confirmBtn.setOnAction(e -> confirmDelivery(f.getId()));
                actions.getChildren().add(confirmBtn);
            }

            Button cancelBtn = new Button("❌  Cancel Order");
            cancelBtn.setStyle("-fx-background-color:#C62828;-fx-text-fill:white;" +
                "-fx-background-radius:8;-fx-padding:9 20;-fx-font-weight:bold;-fx-cursor:hand;");
            cancelBtn.setOnAction(e -> cancelOrder(f.getId(), f.getPaymentMethod()));
            actions.getChildren().add(cancelBtn);

        } else if ("confirmed".equals(f.getDeliveryStatus())) {
            Button cancelBtn = new Button("↩  Cancel & Restore Stock");
            cancelBtn.setStyle("-fx-background-color:#E53935;-fx-text-fill:white;" +
                "-fx-background-radius:8;-fx-padding:9 20;-fx-font-weight:bold;-fx-cursor:hand;");
            cancelBtn.setOnAction(e -> cancelOrder(f.getId(), f.getPaymentMethod()));
            actions.getChildren().add(cancelBtn);

        } else {
            Label done = new Label("Order cancelled — stock restored");
            done.setStyle("-fx-text-fill:#AAA;-fx-font-size:12px;-fx-font-style:italic;");
            actions.getChildren().add(done);
        }

        card.getChildren().addAll(header, sep1, customerRow, sep2, productsRow, actions);
        return card;
    }

    // ─── Actions ──────────────────────────────────────────────────────────────

    private void confirmDelivery(int factureId) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delivery");
        confirm.setHeaderText("Mark this order as delivered?");
        confirm.setContentText("Stock will be permanently deducted for this cash order.");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        if (factureService.confirmCashDelivery(factureId)) {
            showAlert(Alert.AlertType.INFORMATION, "Confirmed",
                "✅ Order #" + factureId + " marked as delivered.\nStock updated.");
            loadOrders();
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not confirm delivery.");
        }
    }

    private void cancelOrder(int factureId, String paymentMethod) {
        String msg = "cash".equals(paymentMethod)
            ? "This will release the reserved stock back to available quantity."
            : "This will restore the deducted stock. If payment was made online, process the refund separately.";

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Cancel Order");
        confirm.setHeaderText("Cancel order #" + factureId + "?");
        confirm.setContentText(msg);
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        if (factureService.cancelOrder(factureId)) {
            showAlert(Alert.AlertType.INFORMATION, "Cancelled",
                "❌ Order #" + factureId + " cancelled.\nStock restored.");
            loadOrders();
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not cancel order.");
        }
    }

    // ─── UI helpers ───────────────────────────────────────────────────────────

    private VBox infoChip(String icon, String label, String value) {
        VBox box = new VBox(2);
        Label l = new Label(icon + "  " + label);
        l.setStyle("-fx-text-fill:#999;-fx-font-size:11px;");
        Label v = new Label(value);
        v.setStyle("-fx-font-size:13px;-fx-font-weight:bold;");
        v.setWrapText(true);
        v.setMaxWidth(180);
        box.getChildren().addAll(l, v);
        return box;
    }

    private String statusIcon(String s) {
        return switch (s) {
            case "confirmed" -> "✅";
            case "cancelled" -> "❌";
            default          -> "⏳";
        };
    }

    private String statusBg(String s) {
        return switch (s) {
            case "confirmed" -> "#E8F5E9";
            case "cancelled" -> "#FFEBEE";
            default          -> "#FFF8E1";
        };
    }

    private String statusFg(String s) {
        return switch (s) {
            case "confirmed" -> "#2E7D32";
            case "cancelled" -> "#C62828";
            default          -> "#E65100";
        };
    }

    private String borderColor(String s) {
        return switch (s) {
            case "confirmed" -> "#A5D6A7";
            case "cancelled" -> "#EF9A9A";
            default          -> "#FFE082";
        };
    }

    @FXML private void goBack() { MainFx.setCenter("/com/example/pi_dev/marketplace/fxml/SellerHome.fxml"); }

    private void showAlert(Alert.AlertType t, String title, String msg) {
        Alert a = new Alert(t); a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}
