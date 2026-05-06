package com.example.pi_dev.Controllers.Users;

import com.example.pi_dev.Controllers.Main.MainLayoutController;
import com.example.pi_dev.enums.RoleEnum;
import com.example.pi_dev.Entities.Users.User;
import com.example.pi_dev.Services.Users.UserService;
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

import java.io.IOException;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public class SettingsController {

    @FXML
    private ImageView profileImageView;

    @FXML
    private TextField fullNameField;

    @FXML
    private TextField emailField;

    @FXML
    private TextField phoneField;

    @FXML
    private PasswordField currentPasswordField;

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label passwordErrorLabel;

    @FXML
    private Label tfaStatusLabel;

    private final UserService userService = new UserService();
    private final ActivityLogService activityLogService = new ActivityLogService();
    private User currentUser;

    @FXML
    public void initialize() {
        currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser != null) {
            fullNameField.setText(currentUser.getFullName());
            emailField.setText(currentUser.getEmail());
            phoneField.setText(currentUser.getPhoneNumber());
            loadProfilePicture();
            updateTfaStatus();
        }
    }

    private void loadProfilePicture() {
        String photoPath = currentUser.getProfilePicture();
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
            profileImageView.setImage(image);
        } else {
            // If even the default is missing, just set it to null to avoid NPE
            profileImageView.setImage(null);
        }
    }

    @FXML
    void handleUploadProfilePic(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Picture");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        File selectedFile = fileChooser.showOpenDialog(((Node) event.getSource()).getScene().getWindow());

        if (selectedFile != null) {
            try {
                File uploadDir = new File("uploads/profiles");
                if (!uploadDir.exists()) uploadDir.mkdirs();

                String fileName = UUID.randomUUID().toString() + "_" + selectedFile.getName();
                File destFile = new File(uploadDir, fileName);
                Files.copy(selectedFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                currentUser.setProfilePicture(fileName);
                userService.updateUser(currentUser);
                activityLogService.log(currentUser.getEmail(), "PROFILE_PIC_UPDATE", "Updated profile picture");
                profileImageView.setImage(new Image(destFile.toURI().toString()));
                System.out.println("Profile picture updated!");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void updateTfaStatus() {
        boolean isTfaEnabled = currentUser.getTfaMethod() != null;
        if (isTfaEnabled) {
            tfaStatusLabel.setText("ON (" + currentUser.getTfaMethod() + ")");
            tfaStatusLabel.getStyleClass().clear();
            tfaStatusLabel.getStyleClass().add("badge-tfa-on");
            tfaStatusLabel.setStyle(""); // remove inline styling 
        } else {
            tfaStatusLabel.setText("OFF");
            tfaStatusLabel.getStyleClass().clear();
            tfaStatusLabel.getStyleClass().add("badge-tfa-on");
            tfaStatusLabel.setStyle("-fx-background-color: #EF4444;"); // Error RED
        }
    }

    @FXML
    void handleUpdateProfile(ActionEvent event) {
        if (currentUser != null) {
            currentUser.setFullName(fullNameField.getText());
            currentUser.setPhoneNumber(phoneField.getText());
            
            try {
                userService.updateUser(currentUser);
                activityLogService.log(currentUser.getEmail(), "PROFILE_UPDATE", "Updated personal information");
                System.out.println("Profile updated!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    void handleChangePassword(ActionEvent event) {
        String currentPass = currentPasswordField.getText();
        String newPass = newPasswordField.getText();
        String confirmPass = confirmPasswordField.getText();
        
        if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            passwordErrorLabel.setText("All fields are required.");
            passwordErrorLabel.setStyle("-fx-text-fill: red;");
            return;
        }
        
        if (!newPass.equals(confirmPass)) {
            passwordErrorLabel.setText("New passwords do not match.");
            passwordErrorLabel.setStyle("-fx-text-fill: red;");
            return;
        }
        
        if (newPass.length() < 6) {
             passwordErrorLabel.setText("Password must be at least 6 characters.");
             passwordErrorLabel.setStyle("-fx-text-fill: red;");
             return;
        }

        boolean success = userService.changePassword(currentUser.getUserId(), currentPass, newPass);
        
        if (success) {
            activityLogService.log(currentUser.getEmail(), "PASSWORD_CHANGE", "User changed their password");
            passwordErrorLabel.setText("Password updated successfully! 2FA disabled.");
            passwordErrorLabel.setStyle("-fx-text-fill: green;");
            currentPasswordField.clear();
            newPasswordField.clear();
            confirmPasswordField.clear();
            // Refresh user to get updated fields if any
            currentUser = userService.getUserById(currentUser.getUserId());
            updateTfaStatus(); // Because we disabled 2FA
        } else {
            passwordErrorLabel.setText("Incorrect current password.");
            passwordErrorLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    void handleEnableApp2FA(ActionEvent event) {
        goTo2FA(event, "QR");
    }

    @FXML
    void handleEnableEmail2FA(ActionEvent event) {
        goTo2FA(event, "EMAIL");
    }

    @FXML
    void handleEnableFace2FA(ActionEvent event) {
        goTo2FA(event, "FACE");
    }

    @FXML
    void handleDisable2FA(ActionEvent event) {
        if (currentUser != null) {
            currentUser.setTfaMethod(null);
            userService.updateUser(currentUser);
            activityLogService.log(currentUser.getEmail(), "2FA_DISABLE", "Disabled two-factor authentication");
            updateTfaStatus();
            System.out.println("2FA Disabled");
        }
    }

    private void goTo2FA(ActionEvent event, String method) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pi_dev/user/2fa.fxml"));
            Parent root = loader.load();
            
            TwoFactorController controller = loader.getController();
            controller.initData(true, method); // Setup Mode with Method
            
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @FXML
    void handleBack(ActionEvent event) {
        User user = UserSession.getInstance().getCurrentUser();
        if (user != null && user.getRole() == RoleEnum.ADMIN) {
            navigateTo("/com/example/pi_dev/user/admin_dashboard.fxml", event);
        } else {
            navigateTo("/com/example/pi_dev/main/main_layout.fxml", event);
        }
    }

    @FXML
    void handleLogout(ActionEvent event) {
        String email = currentUser != null ? currentUser.getEmail() : "Unknown";
        activityLogService.log(email, "SIGNOUT", "User logged out");
        UserSession.getInstance().logout();
        
        if (isInsideMainLayout(event)) {
            notifyMainLayoutLogout(event);
        } else {
            navigateTo("/com/example/pi_dev/user/login.fxml", event);
        }
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
        }
    }

    private boolean isInsideMainLayout(ActionEvent event) {
        Node source = (Node) event.getSource();
        Scene scene = source.getScene();
        if (scene != null && scene.getRoot() instanceof javafx.scene.layout.BorderPane) {
            javafx.scene.layout.BorderPane root = (javafx.scene.layout.BorderPane) scene.getRoot();
            return root.getCenter() instanceof javafx.scene.layout.StackPane && 
                   root.getCenter().getId() != null && 
                   root.getCenter().getId().equals("contentArea");
        }
        return false;
    }

    private void replaceInMainLayout(Parent root, ActionEvent event) {
        Node source = (Node) event.getSource();
        javafx.scene.layout.StackPane contentArea = (javafx.scene.layout.StackPane) source.getScene().lookup("#contentArea");
        if (contentArea != null) {
            contentArea.getChildren().setAll(root);
        }
    }

    private void notifyMainLayoutLogout(ActionEvent event) {
        Node source = (Node) event.getSource();
        Scene scene = source.getScene();
        if (scene != null && scene.getRoot() instanceof javafx.scene.layout.BorderPane) {
            javafx.scene.layout.BorderPane root = (javafx.scene.layout.BorderPane) scene.getRoot();
            Object controller = root.getUserData();
            if (controller instanceof MainLayoutController) {
                ((MainLayoutController) controller).updateUserInfo();
                try {
                    Parent loginRoot = FXMLLoader.load(getClass().getResource("/com/example/pi_dev/user/login.fxml"));
                    replaceInMainLayout(loginRoot, event);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
