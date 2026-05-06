package com.example.pi_dev.Controllers.Main;

import com.example.pi_dev.Session.Session;
import com.example.pi_dev.Test.Marketplace.MainFx;
import com.example.pi_dev.enums.RoleEnum;
import com.example.pi_dev.Entities.Users.User;
import com.example.pi_dev.Utils.Users.UserSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;

import java.io.File;
import java.io.IOException;

public class MainLayoutController {

    @FXML
    private StackPane contentArea;

    @FXML
    private HBox navBarRow;

    @FXML
    private Label userNameLabel;

    @FXML
    private Circle userAvatarCircle;

    @FXML
    private MenuButton userMenuButton;

    @FXML
    public void initialize() {
        // Allow other controllers to find this one
        contentArea.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.getRoot().setUserData(this);
            }
        });
        
        updateUserInfo();
        // Load default view (e.g., Events)
        if (UserSession.getInstance().isLoggedIn()) {
            handleEvents();
        } else {
            loadView("/com/example/pi_dev/user/login.fxml");
        }
    }

    public void updateUserInfo() {
        boolean loggedIn = UserSession.getInstance().isLoggedIn();
        
        // Control navbar visibility based on login status
        if (navBarRow != null) {
            navBarRow.setVisible(loggedIn);
            navBarRow.setManaged(loggedIn);
        }

        if (loggedIn) {
            User user = UserSession.getInstance().getCurrentUser();
            userNameLabel.setText(user.getFullName());
            userNameLabel.setVisible(true);
            userAvatarCircle.setVisible(true);
            userMenuButton.setVisible(true);
            
            // Set avatar
            String photoPath = user.getProfilePicture();
            Image image = null;
            if (photoPath != null && !photoPath.isEmpty()) {
                File file = new File("uploads/profiles/" + photoPath);
                if (file.exists()) {
                    image = new Image(file.toURI().toString());
                }
            }
            if (image == null) {
                java.io.InputStream stream = getClass().getResourceAsStream("/com/example/pi_dev/user/default-avatar.png");
                if (stream != null) {
                    image = new Image(stream);
                }
            }
            if (image != null) {
                userAvatarCircle.setFill(new ImagePattern(image));
            }

            // Clear existing menu items and add dynamic ones
            userMenuButton.getItems().clear();
            
            MenuItem settingsItem = new MenuItem("Settings");
            settingsItem.setOnAction(e -> handleSettings());
            userMenuButton.getItems().add(settingsItem);

            if (user.getRole() == RoleEnum.ADMIN) {
                MenuItem adminItem = new MenuItem("Admin Dashboard");
                adminItem.setOnAction(e -> handleAdminDashboard());
                userMenuButton.getItems().add(adminItem);
            }

            MenuItem logoutItem = new MenuItem("Logout");
            logoutItem.setOnAction(e -> handleLogout());
            userMenuButton.getItems().add(logoutItem);
        } else {
            userNameLabel.setText("");
            userNameLabel.setVisible(false);
            userAvatarCircle.setVisible(false);
            userMenuButton.setVisible(false);
        }
    }

    public void onLoginSuccess() {
        if (UserSession.getInstance().isLoggedIn()) {
            Session.login(
                UserSession.getInstance().getCurrentUser().getUserId().toString()
            );
        }
        updateUserInfo();
        handleEvents();
    }

    @FXML
    private void handleMessaging() {
        if (checkLogin()) loadView("/com/example/pi_dev/messagingchat.fxml");
    }

    @FXML
    private void handleEvents() {
        if (checkLogin()) loadView("/com/example/pi_dev/events/Catalogue.fxml");
    }

    @FXML
    private void handleBooking() {
        if (checkLogin()) {
            // Load the booking AppShell instead of PlaceBrowse directly
            loadView("/com/example/pi_dev/booking/views/AppShell.fxml");
        }
    }

    @FXML
    private void handleBlog() {
        if (checkLogin()) loadView("/com/example/pi_dev/BlogView.fxml");
    }


    @FXML
    private void handleMarketplace() {
        if (checkLogin()) {
            try {
                // Initialize MainFx for integration
                MainFx.initForIntegration(contentArea);

                // Load the RoleSelection.fxml
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pi_dev/marketplace/fxml/RoleSelection.fxml"));
                Node marketplaceView = loader.load();

                // Display it
                contentArea.getChildren().setAll(marketplaceView);

            } catch (Exception e) {
                e.printStackTrace();
                Label errorLabel = new Label("Error loading marketplace: " + e.getMessage());
                contentArea.getChildren().setAll(errorLabel);
            }
        }
    }

    private void handleSettings() {
        loadView("/com/example/pi_dev/user/settings.fxml");
    }

    private void handleAdminDashboard() {
        loadView("/com/example/pi_dev/user/admin_dashboard.fxml");
    }

    private void handleLogout() {
        UserSession.getInstance().logout();
        updateUserInfo();
        loadView("/com/example/pi_dev/user/login.fxml");
    }

    private boolean checkLogin() {
        if (!UserSession.getInstance().isLoggedIn()) {
            loadView("/com/example/pi_dev/user/login.fxml");
            return false;
        }
        return true;
    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();
            
            // If the loaded controller needs to know about this MainLayoutController, we can pass it here
            // Object controller = loader.getController();
            
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load view: " + fxmlPath);
            Label errorLabel = new Label("Error loading view: " + fxmlPath + "\n" + e.getMessage());
            contentArea.getChildren().setAll(errorLabel);
        }
    }
}
