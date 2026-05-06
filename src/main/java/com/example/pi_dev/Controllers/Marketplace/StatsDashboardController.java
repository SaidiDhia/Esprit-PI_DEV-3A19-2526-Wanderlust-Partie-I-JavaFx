package com.example.pi_dev.Controllers.Marketplace;

import com.example.pi_dev.Services.Marketplace.FactureService;
import com.example.pi_dev.Services.Marketplace.ProductService;
import com.example.pi_dev.Entities.Marketplace.Facture;
import com.example.pi_dev.Entities.Marketplace.Product;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import com.example.pi_dev.Test.Marketplace.MainFx;

import java.text.SimpleDateFormat;
import java.util.*;

import static com.example.pi_dev.Services.Marketplace.ProductService.CURRENT_USER_ID;

public class StatsDashboardController {

    // ── Stats labels ──────────────────────────────────────────────────────────
    @FXML private Label totalRevenueLabel;
    @FXML private Label totalOrdersLabel;
    @FXML private Label pendingOrdersLabel;
    @FXML private Label confirmedOrdersLabel;
    @FXML private Label cancelledOrdersLabel;
    @FXML private Label totalProductsLabel;

    // ── Charts ────────────────────────────────────────────────────────────────
    @FXML private BarChart<String, Number> revenueChart;
    @FXML private PieChart statusPieChart;
    @FXML private BarChart<String, Number> topProductsChart;

    private FactureService factureService = new FactureService();
    private ProductService productService = new ProductService();

    @FXML
    public void initialize() {
        loadStats();
        loadRevenueChart();
        loadStatusPieChart();
        loadTopProductsChart();
    }

    // ── Load summary stats ────────────────────────────────────────────────────
    private void loadStats() {
        List<Facture> allOrders = factureService.getFacturesByProductOwner(CURRENT_USER_ID);

        double totalRevenue = allOrders.stream()
                .filter(f -> "confirmed".equals(f.getDeliveryStatus()))
                .mapToDouble(Facture::getTotal)
                .sum();

        long pending   = allOrders.stream().filter(f -> "pending".equals(f.getDeliveryStatus())).count();
        long confirmed = allOrders.stream().filter(f -> "confirmed".equals(f.getDeliveryStatus())).count();
        long cancelled = allOrders.stream().filter(f -> "cancelled".equals(f.getDeliveryStatus())).count();

        List<Product> myProducts = productService.getAllProducts(CURRENT_USER_ID);

        totalRevenueLabel.setText(String.format("%.2f TND", totalRevenue));
        totalOrdersLabel.setText(String.valueOf(allOrders.size()));
        pendingOrdersLabel.setText(String.valueOf(pending));
        confirmedOrdersLabel.setText(String.valueOf(confirmed));
        cancelledOrdersLabel.setText(String.valueOf(cancelled));
        totalProductsLabel.setText(String.valueOf(myProducts.size()));
    }

    // ── Revenue per day bar chart ─────────────────────────────────────────────
    private void loadRevenueChart() {
        List<Facture> allOrders = factureService.getFacturesByProductOwner(CURRENT_USER_ID);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM");

        // Group confirmed orders by date → sum revenue
        Map<String, Double> revenueByDay = new LinkedHashMap<>();
        allOrders.stream()
                .filter(f -> "confirmed".equals(f.getDeliveryStatus()))
                .sorted(Comparator.comparing(Facture::getDate))
                .forEach(f -> {
                    String day = sdf.format(f.getDate());
                    revenueByDay.merge(day, (double) f.getTotal(), Double::sum);
                });

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Revenue (TND)");
        revenueByDay.forEach((day, revenue) ->
                series.getData().add(new XYChart.Data<>(day, revenue)));

        revenueChart.getData().clear();
        revenueChart.getData().add(series);
        revenueChart.setTitle("Revenue per Day");
        revenueChart.getXAxis().setLabel("Date");
        revenueChart.getYAxis().setLabel("TND");
    }

    // ── Order status pie chart ────────────────────────────────────────────────
    private void loadStatusPieChart() {
        List<Facture> allOrders = factureService.getFacturesByProductOwner(CURRENT_USER_ID);

        long pending   = allOrders.stream().filter(f -> "pending".equals(f.getDeliveryStatus())).count();
        long confirmed = allOrders.stream().filter(f -> "confirmed".equals(f.getDeliveryStatus())).count();
        long cancelled = allOrders.stream().filter(f -> "cancelled".equals(f.getDeliveryStatus())).count();

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList(
                new PieChart.Data("⏳ Pending ("   + pending   + ")", pending),
                new PieChart.Data("✅ Confirmed (" + confirmed + ")", confirmed),
                new PieChart.Data("❌ Cancelled (" + cancelled + ")", cancelled)
        );

        statusPieChart.setData(pieData);
        statusPieChart.setTitle("Orders by Status");
    }

    // ── Top products bar chart ────────────────────────────────────────────────
    private void loadTopProductsChart() {
        List<Facture> allOrders = factureService.getFacturesByProductOwner(CURRENT_USER_ID);

        // Count how many times each product was sold (confirmed orders only)
        Map<String, Integer> productSales = new LinkedHashMap<>();
        allOrders.stream()
                .filter(f -> "confirmed".equals(f.getDeliveryStatus()))
                .forEach(f -> {
                    factureService.getFactureProducts(f.getId()).forEach(fp -> {
                        productSales.merge(fp.getProductTitle(), fp.getQuantity(), Integer::sum);
                    });
                });

        // Sort by quantity sold, take top 5
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Units Sold");
        productSales.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .forEach(e -> series.getData().add(
                        new XYChart.Data<>(
                                e.getKey().length() > 12 ? e.getKey().substring(0, 12) + "..." : e.getKey(),
                                e.getValue()
                        )
                ));

        topProductsChart.getData().clear();
        topProductsChart.getData().add(series);
        topProductsChart.setTitle("Top 5 Best-Selling Products");
        topProductsChart.getXAxis().setLabel("Product");
        topProductsChart.getYAxis().setLabel("Units Sold");
    }

    @FXML
    private void goBack() {
        MainFx.setCenter("/com/example/pi_dev/marketplace/fxml/SellerHome.fxml");
    }
}
