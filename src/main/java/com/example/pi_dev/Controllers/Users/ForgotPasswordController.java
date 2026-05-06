package com.example.pi_dev.Controllers.Users;

import com.example.pi_dev.Services.Users.UserService;
import com.example.pi_dev.common.services.ActivityLogService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class ForgotPasswordController {

    @FXML
    private TextField emailField;

    @FXML
    private TextField tokenField;

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private VBox emailContainer;

    @FXML
    private VBox resetContainer;

    @FXML
    private Label statusLabel;

    @FXML
    private Button actionButton;

    private final UserService userService = new UserService();
    private final ActivityLogService activityLogService = new ActivityLogService();
    private boolean isResetStage = false;

    @FXML
    void handleAction(ActionEvent event) {
        if (!isResetStage) {
            // Stage 1: Send Email
            String email = emailField.getText();
            if (email.isEmpty()) {
                statusLabel.setText("Please enter your email.");
                statusLabel.setStyle("-fx-text-fill: red;");
                return;
            }

            statusLabel.setText("Sending email...");
            statusLabel.setStyle("-fx-text-fill: blue;");
            
            // Run in background thread ideally, but for now:
            try {
                userService.initiatePasswordReset(email);
                
                // Switch to Stage 2
                isResetStage = true;
                emailContainer.setVisible(false);
                emailContainer.setManaged(false);
                resetContainer.setVisible(true);
                resetContainer.setManaged(true);
                actionButton.setText("Reset Password");
                statusLabel.setText("Email sent! Check your inbox for the code.");
                statusLabel.setStyle("-fx-text-fill: green;");
                
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : e.toString();
                statusLabel.setText("Email could not be sent. See message below.");
                statusLabel.setStyle("-fx-text-fill: red;");
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Email not sent");
                alert.setHeaderText("Could not send password reset email");
                alert.setContentText(msg);
                alert.setResizable(true);
                alert.getDialogPane().setPrefWidth(500);
                alert.showAndWait();
            }
        } else {
            // Stage 2: Reset Password
            String token = tokenField.getText();
            String newPass = newPasswordField.getText();
            
            if (token.isEmpty() || newPass.isEmpty()) {
                statusLabel.setText("Please fill all fields.");
                statusLabel.setStyle("-fx-text-fill: red;");
                return;
            }

            boolean success = userService.resetPassword(token, newPass);
            if (success) {
                activityLogService.log(emailField.getText(), "PASSWORD_RESET_FORGOT", "User reset password via forgot password flow");
                statusLabel.setText("Password reset successfully! Redirecting...");
                statusLabel.setStyle("-fx-text-fill: green;");
                // Navigate back to login after short delay or immediately
                goBack(event);
            } else {
                statusLabel.setText("Invalid code or error resetting password.");
                statusLabel.setStyle("-fx-text-fill: red;");
            }
        }
    }

    @FXML
    void goBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/example/pi_dev/user/login.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}