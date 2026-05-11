package com.example.pi_dev.Controllers.Users;

import com.example.pi_dev.Controllers.Main.MainLayoutController;
import com.example.pi_dev.Entities.Users.User;
import com.example.pi_dev.Services.Users.UserService;
import com.example.pi_dev.Session.Session;
import com.example.pi_dev.Utils.Users.UserSession;
import com.example.pi_dev.common.services.ActivityLogService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class LoginController implements javafx.fxml.Initializable {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    private UserService userService;
    private ActivityLogService activityLogService;

    @Override
    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        try {
            this.userService = new UserService();
            this.activityLogService = new ActivityLogService();
        } catch (Exception e) {
            e.printStackTrace();
            if (errorLabel != null) {
                errorLabel.setText("Initialization error: " + e.getMessage());
            }
        }
    }

    @FXML
    void handleLogin(ActionEvent event) {
        String email = emailField.getText();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please fill in all fields");
            return;
        }

        String token = userService.login(email, password);

        if (token != null) {
            // Login success
            try {
                // Fetch full user details
                User user = userService.getAllUsers().stream()
                        .filter(u -> u.getEmail().equals(email))
                        .findFirst()
                        .orElseThrow();

                UserSession.getInstance().login(user, token);
                Session.login(user.getUserId().toString());
                activityLogService.log(user.getEmail(), "SIGNIN", "User logged in: " + user.getFullName());

                System.out.println("Login successful!");
                System.out.println("JWT Token: " + token);

                // Check for 2FA
                if (userService.shouldRequireTwoFactor(user.getUserId())) {
                    // Navigate to 2FA Verify
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pi_dev/user/2fa.fxml"));
                    Parent root = loader.load();

                    TwoFactorController controller = loader.getController();
                    controller.initData(false); // Verify Mode (auto-detects method)

                    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                    stage.getScene().setRoot(root);
                    stage.show();
                } else {
                    // Navigate to the common main layout or notify parent
                    if (isInsideMainLayout(event)) {
                        notifyMainLayoutSuccess(event);
                    } else {
                        try {
                            // ── If ADMIN → go directly to Admin Dashboard ──
                            boolean isAdmin = user.getRole() != null &&
                                user.getRole().name().equalsIgnoreCase("ADMIN");

                            String path = isAdmin
                                ? "/com/example/pi_dev/events/ApprovalDashboard.fxml"
                                : "/com/example/pi_dev/main/main_layout.fxml";

                            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
                            Parent root = loader.load();
                            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                            stage.setScene(new Scene(root, 1300, 820));
                            stage.setMaximized(true);
                            stage.show();
                        } catch (IOException e) {
                            e.printStackTrace();
                            errorLabel.setText("Navigation error: " + e.getMessage());
                        }
                    }
                }

            } catch (Exception e) {
                errorLabel.setText("Login failed: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            errorLabel.setText("Invalid credentials");
        }
    }

    @FXML
    void goToSignup(ActionEvent event) {
        navigateTo("/com/example/pi_dev/user/signup.fxml", event);
    }

    @FXML
    void goToForgotPassword(ActionEvent event) {
        navigateTo("/com/example/pi_dev/user/forgot_password.fxml", event);
    }

    @FXML
    void goToMainHome(MouseEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/example/pi_dev/main/main_layout.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1200, 800));
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            errorLabel.setText("Navigation error: " + e.getMessage());
        }
    }

    @FXML
    void goToFaceLogin(ActionEvent event) {
        navigateTo("/com/example/pi_dev/user/face_login.fxml", event);
    }

    private void navigateTo(String fxmlPath, ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            if (isInsideMainLayout(event)) {
                replaceInMainLayout(root, event);
            } else {
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            errorLabel.setText("Navigation error: " + e.getMessage());
        }
    }

    private boolean isInsideMainLayout(ActionEvent event) {
        Node source = (Node) event.getSource();
        Scene scene = source.getScene();
        if (scene != null && scene.getRoot() instanceof javafx.scene.layout.BorderPane) {
            javafx.scene.layout.BorderPane root = (javafx.scene.layout.BorderPane) scene.getRoot();
            return root.getCenter() instanceof StackPane &&
                    root.getCenter().getId() != null &&
                    root.getCenter().getId().equals("contentArea");
        }
        return false;
    }

    private void replaceInMainLayout(Parent root, ActionEvent event) {
        Node source = (Node) event.getSource();
        StackPane contentArea = (StackPane) source.getScene().lookup("#contentArea");
        if (contentArea != null) {
            contentArea.getChildren().setAll(root);
        }
    }

    private void notifyMainLayoutSuccess(ActionEvent event) {
        Node source = (Node) event.getSource();
        Scene scene = source.getScene();
        if (scene != null && scene.getRoot() instanceof javafx.scene.layout.BorderPane) {
            javafx.scene.layout.BorderPane root = (javafx.scene.layout.BorderPane) scene.getRoot();
            // Since MainLayoutController is the controller of the root BorderPane
            Object controller = root.getUserData(); // We should set this in MainLayoutController
            if (controller instanceof MainLayoutController) {
                ((MainLayoutController) controller).onLoginSuccess();
            } else {
                // Fallback: just reload the layout
                try {
                    FXMLLoader loader = new FXMLLoader(
                            getClass().getResource("/com/example/pi_dev/main/main_layout.fxml"));
                    scene.setRoot(loader.load());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
