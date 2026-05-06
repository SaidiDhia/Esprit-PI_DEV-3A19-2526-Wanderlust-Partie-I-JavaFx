package com.example.pi_dev;

import com.example.pi_dev.Database.Messaging.DatabaseConnection;
import com.example.pi_dev.Session.Session;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MessagingMain extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        DatabaseConnection.getInstance();
        Session.login("123e4567-e89b-12d3-a456-426614174000"); //na77iha ta nwalli ena the other guy lanci ta tefhem
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/example/pi_dev/messagingchat.fxml")
        );

        //concept ta scene hen houa esmo 3le jesmo houa lppearence ta lapp
        Scene scene = new Scene(loader.load(), 800, 600);
        scene.getStylesheets().add(//this is for the css I added
                getClass().getResource("/com/example/pi_dev/messagingchat.css").toExternalForm()
        );

        stage.setTitle("WonderLust - Messaging");

        stage.setScene(scene);

        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
