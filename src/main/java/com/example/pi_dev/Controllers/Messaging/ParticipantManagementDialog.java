package com.example.pi_dev.Controllers.Messaging;

import com.example.pi_dev.Entities.Messaging.Conversation;
import com.example.pi_dev.Entities.Messaging.ConversationParticipant;
import com.example.pi_dev.Repositories.Messaging.ConversationRepository;
import com.example.pi_dev.Repositories.Messaging.ConversationUserRepository;
import com.example.pi_dev.Session.Session;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.control.Tooltip;
import java.sql.SQLException;
import java.util.List;

public class ParticipantManagementDialog {

    private final Conversation conversation;
    private final ConversationUserRepository convUserRepo;
    private final ConversationRepository convRepo;
    private final String currentUserId;
    private final boolean isCreator;
    private final String userRole;

    private ListView<ConversationParticipant> participantsListView;
    private ObservableList<ConversationParticipant> participants;
    private Label roleLabel;
    private Button addButton;
    private Button removeButton;
    private Button makeAdminButton;
    private Button removeAdminButton;

    public ParticipantManagementDialog(Conversation conversation) throws SQLException {
        this.conversation = conversation;
        this.convUserRepo = new ConversationUserRepository();
        this.convRepo = new ConversationRepository();
        this.currentUserId = Session.getCurrentUserId();

        // Check if current user is creator
        this.isCreator = convRepo.isUserCreator(conversation.getId(), currentUserId);

        // Get current user's role
        String role = convUserRepo.getUserRoleInConversation(conversation.getId(), currentUserId);
        this.userRole = role != null ? role : "MEMBER";

        loadParticipants();
    }

    private void loadParticipants() throws SQLException {
        List<ConversationParticipant> participantList =
                convUserRepo.getConversationParticipants(conversation.getId());
        participants = FXCollections.observableArrayList(participantList);
    }

    public void show() {
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("Manage Participants - " +
                (conversation.getName() != null ? conversation.getName() : "Conversation " + conversation.getId()));

        VBox mainContainer = new VBox(15);
        mainContainer.setPadding(new Insets(20));
        mainContainer.setStyle("-fx-background-color: white;");

        // Header
        Label headerLabel = new Label("👥 Conversation Participants");
        headerLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1a5f3f;");

        // Info about your role
        HBox roleInfoBox = new HBox(10);
        roleInfoBox.setAlignment(Pos.CENTER_LEFT);
        roleInfoBox.setPadding(new Insets(10));
        roleInfoBox.setStyle("-fx-background-color: #f0f8f0; -fx-background-radius: 8;");

        Label roleInfoLabel = new Label("Your role: ");
        roleInfoLabel.setStyle("-fx-font-weight: bold;");
        roleLabel = new Label(getRoleDisplay(userRole));
        roleLabel.setStyle(getRoleStyle(userRole));

        roleInfoBox.getChildren().addAll(roleInfoLabel, roleLabel);

        // Participants List
        participantsListView = new ListView<>(participants);
        participantsListView.setPrefHeight(300);
        participantsListView.setCellFactory(lv -> new ParticipantCell());

        // Action buttons (only if user has permissions)
        VBox actionBox = new VBox(10);
        actionBox.setPadding(new Insets(10, 0, 0, 0));

        if (isCreator || userRole.equals("ADMIN")) {
            // Add participant section
            HBox addBox = new HBox(10);
            addBox.setAlignment(Pos.CENTER_LEFT);

            TextField emailField = new TextField();
            emailField.setPromptText("Enter email to add");
            emailField.setPrefWidth(300);
            emailField.getStyleClass().add("dialog-field");

            addButton = new Button("Add Participant");
            addButton.getStyleClass().addAll("button", "button-secondary");
            // FIXED: Pass emailField to the addParticipant method
            addButton.setOnAction(e -> addParticipant(emailField.getText(), dialogStage, emailField));

            addBox.getChildren().addAll(emailField, addButton);

            actionBox.getChildren().add(addBox);
        }

        // Close button
        Button closeButton = new Button("Close");
        closeButton.getStyleClass().addAll("button", "button-secondary");
        closeButton.setOnAction(e -> dialogStage.close());
        closeButton.setPrefWidth(100);

        HBox closeBox = new HBox(closeButton);
        closeBox.setAlignment(Pos.CENTER_RIGHT);

        // Add all to main container
        mainContainer.getChildren().addAll(
                headerLabel,
                roleInfoBox,
                new Label("Participants:"),
                participantsListView,
                actionBox,
                closeBox
        );

        Scene scene = new Scene(mainContainer, 500, 500);
        scene.getStylesheets().add(
                getClass().getResource("/com/example/pi_dev/messagingchat.css").toExternalForm()
        );

        dialogStage.setScene(scene);
        dialogStage.showAndWait();
    }

    private String getRoleDisplay(String role) {
        switch (role) {
            case "CREATOR": return "👑 Creator";
            case "ADMIN": return "🔧 Admin";
            default: return "👤 Member";
        }
    }

    private String getRoleStyle(String role) {
        switch (role) {
            case "CREATOR": return "-fx-text-fill: #d4af37; -fx-font-weight: bold;";
            case "ADMIN": return "-fx-text-fill: #2d7a2d; -fx-font-weight: bold;";
            default: return "-fx-text-fill: #666;";
        }
    }

    // FIXED: Added TextField parameter to clear it after successful addition
    private void addParticipant(String email, Stage dialogStage, TextField emailField) {
        if (email == null || email.trim().isEmpty()) {
            showAlert("Error", "Please enter an email address.");
            return;
        }

        try {
            String userId = convUserRepo.findUserIdByEmail(email.trim());
            if (userId == null) {
                showAlert("Error", "User not found with email: " + email);
                return;
            }

            // NEW: Prevent adding yourself
            if (userId.equals(currentUserId)) {
                showAlert("Error", "You cannot add yourself to the conversation.");
                return;
            }

            // Check if user is already in conversation
            if (convUserRepo.isUserInConversation(conversation.getId(), userId)) {
                showAlert("Info", "User is already in this conversation.");
                return;
            }

            // Add user as MEMBER
            convUserRepo.addUserToConversation(conversation.getId(), userId, "MEMBER", currentUserId);

            // Refresh participants list
            loadParticipants();
            participantsListView.setItems(participants);

            showSuccess("User added successfully!");
            emailField.clear();

        } catch (SQLException ex) {
            showAlert("Database Error", ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void removeParticipant(ConversationParticipant participant) {
        if (participant.getUserId().equals(currentUserId)) {
            showAlert("Error", "You cannot remove yourself. Use 'Leave Conversation' instead.");
            return;
        }

        if (participant.getRole().equals("CREATOR")) {
            showAlert("Error", "Cannot remove the conversation creator.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Removal");
        confirm.setHeaderText("Remove " + participant.getDisplayName() + "?");
        confirm.setContentText("This user will no longer have access to this conversation.");

        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    convUserRepo.removeUserFromConversation(conversation.getId(), participant.getUserId());

                    // Refresh participants list
                    loadParticipants();
                    participantsListView.setItems(participants);

                    showSuccess(participant.getDisplayName() + " removed successfully!");
                } catch (SQLException ex) {
                    showAlert("Database Error", ex.getMessage());
                    ex.printStackTrace();
                }
            }
        });
    }

    private void makeAdmin(ConversationParticipant participant) {
        if (participant.getRole().equals("CREATOR")) {
            showAlert("Error", "Creator is already an admin.");
            return;
        }

        try {
            convUserRepo.updateUserRole(conversation.getId(), participant.getUserId(), "ADMIN");

            // Refresh participants list
            loadParticipants();
            participantsListView.setItems(participants);

            showSuccess(participant.getDisplayName() + " is now an admin!");
        } catch (SQLException ex) {
            showAlert("Database Error", ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void removeAdmin(ConversationParticipant participant) {
        if (participant.getRole().equals("CREATOR")) {
            showAlert("Error", "Cannot change creator's role.");
            return;
        }

        try {
            convUserRepo.updateUserRole(conversation.getId(), participant.getUserId(), "MEMBER");

            // Refresh participants list
            loadParticipants();
            participantsListView.setItems(participants);

            showSuccess(participant.getDisplayName() + " is now a regular member.");
        } catch (SQLException ex) {
            showAlert("Database Error", ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Custom cell for participant list
    private class ParticipantCell extends ListCell<ConversationParticipant> {
        @Override
        protected void updateItem(ConversationParticipant participant, boolean empty) {
            super.updateItem(participant, empty);

            if (empty || participant == null) {
                setGraphic(null);
                return;
            }

            HBox container = new HBox(10);
            container.setAlignment(Pos.CENTER_LEFT);
            container.setPadding(new Insets(8));
            container.setStyle("-fx-background-color: transparent;");

            // Avatar/Icon based on role
            Label avatarLabel = new Label();
            switch (participant.getRole()) {
                case "CREATOR":
                    avatarLabel.setText("👑");
                    avatarLabel.setStyle("-fx-font-size: 20px;");
                    break;
                case "ADMIN":
                    avatarLabel.setText("🔧");
                    avatarLabel.setStyle("-fx-font-size: 20px; -fx-text-fill: #2d7a2d;");
                    break;
                default:
                    avatarLabel.setText("👤");
                    avatarLabel.setStyle("-fx-font-size: 20px;");
            }

            // User info
            VBox infoBox = new VBox(3);
            Label nameLabel = new Label(participant.getDisplayName());
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

            // Also add email as tooltip
            Tooltip tooltip = new Tooltip(participant.getEmail() != null ?
                    participant.getEmail() : "No email");
            Tooltip.install(nameLabel, tooltip);

            Label roleLabel = new Label(getRoleDisplay(participant.getRole()));
            roleLabel.setStyle(getRoleStyle(participant.getRole()) + " -fx-font-size: 11px;");

            infoBox.getChildren().addAll(nameLabel, roleLabel);

            HBox.setHgrow(infoBox, Priority.ALWAYS);

            // Action buttons (only for users with permission)
            HBox actionBox = new HBox(5);
            actionBox.setAlignment(Pos.CENTER_RIGHT);

            if ((isCreator || userRole.equals("ADMIN")) && !participant.getUserId().equals(currentUserId)) {
                if (!participant.getRole().equals("CREATOR")) {
                    Button removeBtn = new Button("✖");
                    removeBtn.setStyle("-fx-background-color: #ff6b6b; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 15; -fx-min-width: 30; -fx-min-height: 30;");
                    removeBtn.setOnAction(e -> removeParticipant(participant));

                    if (participant.getRole().equals("ADMIN")) {
                        Button demoteBtn = new Button("⬇ Member");
                        demoteBtn.setStyle("-fx-background-color: #ffa500; -fx-text-fill: white; -fx-background-radius: 15; -fx-padding: 5 10;");
                        demoteBtn.setOnAction(e -> removeAdmin(participant));
                        actionBox.getChildren().add(demoteBtn);
                    } else {
                        Button promoteBtn = new Button("⬆ Admin");
                        promoteBtn.setStyle("-fx-background-color: #5cb85c; -fx-text-fill: white; -fx-background-radius: 15; -fx-padding: 5 10;");
                        promoteBtn.setOnAction(e -> makeAdmin(participant));
                        actionBox.getChildren().add(promoteBtn);
                    }

                    actionBox.getChildren().add(removeBtn);
                }
            } else if (participant.getUserId().equals(currentUserId)) {
                Label youLabel = new Label("(You)");
                youLabel.setStyle("-fx-text-fill: #2d7a2d; -fx-font-style: italic;");
                actionBox.getChildren().add(youLabel);
            }

            container.getChildren().addAll(avatarLabel, infoBox, actionBox);
            setGraphic(container);
        }
    }
}