package com.example.pi_dev.Controllers.Marketplace;

import com.example.pi_dev.Entities.Marketplace.Facture;
import com.example.pi_dev.Entities.Marketplace.FactureProduct;
import com.example.pi_dev.Services.Marketplace.FactureService;
import com.example.pi_dev.Utils.Marketplace.QRCodeGenerator;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;


import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

// iText imports
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.kernel.colors.ColorConstants;
import com.example.pi_dev.Test.Marketplace.MainFx;

public class FactureDetailsController {

    @FXML private Label titleLabel;
    @FXML private Label invoiceNumberLabel;
    @FXML private Label invoiceDateLabel;
    @FXML private Label statusLabel;
    @FXML private Label paymentLabel;
    @FXML private VBox productsContainer;
    @FXML private Label totalLabel;

    private FactureService factureService = new FactureService();
    private Facture currentFacture;
    private List<FactureProduct> products;

    /**
     * Load facture details
     */
    public void loadFactureDetails(int factureId) {
        currentFacture = factureService.getFactureById(factureId);
        products = factureService.getFactureProducts(factureId);

        if (currentFacture == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Invoice not found!");
            return;
        }

        displayFactureInfo();
        displayProducts();
    }

    /**
     * Display facture header information
     */
    private void displayFactureInfo() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy - HH:mm");

        invoiceNumberLabel.setText("Invoice #" + currentFacture.getId());
        invoiceDateLabel.setText("Date: " + sdf.format(currentFacture.getDate()));
        String status = currentFacture.getDeliveryStatus();
        switch (status) {
            case "confirmed" -> { statusLabel.setText("✅ Confirmed"); statusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;"); }
            case "cancelled" -> { statusLabel.setText("❌ Cancelled"); statusLabel.setStyle("-fx-text-fill: #F44336; -fx-font-weight: bold;"); }
            default          -> { statusLabel.setText("⏳ Pending");   statusLabel.setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold;"); }
        }
        //statusLabel.setText("✓ COMPLETED");
        String method = currentFacture.getPaymentMethod();
        paymentLabel.setText("cash".equals(method) ? "💵 Cash on Delivery" : "💳 Credit Card");
        //paymentLabel.setText("Credit Card");
        totalLabel.setText(String.format("%.2f DT", currentFacture.getTotal()));
    }

    /**
     * Display products list
     */
    private void displayProducts() {
        productsContainer.getChildren().clear();

        if (products.isEmpty()) {
            Label noProducts = new Label("No products in this invoice");
            noProducts.setStyle("-fx-text-fill: #999; -fx-font-size: 14px;");
            productsContainer.getChildren().add(noProducts);
            return;
        }

        for (FactureProduct product : products) {
            HBox productCard = createProductCard(product);
            productsContainer.getChildren().add(productCard);
        }
    }

    /**
     * Create product card
     */
    private HBox createProductCard(FactureProduct product) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: #F9F9F9; " +
                "-fx-background-radius: 10; " +
                "-fx-border-color: #E0E0E0; " +
                "-fx-border-radius: 10;");

        // Product Image
        ImageView imageView = new ImageView();
        imageView.setFitWidth(60);
        imageView.setFitHeight(60);
        imageView.setPreserveRatio(true);
        imageView.setStyle("-fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");

        String imagePath = product.getProductImage();
        Image imageToShow;

        try {
            if (imagePath != null && !imagePath.isEmpty()) {
                imageToShow = new Image(imagePath, 60, 60, true, true, true);
                if (imageToShow.isError()) {
                    imageToShow = new Image("https://dummyimage.com/60x60/cccccc/666666&text=No+Photo", 60, 60, true, true, true);
                }
            } else {
                imageToShow = new Image("https://dummyimage.com/60x60/cccccc/666666&text=No+Photo", 60, 60, true, true, true);
            }
        } catch (Exception e) {
            System.err.println("Error loading image: " + e.getMessage());
            imageToShow = new Image("https://dummyimage.com/60x60/cccccc/666666&text=No+Photo", 60, 60, true, true, true);
        }

        imageView.setImage(imageToShow);

        // Product Info
        VBox infoBox = new VBox(5);
        infoBox.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(product.getProductTitle());
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        infoBox.getChildren().add(titleLabel);

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Quantity
        VBox qtyBox = new VBox(3);
        qtyBox.setAlignment(Pos.CENTER);
        qtyBox.setPadding(new Insets(5, 15, 5, 15));
        qtyBox.setStyle("-fx-background-color: white; -fx-background-radius: 8;");

        Label qtyLabel = new Label("x" + product.getQuantity());
        qtyLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        Label qtyText = new Label("Quantity");
        qtyText.setStyle("-fx-text-fill: #999; -fx-font-size: 10px;");

        qtyBox.getChildren().addAll(qtyLabel, qtyText);

        // Unit Price
        VBox priceBox = new VBox(3);
        priceBox.setAlignment(Pos.CENTER);

        Label priceLabel = new Label(String.format("%.2f DT", product.getPrice()));
        priceLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label priceText = new Label("Unit Price");
        priceText.setStyle("-fx-text-fill: #999; -fx-font-size: 10px;");

        priceBox.getChildren().addAll(priceLabel, priceText);

        // Subtotal
        VBox subtotalBox = new VBox(3);
        subtotalBox.setAlignment(Pos.CENTER_RIGHT);

        float subtotal = product.getPrice() * product.getQuantity();
        Label subtotalLabel = new Label(String.format("%.2f DT", subtotal));
        subtotalLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #2E7D32;");

        Label subtotalText = new Label("Subtotal");
        subtotalText.setStyle("-fx-text-fill: #999; -fx-font-size: 10px;");

        subtotalBox.getChildren().addAll(subtotalLabel, subtotalText);

        card.getChildren().addAll(imageView, infoBox, spacer, qtyBox, priceBox, subtotalBox);

        return card;
    }

    /**
     * Download PDF invoice
     */
    @FXML
    private void downloadPDF() {
        try {
            // File chooser
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Invoice PDF");
            fileChooser.setInitialFileName("Invoice_" + currentFacture.getId() + ".pdf");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
            );

            Stage stage = (Stage) productsContainer.getScene().getWindow();
            File file = fileChooser.showSaveDialog(stage);

            if (file != null) {
                generatePDF(file);
                showAlert(Alert.AlertType.INFORMATION, "Success", 
                         "Invoice downloaded successfully!\n\nLocation: " + file.getAbsolutePath());
            }

        } catch (Exception e) {
            System.err.println("Error downloading PDF: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Could not generate PDF: " + e.getMessage());
        }
    }

    /**
     * Generate PDF file (Simple text-based PDF)
     * For production, use libraries like iText or Apache PDFBox
     */
    private void generatePDF(File file) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        PdfWriter writer = new PdfWriter(file.getAbsolutePath());
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        // Title
        document.add(new Paragraph("WONDERLUST.COM")
                .setTextAlignment(TextAlignment.CENTER).setFontSize(20).setBold().setMarginBottom(5));
        document.add(new Paragraph("INVOICE")
                .setTextAlignment(TextAlignment.CENTER).setFontSize(16).setBold().setMarginBottom(20));

        // Invoice Info
        document.add(new Paragraph("Invoice Number: #" + currentFacture.getId()).setBold());
        document.add(new Paragraph("Date: " + sdf.format(currentFacture.getDate())));

        String status = currentFacture.getDeliveryStatus();
        document.add(new Paragraph("Status: " + status.toUpperCase())
                .setFontColor("confirmed".equals(status) ? ColorConstants.GREEN
                        : "cancelled".equals(status) ? ColorConstants.RED
                        : ColorConstants.ORANGE));

        String method = currentFacture.getPaymentMethod();



// Payment method line
        document.add(new Paragraph("Payment Method: " + ("cash".equals(method) ? "Cash on Delivery" : "Credit Card (Stripe)")));
        document.add(new Paragraph("\n"));

// QR code centered below invoice info
        try {
            String qrContent = "WonderLust Invoice\n" +
                    "Order #" + currentFacture.getId() + "\n" +
                    "Date: " + sdf.format(currentFacture.getDate()) + "\n" +
                    "Total: " + String.format("%.2f DT", currentFacture.getTotal()) + "\n" +
                    "Status: " + status + "\n" +
                    "Payment: " + method;

            byte[] qrBytes = QRCodeGenerator.generateQRCode(qrContent, 200, 200);
            com.itextpdf.io.image.ImageData imageData = com.itextpdf.io.image.ImageDataFactory.create(qrBytes);
            com.itextpdf.layout.element.Image qrImage = new com.itextpdf.layout.element.Image(imageData);
            qrImage.setWidth(120).setHeight(120);
            qrImage.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
            document.add(qrImage);
            document.add(new Paragraph("Scan to verify your order")
                    .setFontSize(9)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER));
        } catch (Exception e) {
            System.err.println("QR generation failed: " + e.getMessage());
        }
        document.add(new Paragraph("\n"));

        // Products Table
        document.add(new Paragraph("PURCHASED ITEMS").setBold().setFontSize(14).setMarginBottom(10));

        Table table = new Table(new float[]{3, 1, 2, 2});
        table.setWidth(pdfDoc.getDefaultPageSize().getWidth() - 80);
        table.addHeaderCell("Product");
        table.addHeaderCell("Quantity");
        table.addHeaderCell("Unit Price");
        table.addHeaderCell("Subtotal");

        for (FactureProduct product : products) {
            table.addCell(product.getProductTitle());
            table.addCell(String.valueOf(product.getQuantity()));
            table.addCell(String.format("%.2f DT", product.getPrice()));
            table.addCell(String.format("%.2f DT", product.getPrice() * product.getQuantity()));
        }

        document.add(table);
        document.add(new Paragraph("\n"));

        document.add(new Paragraph("TOTAL: " + String.format("%.2f DT", currentFacture.getTotal()))
                .setBold().setFontSize(18)
                .setTextAlignment(TextAlignment.RIGHT)
                .setFontColor(ColorConstants.GREEN));

        document.add(new Paragraph("\n\n"));
        document.add(new Paragraph("Thank you for your purchase!")
                .setTextAlignment(TextAlignment.CENTER).setItalic().setFontColor(ColorConstants.GRAY));

        document.close();
    }


    /**
     * Go back to facture list
     */
    @FXML
    private void goBack() {
        MainFx.setCenter("/com/example/pi_dev/marketplace/fxml/FactureList.fxml");
    }

    /**
     * Show alert
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
