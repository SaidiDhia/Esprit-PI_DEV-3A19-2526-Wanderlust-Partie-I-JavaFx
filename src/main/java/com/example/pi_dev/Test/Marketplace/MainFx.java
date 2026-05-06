package com.example.pi_dev.Test.Marketplace;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class MainFx extends Application {

    private static Stage mainStage;
    private static Scene mainScene;
    private static BorderPane rootPane;

    // NEW: For integration with main app
    private static Pane integrationContentArea;
    private static boolean isIntegrated = false;

    @Override
    public void start(Stage primaryStage) throws Exception {

        Parent firstPage = FXMLLoader.load(getClass().getResource("/com/example/pi_dev/marketplace/fxml/RoleSelection.fxml"));

        rootPane = new BorderPane();
        rootPane.setCenter(firstPage);

        mainScene = new Scene(rootPane, 900, 650);

        mainStage = primaryStage;
        mainStage.setTitle("Marketplace App");
        mainStage.setScene(mainScene);
        mainStage.setMinWidth(900);
        mainStage.setMinHeight(650);
        mainStage.show();
    }

    /**
     * NEW: Initialize for integration with main app
     * Call this from your MainLayoutController before loading marketplace FXML
     */
    public static void initForIntegration(Pane contentArea) {
        integrationContentArea = contentArea;
        isIntegrated = true;
        // Create a regular BorderPane (no override needed)
        rootPane = new BorderPane();
        System.out.println("✅ MainFx initialized for integration");
    }

    /**
     * Set center directly with an already-loaded Parent node.
     */
    public static void setCenter(Parent node) {
        if (isIntegrated && integrationContentArea != null) {
            // In integrated mode, redirect to main app's content area
            integrationContentArea.getChildren().setAll(node);
        } else if (rootPane != null) {
            // Standalone mode
            rootPane.setCenter(node);
        } else {
            System.err.println("❌ MainFx.rootPane is null - cannot set center");
        }
    }

    /**
     * Set center by FXML path — always reloads fresh.
     */
    public static void setCenter(String fxmlPath) {
        try {
            Parent page = FXMLLoader.load(MainFx.class.getResource(fxmlPath));
            setCenter(page);
        } catch (Exception e) {
            System.err.println("Failed to load FXML: " + fxmlPath);
            e.printStackTrace();
        }
    }

    public static Stage getMainStage() {
        return mainStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}