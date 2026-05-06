package com.example.pi_dev.Controllers.Users;

import com.example.pi_dev.Session.Session;
import com.example.pi_dev.enums.RoleEnum;
import com.example.pi_dev.enums.TFAMethod;
import com.example.pi_dev.Entities.Users.User;
import com.example.pi_dev.Services.Users.UserService;
import com.example.pi_dev.Utils.Users.UserSession;
import com.example.pi_dev.common.services.ActivityLogService;
import com.github.sarxos.webcam.Webcam;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class TwoFactorController {

    @FXML
    private TextField codeField;

    @FXML
    private ImageView qrCodeImage;
    
    @FXML
    private VBox qrContainer;
    
    @FXML
    private VBox faceContainer;
    
    @FXML
    private VBox inputContainer;
    
    @FXML
    private ImageView webcamView;
    
    @FXML
    private Button captureButton;
    
    @FXML
    private Button verifyButton;
    
    @FXML
    private Label instructionLabel;

    @FXML
    private Label errorLabel;

    private final UserService userService = new UserService();
    private final ActivityLogService activityLogService = new ActivityLogService();
    private boolean isSetupMode = false;
    private String method = "QR"; // Default
    
    private Webcam webcam;
    private AtomicBoolean isWebcamRunning = new AtomicBoolean(false);
    private byte[] capturedImageBytes;

    // Called to initialize the view based on mode
    public void initData(boolean setupMode) {
        initData(setupMode, "QR");
    }

    public void initData(boolean setupMode, String method) {
        this.isSetupMode = setupMode;
        this.method = method;
        User user = UserSession.getInstance().getCurrentUser();
        
        // Reset UI state
        qrContainer.setVisible(false);
        qrContainer.setManaged(false);
        faceContainer.setVisible(false);
        faceContainer.setManaged(false);
        inputContainer.setVisible(false);
        inputContainer.setManaged(false);
        captureButton.setVisible(false);
        captureButton.setManaged(false);
        verifyButton.setVisible(true);
        verifyButton.setManaged(true);
        errorLabel.setText("");

        if (setupMode) {
            setupForEnable(user);
        } else {
            setupForVerify(user);
        }
    }
    
    private void setupForEnable(User user) {
        switch (method) {
            case "QR":
                instructionLabel.setText("Scan the QR code below with your Google Authenticator app.");
                qrContainer.setVisible(true);
                qrContainer.setManaged(true);
                inputContainer.setVisible(true);
                inputContainer.setManaged(true);
                
                try {
                    byte[] qrBytes = userService.setupTwoFactorQR(user.getUserId());
                    Image image = new Image(new ByteArrayInputStream(qrBytes));
                    qrCodeImage.setImage(image);
                } catch (Exception e) {
                    errorLabel.setText("Error generating QR code: " + e.getMessage());
                    e.printStackTrace();
                }
                break;
                
            case "EMAIL":
                instructionLabel.setText("A verification code has been sent to " + user.getEmail());
                inputContainer.setVisible(true);
                inputContainer.setManaged(true);
                try {
                    userService.setupTwoFactorEmail(user.getUserId());
                } catch (Exception e) {
                    errorLabel.setText("Error sending email: " + e.getMessage());
                    e.printStackTrace();
                }
                break;
                
            case "FACE":
                instructionLabel.setText("Position your face in the camera and click Capture.");
                faceContainer.setVisible(true);
                faceContainer.setManaged(true);
                captureButton.setVisible(true);
                captureButton.setManaged(true);
                verifyButton.setVisible(false); 
                verifyButton.setManaged(false); // Hide completely
                verifyButton.setText("Enable Face ID");
                verifyButton.setDisable(true);
                startWebcam();
                break;
        }
    }
    
    private void setupForVerify(User user) {
        TFAMethod userMethod = user.getTfaMethod();
        if (userMethod == null) userMethod = TFAMethod.QR; // Fallback
        
        // If verify mode, method might be passed in or we use user's method
        // But here we rely on user's stored method mainly, unless we want to force one?
        // Let's assume we use user's method.
        
        if (userMethod == TFAMethod.QR) {
            instructionLabel.setText("Enter the 6-digit code from your authenticator app.");
            inputContainer.setVisible(true);
            inputContainer.setManaged(true);
            this.method = "QR";
        } else if (userMethod == TFAMethod.EMAIL) {
            instructionLabel.setText("A verification code has been sent to " + user.getEmail());
            inputContainer.setVisible(true);
            inputContainer.setManaged(true);
            this.method = "EMAIL";
            try {
                userService.sendTwoFactorCodeEmail(user.getUserId());
            } catch (Exception e) {
                errorLabel.setText("Error sending email.");
            }
        } else if (userMethod == TFAMethod.FACE) {
            instructionLabel.setText("Position your face to verify.");
            faceContainer.setVisible(true);
            faceContainer.setManaged(true);
            captureButton.setVisible(true);
            captureButton.setManaged(true);
            captureButton.setText("Verify Face");
            verifyButton.setVisible(false); 
            verifyButton.setManaged(false);
            this.method = "FACE";
            startWebcam();
        }
    }

    private void startWebcam() {
        if (isWebcamRunning.get()) return;
        
        Thread thread = new Thread(() -> {
            try {
                webcam = Webcam.getDefault();
                if (webcam != null) {
                    webcam.open();
                    isWebcamRunning.set(true);
                    
                    while (isWebcamRunning.get()) {
                        BufferedImage bImage = webcam.getImage();
                        if (bImage != null) {
                            Platform.runLater(() -> {
                                Image image = SwingFXUtils.toFXImage(bImage, null);
                                webcamView.setImage(image);
                            });
                        }
                        try { Thread.sleep(50); } catch (InterruptedException e) { break; }
                    }
                } else {
                    Platform.runLater(() -> errorLabel.setText("No webcam found!"));
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> errorLabel.setText("Webcam error: " + e.getMessage()));
            }
        });
        thread.setDaemon(true);
        thread.start();
    }
    
    private void stopWebcam() {
        isWebcamRunning.set(false);
        if (webcam != null && webcam.isOpen()) {
            webcam.close();
        }
    }

    @FXML
    void handleCapture(ActionEvent event) {
        if (webcam != null && webcam.isOpen()) {
            BufferedImage bImage = webcam.getImage();
            if (bImage != null) {
                try {
                    // 1. Convert captured frame to PNG bytes
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(bImage, "png", baos);
                    capturedImageBytes = baos.toByteArray();
                    
                    // 2. Freeze the webcam preview with the captured image
                    Image image = SwingFXUtils.toFXImage(bImage, null);
                    webcamView.setImage(image);
                    stopWebcam();
                    
                    if (isSetupMode) {
                        // 3a. SETUP MODE: We are configuring Face ID.
                        // Enable the "Enable Face ID" button which will call HandleVerify to save this base image.
                        verifyButton.setVisible(true);
                        verifyButton.setManaged(true);
                        verifyButton.setDisable(false);
                        instructionLabel.setText("Face captured. Click Enable to save.");
                    } else {
                        // 3b. VERIFY MODE: We are logging in.
                        // Immediatly verify this capture against the saved base image.
                        verifyFaceLogin();
                    }
                    
                } catch (IOException e) {
                    e.printStackTrace();
                    errorLabel.setText("Failed to capture image.");
                }
            }
        }
    }
    
    private void verifyFaceLogin() {
        User user = UserSession.getInstance().getCurrentUser();
        // 4. Send this captured image to UserService to compare with the configured Face ID
        boolean isValid = userService.verifyTwoFactorFace(user.getUserId(), capturedImageBytes);
        
        if (isValid) {
             System.out.println("Face Verified!");
             activityLogService.log(user.getEmail(), "2FA_VERIFY_SUCCESS", "Face verification successful");
             navigateToHome(null); // Navigate based on role (Admin/User)
        } else {
            errorLabel.setText("Face verification failed. Try again.");
            startWebcam(); // Restart webcam to capture again
        }
    }

    @FXML
    void handleVerify(ActionEvent event) {
        User user = UserSession.getInstance().getCurrentUser();
        
        if (method.equals("FACE")) {
            // Note: In Verify (Login) mode, face is checked within handleCapture() via verifyFaceLogin().
            // This block applies to Setup mode when the user clicks "Enable Face ID".
            if (isSetupMode && capturedImageBytes != null) {
                try {
                    // 1. Save the captured image as the reference photo for DeepFace comparison later
                    userService.setupTwoFactorFace(user.getUserId(), capturedImageBytes);
                    // 2. Mark Face ID as the chosen 2FA method
                    userService.finalizeTwoFactorSetup(user.getUserId(), TFAMethod.FACE);
                    user.setTfaMethod(TFAMethod.FACE); 
                    
                    activityLogService.log(user.getEmail(), "2FA_SETUP", "Enabled Face ID 2FA");
                    navigateTo("/com/example/pi_dev/user/settings.fxml", event);
                } catch (Exception e) {
                    errorLabel.setText("Error saving face: " + e.getMessage());
                }
            }
            return;
        }
        
        // QR or EMAIL
        String codeStr = codeField.getText();
        if (codeStr.isEmpty()) {
            errorLabel.setText("Please enter the code.");
            return;
        }

        try {
            int code = Integer.parseInt(codeStr);
            boolean isValid = userService.verifyTwoFactor(user.getUserId(), code);
            
            if (isValid) {
                System.out.println("2FA Verified!");
                activityLogService.log(user.getEmail(), "2FA_VERIFY_SUCCESS", "Verified 2FA code (" + method + ")");
                
                if (isSetupMode) {
                     TFAMethod newMethod = method.equals("EMAIL") ? TFAMethod.EMAIL : TFAMethod.QR;
                     userService.finalizeTwoFactorSetup(user.getUserId(), newMethod);
                     user.setTfaMethod(newMethod); // Update local session
                     activityLogService.log(user.getEmail(), "2FA_SETUP", "Enabled " + newMethod + " 2FA");
                }

                navigateToHome(event);
            } else {
                errorLabel.setText("Invalid code. Please try again.");
            }
        } catch (NumberFormatException e) {
            errorLabel.setText("Code must be numbers only.");
        }
    }

    private void navigateToHome(ActionEvent event) {
        User user = UserSession.getInstance().getCurrentUser();
        
        // Ensure messaging session is initialized
        if (user != null) {
            Session.login(user.getUserId().toString());
        }

        if (isInsideHomeView(event)) {
            closeHomeOverlay(event);
            return;
        }

        String path = "/com/example/pi_dev/user/settings.fxml";
        if (user.getRole() == RoleEnum.ADMIN) {
            path = "/com/example/pi_dev/user/admin_dashboard.fxml";
        }
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Parent root = loader.load();
            Stage stage;
            
            if (event != null) {
                stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            } else {
                if (verifyButton.getScene() != null) {
                    stage = (Stage) verifyButton.getScene().getWindow();
                } else if (webcamView.getScene() != null) {
                    stage = (Stage) webcamView.getScene().getWindow();
                } else {
                    return;
                }
            }
            
            stage.getScene().setRoot(root);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isInsideHomeView(ActionEvent event) {
        Node source = null;
        if (event != null) {
            source = (Node) event.getSource();
        } else if (verifyButton.getScene() != null) {
            source = verifyButton;
        }

        if (source != null && source.getScene() != null && source.getScene().getRoot() instanceof javafx.scene.layout.BorderPane) {
            javafx.scene.layout.BorderPane root = (javafx.scene.layout.BorderPane) source.getScene().getRoot();
            return root.getCenter() instanceof javafx.scene.layout.StackPane && 
                   root.getCenter().getId() != null && 
                   root.getCenter().getId().equals("mainContentArea");
        }
        return false;
    }

    private void closeHomeOverlay(ActionEvent event) {
        // No overlay to close
    }

    @FXML
    void goBack(ActionEvent event) {
        stopWebcam();
        if (isInsideHomeView(event)) {
            closeHomeOverlay(event);
            return;
        }
        if (isSetupMode) {
            navigateTo("/com/example/pi_dev/user/settings.fxml", event);
        } else {
             navigateTo("/com/example/pi_dev/user/login.fxml", event);
             UserSession.getInstance().logout();
        }
    }
    
    private void navigateTo(String fxmlPath, ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) (event != null ? ((Node) event.getSource()).getScene().getWindow() : errorLabel.getScene().getWindow());
            stage.getScene().setRoot(root);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
