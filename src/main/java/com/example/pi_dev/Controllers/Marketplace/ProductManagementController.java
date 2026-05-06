package com.example.pi_dev.Controllers.Marketplace;

import com.example.pi_dev.Entities.Marketplace.Product;
import com.example.pi_dev.Services.Marketplace.ProductService;
import com.example.pi_dev.Utils.Marketplace.ImageUploader;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import com.example.pi_dev.Test.Marketplace.MainFx;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static com.example.pi_dev.Services.Marketplace.ProductService.CURRENT_USER_ID;

public class ProductManagementController {

    @FXML
    private FlowPane productFlow;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> typeFilter;

    @FXML
    private ComboBox<String> categoryFilter;

    private final ProductService ps = new ProductService();
    private ObservableList<Product> productList;

    @FXML
    public void initialize() {
        // --- FILTER SETUP ---
        categoryFilter.setItems(FXCollections.observableArrayList("All Categories", "camping", "nature", "beach"));
        categoryFilter.setValue("All Categories");

        typeFilter.setItems(FXCollections.observableArrayList("All Types", "For Sale", "For Rent"));
        typeFilter.setValue("All Types");

        // Load products as cards
        loadProducts();
    }

    @FXML
    private void goToAvailableProduct() {
        MainFx.setCenter("/com/example/pi_dev/marketplace/fxml/ProductAvailable.fxml");
    }

    @FXML
    private void goToFactureList(ActionEvent actionEvent) {
        MainFx.setCenter("/com/example/pi_dev/marketplace/fxml/FactureList.fxml");
    }

    private void loadProducts() {
        productFlow.getChildren().clear();
        List<Product> list = ps.getAllProducts(CURRENT_USER_ID);
        productList = FXCollections.observableArrayList(list);

        for (Product p : productList) {
            productFlow.getChildren().add(createProductCard(p));
        }
    }

    private VBox createProductCard(Product p) {
        VBox card = new VBox(10);
        card.getStyleClass().add("product-card-management");
        card.setPadding(new Insets(10));

        // --- IMAGE ---
        ImageView imageView = new ImageView();
        imageView.setFitWidth(150);
        imageView.setFitHeight(150);
        imageView.setPreserveRatio(true);

        String imagePath = p.getImage();
        Image imageToShow;

        try {
            if (imagePath != null && !imagePath.isEmpty()) {
                imageToShow = new Image(imagePath, 150, 150, true, true, true);
                if (imageToShow.isError()) {
                    imageToShow = new Image("https://dummyimage.com/180x120/cccccc/666666&text=No+Photo", 150, 150, true, true, true);
                }
            } else {
                imageToShow = new Image("https://dummyimage.com/180x120/cccccc/666666&text=No+Photo", 150, 150, true, true, true);
            }
        } catch (Exception e) {
            System.err.println("Error loading image: " + e.getMessage());
            imageToShow = new Image("https://dummyimage.com/180x120/cccccc/666666&text=No+Photo", 150, 150, true, true, true);
        }

        imageView.setImage(imageToShow);


        // --- TITLE ---
        Label title = new Label(p.getTitle());
        title.getStyleClass().add("title");

        // --- DESCRIPTION ---
        Label desc = new Label(p.getDescription());
        desc.getStyleClass().add("description");
        desc.setWrapText(true);

        // --- PRICE ---
        Label price = new Label("Price: " + p.getPrice() + " DT");
        price.getStyleClass().add("price");

        // --- QUANTITY ---
        Label quantity = new Label("Quantity: " + p.getQuantity());

        // --- TYPE ---
        Label type = new Label("Type: " + p.getType());

        // --- CATEGORY ---
        Label category = new Label("Category: " + p.getCategory());

        // --- CREATED DATE ---
        Label createdDate = new Label("Created: " + p.getCreatedDate());

        // --- ACTION BUTTONS ---
        Button edit = new Button("Edit");
        edit.getStyleClass().add("btn-edit");
        edit.setPrefWidth(90);
        edit.setPrefHeight(32);
        edit.setOnAction(e -> openUpdateDialog(p));

        Button delete = new Button("Delete");
        delete.getStyleClass().add("btn-delete");
        delete.setPrefWidth(90);
        delete.setPrefHeight(32);
        delete.setOnAction(e -> deleteProduct(p));

        HBox actions = new HBox(8, edit, delete);
        actions.setAlignment(javafx.geometry.Pos.CENTER);

        // --- ADD ALL ELEMENTS TO CARD ---
        card.getChildren().addAll(imageView, title, desc, price, quantity, type, category, createdDate, actions);

        return card;
    }


    @FXML
    private void handleSearch() {
        String keyword = searchField.getText().toLowerCase();
        productFlow.getChildren().clear();

        productList.stream()
                .filter(p -> p.getTitle().toLowerCase().contains(keyword))
                .forEach(p -> productFlow.getChildren().add(createProductCard(p)));
    }

    @FXML
    private void handleFilter() {
        String selectedCategory = categoryFilter.getValue();
        String selectedType = typeFilter.getValue();

        productFlow.getChildren().clear();

        productList.stream()
                .filter(p -> (selectedCategory.equals("All Categories") || p.getCategory().equalsIgnoreCase(selectedCategory)) &&
                        (selectedType.equals("All Types") || p.getType().equalsIgnoreCase(selectedType)))
                .forEach(p -> productFlow.getChildren().add(createProductCard(p)));
    }


    @FXML
    private void handleAdd() {
        Dialog<Product> dialog = new Dialog<>();
        dialog.setTitle("New Product");
        dialog.setResizable(true);

        ButtonType addButtonType = new ButtonType("Add Product", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #F8F9FA;");

        // Header
        VBox header = new VBox(2);
        header.setPadding(new Insets(12, 20, 10, 20));
        header.setStyle("-fx-background-color: #1B5E20;");
        Label headerTitle = new Label("➕  Add New Product");
        headerTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label headerSub = new Label("Fill in the product details below");
        headerSub.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.7);");
        header.getChildren().addAll(headerTitle, headerSub);

        // Form
        VBox form = new VBox(6);
        form.setPadding(new Insets(12, 20, 12, 20));
        form.setStyle("-fx-background-color: #F8F9FA;");

        TextField titleField       = styledField("e.g. Tente de camping 3 places");
        TextField descriptionField = styledField("Describe your product...");
        TextField priceField       = styledField("e.g. 150");
        TextField quantityField    = styledField("e.g. 10");

        ComboBox<String> typeField = new ComboBox<>(FXCollections.observableArrayList("For Sale", "For Rent"));
        typeField.setPromptText("Select type");
        typeField.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> categoryField = new ComboBox<>(FXCollections.observableArrayList("camping", "hiking", "beach"));
        categoryField.setPromptText("Select category");
        categoryField.setMaxWidth(Double.MAX_VALUE);

        Label titleError    = errorLabel();
        Label typeError     = errorLabel();
        Label priceError    = errorLabel();
        Label quantityError = errorLabel();
        Label categoryError = errorLabel();

        File[] selectedFile = new File[1];
        Label imageLabel = new Label("No image selected");
        imageLabel.setStyle("-fx-text-fill: #999; -fx-font-size: 11px;");
        ImageView imagePreview = new ImageView();
        imagePreview.setFitWidth(60);
        imagePreview.setFitHeight(60);
        imagePreview.setPreserveRatio(true);
        imagePreview.setVisible(false);

        Button chooseImageBtn = new Button("📷 Choose Image");
        chooseImageBtn.setStyle("-fx-background-color: white; -fx-text-fill: #2E7D32; -fx-border-color: #2E7D32; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 5 12; -fx-font-size: 11px; -fx-cursor: hand;");
        chooseImageBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
            File file = fc.showOpenDialog(dialog.getDialogPane().getScene().getWindow());
            if (file != null) {
                selectedFile[0] = file;
                imageLabel.setText(file.getName());
                imageLabel.setStyle("-fx-text-fill: #2E7D32; -fx-font-size: 11px;");
                imagePreview.setImage(new Image(file.toURI().toString(), 60, 60, true, true));
                imagePreview.setVisible(true);
            }
        });

        HBox imageRow = new HBox(10, chooseImageBtn, imageLabel, imagePreview);
        imageRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        form.getChildren().addAll(
                formRow("Title *", titleField, titleError),
                formRow("Description", descriptionField, new Label()),
                formRow("Type *", typeField, typeError),
                formRow("Price (TND) *", priceField, priceError),
                formRow("Quantity *", quantityField, quantityError),
                formRow("Category *", categoryField, categoryError),
                sectionLabel("Product Image"),
                imageRow
        );

        ScrollPane scrollPane = new ScrollPane(form);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background-color: #F8F9FA; -fx-background: #F8F9FA;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        root.getChildren().addAll(header, scrollPane);

        dialog.getDialogPane().setContent(root);
        dialog.getDialogPane().setPrefWidth(440);
        dialog.getDialogPane().setPrefHeight(480);
        dialog.getDialogPane().setStyle("-fx-background-color: #F8F9FA; -fx-padding: 0;");

        Button addBtn = (Button) dialog.getDialogPane().lookupButton(addButtonType);
        addBtn.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 8 20;");

        addBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            boolean valid = true;
            if (titleField.getText().trim().isEmpty()) { titleError.setText("⚠ Required"); valid = false; } else titleError.setText("");
            if (typeField.getValue() == null) { typeError.setText("⚠ Required"); valid = false; } else typeError.setText("");
            try { float pv = Float.parseFloat(priceField.getText()); if (pv < 0) { priceError.setText("⚠ Must be ≥ 0"); valid = false; } else priceError.setText(""); }
            catch (Exception e) { priceError.setText("⚠ Invalid price"); valid = false; }
            try { int q = Integer.parseInt(quantityField.getText()); if (q < 0) { quantityError.setText("⚠ Must be ≥ 0"); valid = false; } else quantityError.setText(""); }
            catch (Exception e) { quantityError.setText("⚠ Invalid quantity"); valid = false; }
            if (categoryField.getValue() == null) { categoryError.setText("⚠ Required"); valid = false; } else categoryError.setText("");
            if (!valid) event.consume();
        });

        dialog.setResultConverter(btn -> {
            if (btn == addButtonType) {
                Product p = new Product();
                p.setTitle(titleField.getText().trim());
                p.setDescription(descriptionField.getText().trim());
                p.setType(typeField.getValue());
                p.setPrice(Float.parseFloat(priceField.getText()));
                p.setQuantity(Integer.parseInt(quantityField.getText()));
                p.setCategory(categoryField.getValue());
                p.setCreatedDate(new Date());
                if (selectedFile[0] != null) p.setImage(ImageUploader.uploadImage(selectedFile[0]));
                return p;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(p -> { ps.addProduct(p); loadProducts(); showAlert("✅ Success", "Product added successfully!"); });
    }

    private void openUpdateDialog(Product p) {
        Dialog<Product> dialog = new Dialog<>();
        dialog.setTitle("Update Product");
        dialog.setResizable(true);

        ButtonType updateButtonType = new ButtonType("Save Changes", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(updateButtonType, ButtonType.CANCEL);

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #F8F9FA;");

        // Header
        VBox header = new VBox(2);
        header.setPadding(new Insets(12, 20, 10, 20));
        header.setStyle("-fx-background-color: #1565C0;");
        Label headerTitle = new Label("✏️  Update Product");
        headerTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label headerSub = new Label("Editing: " + p.getTitle());
        headerSub.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.7);");
        header.getChildren().addAll(headerTitle, headerSub);

        // Form
        VBox form = new VBox(6);
        form.setPadding(new Insets(12, 20, 12, 20));
        form.setStyle("-fx-background-color: #F8F9FA;");

        TextField titleField = styledField(p.getTitle()); titleField.setText(p.getTitle());
        TextField descriptionField = styledField(p.getDescription()); descriptionField.setText(p.getDescription());
        TextField priceField = styledField(String.valueOf(p.getPrice())); priceField.setText(String.valueOf(p.getPrice()));
        TextField quantityField = styledField(String.valueOf(p.getQuantity())); quantityField.setText(String.valueOf(p.getQuantity()));

        ComboBox<String> typeField = new ComboBox<>(FXCollections.observableArrayList("For Sale", "For Rent"));
        typeField.setValue(p.getType());
        typeField.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> categoryField = new ComboBox<>(FXCollections.observableArrayList("camping", "hiking", "beach"));
        categoryField.setValue(p.getCategory());
        categoryField.setMaxWidth(Double.MAX_VALUE);

        Label titleError = errorLabel(), typeError = errorLabel(), priceError = errorLabel(),
                qtyError = errorLabel(), catError = errorLabel(), changeError = errorLabel();

        File[] selectedFile = new File[1];
        ImageView imagePreview = new ImageView();
        imagePreview.setFitWidth(60); imagePreview.setFitHeight(60); imagePreview.setPreserveRatio(true);
        if (p.getImage() != null && !p.getImage().isEmpty())
            imagePreview.setImage(new Image(p.getImage(), 60, 60, true, true));

        Button chooseImageBtn = new Button("📷 Change Image");
        chooseImageBtn.setStyle("-fx-background-color: white; -fx-text-fill: #1565C0; -fx-border-color: #1565C0; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 5 12; -fx-font-size: 11px; -fx-cursor: hand;");
        chooseImageBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
            File file = fc.showOpenDialog(dialog.getDialogPane().getScene().getWindow());
            if (file != null) { selectedFile[0] = file; imagePreview.setImage(new Image(file.toURI().toString(), 60, 60, true, true)); }
        });

        HBox imageRow = new HBox(10, chooseImageBtn, imagePreview);
        imageRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        form.getChildren().addAll(
                formRow("Title *", titleField, titleError),
                formRow("Description", descriptionField, new Label()),
                formRow("Type *", typeField, typeError),
                formRow("Price (TND) *", priceField, priceError),
                formRow("Quantity *", quantityField, qtyError),
                formRow("Category *", categoryField, catError),
                changeError,
                sectionLabel("Product Image"),
                imageRow
        );

        ScrollPane scrollPane = new ScrollPane(form);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background-color: #F8F9FA; -fx-background: #F8F9FA;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        root.getChildren().addAll(header, scrollPane);

        dialog.getDialogPane().setContent(root);
        dialog.getDialogPane().setPrefWidth(440);
        dialog.getDialogPane().setPrefHeight(480);
        dialog.getDialogPane().setStyle("-fx-background-color: #F8F9FA; -fx-padding: 0;");

        Button saveBtn = (Button) dialog.getDialogPane().lookupButton(updateButtonType);
        saveBtn.setStyle("-fx-background-color: #1565C0; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 8 20;");

        saveBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            boolean valid = true;
            if (titleField.getText().trim().isEmpty()) { titleError.setText("⚠ Required"); valid = false; } else titleError.setText("");
            if (typeField.getValue() == null) { typeError.setText("⚠ Required"); valid = false; } else typeError.setText("");
            try { float pVal = Float.parseFloat(priceField.getText()); if (pVal < 0) { priceError.setText("⚠ ≥ 0"); valid = false; } else priceError.setText(""); }
            catch (Exception e) { priceError.setText("⚠ Invalid"); valid = false; }
            try { int qVal = Integer.parseInt(quantityField.getText()); if (qVal < 0) { qtyError.setText("⚠ ≥ 0"); valid = false; } else qtyError.setText(""); }
            catch (Exception e) { qtyError.setText("⚠ Invalid"); valid = false; }
            if (categoryField.getValue() == null) { catError.setText("⚠ Required"); valid = false; } else catError.setText("");
            boolean changed = !titleField.getText().equals(p.getTitle()) || !descriptionField.getText().equals(p.getDescription()) ||
                    !typeField.getValue().equals(p.getType()) || !priceField.getText().equals(String.valueOf(p.getPrice())) ||
                    !quantityField.getText().equals(String.valueOf(p.getQuantity())) || !categoryField.getValue().equals(p.getCategory()) || selectedFile[0] != null;
            if (!changed) { changeError.setText("⚠ No changes detected"); valid = false; } else changeError.setText("");
            if (!valid) event.consume();
        });

        dialog.setResultConverter(btn -> {
            if (btn == updateButtonType) {
                p.setTitle(titleField.getText().trim());
                p.setDescription(descriptionField.getText().trim());
                p.setType(typeField.getValue());
                p.setPrice(Float.parseFloat(priceField.getText()));
                p.setQuantity(Integer.parseInt(quantityField.getText()));
                p.setCategory(categoryField.getValue());
                if (selectedFile[0] != null) p.setImage(ImageUploader.uploadImage(selectedFile[0]));
                return p;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(updated -> { ps.updateProduct(updated); loadProducts(); showAlert("✅ Updated", "Product updated successfully!"); });
    }


    // ── UI Helper methods — ADD THESE TOO ─────────────────────────────────────

    private TextField styledField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setPrefWidth(Double.MAX_VALUE);
        tf.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #E0E0E0;" +
                        "-fx-border-radius: 6;" +
                        "-fx-background-radius: 6;" +
                        "-fx-padding: 6 10;" +
                        "-fx-font-size: 11px;");

        tf.focusedProperty().addListener((o, was, now) ->
                tf.setStyle(tf.getStyle().replace(
                        now ? "#E0E0E0" : "#43A047",
                        now ? "#43A047" : "#E0E0E0")));
        return tf;
    }

    private String comboStyle() {
        return "-fx-background-color: white;" +
                "-fx-border-color: #E0E0E0;" +
                "-fx-border-radius: 6;" +
                "-fx-background-radius: 6;" +
                "-fx-padding: 2 6;" +
                "-fx-font-size: 11px;";
    }

    private Label errorLabel() {
        Label l = new Label();
        l.setStyle("-fx-text-fill: #E53935; -fx-font-size: 11px;");
        return l;
    }

    private Label sectionLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #666;");
        return l;
    }

    private VBox formRow(String labelText, javafx.scene.Node field, javafx.scene.Node error) {
        VBox box = new VBox(4);
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #555;");
        field.setStyle(((javafx.scene.control.Control) field).getStyle());
        VBox.setVgrow(field, javafx.scene.layout.Priority.NEVER);
        box.getChildren().addAll(lbl, field, error);
        return box;
    }

    private void deleteProduct(Product p) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setHeaderText("Delete Product?");
        alert.setContentText("Are you sure you want to delete " + p.getTitle() + "?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            ps.deleteProduct(p.getId());
            loadProducts();
            showAlert("Deleted", "Product deleted successfully!");
        }
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(msg);
        alert.show();
    }
    @FXML
    private void openDashboard() {
        MainFx.setCenter("/com/example/pi_dev/marketplace/fxml/StatsDashboard.fxml");
    }
    @FXML
    private void goToRoleSelection() {
        MainFx.setCenter("/com/example/pi_dev/marketplace/fxml/RoleSelection.fxml");
    }






    @FXML private void openOrders() { MainFx.setCenter("/com/example/pi_dev/marketplace/fxml/OrdersManagement.fxml"); }
}
