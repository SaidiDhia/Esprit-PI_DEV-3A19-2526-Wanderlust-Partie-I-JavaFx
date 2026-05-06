package com.example.pi_dev.Controllers.Users;

import com.example.pi_dev.enums.RoleEnum;
import com.example.pi_dev.Entities.Users.User;
import com.example.pi_dev.common.services.ActivityLogService;
import com.example.pi_dev.Services.Users.UserService;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.io.IOException;
import java.util.UUID;

import javafx.scene.shape.Circle;
import javafx.scene.image.Image;
import javafx.scene.paint.ImagePattern;
import javafx.stage.FileChooser;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class SignupController {

    @FXML
    private Circle profileCircle;

    @FXML
    private TextField fullNameField;

    @FXML
    private TextField emailField;

    @FXML
    private TextField phoneField;

    @FXML
    private ComboBox<RoleEnum> roleComboBox;

    @FXML
    private PasswordField passwordField;

    @FXML
    private ProgressBar strengthBar;

    @FXML
    private Label strengthLabel;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label fullNameError;
    @FXML
    private Label emailError;
    @FXML
    private Label phoneError;
    @FXML
    private Label passwordError;
    @FXML
    private Label confirmPasswordError;

    @FXML
    private Label errorLabel;

    private final UserService userService = new UserService();
    private final ActivityLogService activityLogService = new ActivityLogService();
    private File selectedImageFile;

    @FXML
    public void initialize() {
        // Filter out ADMIN from roles
        java.util.List<RoleEnum> filteredRoles = new java.util.ArrayList<>();
        for (RoleEnum role : RoleEnum.values()) {
            if (role != RoleEnum.ADMIN) {
                filteredRoles.add(role);
            }
        }
        roleComboBox.setItems(FXCollections.observableArrayList(filteredRoles));
        roleComboBox.getSelectionModel().select(RoleEnum.PARTICIPANT); // Default

        // Password strength listener
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
            updatePasswordStrength(newVal);
        });
    }

    private void updatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            animateStrengthBar(0);
            strengthBar.setStyle("-fx-accent: #E5E7EB;");
            strengthLabel.setText("Strength: None");
            strengthLabel.setStyle("-fx-text-fill: #6B7280;");
            return;
        }

        double strength = 0;
        int criteriaMet = 0;

        if (password.length() >= 8)
            criteriaMet++;
        if (password.matches(".*[a-z].*"))
            criteriaMet++;
        if (password.matches(".*[A-Z].*"))
            criteriaMet++;
        if (password.matches(".*[0-9].*"))
            criteriaMet++;
        if (password.matches(".*[!@#$%^&*(),.?\":{}|<>].*"))
            criteriaMet++;

        strength = criteriaMet / 5.0;
        animateStrengthBar(strength);

        // Clear existing style classes
        strengthBar.getStyleClass().removeAll("very-weak", "weak", "medium", "good", "strong");

        // Update colors and labels based on progress
        if (strength <= 0.2) {
            strengthBar.getStyleClass().add("very-weak");
            strengthBar.setStyle("-fx-accent: #EF4444;");
            strengthLabel.setText("Strength: Very Weak");
            strengthLabel.setStyle("-fx-text-fill: #EF4444;");
        } else if (strength <= 0.4) {
            strengthBar.getStyleClass().add("weak");
            strengthBar.setStyle("-fx-accent: #F97316;");
            strengthLabel.setText("Strength: Weak");
            strengthLabel.setStyle("-fx-text-fill: #F97316;");
        } else if (strength <= 0.6) {
            strengthBar.getStyleClass().add("medium");
            strengthBar.setStyle("-fx-accent: #F59E0B;");
            strengthLabel.setText("Strength: Medium");
            strengthLabel.setStyle("-fx-text-fill: #F59E0B;");
        } else if (strength <= 0.8) {
            strengthBar.getStyleClass().add("good");
            strengthBar.setStyle("-fx-accent: #84CC16;");
            strengthLabel.setText("Strength: Good");
            strengthLabel.setStyle("-fx-text-fill: #84CC16;");
        } else {
            strengthBar.getStyleClass().add("strong");
            strengthBar.setStyle("-fx-accent: #10B981;");
            strengthLabel.setText("Strength: Strong");
            strengthLabel.setStyle("-fx-text-fill: #10B981;");
        }
    }

    private void animateStrengthBar(double targetValue) {
        Timeline timeline = new Timeline();
        KeyValue keyValue = new KeyValue(strengthBar.progressProperty(), targetValue);
        KeyFrame keyFrame = new KeyFrame(Duration.millis(300), keyValue);
        timeline.getKeyFrames().add(keyFrame);
        timeline.play();
    }

    @FXML
    void handleUploadPhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Picture");
        fileChooser.getExtensionFilters()
                .add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));

        File file = fileChooser.showOpenDialog(profileCircle.getScene().getWindow());
        if (file != null) {
            selectedImageFile = file;
            Image image = new Image(file.toURI().toString());
            profileCircle.setFill(new ImagePattern(image));
        }
    }

    @FXML
    void handleSignup(ActionEvent event) {
        if (!validateInput())
            return;

        User newUser = new User();
        newUser.setUserId(UUID.randomUUID());
        newUser.setFullName(fullNameField.getText());
        newUser.setEmail(emailField.getText());
        newUser.setPhoneNumber(phoneField.getText());
        newUser.setRole(roleComboBox.getValue());
        newUser.setPasswordHash(passwordField.getText());
        newUser.setIsActive(true); // Default active

        // Handle Profile Picture
        if (selectedImageFile != null) {
            try {
                File uploadDir = new File("uploads/profiles");
                if (!uploadDir.exists())
                    uploadDir.mkdirs();

                String fileName = UUID.randomUUID().toString() + "_" + selectedImageFile.getName();
                File destFile = new File(uploadDir, fileName);
                Files.copy(selectedImageFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                newUser.setProfilePicture(fileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            userService.register(newUser);
            activityLogService.log(newUser.getEmail(), "SIGNUP", "New user registered: " + newUser.getFullName());
            // Navigate to Login
            if (isInsideMainLayout(event)) {
                Parent root = FXMLLoader.load(getClass().getResource("/com/example/pi_dev/user/login.fxml"));
                replaceInMainLayout(root, event);
            } else {
                goToLogin(event);
            }
        } catch (IllegalArgumentException e) {
            String message = e.getMessage() != null ? e.getMessage() : "Registration failed";
            if (message.toLowerCase().contains("email")) {
                showError(emailError, message);
            } else {
                errorLabel.setText("Registration failed: " + message);
            }
        } catch (Exception e) {
            errorLabel.setText("Registration failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void clearErrors() {
        fullNameError.setVisible(false);
        fullNameError.setManaged(false);
        emailError.setVisible(false);
        emailError.setManaged(false);
        phoneError.setVisible(false);
        phoneError.setManaged(false);
        passwordError.setVisible(false);
        passwordError.setManaged(false);
        confirmPasswordError.setVisible(false);
        confirmPasswordError.setManaged(false);
        errorLabel.setText("");
    }

    private void showError(Label errorNode, String message) {
        errorNode.setText(message);
        errorNode.setVisible(true);
        errorNode.setManaged(true);
    }

    private boolean validateInput() {
        clearErrors();
        boolean isValid = true;

        String fullName = fullNameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (fullName.isEmpty()) {
            showError(fullNameError, "Full Name is required");
            isValid = false;
        } else if (fullName.length() < 3) {
            showError(fullNameError, "Full Name must be at least 3 characters");
            isValid = false;
        }

        if (email.isEmpty()) {
            showError(emailError, "Email is required");
            isValid = false;
        } else if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showError(emailError, "Please enter a valid email address");
            isValid = false;
        }

        if (phone.isEmpty()) {
            showError(phoneError, "Phone Number is required");
            isValid = false;
        } else if (!phone.matches("^\\+?[0-9]{8,15}$")) {
            showError(phoneError, "Please enter a valid phone number (8-15 digits)");
            isValid = false;
        }

        if (password.isEmpty()) {
            showError(passwordError, "Password is required");
            isValid = false;
        } else if (password.length() < 6) {
            showError(passwordError, "Password must be at least 6 characters long");
            isValid = false;
        } else if (!password.matches(".*[a-zA-Z].*") || !password.matches(".*[0-9].*")) {
            showError(passwordError, "Password must contain at least one letter and one number");
            isValid = false;
        } else if (strengthLabel.getText().contains("Weak")) {
            showError(passwordError, "Password is too weak. Please use a stronger password.");
            isValid = false;
        }

        if (confirmPassword.isEmpty()) {
            showError(confirmPasswordError, "Please confirm your password");
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            showError(confirmPasswordError, "Passwords do not match");
            isValid = false;
        }

        return isValid;
    }

    @FXML
    void goToLogin(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/example/pi_dev/user/login.fxml"));
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
        javafx.scene.layout.StackPane contentArea = (javafx.scene.layout.StackPane) source.getScene()
                .lookup("#contentArea");
        if (contentArea != null) {
            contentArea.getChildren().setAll(root);
        }
    }

}
