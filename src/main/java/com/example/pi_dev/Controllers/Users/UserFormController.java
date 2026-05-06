package com.example.pi_dev.Controllers.Users;

import com.example.pi_dev.enums.RoleEnum;
import com.example.pi_dev.Entities.Users.User;
import com.example.pi_dev.Services.Users.UserService;
import com.example.pi_dev.Utils.Users.UserSession;
import com.example.pi_dev.common.services.ActivityLogService;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.util.UUID;

import javafx.scene.shape.Circle;
import javafx.scene.image.Image;
import javafx.scene.paint.ImagePattern;
import javafx.stage.FileChooser;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class UserFormController {

    @FXML private Circle profileCircle;
    @FXML private Label titleLabel;
    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<RoleEnum> roleComboBox;
    @FXML private CheckBox isActiveCheckBox;
    @FXML private Label errorLabel;

    private UserService userService;
    private final ActivityLogService activityLogService = new ActivityLogService();
    private User user;
    private boolean isEditMode = false;
    private Runnable onSaveCallback;
    private File selectedImageFile;

    public void setService(UserService userService) {
        this.userService = userService;
    }
    
    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }

    @FXML
    public void initialize() {
        roleComboBox.setItems(FXCollections.observableArrayList(RoleEnum.values()));
        roleComboBox.getSelectionModel().select(RoleEnum.PARTICIPANT);
        isActiveCheckBox.setSelected(true);
    }

    public void setUser(User user) {
        this.user = user;
        if (user != null) {
            isEditMode = true;
            titleLabel.setText("Edit User");
            fullNameField.setText(user.getFullName());
            emailField.setText(user.getEmail());
            phoneField.setText(user.getPhoneNumber());
            roleComboBox.getSelectionModel().select(user.getRole());
            isActiveCheckBox.setSelected(user.getIsActive() != null ? user.getIsActive() : true);
            
            if (user.getProfilePicture() != null && !user.getProfilePicture().isEmpty()) {
                try {
                    String photoPath = user.getProfilePicture();
                    File file = new File("uploads/profiles/" + photoPath);
                    if (file.exists()) {
                        profileCircle.setFill(new ImagePattern(new Image(file.toURI().toString())));
                    } else {
                        // Try direct path in case it was stored as full path before
                        File directFile = new File(photoPath);
                        if (directFile.exists()) {
                            profileCircle.setFill(new ImagePattern(new Image(directFile.toURI().toString())));
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Failed to load profile picture: " + user.getProfilePicture());
                }
            }
        } else {
            isEditMode = false;
            titleLabel.setText("Add New User");
            this.user = new User();
        }
    }

    @FXML
    void handleUploadPhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Picture");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        
        File file = fileChooser.showOpenDialog(profileCircle.getScene().getWindow());
        if (file != null) {
            selectedImageFile = file;
            Image image = new Image(file.toURI().toString());
            profileCircle.setFill(new ImagePattern(image));
        }
    }

    @FXML
    void handleSave(ActionEvent event) {
        String fullName = fullNameField.getText();
        String email = emailField.getText();
        String phone = phoneField.getText();
        String password = passwordField.getText();
        RoleEnum role = roleComboBox.getValue();
        boolean isActive = isActiveCheckBox.isSelected();

        if (fullName.isEmpty() || email.isEmpty()) {
            errorLabel.setText("Name and Email are required.");
            return;
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            errorLabel.setText("Please enter a valid email address");
            return;
        }

        if (!phone.isEmpty() && !phone.matches("^\\+?[0-9]{8,15}$")) {
            errorLabel.setText("Please enter a valid phone number (8-15 digits)");
            return;
        }

        if (!isEditMode && password.isEmpty()) {
            errorLabel.setText("Password is required for new users.");
            return;
        }

        if (!password.isEmpty() && password.length() < 6) {
            errorLabel.setText("Password must be at least 6 characters long");
            return;
        }

        try {
            user.setFullName(fullName);
            user.setEmail(email);
            user.setPhoneNumber(phone);
            user.setRole(role);
            user.setIsActive(isActive);

            // Handle Profile Picture
            if (selectedImageFile != null) {
                File uploadDir = new File("uploads/profiles");
                if (!uploadDir.exists()) uploadDir.mkdirs();
                
                String fileName = UUID.randomUUID().toString() + "_" + selectedImageFile.getName();
                File destFile = new File(uploadDir, fileName);
                Files.copy(selectedImageFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                user.setProfilePicture(fileName);
            }

            if (isEditMode) {
                userService.updateUser(user); 
                activityLogService.log(UserSession.getInstance().getCurrentUser().getEmail(), "USER_UPDATE", "Updated user: " + user.getEmail());
                if (!password.isEmpty()) {
                     userService.adminUpdatePassword(user.getUserId(), password);
                     activityLogService.log(UserSession.getInstance().getCurrentUser().getEmail(), "USER_PASSWORD_RESET", "Reset password for user: " + user.getEmail());
                }
            } else {
                user.setUserId(UUID.randomUUID());
                user.setPasswordHash(password); // Register will hash it
                user.setCreatedAt(LocalDateTime.now());
                userService.register(user);
                activityLogService.log(UserSession.getInstance().getCurrentUser().getEmail(), "USER_CREATE", "Created new user: " + user.getEmail());
            }

            if (onSaveCallback != null) {
                onSaveCallback.run();
            }
            closeStage();
        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Error saving user: " + e.getMessage());
        }
    }

    @FXML
    void handleCancel(ActionEvent event) {
        closeStage();
    }

    private void closeStage() {
        Stage stage = (Stage) fullNameField.getScene().getWindow();
        stage.close();
    }
}