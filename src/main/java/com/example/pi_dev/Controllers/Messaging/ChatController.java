package com.example.pi_dev.Controllers.Messaging;

import com.example.pi_dev.Entities.Messaging.Conversation;
import com.example.pi_dev.Entities.Messaging.Message;
import com.example.pi_dev.Entities.Messaging.Reaction;
import com.example.pi_dev.Repositories.Messaging.*;
import com.example.pi_dev.Services.Messaging.*;
import com.example.pi_dev.Session.Session;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.FileChooser;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.LineUnavailableException;

/**
 * Controller class for the chat interface.
 * Manages conversations, messages, and user interactions in the messaging system.
 */
public class ChatController {

    // ==================== FXML UI Components ====================

    // Header Components
    @FXML private Button themeBtn;

    // Chat Tab Components
    @FXML private ListView<Conversation> conversationList;
    @FXML private ListView<Message> messageList;
    @FXML private TextField messageInput;
    @FXML private Label conversationTitleLabel;
    @FXML private Button updateConversationBtn;
    @FXML private Button deleteConversationBtn;
    @FXML private TextField conversationSearchField;

    // Contacts Tab Components
    @FXML private Tab contactsTab;
    @FXML private ScrollPane contactsScrollPane;
    @FXML private FlowPane contactsFlow;
    @FXML private Tab archivedTab;
    @FXML private ListView<Conversation> archivedConversationList;
    @FXML private Button unarchiveAllBtn;

    @FXML private Button attachImageBtn;
    @FXML private MenuButton attachMenuBtn;

    // Voice recording fields
    @FXML private Button voiceRecordBtn;
    @FXML private HBox recordingIndicator;
    @FXML private Label recordingTimeLabel;
    @FXML private Button stopRecordingBtn;
    @FXML private Button cancelRecordingBtn;

    // Smart Reply button
    @FXML private Button smartReplyBtn;

    @FXML private Button stopSpeechBtn;

    @FXML private HBox emojiSuggestionBox;
    @FXML private Button emoji1Btn, emoji2Btn, emoji3Btn;
    // Services
    private final FileUploadService uploadService = new FileUploadService();
    private final GeminiService geminiService = new GeminiService();
    private AudioRecorderService audioRecorderService;
    private Timeline recordingTimer;
    private int recordingSeconds = 0;

    // Repositories
    private final ConversationRepository conversationRepo = new ConversationRepository();
    private final MessageRepository messageRepo = new MessageRepository();
    private final ConversationUserRepository conversationUserRepo = new ConversationUserRepository();
    private final UserRepository userRepo = new UserRepository();

    // State Variables
    private Conversation selectedConversation;
    private boolean darkMode = false;
    private ObservableList<Conversation> conversations = FXCollections.observableArrayList();
    private ObservableList<Conversation> archivedConversations = FXCollections.observableArrayList();
    private final ReactionRepository reactionRepo = new ReactionRepository();

    //bad words
    private MessageFilterService filterService;


    private SimpleTTSService ttsService;


    private void setupImageHandling() {
        if (attachImageBtn != null) {
            attachImageBtn.setOnAction(e -> handleAttachImage());
        }
    }

    /**
     * Initializes the controller after FXML loading.
     */
    @FXML
    public void initialize() {
        loadConversations();
        conversationList.setItems(conversations);
        conversationList.setFixedCellSize(60);
        messageList.setFixedCellSize(-1);

        messageList.getStyleClass().add("message-list");
        if (conversationSearchField != null) {
            conversationSearchField.getStyleClass().add("search-field");
        }

        Label placeholderLabel = new Label("Select a conversation to start chatting");
        placeholderLabel.getStyleClass().add("placeholder-text");
        messageList.setPlaceholder(placeholderLabel);

        if (conversationSearchField != null) {
            conversationSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
                filterConversations(newVal);
            });
        }

        audioRecorderService = new AudioRecorderService();
        recordingIndicator.setVisible(false);
        recordingIndicator.setManaged(false);

        //initialize suggestion
        setupEmojiSuggestions();


        ttsService = new SimpleTTSService();
        ttsService.listWindowsVoices(); // Optional: see available voices (Windows only)

        // In your initialize() method, add:
        filterService = new MessageFilterService();
        System.out.println("✅ Message Filter Service initialized with " +
                filterService.getFilteredWords().size() + " words");

        // Conversation selection listener
        conversationList.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> {
                    selectedConversation = newVal;
                    if (newVal != null) {
                        if (conversationTitleLabel != null) {
                            conversationTitleLabel.setText(newVal.getName() != null ?
                                    newVal.getName() : "Conversation " + newVal.getId());
                            conversationTitleLabel.getStyleClass().add("conversation-title");
                        }
                        if (updateConversationBtn != null) {
                            updateConversationBtn.setVisible(true);
                        }
                        if (deleteConversationBtn != null) {
                            deleteConversationBtn.setVisible(true);
                        }
                    } else {
                        if (updateConversationBtn != null) updateConversationBtn.setVisible(false);
                        if (deleteConversationBtn != null) deleteConversationBtn.setVisible(false);
                        if (conversationTitleLabel != null) {
                            conversationTitleLabel.setText("Select a conversation");
                        }
                    }
                    loadMessages();
                });

        // Conversation cell factory
        conversationList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Conversation c, boolean empty) {
                super.updateItem(c, empty);
                if (empty || c == null) {
                    setGraphic(null);
                    return;
                }

                Label title = new Label(c.getName() != null ? c.getName() : "Conversation " + c.getId());
                title.getStyleClass().add("conv-title");

                Label subtitle = new Label(c.getType());
                subtitle.getStyleClass().add("conv-subtitle");

                Label lastMsg = new Label("Click to view messages");
                lastMsg.getStyleClass().add("last-message-preview");

                VBox textBox = new VBox(title, subtitle, lastMsg);
                textBox.setSpacing(2);

                Button menuBtn = new Button("⋮");
                menuBtn.getStyleClass().add("menu-button");

                ContextMenu menu = new ContextMenu();
                MenuItem rename = new MenuItem("Rename");
                MenuItem manageParticipants = new MenuItem("Manage Participants");

                MenuItem pinItem = new MenuItem(c.isPinned() ? "Unpin" : "Pin");
                pinItem.setOnAction(e -> {
                    try {
                        boolean newPinState = !c.isPinned();
                        conversationRepo.updatePinStatus(c.getId(), Session.getCurrentUserId(), newPinState);
                        c.setPinned(newPinState);
                        loadConversations();
                        showInfo(newPinState ? "Conversation pinned!" : "Conversation unpinned!");
                    } catch (SQLException ex) {
                        showError("Failed to update pin status: " + ex.getMessage());
                    }
                });

                MenuItem archiveItem = new MenuItem(c.isArchived() ? "Unarchive" : "Archive");
                archiveItem.setOnAction(e -> {
                    try {
                        boolean newArchiveState = !c.isArchived();
                        conversationRepo.updateArchiveStatus(c.getId(), Session.getCurrentUserId(), newArchiveState);
                        c.setArchived(newArchiveState);
                        loadConversations();
                        if (archivedTab != null) {
                            loadArchivedConversations();
                        }
                        showInfo(newArchiveState ? "Conversation archived!" : "Conversation unarchived!");
                    } catch (SQLException ex) {
                        showError("Failed to update archive status: " + ex.getMessage());
                    }
                });

                MenuItem muteItem = new MenuItem(c.isMuteNotifications() ? "Unmute" : "Mute");
                muteItem.setOnAction(e -> {
                    try {
                        boolean newMuteState = !c.isMuteNotifications();
                        conversationRepo.updateMuteStatus(c.getId(), Session.getCurrentUserId(), newMuteState);
                        c.setMuteNotifications(newMuteState);
                        showInfo(newMuteState ? "Notifications muted!" : "Notifications unmuted!");
                    } catch (SQLException ex) {
                        showError("Failed to update mute status: " + ex.getMessage());
                    }
                });

                SeparatorMenuItem separator = new SeparatorMenuItem();
                menu.getItems().addAll(rename, pinItem, archiveItem, muteItem, separator, manageParticipants);

                manageParticipants.setOnAction(e -> {
                    if (c.getType().equals("GROUP")) {
                        handleManageParticipants();
                    } else {
                        showInfo("Personal conversations don't have participant management.");
                    }
                });

                menuBtn.setOnAction(e -> menu.show(menuBtn, Side.BOTTOM, 0, 0));

                rename.setOnAction(e -> {
                    TextInputDialog dialog = new TextInputDialog(c.getName());
                    dialog.setTitle("Rename Conversation");
                    dialog.setHeaderText("Enter new name for conversation");
                    dialog.setContentText("Name:");
                    DialogPane dialogPane = dialog.getDialogPane();
                    dialogPane.getStyleClass().add("dialog-pane");

                    dialog.showAndWait().ifPresent(newName -> {
                        try {
                            conversationRepo.updateName(c.getId(), newName.trim());
                            loadConversations();
                            showInfo("Conversation renamed successfully!");
                        } catch (SQLException ex) {
                            showError("Failed to rename: " + ex.getMessage());
                        }
                    });
                });

                HBox row = new HBox(textBox, menuBtn);
                row.setSpacing(10);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(5));
                HBox.setHgrow(textBox, Priority.ALWAYS);
                setGraphic(row);
            }
        });

        // Message cell factory with all features
        messageList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Message msg, boolean empty) {
                super.updateItem(msg, empty);

                if (empty || msg == null) {
                    setGraphic(null);
                    return;
                }

                String currentUser = Session.getCurrentUserId();
                boolean isMine = msg.getSenderId().equals(currentUser);

                // Get sender's full name
                String senderName = "User " + msg.getSenderId();
                try {
                    String fullName = userRepo.getUserFullName(msg.getSenderId());
                    if (fullName != null && !fullName.isEmpty()) {
                        senderName = fullName;
                    }
                } catch (SQLException e) {
                    // Ignore
                }

                // Menu button
                Button menuBtn = new Button("⋮");
                menuBtn.getStyleClass().add("menu-button");

                // In your message cell factory, find where you create the menu
                ContextMenu menu = new ContextMenu();
                MenuItem edit = new MenuItem("Edit");
                MenuItem delete = new MenuItem("Delete");
                MenuItem translate = new MenuItem("🌐 Translate");
                MenuItem listen = new MenuItem("🔊 Listen");
                MenuItem summarize = new MenuItem("📋 Summarize");

// Set actions FIRST
                summarize.setOnAction(e -> summarizeMessage(msg));
                listen.setOnAction(e -> {
                    if (msg.isText()) {
                        ttsService.speakMessage(msg.getContent());
                        showInfo("🔊 Speaking message...");
                        listen.setDisable(true);
                        new Thread(() -> {
                            try { Thread.sleep(2000); } catch (InterruptedException ex) {}
                            Platform.runLater(() -> listen.setDisable(false));
                        }).start();
                    } else {
                        showError("Can only listen to text messages");
                    }
                });
                translate.setOnAction(e -> translateMessage(msg));

// Clear and rebuild menu properly
                menu.getItems().clear();

// Always add common items
                menu.getItems().addAll(listen, translate);

// Add summarize ONLY for long text messages
                if (msg.isText() && msg.getContent().length() > 200) {
                    menu.getItems().add(summarize);
                }

// Add edit/delete based on ownership
                if (msg.isText() && isMine) {
                    menu.getItems().addAll(edit, delete);
                } else if (!msg.isText() && isMine) {
                    menu.getItems().add(delete);
                }
                translate.setOnAction(e -> translateMessage(msg));

                menuBtn.setOnAction(e -> menu.show(menuBtn, Side.BOTTOM, 0, 0));
                menuBtn.setVisible(true);

                edit.setOnAction(e -> {
                    if (!msg.isText()) return;

                    TextInputDialog dialog = new TextInputDialog(msg.getContent());
                    dialog.setTitle("Edit Message");
                    dialog.setHeaderText("Edit your message");
                    dialog.setContentText("New content:");
                    DialogPane dialogPane = dialog.getDialogPane();
                    dialogPane.getStyleClass().add("dialog-pane");

                    dialog.showAndWait().ifPresent(newText -> {
                        try {
                            messageRepo.update(msg.getId(), currentUser, newText);
                            loadMessages();
                        } catch (SQLException ex) {
                            showError(ex.getMessage());
                        }
                    });
                });

                Label header = new Label(senderName + " • " +
                        msg.getCreatedAt().toLocalTime().withNano(0));
                header.getStyleClass().add("message-header");

                delete.setOnAction(e -> {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Delete message");
                    confirm.setHeaderText("Are you sure?");
                    confirm.setContentText("This action cannot be undone.");
                    DialogPane dialogPane = confirm.getDialogPane();
                    dialogPane.getStyleClass().add("dialog-pane");

                    confirm.showAndWait().ifPresent(result -> {
                        if (result == ButtonType.OK) {
                            try {
                                messageRepo.delete(msg.getId(), currentUser);
                                loadMessages();
                            } catch (SQLException ex) {
                                showError(ex.getMessage());
                            }
                        }
                    });
                });

                VBox bubble = new VBox(5);
                bubble.setMaxWidth(350);
                bubble.getChildren().add(header);

                bubble.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2) {
                        showReactionPicker(msg, bubble);
                        event.consume();
                    }
                });
                updateReactionDisplay(msg, bubble);

                if (msg.isImage()) {
                    displayImageMessage(msg, bubble);
                } else if (msg.isVideo()) {
                    displayVideoMessage(msg, bubble);
                } else if (msg.isAudio()) {
                    displayAudioMessage(msg, bubble);
                } else if (msg.isFile()) {
                    displayFileMessage(msg, bubble);
                } else {
                    displayTextMessage(msg, bubble);
                }

                if (isMine) {
                    bubble.getStyleClass().add("mine");
                    bubble.setStyle("-fx-background-color: #86A7BF; -fx-background-radius: 15 15 2 15; -fx-padding: 8;");
                } else {
                    bubble.getStyleClass().add("theirs");
                    bubble.setStyle("-fx-background-color: white; -fx-background-radius: 15 15 15 2; -fx-padding: 8; -fx-border-color: #e0e0e0; -fx-border-radius: 15 15 15 2;");
                }

                HBox messageRow = isMine ? new HBox(menuBtn, bubble) : new HBox(bubble, menuBtn);
                messageRow.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                messageRow.setSpacing(6);

                HBox container = new HBox(messageRow);
                container.setPadding(new Insets(5));
                container.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                setGraphic(container);
            }

            private void displayTextMessage(Message msg, VBox bubble) {
                Text text = new Text(msg.getContent());
                text.setStyle("-fx-fill: #333;");
                TextFlow textFlow = new TextFlow(text);
                textFlow.setMaxWidth(300);
                bubble.getChildren().add(textFlow);
            }

            private void displayImageMessage(Message msg, VBox bubble) {
                try {
                    String fileUrl = msg.getFileUrl();
                    if (fileUrl != null) {
                        File imageFile = new File(fileUrl);
                        if (imageFile.exists()) {
                            Image image = new Image(imageFile.toURI().toString());
                            ImageView imageView = new ImageView(image);
                            imageView.setFitWidth(250);
                            imageView.setPreserveRatio(true);
                            imageView.setStyle("-fx-cursor: hand;");
                            imageView.setOnMouseClicked(e -> showFullImage(fileUrl));
                            bubble.getChildren().add(imageView);

                            if (msg.getFileName() != null || msg.getFileSize() > 0) {
                                Label info = new Label();
                                String infoText = "📷 ";
                                if (msg.getFileName() != null) infoText += msg.getFileName();
                                if (msg.getFileSize() > 0) infoText += " • " + formatFileSize(msg.getFileSize());
                                info.setText(infoText);
                                info.setStyle("-fx-font-size: 10px; -fx-text-fill: #999;");
                                bubble.getChildren().add(info);
                            }
                        } else {
                            showErrorPlaceholder(bubble, "Image file not found");
                        }
                    }
                } catch (Exception e) {
                    showErrorPlaceholder(bubble, "Failed to load image");
                }
            }
            // Add these methods inside your cell factory
            private void showReactionPicker(Message msg, VBox bubble) {
                // Create reaction popup
                ContextMenu reactionMenu = new ContextMenu();

                String[] emojis = {"👍", "❤️", "😂", "😮", "😢", "😡"};
                String[] names = {"Like", "Love", "Haha", "Wow", "Sad", "Angry"};

                for (int i = 0; i < emojis.length; i++) {
                    MenuItem item = new MenuItem(emojis[i] + " " + names[i]);
                    String reaction = emojis[i];
                    item.setOnAction(e -> addReaction(msg, reaction, bubble));
                    reactionMenu.getItems().add(item);
                }

                reactionMenu.show(bubble, Side.TOP, 0, 0);
            }

            private void addReaction(Message msg, String reaction, VBox bubble) {
                String currentUser = Session.getCurrentUserId();

                new Thread(() -> {
                    try {
                        // Check if user already reacted
                        String existing = reactionRepo.getUserReaction(msg.getId(), currentUser);

                        if (existing != null) {
                            if (existing.equals(reaction)) {
                                // Same reaction - remove it
                                reactionRepo.removeReaction(msg.getId(), currentUser);
                            } else {
                                // Different reaction - update
                                reactionRepo.removeReaction(msg.getId(), currentUser);
                                reactionRepo.addReaction(msg.getId(), currentUser, reaction);
                            }
                        } else {
                            // New reaction
                            reactionRepo.addReaction(msg.getId(), currentUser, reaction);
                        }

                        // Update UI to show reactions
                        javafx.application.Platform.runLater(() -> {
                            updateReactionDisplay(msg, bubble);
                        });

                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }).start();
            }

            private void updateReactionDisplay(Message msg, VBox bubble) {
                try {
                    List<Reaction> reactions = reactionRepo.getReactionsForMessage(msg.getId());

                    // Remove old reaction bar if exists
                    bubble.getChildren().removeIf(node ->
                            node instanceof HBox && "reaction-bar".equals(node.getStyleClass().stream().findFirst().orElse("")));

                    if (!reactions.isEmpty()) {
                        // Group reactions by type
                        java.util.Map<String, Long> counts = reactions.stream()
                                .collect(java.util.stream.Collectors.groupingBy(
                                        Reaction::getReaction, java.util.stream.Collectors.counting()));

                        // Create reaction bar
                        HBox reactionBar = new HBox(5);
                        reactionBar.setAlignment(Pos.CENTER_LEFT);
                        reactionBar.setPadding(new Insets(5, 0, 0, 0));
                        reactionBar.getStyleClass().add("reaction-bar");

                        counts.forEach((emoji, count) -> {
                            Label reactionLabel = new Label(emoji + " " + count);
                            reactionLabel.setStyle("-fx-background-color: #f0f0f0; -fx-background-radius: 12; -fx-padding: 2 8; -fx-font-size: 12px; -fx-text-fill: #333333;");
                            reactionLabel.setTooltip(new Tooltip(getReactionUsers(reactions, emoji)));
                            reactionBar.getChildren().add(reactionLabel);
                        });

                        bubble.getChildren().add(reactionBar);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            private String getReactionUsers(List<Reaction> reactions, String emoji) {
                return reactions.stream()
                        .filter(r -> r.getReaction().equals(emoji))
                        .map(r -> r.getUserFullName() != null ? r.getUserFullName() : r.getUserId())
                        .collect(java.util.stream.Collectors.joining("\n"));
            }
            private void displayVideoMessage(Message msg, VBox bubble) {
                try {
                    String fileUrl = msg.getFileUrl();
                    if (fileUrl != null && new File(fileUrl).exists()) {
                        Label videoThumbnail = new Label("🎥");
                        videoThumbnail.setStyle("-fx-font-size: 48px; -fx-text-fill: #2d7a2d; -fx-background-color: #f0f0f0; -fx-background-radius: 8; -fx-padding: 20;");
                        videoThumbnail.setPrefSize(200, 120);
                        videoThumbnail.setAlignment(Pos.CENTER);
                        videoThumbnail.setStyle(videoThumbnail.getStyle() + "-fx-cursor: hand;");
                        videoThumbnail.setOnMouseClicked(e -> openFile(fileUrl));
                        bubble.getChildren().add(videoThumbnail);

                        VBox infoBox = new VBox(2);
                        Label fileName = new Label(msg.getFileName() != null ? msg.getFileName() : "Video");
                        fileName.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
                        String infoText = "";
                        if (msg.getDuration() != null && msg.getDuration() > 0) infoText += formatDuration(msg.getDuration());
                        if (msg.getFileSize() > 0) {
                            if (!infoText.isEmpty()) infoText += " • ";
                            infoText += formatFileSize(msg.getFileSize());
                        }
                        Label fileInfo = new Label(infoText);
                        fileInfo.setStyle("-fx-font-size: 10px; -fx-text-fill: #999;");
                        infoBox.getChildren().addAll(fileName, fileInfo);
                        bubble.getChildren().add(infoBox);
                    } else {
                        showErrorPlaceholder(bubble, "Video file not found");
                    }
                } catch (Exception e) {
                    showErrorPlaceholder(bubble, "Failed to load video");
                }
            }

            private void displayAudioMessage(Message msg, VBox bubble) {
                try {
                    String fileUrl = msg.getFileUrl();
                    if (fileUrl != null && new File(fileUrl).exists()) {
                        HBox audioBox = new HBox(10);
                        audioBox.setAlignment(Pos.CENTER_LEFT);
                        audioBox.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 20; -fx-padding: 8; -fx-cursor: hand;");
                        audioBox.setOnMouseClicked(e -> openFile(fileUrl));

                        Label playIcon = new Label("▶");
                        playIcon.setStyle("-fx-font-size: 20px; -fx-text-fill: #2d7a2d; -fx-min-width: 30;");

                        VBox infoBox = new VBox(2);
                        Label fileName = new Label(msg.getFileName() != null ? msg.getFileName() : "Audio");
                        fileName.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");

                        String duration = "";
                        if (msg.getDuration() != null && msg.getDuration() > 0) {
                            duration = formatDuration(msg.getDuration());
                        } else {
                            duration = formatFileSize(msg.getFileSize());
                        }
                        Label durationLabel = new Label(duration);
                        durationLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #999;");

                        infoBox.getChildren().addAll(fileName, durationLabel);
                        audioBox.getChildren().addAll(playIcon, infoBox);
                        HBox.setHgrow(infoBox, Priority.ALWAYS);
                        bubble.getChildren().add(audioBox);
                    } else {
                        showErrorPlaceholder(bubble, "Audio file not found");
                    }
                } catch (Exception e) {
                    showErrorPlaceholder(bubble, "Failed to load audio");
                }
            }

            private void displayFileMessage(Message msg, VBox bubble) {
                HBox fileBox = new HBox(8);
                fileBox.setAlignment(Pos.CENTER_LEFT);
                fileBox.setStyle("-fx-cursor: hand; -fx-background-color: #f5f5f5; -fx-background-radius: 8; -fx-padding: 8;");
                fileBox.setOnMouseClicked(e -> openFile(msg.getFileUrl()));

                Label iconLabel = new Label("📎");
                iconLabel.setStyle("-fx-font-size: 24px;");

                VBox fileInfo = new VBox(2);
                Label fileName = new Label(msg.getFileName() != null ? msg.getFileName() : "Unknown file");
                fileName.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #2d7a2d;");
                Label fileSize = new Label(formatFileSize(msg.getFileSize()));
                fileSize.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

                fileInfo.getChildren().addAll(fileName, fileSize);
                fileBox.getChildren().addAll(iconLabel, fileInfo);
                HBox.setHgrow(fileInfo, Priority.ALWAYS);
                bubble.getChildren().add(fileBox);
            }

            private void openFile(String filePath) {
                try {
                    File file = new File(filePath);
                    if (file.exists()) {
                        Desktop.getDesktop().open(file);
                    } else {
                        showError("File not found: " + filePath);
                    }
                } catch (IOException e) {
                    showError("Cannot open file: " + e.getMessage());
                }
            }

            private String formatDuration(int seconds) {
                int minutes = seconds / 60;
                int secs = seconds % 60;
                return String.format("%d:%02d", minutes, secs);
            }

            private void showErrorPlaceholder(VBox bubble, String errorMsg) {
                Label errorLabel = new Label("❌ " + errorMsg);
                errorLabel.setStyle("-fx-text-fill: #ff4444; -fx-font-size: 11px;");
                bubble.getChildren().add(errorLabel);
            }

            private String formatFileSize(long bytes) {
                if (bytes <= 0) return "0 B";
                String[] units = {"B", "KB", "MB", "GB", "TB"};
                int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
                return String.format("%.1f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
            }

            private void showFullImage(String imagePath) {
                try {
                    File imageFile = new File(imagePath);
                    if (!imageFile.exists()) {
                        showError("Image file not found");
                        return;
                    }

                    Stage imageStage = new Stage();
                    imageStage.setTitle("Image Preview");

                    Image image = new Image(imageFile.toURI().toString());
                    ImageView imageView = new ImageView(image);
                    imageView.setPreserveRatio(true);
                    imageView.setSmooth(true);

                    ScrollPane scrollPane = new ScrollPane();
                    scrollPane.setContent(imageView);
                    scrollPane.setPannable(true);
                    scrollPane.setFitToWidth(true);
                    scrollPane.setFitToHeight(true);
                    scrollPane.setStyle("-fx-background-color: #2d2d2d;");

                    Button downloadBtn = new Button("⬇ Download");
                    downloadBtn.setStyle("-fx-background-color: #2d7a2d; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 8 15;");
                    downloadBtn.setOnAction(e -> downloadFile(imageFile));

                    Button closeBtn = new Button("✖ Close");
                    closeBtn.setStyle("-fx-background-color: #ff6b6b; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 8 15;");
                    closeBtn.setOnAction(e -> imageStage.close());

                    HBox buttonBar = new HBox(10, downloadBtn, closeBtn);
                    buttonBar.setAlignment(Pos.CENTER);
                    buttonBar.setPadding(new Insets(10));
                    buttonBar.setStyle("-fx-background-color: #f0f0f0;");

                    BorderPane root = new BorderPane();
                    root.setCenter(scrollPane);
                    root.setBottom(buttonBar);

                    imageStage.setScene(new Scene(root, 900, 700));
                    imageStage.show();
                } catch (Exception e) {
                    showError("Failed to open image: " + e.getMessage());
                }
            }

            private void downloadFile(File sourceFile) {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Save File");
                fileChooser.setInitialFileName(sourceFile.getName());
                File destination = fileChooser.showSaveDialog(themeBtn.getScene().getWindow());
                if (destination != null) {
                    try {
                        Files.copy(sourceFile.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        showInfo("File downloaded successfully!");
                    } catch (IOException e) {
                        showError("Failed to download file: " + e.getMessage());
                    }
                }
            }
        });

        messageInput.setOnAction(e -> sendMessage());
        messageInput.getStyleClass().add("message-input");

        if (updateConversationBtn != null) {
            updateConversationBtn.setOnAction(e -> handleUpdateConversation());
            updateConversationBtn.setVisible(false);
        }
        if (deleteConversationBtn != null) {
            deleteConversationBtn.setOnAction(e -> handleDeleteConversation());
            deleteConversationBtn.setVisible(false);
        }

        if (themeBtn != null) themeBtn.getStyleClass().add("theme-button");

        if (contactsTab != null) loadContacts();
        if (archivedTab != null) {
            loadArchivedConversations();
            if (unarchiveAllBtn != null) unarchiveAllBtn.setOnAction(e -> handleUnarchiveAll());
        }

        setupImageHandling();
    }

    // ==================== Archived Conversations ====================

    private class ArchivedConversationCell extends ListCell<Conversation> {
        @Override
        protected void updateItem(Conversation c, boolean empty) {
            super.updateItem(c, empty);
            if (empty || c == null) {
                setGraphic(null);
                return;
            }

            HBox container = new HBox(10);
            container.setAlignment(Pos.CENTER_LEFT);
            container.setPadding(new Insets(10));
            container.setStyle("-fx-background-color: transparent; -fx-border-color: transparent transparent #e0e0e0 transparent;");

            Label archiveIcon = new Label("📦");
            archiveIcon.setStyle("-fx-font-size: 20px; -fx-text-fill: #6c757d;");

            VBox infoBox = new VBox(3);
            Label nameLabel = new Label(c.getName() != null ? c.getName() : "Conversation " + c.getId());
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #6c757d;");
            Label typeLabel = new Label(c.getType() + " • Archived");
            typeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #999;");
            infoBox.getChildren().addAll(nameLabel, typeLabel);
            HBox.setHgrow(infoBox, Priority.ALWAYS);

            Button unarchiveBtn = new Button("Unarchive");
            unarchiveBtn.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-background-radius: 15; -fx-padding: 5 15;");
            unarchiveBtn.setOnAction(e -> {
                try {
                    conversationRepo.updateArchiveStatus(c.getId(), Session.getCurrentUserId(), false);
                    loadConversations();
                    loadArchivedConversations();
                    showInfo("Conversation unarchived!");
                } catch (SQLException ex) {
                    showError("Failed to unarchive: " + ex.getMessage());
                }
            });

            container.getChildren().addAll(archiveIcon, infoBox, unarchiveBtn);
            setGraphic(container);
        }
    }

    private void handleUnarchiveAll() {
        if (archivedConversations.isEmpty()) {
            showInfo("No archived conversations to unarchive.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Unarchive All");
        confirm.setHeaderText("Unarchive all conversations?");
        confirm.setContentText("This will move all archived conversations back to your main list.");

        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    for (Conversation c : archivedConversations) {
                        conversationRepo.updateArchiveStatus(c.getId(), Session.getCurrentUserId(), false);
                    }
                    loadConversations();
                    loadArchivedConversations();
                    showInfo("All conversations unarchived!");
                } catch (SQLException e) {
                    showError("Failed to unarchive all: " + e.getMessage());
                }
            }
        });
    }

    // ==================== Filter & Load ====================

    private void filterConversations(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            conversationList.setItems(conversations);
        } else {
            ObservableList<Conversation> filtered = FXCollections.observableArrayList();
            for (Conversation c : conversations) {
                if (c.getName() != null && c.getName().toLowerCase().contains(searchText.toLowerCase())) {
                    filtered.add(c);
                } else if (c.getType().toLowerCase().contains(searchText.toLowerCase())) {
                    filtered.add(c);
                }
            }
            conversationList.setItems(filtered);
        }
    }

    private void loadContacts() {
        if (contactsFlow != null) {
            contactsFlow.getChildren().clear();
            String[] sampleContacts = {
                    "Ahmed Ben Salem", "Sarra Trabelsi", "Mehdi Khemiri",
                    "Nour Ben Ali", "Yasmine Mhiri", "Omar Jelliti"
            };
            for (String contactName : sampleContacts) {
                VBox contactCard = createContactCard(contactName);
                contactsFlow.getChildren().add(contactCard);
            }
        }
    }

    private VBox createContactCard(String name) {
        VBox card = new VBox(10);
        card.getStyleClass().add("contact-card");
        card.setPrefWidth(200);
        card.setAlignment(Pos.CENTER);

        Label avatar = new Label("👤");
        avatar.getStyleClass().add("contact-avatar");

        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("contact-name");

        Label status = new Label("Online");
        status.getStyleClass().add("contact-status");

        Button messageBtn = new Button("Message");
        messageBtn.getStyleClass().add("button-secondary");
        messageBtn.setPrefWidth(120);

        card.getChildren().addAll(avatar, nameLabel, status, messageBtn);
        return card;
    }

    private String getCurrentUserEmail() {
        try {
            return userRepo.getUserEmail(Session.getCurrentUserId());
        } catch (SQLException e) {
            return null;
        }
    }

    private void loadConversations() {
        try {
            conversations.setAll(conversationRepo.findByUser(Session.getCurrentUserId()));
            conversationList.setItems(conversations);
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    private void loadMessages() {
        if (selectedConversation == null) return;
        try {
            messageList.getItems().setAll(
                    messageRepo.findByConversation(selectedConversation.getId(), Session.getCurrentUserId())
            );
            if (!messageList.getItems().isEmpty()) {
                messageList.scrollTo(messageList.getItems().size() - 1);
            }
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    private void loadArchivedConversations() {
        try {
            archivedConversations.setAll(conversationRepo.findArchivedByUser(Session.getCurrentUserId()));
            archivedConversationList.setItems(archivedConversations);
            archivedConversationList.setCellFactory(list -> new ArchivedConversationCell());
        } catch (SQLException e) {
            showError("Failed to load archived conversations: " + e.getMessage());
        }
    }

    // ==================== Message Actions ====================

    //baddaltha bch twalli bel filter
    @FXML
    private void sendMessage() {
        if (selectedConversation == null || messageInput.getText().isBlank())
            return;

        String originalMessage = messageInput.getText();

        // Filter the message
        MessageFilterService.FilterResult result = filterService.validateMessage(originalMessage);

        if (!result.isAllowed()) {
            // Message contains strong profanity - block it
            showError(result.getWarning());
            messageInput.clear();
            return;
        }

        // Create message with filtered content
        Message msg = new Message(
                selectedConversation.getId(),
                Session.getCurrentUserId(),
                result.getFilteredMessage() // Use filtered version
        );

        try {
            messageRepo.create(msg);
            messageInput.clear();
            loadMessages();

            // Show warning if message was filtered
            if (result.getWarning() != null) {
                showInfo(result.getWarning());
            }

        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    // ==================== Media Attachment ====================

    @FXML
    private void handleAttachImage() {
        if (selectedConversation == null) {
            showError("Please select a conversation first.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );

        File selectedFile = fileChooser.showOpenDialog(themeBtn.getScene().getWindow());
        if (selectedFile != null) sendImageMessage(selectedFile);
    }

    @FXML
    private void handleAttachVideo() {
        if (selectedConversation == null) {
            showError("Please select a conversation first.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Video");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Video Files", "*.mp4", "*.avi", "*.mov", "*.mkv", "*.wmv")
        );

        File selectedFile = fileChooser.showOpenDialog(themeBtn.getScene().getWindow());
        if (selectedFile != null) sendVideoMessage(selectedFile);
    }

    @FXML
    private void handleAttachAudio() {
        if (selectedConversation == null) {
            showError("Please select a conversation first.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Audio");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Audio Files", "*.mp3", "*.wav", "*.aac", "*.ogg", "*.m4a")
        );

        File selectedFile = fileChooser.showOpenDialog(themeBtn.getScene().getWindow());
        if (selectedFile != null) sendAudioMessage(selectedFile);
    }

    @FXML
    private void handleAttachFile() {
        if (selectedConversation == null) {
            showError("Please select a conversation first.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File");
        File selectedFile = fileChooser.showOpenDialog(themeBtn.getScene().getWindow());
        if (selectedFile != null) sendFileMessage(selectedFile);
    }

    private void sendImageMessage(File imageFile) {
        try {
            showInfo("Uploading image...");
            String filePath = uploadService.saveFile(imageFile, "IMAGE");
            String thumbnailPath = uploadService.generateThumbnail(filePath);

            Message msg = new Message(
                    selectedConversation.getId(),
                    Session.getCurrentUserId(),
                    "📷 Image", "IMAGE", filePath
            );
            msg.setThumbnailUrl(thumbnailPath);
            msg.setFileSize(imageFile.length());
            msg.setFileName(imageFile.getName());
            msg.setMimeType(Files.probeContentType(imageFile.toPath()));

            messageRepo.create(msg);
            loadMessages();
        } catch (IOException | SQLException e) {
            showError("Failed to send image: " + e.getMessage());
        }
    }

    private void sendVideoMessage(File videoFile) {
        try {
            showInfo("Uploading video...");
            String filePath = uploadService.saveFile(videoFile, "VIDEO");
            int duration = 0;

            Message msg = new Message(
                    selectedConversation.getId(),
                    Session.getCurrentUserId(),
                    "🎥 Video", "VIDEO", filePath
            );
            msg.setFileSize(videoFile.length());
            msg.setFileName(videoFile.getName());
            msg.setMimeType(Files.probeContentType(videoFile.toPath()));
            msg.setDuration(duration);

            messageRepo.create(msg);
            loadMessages();
        } catch (IOException | SQLException e) {
            showError("Failed to send video: " + e.getMessage());
        }
    }

    private void sendAudioMessage(File audioFile) {
        try {
            showInfo("Uploading audio...");
            String filePath = uploadService.saveFile(audioFile, "AUDIO");
            int duration = 0;

            Message msg = new Message(
                    selectedConversation.getId(),
                    Session.getCurrentUserId(),
                    "🎵 Audio", "AUDIO", filePath
            );
            msg.setFileSize(audioFile.length());
            msg.setFileName(audioFile.getName());
            msg.setMimeType(Files.probeContentType(audioFile.toPath()));
            msg.setDuration(duration);

            messageRepo.create(msg);
            loadMessages();
        } catch (IOException | SQLException e) {
            showError("Failed to send audio: " + e.getMessage());
        }
    }

    private void sendFileMessage(File file) {
        try {
            showInfo("Uploading file...");
            String filePath = uploadService.saveFile(file, "FILE");

            Message msg = new Message(
                    selectedConversation.getId(),
                    Session.getCurrentUserId(),
                    "📎 File", "FILE", filePath
            );
            msg.setFileSize(file.length());
            msg.setFileName(file.getName());
            msg.setMimeType(Files.probeContentType(file.toPath()));

            messageRepo.create(msg);
            loadMessages();
        } catch (IOException | SQLException e) {
            showError("Failed to send file: " + e.getMessage());
        }
    }

    // ==================== Voice Recording ====================

    @FXML
    private void toggleVoiceRecording() {
        if (selectedConversation == null) {
            showError("Please select a conversation first.");
            return;
        }

        try {
            voiceRecordBtn.setVisible(false);
            voiceRecordBtn.setManaged(false);
            recordingIndicator.setVisible(true);
            recordingIndicator.setManaged(true);

            recordingSeconds = 0;
            recordingTimeLabel.setText("0:00");

            recordingTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                recordingSeconds++;
                int minutes = recordingSeconds / 60;
                int seconds = recordingSeconds % 60;
                recordingTimeLabel.setText(String.format("%d:%02d", minutes, seconds));
            }));
            recordingTimer.setCycleCount(Timeline.INDEFINITE);
            recordingTimer.play();

            audioRecorderService.startRecording();
        } catch (LineUnavailableException e) {
            showError("Could not start recording: " + e.getMessage());
            resetRecordingUI();
        }
    }

    @FXML
    private void stopVoiceRecording() {
        try {
            if (recordingTimer != null) recordingTimer.stop();
            File audioFile = audioRecorderService.stopRecording();
            if (audioFile != null && audioFile.exists()) sendVoiceMessage(audioFile);
        } catch (IOException e) {
            showError("Failed to save recording: " + e.getMessage());
        } finally {
            resetRecordingUI();
        }
    }

    @FXML
    private void cancelVoiceRecording() {
        if (recordingTimer != null) recordingTimer.stop();
        audioRecorderService.cancelRecording();
        resetRecordingUI();
        showInfo("Recording cancelled");
    }

    private void resetRecordingUI() {
        voiceRecordBtn.setVisible(true);
        voiceRecordBtn.setManaged(true);
        recordingIndicator.setVisible(false);
        recordingIndicator.setManaged(false);
    }

    private void sendVoiceMessage(File audioFile) {
        try {
            showInfo("Sending voice message...");
            int duration = AudioRecorderService.getAudioDuration(audioFile);
            String filePath = uploadService.saveFile(audioFile, "AUDIO");

            Message msg = new Message(
                    selectedConversation.getId(),
                    Session.getCurrentUserId(),
                    "🎤 Voice message", "AUDIO", filePath
            );
            msg.setFileSize(audioFile.length());
            msg.setFileName(audioFile.getName());
            msg.setMimeType("audio/wav");
            msg.setDuration(duration);

            messageRepo.create(msg);
            loadMessages();
            audioFile.delete();
        } catch (IOException | SQLException e) {
            showError("Failed to send voice message: " + e.getMessage());
        }
    }

    // ==================== Gemini AI Features ====================

    /**
     * Translate a message using Gemini API
     */
    private void translateMessage(Message msg) {
        // Language choices
        List<String> languages = List.of("English", "French", "Spanish", "German", "Italian", "Arabic", "Chinese", "Japanese");
        ChoiceDialog<String> dialog = new ChoiceDialog<>("English", languages);
        dialog.setTitle("🌐 Translate Message");
        dialog.setHeaderText("Choose target language");

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStyleClass().add("dialog-pane");

        dialog.showAndWait().ifPresent(language -> {
            // Show loading
            Alert loading = new Alert(Alert.AlertType.INFORMATION);
            loading.setTitle("Translating");
            loading.setHeaderText(null);
            loading.setContentText("⏳ Translating to " + language + "...");
            loading.show();

            String prompt = String.format(
                    "Translate this message to %s. Only return the translation, nothing else:\n\n%s",
                    language, msg.getContent()
            );

            new Thread(() -> {
                try {
                    String translation = geminiService.generateResponse(prompt);

                    javafx.application.Platform.runLater(() -> {
                        loading.close();

                        // Show translation result
                        Alert result = new Alert(Alert.AlertType.INFORMATION);
                        result.setTitle("Translation");
                        result.setHeaderText("Translated to " + language);

                        TextArea textArea = new TextArea(translation);
                        textArea.setWrapText(true);
                        textArea.setEditable(false);
                        textArea.setPrefRowCount(5);
                        textArea.setPrefWidth(400);
                        textArea.setStyle("-fx-font-size: 14px;");

                        result.getDialogPane().setContent(textArea);
                        result.getDialogPane().getStyleClass().add("dialog-pane");

                        ButtonType useButton = new ButtonType("Use in Message", ButtonBar.ButtonData.OK_DONE);
                        result.getButtonTypes().setAll(useButton, ButtonType.CLOSE);

                        result.showAndWait().ifPresent(res -> {
                            if (res == useButton) {
                                messageInput.setText(translation);
                                messageInput.requestFocus();
                            }
                        });
                    });
                } catch (Exception e) {
                    javafx.application.Platform.runLater(() -> {
                        loading.close();
                        showError("Translation failed: " + e.getMessage());
                    });
                }
            }).start();
        });
    }

    /**
     * Handle smart reply button - generates AI suggestions based on conversation
     */
    @FXML
    private void handleSmartReply() {
        if (selectedConversation == null) {
            showError("Please select a conversation first.");
            return;
        }

        List<Message> recentMessages = messageList.getItems();
        if (recentMessages.isEmpty()) {
            showError("No messages to generate reply from.");
            return;
        }

        smartReplyBtn.setDisable(true);
        smartReplyBtn.setText("⏳ Thinking...");

        // Build conversation context
        StringBuilder context = new StringBuilder();
        context.append("Here's a conversation history. Generate a helpful, natural reply:\n\n");

        int start = Math.max(0, recentMessages.size() - 5);
        for (int i = start; i < recentMessages.size(); i++) {
            Message msg = recentMessages.get(i);
            String sender = msg.getSenderId().equals(Session.getCurrentUserId()) ? "Me" : "User";
            context.append(sender).append(": ").append(msg.getContent()).append("\n");
        }

        context.append("\nGenerate a single, natural reply to continue this conversation:");

        new Thread(() -> {
            try {
                String suggestion = geminiService.generateResponse(context.toString());

                javafx.application.Platform.runLater(() -> {
                    showSmartReplyDialog(suggestion);
                    smartReplyBtn.setDisable(false);
                    smartReplyBtn.setText("🤖 Smart Reply");
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    showError("Failed to generate reply: " + e.getMessage());
                    smartReplyBtn.setDisable(false);
                    smartReplyBtn.setText("🤖 Smart Reply");
                });
            }
        }).start();
    }

    /**
     * Show smart reply suggestion dialog
     */
    private void showSmartReplyDialog(String suggestion) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("🤖 AI Smart Reply");
        dialog.setHeaderText("Suggested Reply:");

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStyleClass().add("dialog-pane");
        dialogPane.setPrefWidth(400);

        TextArea suggestionArea = new TextArea(suggestion);
        suggestionArea.setWrapText(true);
        suggestionArea.setPrefRowCount(3);
        suggestionArea.setEditable(true);
        suggestionArea.setStyle("-fx-font-size: 14px; -fx-padding: 10;");

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.getChildren().addAll(
                new Label("You can edit this before using:"),
                suggestionArea
        );

        dialogPane.setContent(content);
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        okButton.setText("Use This Reply");
        okButton.setStyle("-fx-background-color: #2d7a2d; -fx-text-fill: white;");

        Button cancelButton = (Button) dialogPane.lookupButton(ButtonType.CANCEL);
        cancelButton.setText("Cancel");

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                messageInput.setText(suggestionArea.getText());
                messageInput.requestFocus();
            }
        });
    }

    // ==================== Conversation Management ====================

    @FXML
    private void handleCreateConversation() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Create Conversation");
        dialog.setHeaderText("Start a new conversation");

        TextField nameField = new TextField();
        nameField.setPromptText("Conversation name");
        nameField.getStyleClass().add("dialog-field");

        ChoiceBox<String> typeChoice = new ChoiceBox<>();
        typeChoice.getItems().addAll("PERSONAL", "GROUP");
        typeChoice.setValue("PERSONAL");
        typeChoice.getStyleClass().add("dialog-field");

        TextField emailField = new TextField();
        emailField.setPromptText("Participant email");
        emailField.getStyleClass().add("dialog-field");

        ListView<String> participantsList = new ListView<>();
        participantsList.setPrefHeight(100);
        participantsList.getStyleClass().add("participants-list");

        Button addBtn = new Button("Add Participant");
        addBtn.setMaxWidth(Double.MAX_VALUE);
        addBtn.getStyleClass().addAll("button", "button-secondary");

        String currentUserEmail = getCurrentUserEmail();

        addBtn.setOnAction(e -> {
            String email = emailField.getText().trim();
            if (email.isBlank()) {
                showError("Please enter an email address.");
                return;
            }
            if (email.equals(currentUserEmail)) {
                showError("You cannot add yourself to the conversation.");
                return;
            }
            if (participantsList.getItems().contains(email)) {
                showError("This participant is already added.");
                return;
            }
            participantsList.getItems().add(email);
            emailField.clear();
        });

        emailField.setOnAction(e -> addBtn.fire());

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.getStyleClass().add("dialog-content");
        content.getChildren().addAll(
                createStyledLabel("Conversation Name:"), nameField,
                createStyledLabel("Conversation Type:"), typeChoice,
                createStyledLabel("Add Participants by Email:"), emailField, addBtn,
                createStyledLabel("Participants:"), participantsList
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().getStyleClass().add("dialog-pane");

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.getStyleClass().add("button");

        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.getStyleClass().add("button-secondary");

        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (nameField.getText().trim().isEmpty()) {
                showError("Conversation name cannot be empty.");
                event.consume();
            } else if (participantsList.getItems().isEmpty()) {
                showError("Please add at least one participant.");
                event.consume();
            }
        });

        dialog.showAndWait().ifPresent(result -> {
            if (result != ButtonType.OK) return;

            String name = nameField.getText().trim();
            String type = typeChoice.getValue();

            if (name.isBlank()) {
                showError("Conversation name cannot be empty.");
                return;
            }

            if (type.equals("PERSONAL") && participantsList.getItems().size() != 1) {
                showError("Personal conversation must have exactly 1 participant.");
                return;
            }

            try {
                List<String> validatedUserIds = new ArrayList<>();
                for (String email : participantsList.getItems()) {
                    String userId = conversationUserRepo.findUserIdByEmail(email);
                    if (userId == null) {
                        showError("User not found with email: " + email);
                        return;
                    }
                    validatedUserIds.add(userId);
                }

                Conversation c = new Conversation();
                c.setName(name);
                c.setType(type);

                String currentUser = Session.getCurrentUserId();
                long conversationId = conversationRepo.create(c, currentUser);

                if (conversationId == -1) {
                    showError("Failed to create conversation.");
                    return;
                }

                conversationUserRepo.addUserToConversation(conversationId, currentUser, "CREATOR", currentUser);
                for (String userId : validatedUserIds) {
                    conversationUserRepo.addUserToConversation(conversationId, userId, "MEMBER", currentUser);
                }

                loadConversations();
                showInfo("Conversation created successfully!");
            } catch (Exception ex) {
                showError("Failed to create conversation: " + ex.getMessage());
                ex.printStackTrace();
            }
        });
    }
    //method summirize
    private void summarizeMessage(Message msg) {
        String prompt = String.format(
                "Summarize this message in 1-2 sentences:\n\n%s",
                msg.getContent()
        );

        Alert loading = new Alert(Alert.AlertType.INFORMATION);
        loading.setTitle("Summarizing");
        loading.setHeaderText(null);
        loading.setContentText("⏳ Generating summary...");
        loading.show();

        new Thread(() -> {
            try {
                String summary = geminiService.generateResponse(prompt);

                javafx.application.Platform.runLater(() -> {
                    loading.close();

                    Alert result = new Alert(Alert.AlertType.INFORMATION);
                    result.setTitle("Message Summary");
                    result.setHeaderText("Summary:");

                    TextArea textArea = new TextArea(summary);
                    textArea.setWrapText(true);
                    textArea.setEditable(false);
                    textArea.setPrefRowCount(3);
                    textArea.setPrefWidth(400);

                    result.getDialogPane().setContent(textArea);
                    result.getDialogPane().getStyleClass().add("dialog-pane");
                    result.showAndWait();
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    loading.close();
                    showError("Summarization failed: " + e.getMessage());
                });
            }
        }).start();
    }
    @FXML
    private void summarizeConversation() {
        if (selectedConversation == null) {
            showError("Please select a conversation first.");
            return;
        }

        if (messageList.getItems().isEmpty()) {
            showError("No messages to summarize");
            return;
        }

        List<Message> messages = messageList.getItems();

        // Show loading dialog
        Alert loading = new Alert(Alert.AlertType.INFORMATION);
        loading.setTitle("Summarizing");
        loading.setHeaderText(null);
        loading.setContentText("⏳ Generating conversation summary...");
        loading.show();

        // Build conversation context
        StringBuilder context = new StringBuilder();
        context.append("Summarize this conversation in 3-5 bullet points. Be concise:\n\n");

        int start = Math.max(0, messages.size() - 20); // Last 20 messages
        for (int i = start; i < messages.size(); i++) {
            Message msg = messages.get(i);

            // Get sender name
            String senderName = "User";
            try {
                String fullName = userRepo.getUserFullName(msg.getSenderId());
                if (fullName != null && !fullName.isEmpty()) {
                    senderName = fullName;
                } else {
                    senderName = "User " + msg.getSenderId();
                }
            } catch (SQLException e) {
                senderName = "User " + msg.getSenderId();
            }

            // Add message with appropriate prefix
            String prefix = msg.getSenderId().equals(Session.getCurrentUserId()) ? "Me" : senderName;

            // Show message type
            String content = msg.getContent();
            if (msg.isImage()) content = "[Image]";
            else if (msg.isVideo()) content = "[Video]";
            else if (msg.isAudio()) content = "[Audio]";
            else if (msg.isFile()) content = "[File]";

            context.append(prefix).append(": ").append(content).append("\n");
        }

        context.append("\nProvide a concise summary with 3-5 bullet points:");

        String prompt = context.toString();

        // Call API in background
        new Thread(() -> {
            try {
                String summary = geminiService.generateResponse(prompt);

                javafx.application.Platform.runLater(() -> {
                    loading.close();

                    // Show summary in dialog
                    Dialog<ButtonType> dialog = new Dialog<>();
                    dialog.setTitle("📋 Conversation Summary");
                    dialog.setHeaderText("Summary of last " + Math.min(20, messages.size()) + " messages");

                    DialogPane dialogPane = dialog.getDialogPane();
                    dialogPane.getStyleClass().add("dialog-pane");
                    dialogPane.setPrefWidth(500);
                    dialogPane.setPrefHeight(400);

                    TextArea summaryArea = new TextArea(summary);
                    summaryArea.setWrapText(true);
                    summaryArea.setEditable(false);
                    summaryArea.setStyle("-fx-font-size: 14px; -fx-padding: 15;");

                    ScrollPane scrollPane = new ScrollPane(summaryArea);
                    scrollPane.setFitToWidth(true);
                    scrollPane.setFitToHeight(true);

                    dialogPane.setContent(scrollPane);
                    dialogPane.getButtonTypes().add(ButtonType.CLOSE);

                    dialog.showAndWait();
                });

            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    loading.close();
                    showError("Failed to summarize: " + e.getMessage());
                });
            }
        }).start();
    }

    private Label createStyledLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("dialog-label");
        return label;
    }

    @FXML
    private void handleUpdateConversation() {
        if (selectedConversation == null) {
            showError("Please select a conversation first.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog(selectedConversation.getName());
        dialog.setTitle("Update Conversation");
        dialog.setHeaderText("Rename conversation");
        dialog.setContentText("New name:");

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStyleClass().add("dialog-pane");

        var result = dialog.showAndWait();
        if (result.isEmpty()) return;

        String newName = result.get().trim();
        if (newName.isEmpty()) {
            showError("Name cannot be empty.");
            return;
        }

        try {
            conversationRepo.updateName(selectedConversation.getId(), newName);
            loadConversations();
            if (conversationTitleLabel != null) conversationTitleLabel.setText(newName);
            showInfo("Conversation updated successfully!");
        } catch (SQLException e) {
            showError("Failed to update conversation: " + e.getMessage());
        }
    }
    // EMOGIES
    private void setupEmojiSuggestions() {
        messageInput.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.length() > 10 && !newVal.equals(oldVal)) {
                suggestEmojis(newVal);
            }
        });
    }

    private void suggestEmojis(String text) {
        String prompt = String.format(
                "Based on this message: '%s', suggest 3 relevant emojis. " +
                        "Return ONLY the emojis separated by spaces, nothing else.",
                text
        );

        new Thread(() -> {
            try {
                String suggestion = geminiService.generateResponse(prompt);
                String[] emojis = suggestion.trim().split("\\s+");

                javafx.application.Platform.runLater(() -> {
                    if (emojis.length >= 3) {
                        emoji1Btn.setText(emojis[0]);
                        emoji2Btn.setText(emojis[1]);
                        emoji3Btn.setText(emojis[2]);
                        emojiSuggestionBox.setVisible(true);
                        emojiSuggestionBox.setManaged(true);

                        // Add click handlers
                        emoji1Btn.setOnAction(e -> insertEmoji(emojis[0]));
                        emoji2Btn.setOnAction(e -> insertEmoji(emojis[1]));
                        emoji3Btn.setOnAction(e -> insertEmoji(emojis[2]));
                    }
                });
            } catch (Exception e) {
                // Ignore suggestion failures
            }
        }).start();
    }



    private void insertEmoji(String emoji) {
        messageInput.setText(messageInput.getText() + " " + emoji);
        messageInput.positionCaret(messageInput.getText().length());
        emojiSuggestionBox.setVisible(false);
        emojiSuggestionBox.setManaged(false);
    }

    @FXML
    private void handleDeleteConversation() {
        if (selectedConversation == null) {
            showError("Please select a conversation first.");
            return;
        }

        try {
            boolean isCreator = conversationRepo.isUserCreator(selectedConversation.getId(), Session.getCurrentUserId());
            if (!isCreator) {
                showError("Only the conversation creator can delete it.");
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Delete Conversation");
            confirm.setHeaderText("Delete \"" + selectedConversation.getName() + "\"?");
            confirm.setContentText("This will delete all messages and cannot be undone.");

            DialogPane dialogPane = confirm.getDialogPane();
            dialogPane.getStyleClass().add("dialog-pane");

            confirm.showAndWait().ifPresent(result -> {
                if (result == ButtonType.OK) {
                    try {
                        conversationRepo.delete(selectedConversation.getId());
                        loadConversations();
                        messageList.getItems().clear();

                        selectedConversation = null;
                        if (conversationTitleLabel != null) conversationTitleLabel.setText("Select a conversation");
                        if (deleteConversationBtn != null) deleteConversationBtn.setVisible(false);
                        if (updateConversationBtn != null) updateConversationBtn.setVisible(false);

                        showInfo("Conversation deleted successfully!");
                    } catch (SQLException e) {
                        showError("Failed to delete conversation: " + e.getMessage());
                    }
                }
            });
        } catch (SQLException e) {
            showError("Failed to check permissions: " + e.getMessage());
        }
    }

    @FXML
    private void handleManageParticipants() {
        if (selectedConversation == null) {
            showError("Please select a conversation first.");
            return;
        }

        try {
            ParticipantManagementDialog dialog = new ParticipantManagementDialog(selectedConversation);
            dialog.show();
            loadConversations();
        } catch (SQLException e) {
            showError("Failed to load participants: " + e.getMessage());
            e.printStackTrace();
        }
    }
    @FXML
    private void stopSpeaking() {
        if (ttsService != null) {
            ttsService.stopSpeaking();
            showInfo("⏹️ Speech stopped");
        }
    }

    @FXML
    private void toggleTheme() {
        Scene scene = themeBtn.getScene();
        scene.getStylesheets().clear();

        if (darkMode) {
            scene.getStylesheets().add(getClass().getResource("/com/example/pi_dev/messagingchat.css").toExternalForm());
            themeBtn.setText("🌙");
        } else {
            scene.getStylesheets().add(getClass().getResource("/com/example/pi_dev/messagingchat-dark.css").toExternalForm());
            themeBtn.setText("☀️");
        }

        darkMode = !darkMode;
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("An error occurred");
        alert.setContentText(msg);
        alert.getDialogPane().getStyleClass().add("dialog-pane");
        alert.showAndWait();
    }

    private void showInfo(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.getDialogPane().getStyleClass().add("dialog-pane");
        alert.showAndWait();
    }
}