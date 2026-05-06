package com.example.pi_dev.Controllers.Marketplace;

import com.example.pi_dev.Entities.Marketplace.DeliveryAddress;
import com.example.pi_dev.Entities.Marketplace.FactureProduct;
import com.example.pi_dev.Services.Marketplace.CartService;
import com.example.pi_dev.Services.Marketplace.EmailService;
import com.example.pi_dev.Services.Marketplace.FactureService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import com.example.pi_dev.Test.Marketplace.MainFx;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static com.example.pi_dev.Services.Marketplace.ProductService.CURRENT_USER_ID;

public class LivraisonController {

    // ── Stripe config ─────────────────────────────────────────────────────────
    private static final String STRIPE_SECRET_KEY = "sk_test_51T3MZvE9gq7T0ndukMZbiahEcX3bPMTdKFDUYnjkdetZpGNRJlLrerMMKp5AAWFLspm7GoxL50tUh7WgTPNGHFOE00JpbjRV5t";
    private static final String STRIPE_BASE_URL   = "https://api.stripe.com/v1";
    private static final String SUCCESS_URL        = "https://example.com/success";
    private static final String CANCEL_URL         = "https://example.com/cancel";

    // ── FXML fields ───────────────────────────────────────────────────────────
    @FXML private Label       totalSummaryLabel;
    @FXML private Label       totalBottomLabel;

    @FXML private TextField   fullNameField;
    @FXML private TextField   phoneField;
    @FXML private TextField   addressField;
    @FXML private TextField   cityField;
    @FXML private TextField   postalField;
    @FXML private TextArea    notesField;
    @FXML private TextField   emailField;       // NEW — optional

    @FXML private Label       fullNameError;
    @FXML private Label       phoneError;
    @FXML private Label       addressError;
    @FXML private Label       cityError;

    @FXML private RadioButton cashRadio;
    @FXML private RadioButton onlineRadio;
    @FXML private HBox        cashCard;
    @FXML private HBox        onlineCard;

    // ── Services ──────────────────────────────────────────────────────────────
    private CartService    cartService    = new CartService();
    private EmailService   emailService   = new EmailService();
    private FactureService factureService = new FactureService();

    private double totalAmount = 0.0;

    // ── Init ──────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        totalAmount = cartService.calculateTotal(CURRENT_USER_ID);
        String fmt  = String.format("%.3f TND", totalAmount);
        totalSummaryLabel.setText(fmt);
        totalBottomLabel.setText(fmt);
        highlightCash();

        fullNameField.focusedProperty().addListener((o, was, now) -> { if (!now) validateFullName(); });
        phoneField.focusedProperty().addListener((o, was, now)    -> { if (!now) validatePhone(); });
        addressField.focusedProperty().addListener((o, was, now)  -> { if (!now) validateAddress(); });
        cityField.focusedProperty().addListener((o, was, now)     -> { if (!now) validateCity(); });
    }

    // ── Payment selection ─────────────────────────────────────────────────────

    @FXML private void selectCash()   { cashRadio.setSelected(true);   highlightCash(); }
    @FXML private void selectOnline() { onlineRadio.setSelected(true); highlightOnline(); }

    private void highlightCash() {
        cashCard.setStyle("-fx-background-color:#F9FBE7;-fx-background-radius:10;-fx-border-color:#CDDC39;-fx-border-radius:10;-fx-border-width:2;-fx-padding:15;-fx-cursor:hand;");
        onlineCard.setStyle("-fx-background-color:#F5F5F5;-fx-background-radius:10;-fx-border-color:#E0E0E0;-fx-border-radius:10;-fx-border-width:2;-fx-padding:15;-fx-cursor:hand;");
    }

    private void highlightOnline() {
        onlineCard.setStyle("-fx-background-color:#E8F5E9;-fx-background-radius:10;-fx-border-color:#43A047;-fx-border-radius:10;-fx-border-width:2;-fx-padding:15;-fx-cursor:hand;");
        cashCard.setStyle("-fx-background-color:#F5F5F5;-fx-background-radius:10;-fx-border-color:#E0E0E0;-fx-border-radius:10;-fx-border-width:2;-fx-padding:15;-fx-cursor:hand;");
    }

    // ── Confirm order button ──────────────────────────────────────────────────

    @FXML
    private void confirmOrder() {
        boolean ok = validateFullName() & validatePhone() & validateAddress() & validateCity();
        if (!ok) {
            showAlert(Alert.AlertType.WARNING, "Form Error",
                    "Please fix the highlighted fields before continuing.");
            return;
        }

        DeliveryAddress da = buildDeliveryAddress();

        if (cashRadio.isSelected()) {
            handleCashOrder(da);
        } else {
            handleStripeOrder(da);
        }
    }

    // ─── CASH flow ────────────────────────────────────────────────────────────

    private void handleCashOrder(DeliveryAddress da) {
        boolean success = cartService.buyCartCash(CURRENT_USER_ID, da);
        if (!success) {
            showAlert(Alert.AlertType.ERROR, "Order Failed",
                    "Could not place order. Items may be out of stock.");
            return;
        }

        // Get the most recent facture for this user to retrieve its ID and products for the email
        var factures = factureService.getFacturesByUser(CURRENT_USER_ID);
        if (!factures.isEmpty()) {
            var latestFacture = factures.get(0); // most recent first
            int factureId     = latestFacture.getId();
            List<FactureProduct> items = factureService.getFactureProducts(factureId);

            // Send email in background — only if user provided one
            emailService.sendCashOrderConfirmation(
                    da.getEmail(), da, items, latestFacture.getTotal(), factureId
            );
        }

        showAlert(Alert.AlertType.INFORMATION, "Order Placed! 🎉",
                "✅ Your order has been placed!\n\n" +
                        "📦 Delivery to: " + da.getAddress() + ", " + da.getCity() + "\n" +
                        "💵 Payment: Cash on Delivery\n\n" +
                        (!da.getEmail().isEmpty()
                                ? "📧 Confirmation email sent to: " + da.getEmail()
                                : "ℹ️  No email provided — you can track your order in the app."));

        MainFx.setCenter("/com/example/pi_dev/marketplace/fxml/FactureList.fxml");
    }

    // ─── STRIPE flow — open WebView directly in the same window ──────────────

    private void handleStripeOrder(DeliveryAddress da) {
        // Show loading state on the button
        Button confirmBtn = getConfirmButton();
        if (confirmBtn != null) {
            confirmBtn.setDisable(true);
            confirmBtn.setText("⏳  Connecting to Stripe...");
        }

        new Thread(() -> {
            try {
                long amountCents = Math.round(totalAmount * 100);
                StripeSession session = createStripeSession(
                        amountCents,
                        da.getEmail().isEmpty() ? "customer@wonderlust.com" : da.getEmail(),
                        da.getFullName()
                );

                Platform.runLater(() -> {
                    if (confirmBtn != null) {
                        confirmBtn.setDisable(false);
                        confirmBtn.setText("Continue  →");
                    }
                    if (session == null || session.url == null) {
                        showAlert(Alert.AlertType.ERROR, "Stripe Error",
                                "Could not create payment session.\nCheck your Stripe API key.");
                        return;
                    }
                    openStripeWebView(session.url, da);
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    if (confirmBtn != null) {
                        confirmBtn.setDisable(false);
                        confirmBtn.setText("Continue  →");
                    }
                    showAlert(Alert.AlertType.ERROR, "Network Error", e.getMessage());
                });
            }
        }, "stripe-session").start();
    }

    // ─── Open Stripe directly inside JavaFX window ───────────────────────────

    private void openStripeWebView(String stripeUrl, DeliveryAddress da) {
        WebView   webView = new WebView();
        WebEngine engine  = webView.getEngine();

        // Fake Chrome UA — Stripe blocks default JavaFX WebView UA
        engine.setUserAgent(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/124.0.0.0 Safari/537.36"
        );

        // Watch for Stripe redirect URLs to detect success or cancel
        engine.locationProperty().addListener((obs, oldUrl, newUrl) -> {
            if (newUrl == null) return;
            if (newUrl.startsWith(SUCCESS_URL)) {
                Platform.runLater(() -> finalizeStripeOrder(da));
            } else if (newUrl.startsWith(CANCEL_URL)) {
                Platform.runLater(this::restoreLivraisonView);
            }
        });

        engine.load(stripeUrl);

        // ── Top bar inside WebView panel ──────────────────────────────────────
        Label titleLabel = new Label("🔒  Secure Payment — Stripe");
        titleLabel.setStyle(
                "-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#333;"
        );

        // Update title as Stripe page loads
        engine.titleProperty().addListener((obs, o, newTitle) -> {
            if (newTitle != null && !newTitle.isBlank())
                Platform.runLater(() -> titleLabel.setText("🔒  " + newTitle));
        });

        Button cancelBtn = new Button("✖  Cancel");
        cancelBtn.setStyle(
                "-fx-background-color:transparent;" +
                        "-fx-text-fill:#E53935;" +
                        "-fx-border-color:#E53935;" +
                        "-fx-border-radius:8;" +
                        "-fx-background-radius:8;" +
                        "-fx-padding:6 16;" +
                        "-fx-cursor:hand;" +
                        "-fx-font-size:12px;"
        );
        cancelBtn.setOnAction(e -> restoreLivraisonView());

        Label amountChip = new Label(String.format("%.3f TND", totalAmount));
        amountChip.setStyle(
                "-fx-background-color:#E8F5E9;" +
                        "-fx-text-fill:#2E7D32;" +
                        "-fx-background-radius:20;" +
                        "-fx-padding:4 14;" +
                        "-fx-font-weight:bold;" +
                        "-fx-font-size:13px;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topBar = new HBox(12, titleLabel, spacer, amountChip, cancelBtn);
        topBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        topBar.setStyle(
                "-fx-background-color:white;" +
                        "-fx-border-color:#E0E0E0;" +
                        "-fx-border-width:0 0 1 0;" +
                        "-fx-padding:10 16;"
        );

        BorderPane webPane = new BorderPane();
        webPane.setTop(topBar);
        webPane.setCenter(webView);

        // Replace the entire scene root with the WebView pane
        javafx.scene.Scene scene = fullNameField.getScene();
        BorderPane mainRoot = (BorderPane) scene.getRoot();
        mainRoot.setCenter(webPane);
        // Hide the top bar and bottom bar of livraison while stripe is open
        mainRoot.setTop(null);
        mainRoot.setBottom(null);
    }

    // ─── Restore livraison view if user cancels Stripe ────────────────────────

    private void restoreLivraisonView() {
        // Simply reload the Livraison page fresh
        MainFx.setCenter("/com/example/pi_dev/marketplace/fxml/Livraison.fxml");
    }

    // ─── Finalize after Stripe payment confirmed ──────────────────────────────

    private void finalizeStripeOrder(DeliveryAddress da) {
        boolean success = cartService.buyCartOnline(CURRENT_USER_ID, da);
        if (!success) {
            showAlert(Alert.AlertType.ERROR, "Save Failed",
                    "Payment confirmed but order could not be saved.\nContact support.");
            return;
        }

        // Get latest facture for email
        var factures = factureService.getFacturesByUser(CURRENT_USER_ID);
        if (!factures.isEmpty()) {
            var latestFacture = factures.get(0);
            int factureId     = latestFacture.getId();
            List<FactureProduct> items = factureService.getFactureProducts(factureId);

            // Send confirmation email in background if email was provided
            emailService.sendOnlineOrderConfirmation(
                    da.getEmail(), da, items, latestFacture.getTotal(), factureId
            );
        }

        showAlert(Alert.AlertType.INFORMATION, "Payment Confirmed! 🎉",
                "✅ Your payment was confirmed by Stripe!\n\n" +
                        "📦 Delivery to: " + da.getAddress() + ", " + da.getCity() + "\n\n" +
                        (!da.getEmail().isEmpty()
                                ? "📧 Confirmation email sent to: " + da.getEmail()
                                : "ℹ️  Track your order in the app."));

        MainFx.setCenter("/com/example/pi_dev/marketplace/fxml/FactureList.fxml");
    }

    // ─── Stripe API ───────────────────────────────────────────────────────────

    private StripeSession createStripeSession(long amountCents, String email, String name)
            throws Exception {
        URL url = new URL(STRIPE_BASE_URL + "/checkout/sessions");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Basic " +
                Base64.getEncoder().encodeToString((STRIPE_SECRET_KEY + ":").getBytes(StandardCharsets.UTF_8)));
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(15_000);

        String body = "payment_method_types[0]=card"
                + "&mode=payment"
                + "&customer_email=" + urlEncode(email)
                + "&line_items[0][price_data][currency]=usd"
                + "&line_items[0][price_data][unit_amount]=" + amountCents
                + "&line_items[0][price_data][product_data][name]=" + urlEncode("WonderLust Order")
                + "&line_items[0][price_data][product_data][description]=" + urlEncode("Delivery to " + name)
                + "&line_items[0][quantity]=1"
                + "&success_url=" + urlEncode(SUCCESS_URL)
                + "&cancel_url="  + urlEncode(CANCEL_URL);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        String response = readFull(conn);
        String id   = extractStr(response, "id");
        String sUrl = extractStr(response, "url");
        return (id != null && sUrl != null) ? new StripeSession(id, sUrl) : null;
    }

    // ─── Build delivery address from form ────────────────────────────────────

    private DeliveryAddress buildDeliveryAddress() {
        DeliveryAddress da = new DeliveryAddress();
        da.setFullName(fullNameField.getText().trim());
        da.setPhone(phoneField.getText().trim());
        da.setAddress(addressField.getText().trim());
        da.setCity(cityField.getText().trim());
        da.setPostalCode(postalField.getText().trim());
        da.setNotes(notesField.getText().trim());
        da.setEmail(emailField != null ? emailField.getText().trim() : "");
        return da;
    }

    // ─── Validation ───────────────────────────────────────────────────────────

    private boolean validateFullName() {
        String v  = fullNameField.getText().trim();
        boolean ok = v.length() >= 3 && v.matches("[a-zA-ZÀ-ÿ\\s'-]+");
        setError(fullNameField, fullNameError, "⚠  Name must be at least 3 letters", !ok);
        return ok;
    }

    private boolean validatePhone() {
        String v  = phoneField.getText().trim().replaceAll("[\\s\\-\\+]", "");
        boolean ok = v.matches("\\d{8,15}");
        setError(phoneField, phoneError, "⚠  Enter a valid phone number (8–15 digits)", !ok);
        return ok;
    }

    private boolean validateAddress() {
        String v  = addressField.getText().trim();
        boolean ok = v.length() >= 5;
        setError(addressField, addressError, "⚠  Address must be at least 5 characters", !ok);
        return ok;
    }

    private boolean validateCity() {
        String v  = cityField.getText().trim();
        boolean ok = v.length() >= 2;
        setError(cityField, cityError, "⚠  Please enter your city", !ok);
        return ok;
    }

    private void setError(TextField field, Label errorLabel, String msg, boolean hasError) {
        errorLabel.setText(msg);
        errorLabel.setVisible(hasError);
        errorLabel.setManaged(hasError);
        field.setStyle(hasError
                ? "-fx-background-radius:8;-fx-border-color:#E53935;-fx-border-radius:8;-fx-padding:10;-fx-font-size:13px;"
                : "-fx-background-radius:8;-fx-border-color:#43A047;-fx-border-radius:8;-fx-padding:10;-fx-font-size:13px;"
        );
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /** Find the confirm button in the scene to update its text during loading */
    private Button getConfirmButton() {
        try {
            javafx.scene.Scene scene = fullNameField.getScene();
            if (scene == null) return null;
            return (Button) scene.lookup(".button-confirm");
        } catch (Exception e) {
            return null;
        }
    }

    private String readFull(HttpURLConnection conn) throws Exception {
        int code = conn.getResponseCode();
        java.io.InputStream s = (code >= 200 && code < 300)
                ? conn.getInputStream() : conn.getErrorStream();
        return new String(s.readAllBytes(), StandardCharsets.UTF_8);
    }

    private String extractStr(String json, String key) {
        String m = "\"" + key + "\"";
        int ki = json.indexOf(m);
        if (ki == -1) return null;
        int ci = json.indexOf(':', ki + m.length());
        if (ci == -1) return null;
        int i = ci + 1;
        while (i < json.length() && json.charAt(i) == ' ') i++;
        if (i >= json.length() || json.charAt(i) != '"') return null;
        i++;
        StringBuilder sb = new StringBuilder();
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\') { i += 2; continue; }
            if (c == '"') break;
            sb.append(c);
            i++;
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    private String urlEncode(String s) {
        try { return java.net.URLEncoder.encode(s, "UTF-8"); }
        catch (Exception e) { return s; }
    }

    @FXML private void goBack() { MainFx.setCenter("/com/example/pi_dev/marketplace/fxml/Cart.fxml"); }

    private void showAlert(Alert.AlertType t, String title, String msg) {
        Alert a = new Alert(t);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }

    private static class StripeSession {
        final String id, url;
        StripeSession(String i, String u) { id = i; url = u; }
    }
}