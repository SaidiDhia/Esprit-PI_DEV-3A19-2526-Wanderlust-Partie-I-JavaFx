package com.example.pi_dev.Controllers.Marketplace;

import com.example.pi_dev.Entities.Marketplace.Product;
import com.example.pi_dev.Services.Marketplace.CartService;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import com.example.pi_dev.Test.Marketplace.MainFx;

import java.util.Map;

import static com.example.pi_dev.Services.Marketplace.ProductService.CURRENT_USER_ID;

/**
 * Controller for Shopping Cart Page.
 * Clicking "Buy Now" now navigates to the Livraison (Delivery & Payment) page
 * instead of buying immediately.
 */
public class CartController {

    @FXML private VBox  cartItemsContainer;
    @FXML private VBox  emptyCartMessage;
    @FXML private Label totalLabel;

    private CartService cartService = new CartService();

    @FXML
    public void initialize() {
        loadCart();
    }

    // ─── Load cart ────────────────────────────────────────────────────────────

    private void loadCart() {
        cartItemsContainer.getChildren().clear();

        Map<Product, Integer> cartItems = cartService.getCartItems(CURRENT_USER_ID);

        if (cartItems.isEmpty()) {
            emptyCartMessage.setVisible(true);
            emptyCartMessage.setManaged(true);
            cartItemsContainer.setVisible(false);
            totalLabel.setText("0.00 DT");
            return;
        }

        emptyCartMessage.setVisible(false);
        emptyCartMessage.setManaged(false);
        cartItemsContainer.setVisible(true);

        double total = 0.0;
        for (Map.Entry<Product, Integer> entry : cartItems.entrySet()) {
            Product product  = entry.getKey();
            int     quantity = entry.getValue();
            cartItemsContainer.getChildren().add(createCartItemCard(product, quantity));
            total += product.getPrice() * quantity;
        }

        totalLabel.setText(String.format("%.2f DT", total));
    }

    // ─── Cart item card ───────────────────────────────────────────────────────

    private HBox createCartItemCard(Product product, int quantity) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(15));
        card.setStyle(
            "-fx-background-color: white; " +
            "-fx-background-radius: 10; " +
            "-fx-border-color: #E0E0E0; " +
            "-fx-border-radius: 10; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);"
        );

        // Product image
        ImageView imageView = new ImageView();
        imageView.setFitWidth(80);
        imageView.setFitHeight(80);
        imageView.setPreserveRatio(true);

        String imagePath = product.getImage();
        Image imageToShow;
        try {
            if (imagePath != null && !imagePath.isEmpty()) {
                imageToShow = new Image(imagePath, 80, 80, true, true, true);
                if (imageToShow.isError()) {
                    imageToShow = new Image(
                        "https://dummyimage.com/80x80/cccccc/666666&text=No+Photo",
                        80, 80, true, true, true
                    );
                }
            } else {
                imageToShow = new Image(
                    "https://dummyimage.com/80x80/cccccc/666666&text=No+Photo",
                    80, 80, true, true, true
                );
            }
        } catch (Exception e) {
            imageToShow = new Image(
                "https://dummyimage.com/80x80/cccccc/666666&text=No+Photo",
                80, 80, true, true, true
            );
        }
        imageView.setImage(imageToShow);

        // Product info
        VBox infoBox = new VBox(5);
        infoBox.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(product.getTitle());
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label typeLabel = new Label(product.getType());
        typeLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");

        Label priceLabel = new Label(String.format("%.2f DT", product.getPrice()));
        priceLabel.setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: bold;");

        infoBox.getChildren().addAll(titleLabel, typeLabel, priceLabel);

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Quantity controls
        HBox quantityBox = new HBox(10);
        quantityBox.setAlignment(Pos.CENTER);

        Button decreaseBtn = new Button("-");
        decreaseBtn.getStyleClass().add("btn-qty");
        decreaseBtn.setOnAction(e -> {
            int newQty = quantity - 1;
            if (newQty < 1) {
                cartService.removeFromCart(CURRENT_USER_ID, product.getId());
            } else {
                cartService.updateCartItemQuantity(CURRENT_USER_ID, product.getId(), newQty);
            }
            loadCart();
        });

        Label qtyLabel = new Label(String.valueOf(quantity));
        qtyLabel.setStyle(
            "-fx-font-weight: bold; -fx-font-size: 16px; " +
            "-fx-min-width: 30; -fx-alignment: center;"
        );

        Button increaseBtn = new Button("+");
        increaseBtn.getStyleClass().add("btn-qty");
        increaseBtn.setOnAction(e -> {
            int newQty = quantity + 1;
            if (cartService.updateCartItemQuantity(CURRENT_USER_ID, product.getId(), newQty)) {
                loadCart();
            } else {
                showAlert(Alert.AlertType.WARNING, "Stock Limit",
                    "Cannot add more. Stock: " + product.getQuantity());
            }
        });

        quantityBox.getChildren().addAll(decreaseBtn, qtyLabel, increaseBtn);

        // Subtotal
        Label subtotalLabel = new Label(
            String.format("%.2f DT", product.getPrice() * quantity)
        );
        subtotalLabel.setStyle(
            "-fx-font-size: 16px; -fx-font-weight: bold; " +
            "-fx-min-width: 100; -fx-alignment: center-right;"
        );

        // Delete button
        Button deleteBtn = new Button("🗑");
        deleteBtn.getStyleClass().add("btn-delete");
        deleteBtn.setOnAction(e -> {
            cartService.removeFromCart(CURRENT_USER_ID, product.getId());
            loadCart();
        });

        card.getChildren().addAll(imageView, infoBox, spacer, quantityBox, subtotalLabel, deleteBtn);
        return card;
    }

    // ─── Buy Now → go to Livraison page ──────────────────────────────────────

    /**
     * Instead of buying immediately, navigate to the Livraison page
     * where the user fills delivery info and picks a payment method.
     */
    @FXML
    private void buy() {
        Map<Product, Integer> items = cartService.getCartItems(CURRENT_USER_ID);

        if (items.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Empty Cart", "Your cart is empty!");
            return;
        }

        // Go to delivery & payment selection page
        MainFx.setCenter("/com/example/pi_dev/marketplace/fxml/Livraison.fxml");
    }

    // ─── Navigation ──────────────────────────────────────────────────────────

    @FXML
    private void goBack() {
        MainFx.setCenter("/com/example/pi_dev/marketplace/fxml/BuyerHome.fxml");
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
