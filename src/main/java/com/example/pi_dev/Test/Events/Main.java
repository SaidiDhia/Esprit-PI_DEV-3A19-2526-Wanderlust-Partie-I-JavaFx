package com.example.pi_dev.Test.Events;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Parent;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pi_dev/events/Catalogue.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root);

            primaryStage.setTitle("Wanderlust - Catalogue");
            primaryStage.setScene(scene);
            primaryStage.setWidth(1400);
            primaryStage.setHeight(900);
            primaryStage.setMinWidth(1200);
            primaryStage.setMinHeight(700);
            primaryStage.centerOnScreen();
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}