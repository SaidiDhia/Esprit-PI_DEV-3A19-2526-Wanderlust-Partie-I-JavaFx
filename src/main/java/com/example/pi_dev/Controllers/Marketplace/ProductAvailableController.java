package com.example.pi_dev.Controllers.Marketplace;

import com.example.pi_dev.Entities.Marketplace.Product;
import com.example.pi_dev.Services.Marketplace.CartService;
import com.example.pi_dev.Services.Marketplace.CurrencyService;
import com.example.pi_dev.Services.Marketplace.ProductService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import com.example.pi_dev.Test.Marketplace.MainFx;

import java.util.List;
import java.util.stream.Collectors;

import static com.example.pi_dev.Services.Marketplace.ProductService.CURRENT_USER_ID;

public class ProductAvailableController {

    @FXML private FlowPane productFlow;
    @FXML private Label cartBadge;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private ComboBox<String> typeFilter;
    @FXML private ComboBox<String> sortFilter;

    private ProductService productService = new ProductService();
    private CartService cartService = new CartService();
    private List<Product> allProducts;

    @FXML
    public void initialize() {
        if (categoryFilter != null)
            categoryFilter.setItems(FXCollections.observableArrayList(
                    "All Categories", "camping", "hiking", "beach"));
        if (typeFilter != null)
            typeFilter.setItems(FXCollections.observableArrayList(
                    "All Types", "For Sale", "For Rent"));
        if (sortFilter != null)
            sortFilter.setItems(FXCollections.observableArrayList(
                    "Sort by Price", "Price: Low to High", "Price: High to Low"));

        loadProducts();
        refreshBadge();
    }

    private void loadProducts() {
        allProducts = productService.getAllAvailableProducts(CURRENT_USER_ID);
        renderProducts(allProducts);
    }

    private void renderProducts(List<Product> products) {
        productFlow.getChildren().clear();
        if (products.isEmpty()) {
            Label noProducts = new Label("No products found 😕");
            noProducts.setStyle("-fx-font-size: 18px; -fx-text-fill: #999;");
            productFlow.getChildren().add(noProducts);
            return;
        }
        for (Product product : products)
            productFlow.getChildren().add(createProductCard(product));
    }

    @FXML private void handleSearch() { applyFilters(); }
    @FXML private void handleFilter() { applyFilters(); }
    @FXML private void handleSort()   { applyFilters(); }

    @FXML
    private void resetFilters() {
        if (searchField    != null) searchField.clear();
        if (categoryFilter != null) categoryFilter.setValue("All Categories");
        if (typeFilter     != null) typeFilter.setValue("All Types");
        if (sortFilter     != null) sortFilter.setValue("Sort by Price");
        renderProducts(allProducts);
    }

    private void applyFilters() {
        String search   = searchField    != null ? searchField.getText().toLowerCase().trim() : "";
        String category = categoryFilter != null ? categoryFilter.getValue() : "All Categories";
        String type     = typeFilter     != null ? typeFilter.getValue()     : "All Types";
        String sort     = sortFilter     != null ? sortFilter.getValue()     : "Sort by Price";

        List<Product> filtered = allProducts.stream()
                .filter(p -> search.isEmpty()
                          || p.getTitle().toLowerCase().contains(search)
                          || p.getDescription().toLowerCase().contains(search))
                .filter(p -> category == null || category.equals("All Categories")
                          || p.getCategory().equalsIgnoreCase(category))
                .filter(p -> type == null || type.equals("All Types")
                          || p.getType().equalsIgnoreCase(type))
                .collect(Collectors.toList());

        if ("Price: Low to High".equals(sort))
            filtered.sort((a, b) -> Float.compare(a.getPrice(), b.getPrice()));
        else if ("Price: High to Low".equals(sort))
            filtered.sort((a, b) -> Float.compare(b.getPrice(), a.getPrice()));

        renderProducts(filtered);
    }

    private VBox createProductCard(Product product) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPrefWidth(220);
        card.setMaxWidth(220);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15;" +
                      "-fx-border-color: #E0E0E0; -fx-border-radius: 15;" +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 3);");

        // Image
        ImageView imageView = new ImageView();
        imageView.setFitWidth(180);
        imageView.setFitHeight(120);
        imageView.setPreserveRatio(true);
        String imagePath = product.getImage();
        try {
            Image img = (imagePath != null && !imagePath.isEmpty())
                    ? new Image(imagePath, 180, 120, true, true, true)
                    : new Image("https://dummyimage.com/180x120/cccccc/666666&text=No+Photo", 180, 120, true, true, true);
            imageView.setImage(img.isError()
                    ? new Image("https://dummyimage.com/180x120/cccccc/666666&text=No+Photo", 180, 120, true, true, true)
                    : img);
        } catch (Exception e) {
            imageView.setImage(new Image("https://dummyimage.com/180x120/cccccc/666666&text=No+Photo", 180, 120, true, true, true));
        }

        Label titleLabel = new Label(product.getTitle());
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(180);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        Label typeLabel = new Label(product.getType());
        typeLabel.setStyle("-fx-background-color: " +
                ("For Rent".equals(product.getType()) ? "#E3F2FD; -fx-text-fill: #1565C0;"
                                                       : "#E8F5E9; -fx-text-fill: #2E7D32;") +
                " -fx-background-radius: 10; -fx-padding: 2 8; -fx-font-size: 11px; -fx-font-weight: bold;");

        Label catLabel = new Label("📂 " + product.getCategory());
        catLabel.setStyle("-fx-text-fill: #999; -fx-font-size: 11px;");

        Label priceLabel = new Label(String.format("%.2f TND", product.getPrice()));
        priceLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");

        int available = product.getAvailableQuantity();
        Label stockLabel = new Label(available > 0 ? "✅ In Stock (" + available + ")" : "❌ Out of Stock");
        stockLabel.setStyle("-fx-text-fill: " + (available > 0 ? "#4CAF50" : "#F44336") + "; -fx-font-size: 11px;");

        Label convertedLabel = new Label();
        convertedLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #1565C0; -fx-font-weight: bold;");
        convertedLabel.setVisible(false);
        convertedLabel.setManaged(false);
        convertedLabel.setWrapText(true);
        convertedLabel.setMaxWidth(180);

        Button currencyBtn = new Button("💱 See in EUR/USD");
        currencyBtn.setPrefWidth(180);
        currencyBtn.setStyle("-fx-background-color: #E3F2FD; -fx-text-fill: #1565C0;" +
                             "-fx-background-radius: 10; -fx-cursor: hand; -fx-font-size: 11px;");
        currencyBtn.setOnAction(e -> {
            if (convertedLabel.isVisible()) {
                convertedLabel.setVisible(false);
                convertedLabel.setManaged(false);
                currencyBtn.setText("💱 See in EUR/USD");
            } else {
                currencyBtn.setText("⏳ Loading...");
                currencyBtn.setDisable(true);
                new Thread(() -> {
                    String converted = CurrencyService.getConvertedLabel(product.getPrice());
                    Platform.runLater(() -> {
                        convertedLabel.setText("💱 " + converted);
                        convertedLabel.setVisible(true);
                        convertedLabel.setManaged(true);
                        currencyBtn.setText("💱 Hide conversion");
                        currencyBtn.setDisable(false);
                    });
                }).start();
            }
        });

        Spinner<Integer> quantitySpinner = new Spinner<>(1, Math.max(1, available), 1);
        quantitySpinner.setPrefWidth(100);
        quantitySpinner.setEditable(true);
        quantitySpinner.setDisable(available == 0);

        // View Details button — loads into BuyerHome center to keep top nav
        Button detailsBtn = new Button("🔍 View Details");
        detailsBtn.setPrefWidth(180);
        detailsBtn.setStyle("-fx-background-color: #F5F5F5; -fx-text-fill: #333;" +
                           "-fx-background-radius: 10; -fx-cursor: hand; -fx-font-size: 11px;" +
                           "-fx-border-color: #E0E0E0; -fx-border-radius: 10;");
        detailsBtn.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pi_dev/marketplace/fxml/ProductDetails.fxml"));
                Parent page = loader.load();
                ProductDetailsController controller = loader.getController();
                controller.setProduct(product);
                MainFx.setCenter(page);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        Button addButton = new Button(available > 0 ? "🛒 Add to Cart" : "Out of Stock");
        addButton.setPrefWidth(180);
        addButton.setDisable(available == 0);
        addButton.setStyle("-fx-background-color: " + (available > 0 ? "#2E7D32" : "#BDBDBD") +
                           "; -fx-text-fill: white; -fx-background-radius: 10;" +
                           " -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8;");
        addButton.setOnAction(event -> {
            int quantity = quantitySpinner.getValue();
            boolean success = cartService.addToCart(CURRENT_USER_ID, product.getId(), quantity);
            if (success) {
                refreshBadge();
                showAlert(Alert.AlertType.INFORMATION, "Added! ✅",
                        quantity + " × " + product.getTitle() + " added to cart!");
                quantitySpinner.getValueFactory().setValue(1);
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Could not add. Check stock.");
            }
        });

        card.getChildren().addAll(imageView, titleLabel, typeLabel, catLabel,
                priceLabel, stockLabel, currencyBtn, convertedLabel,
                quantitySpinner, detailsBtn, addButton);
        return card;
    }

    @FXML private void goToCart()              { MainFx.setCenter("/com/example/pi_dev/marketplace/fxml/Cart.fxml"); }
    @FXML private void goToFactureList()       { MainFx.setCenter("/com/example/pi_dev/marketplace/fxml/FactureList.fxml"); }
    @FXML private void goToRoleSelection()     { MainFx.setCenter("/com/example/pi_dev/marketplace/fxml/RoleSelection.fxml"); }
    @FXML private void goToProductManagement() { MainFx.setCenter("/com/example/pi_dev/marketplace/fxml/SellerHome.fxml"); }
    @FXML private void refresh()               { loadProducts(); refreshBadge(); }

    private void refreshBadge() {
        if (cartBadge == null) return;
        int count = cartService.getCartItems(CURRENT_USER_ID).values()
                .stream().mapToInt(Integer::intValue).sum();
        cartBadge.setText("🛒 My Cart (" + count + ")");
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    public void goToFactureList(ActionEvent actionEvent) {
        MainFx.setCenter("/com/example/pi_dev/marketplace/fxml/FactureList.fxml");
    }
}
