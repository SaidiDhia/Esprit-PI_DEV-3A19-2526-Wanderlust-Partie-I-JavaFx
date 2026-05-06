package com.example.pi_dev.BlogGUI;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import javafx.stage.Screen;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pi_dev/BlogView.fxml"));
            Parent root = loader.load();

            // Get screen dimensions for proper fullscreen sizing
            double screenWidth  = Screen.getPrimary().getVisualBounds().getWidth();
            double screenHeight = Screen.getPrimary().getVisualBounds().getHeight();

            Scene scene = new Scene(root, screenWidth, screenHeight);
            primaryStage.setTitle("Blog");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);

            // Start maximized (fills screen without hiding taskbar)
            primaryStage.setMaximized(true);

            // F11 toggles true fullscreen (hides taskbar)
            scene.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
                if (e.getCode() == KeyCode.F11) {
                    primaryStage.setFullScreen(!primaryStage.isFullScreen());
                }
                // ESC exits fullscreen (JavaFX does this by default, but make it explicit)
                if (e.getCode() == KeyCode.ESCAPE && primaryStage.isFullScreen()) {
                    primaryStage.setFullScreen(false);
                }
            });

            // Hide the "Press ESC to exit fullscreen" overlay message
            primaryStage.setFullScreenExitHint("");

            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}