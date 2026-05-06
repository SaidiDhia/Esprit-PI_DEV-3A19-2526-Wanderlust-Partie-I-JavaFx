package com.example.pi_dev.Controllers.Messaging;

import com.example.pi_dev.Entities.Messaging.Message;
import com.example.pi_dev.Session.Session;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.io.File;

public class ImageMessageCell extends ListCell<Message> {

    @Override
    protected void updateItem(Message msg, boolean empty) {
        super.updateItem(msg, empty);

        if (empty || msg == null) {
            setGraphic(null);
            return;
        }

        boolean isMine = msg.getSenderId().equals(Session.getCurrentUserId());

        // Create container
        HBox container = new HBox(10);
        container.setPadding(new Insets(5));
        container.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        // Message bubble
        VBox bubble = new VBox(5);
        bubble.setMaxWidth(300);

        // Sender name and time
        Label header = new Label(
                (msg.getSenderName() != null ? msg.getSenderName() : "User " + msg.getSenderId()) +
                        " • " + msg.getCreatedAt().toLocalTime().withNano(0)
        );
        header.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");

        bubble.getChildren().add(header);

        // Image content
        if (msg.isImage() && msg.getFileUrl() != null) {
            try {
                ImageView imageView = new ImageView();
                Image image = new Image(new File(msg.getFileUrl()).toURI().toString());
                imageView.setImage(image);

                // Scale image to fit
                imageView.setFitWidth(250);
                imageView.setPreserveRatio(true);
                imageView.setStyle("-fx-cursor: hand;");

                // Add click to enlarge
                imageView.setOnMouseClicked(e -> showFullImage(msg.getFileUrl()));

                bubble.getChildren().add(imageView);

                // Add image info
                Label info = new Label("📷 Image" +
                        (msg.getFileSize() > 0 ? " • " + formatFileSize(msg.getFileSize()) : ""));
                info.setStyle("-fx-font-size: 10px; -fx-text-fill: #999;");
                bubble.getChildren().add(info);

            } catch (Exception e) {
                bubble.getChildren().add(new Label("❌ Failed to load image"));
            }
        }

        // Style bubble based on sender
        bubble.setStyle(isMine ?
                "-fx-background-color: #dcf8c6; -fx-background-radius: 15 15 2 15; -fx-padding: 8;" :
                "-fx-background-color: white; -fx-background-radius: 15 15 15 2; -fx-padding: 8; -fx-border-color: #e0e0e0;"
        );

        container.getChildren().add(bubble);
        setGraphic(container);
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    private void showFullImage(String path) {
        // Will implement later for fullscreen preview
    }
}