package com.example.pi_dev;

import com.example.pi_dev.Database.Users.UserDatabaseConnection;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;

public class UserMain extends Application {
    @Override
    public void start(Stage stage) throws IOException, SQLException {
        UserDatabaseConnection.getInstance();
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/example/pi_dev/main/main_layout.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1200, 800);
        stage.setTitle("WonderLust");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
