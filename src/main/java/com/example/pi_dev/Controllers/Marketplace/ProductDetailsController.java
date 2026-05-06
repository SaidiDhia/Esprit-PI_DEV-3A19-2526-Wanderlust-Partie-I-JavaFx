package com.example.pi_dev.Controllers.Marketplace;

import com.example.pi_dev.Entities.Marketplace.Product;
import com.example.pi_dev.Services.Marketplace.CartService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import com.example.pi_dev.Test.Marketplace.MainFx;

import static com.example.pi_dev.Services.Marketplace.ProductService.CURRENT_USER_ID;

public class ProductDetailsController {

    @FXML private ImageView productImage;
    @FXML private Label titleLabel;
    @FXML private Label typeLabel;
    @FXML private Label categoryLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label priceLabel;
    @FXML private Label stockLabel;
    @FXML private Label dateLabel;
    @FXML private Spinner<Integer> quantitySpinner;
    @FXML private Button addToCartBtn;
    @FXML private Label cartBadge;

    private Product product;
    private CartService cartService = new CartService();

    public void setProduct(Product product) {
        this.product = product;
        loadDetails();
        refreshBadge();
    }

    private void loadDetails() {
        String imagePath = product.getImage();
        try {
            Image img = (imagePath != null && !imagePath.isEmpty())
                    ? new Image(imagePath, 200, 180, true, true, true)
                    : new Image("https://dummyimage.com/200x180/cccccc/666666&text=No+Photo", 200, 180, true, true, true);
            productImage.setImage(img.isError()
                    ? new Image("https://dummyimage.com/200x180/cccccc/666666&text=No+Photo", 200, 180, true, true, true)
                    : img);
        } catch (Exception e) {
            productImage.setImage(new Image("https://dummyimage.com/200x180/cccccc/666666&text=No+Photo", 200, 180, true, true, true));
        }

        titleLabel.setText(product.getTitle());

        typeLabel.setText(product.getType());
        typeLabel.setStyle("-fx-background-color: " +
                ("For Rent".equals(product.getType()) ? "#E3F2FD; -fx-text-fill: #1565C0;"
                                                      : "#E8F5E9; -fx-text-fill: #2E7D32;") +
                " -fx-background-radius: 10; -fx-padding: 3 12; -fx-font-size: 12px; -fx-font-weight: bold;");

        categoryLabel.setText("📂 " + product.getCategory());

        descriptionLabel.setText(product.getDescription() != null && !product.getDescription().isEmpty()
                ? product.getDescription() : "No description available.");

        priceLabel.setText(String.format("%.2f TND", product.getPrice()));

        int available = product.getAvailableQuantity();
        stockLabel.setText(available > 0 ? "✅ In Stock (" + available + " available)" : "❌ Out of Stock");
        stockLabel.setStyle("-fx-text-fill: " + (available > 0 ? "#4CAF50" : "#F44336") + "; -fx-font-size: 12px;");

        dateLabel.setText(product.getCreatedDate() != null ? product.getCreatedDate().toString() : "-");

        SpinnerValueFactory<Integer> valueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Math.max(1, available), 1);
        quantitySpinner.setValueFactory(valueFactory);
        quantitySpinner.setDisable(available == 0);

        addToCartBtn.setDisable(available == 0);
        if (available == 0) {
            addToCartBtn.setText("Out of Stock");
            addToCartBtn.setStyle("-fx-background-color: #BDBDBD; -fx-text-fill: white;" +
                    "-fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 13 0; -fx-font-size: 14px;");
        }
    }

    private void refreshBadge() {
        if (cartBadge == null) return;
        int count = cartService.getCartItems(CURRENT_USER_ID).values()
                .stream().mapToInt(Integer::intValue).sum();
        cartBadge.setText("🛒 My Cart (" + count + ")");
    }

    @FXML
    private void addToCart() {
        int quantity = quantitySpinner.getValue();
        boolean success = cartService.addToCart(CURRENT_USER_ID, product.getId(), quantity);
        Alert alert = new Alert(success ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
        alert.setTitle(success ? "Added! ✅" : "Error");
        alert.setHeaderText(null);
        alert.setContentText(success
                ? quantity + " × " + product.getTitle() + " added to cart!"
                : "Could not add. Check stock.");
        alert.showAndWait();
        refreshBadge();
    }

    @FXML private void goBack()     { MainFx.setCenter("/com/example/pi_dev/marketplace/fxml/BuyerHome.fxml"); }
    @FXML private void goToCart()   { MainFx.setCenter("/com/example/pi_dev/marketplace/fxml/Cart.fxml"); }
    @FXML private void goToOrders() { MainFx.setCenter("/com/example/pi_dev/marketplace/fxml/FactureList.fxml"); }

    @FXML private void goToRoleSelection()     { MainFx.setCenter("/com/example/pi_dev/marketplace/fxml/RoleSelection.fxml"); }
}
