package com.example.pi_dev.Controllers.Users;

import com.example.pi_dev.enums.RoleEnum;
import com.example.pi_dev.Entities.Users.User;
import com.example.pi_dev.Services.Users.UserService;
import com.example.pi_dev.Utils.Users.UserSession;
import com.example.pi_dev.common.services.ActivityLogService;
import com.example.pi_dev.common.models.ActivityLog;
import com.example.pi_dev.common.ApiConfiguration;
import com.example.pi_dev.Services.Users.*;
import com.example.pi_dev.Entities.Users.RiskScore;
import com.example.pi_dev.Repositories.Users.InMemoryRiskScoreRepository;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.application.Platform;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import javafx.stage.FileChooser;

public class AdminDashboardController {

    @FXML private TextField searchField;
    @FXML private ComboBox<RoleEnum> roleFilter;
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, User> colAvatar;
    @FXML private TableColumn<User, String> colName;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colPhone;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colStatus;
    @FXML private TableColumn<User, Void> colActions;
    @FXML private VBox activityLogContainer;

    @FXML private Tab tabRiskManagement;
    @FXML private FlowPane riskCardsContainer;

    private final UserService userService = new UserService();
    private final ActivityLogService activityLogService = new ActivityLogService();
    private final ActivityLoggerService activityLoggerService = new ActivityLoggerService(new com.example.pi_dev.Repositories.Users.InMemoryActivityLogRepository());
    private final RiskMonitor riskMonitor;
    
    public AdminDashboardController() {
        this.riskMonitor = new RiskMonitor(
            new RiskScoringEngine(),
            new AnomalyDetectionService(ApiConfiguration.ANOMALY_API_URL),
            new MessageToxicityService(ApiConfiguration.TOXICITY_API_URL),
            new BotBehaviorService(ApiConfiguration.BOT_BEHAVIOR_API_URL),
            new InMemoryRiskScoreRepository(),
            activityLoggerService
        );
    }
    private ObservableList<User> masterData = FXCollections.observableArrayList();
    private FilteredList<User> filteredData;

    @FXML private VBox dashboardActivityLogContainer;
    @FXML private Label adminCountLabel;
    @FXML private Label hostCountLabel;
    @FXML private Label participantCountLabel;
    
    @FXML private TabPane adminTabPane;
    @FXML private Tab tabManageUsers;
    @FXML private Tab tabActivityLogs;

    @FXML
    public void initialize() {
        setupTable();
        setupFilters();
        // Cards logic driven dynamically on data load
        loadData();
        loadActivityLogs();
        updateDashboardDistribution();
        Platform.runLater(() -> handleRefreshRiskScores(null));
    }

    // Unused if we ditch table view, remaining for backward compatibility
    public static class RiskScoreItem {
        private final User user;
        private final RiskScore riskScore;
        public RiskScoreItem(User user, RiskScore riskScore) {
            this.user = user;
            this.riskScore = riskScore;
        }
        public User getUser() { return user; }
        public RiskScore getRiskScore() { return riskScore; }
    }

    @FXML
    public void loadActivityLogs() {
        try {
            List<ActivityLog> logs = activityLogService.findAll();
            activityLogContainer.getChildren().clear();
            if (dashboardActivityLogContainer != null) {
                dashboardActivityLogContainer.getChildren().clear();
            }
            // Display up to 5 on the dashboard, all in the main tab
            int limit = 5;
            for (ActivityLog log : logs) {
                activityLogContainer.getChildren().add(createNotificationCard(log));
                if (dashboardActivityLogContainer != null && limit > 0) {
                    dashboardActivityLogContainer.getChildren().add(createNotificationCard(log));
                    limit--;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateDashboardDistribution() {
        if(adminCountLabel == null) return;
        long admins = masterData.stream().filter(u -> u.getRole() == RoleEnum.ADMIN).count();
        long hosts = masterData.stream().filter(u -> u.getRole() == RoleEnum.HOST).count();
        long participants = masterData.stream().filter(u -> u.getRole() == RoleEnum.PARTICIPANT).count();
        
        adminCountLabel.setText(String.valueOf(admins));
        hostCountLabel.setText(String.valueOf(hosts));
        participantCountLabel.setText(String.valueOf(participants));
    }

    private VBox createNotificationCard(ActivityLog log) {
        VBox card = new VBox();
        card.getStyleClass().addAll("log-row-container");

        HBox mainLayout = new HBox(15);
        mainLayout.setAlignment(Pos.CENTER_LEFT);

        User user = null;
        if (log.getUserEmail() != null && !log.getUserEmail().equals("System")) {
            user = userService.getUserByEmail(log.getUserEmail());
        }
        
        String actionType = log.getAction() != null ? log.getAction() : "UNKNOWN";
        
        // Determine borders and colors
        if (actionType.equals("SIGNIN") || actionType.equals("SIGNUP")) {
            card.getStyleClass().add("log-border-green");
        } else if (actionType.equals("USER_DELETE") || actionType.equals("USER_BAN")) {
             card.getStyleClass().add("log-border-red");
        } else {
             card.getStyleClass().add("log-border-gray");
        }
        
        // Left - Action Icon
        String iconEmoji = switch (actionType) {
            case "SIGNIN", "SIGNUP" -> "✅";
            case "SIGNOUT" -> "👋";
            case "USER_DELETE" -> "🗑️";
            case "TFA_SETUP" -> "🔑";
            default -> "ℹ️";
        };
        Label actionIcon = new Label(iconEmoji);
        actionIcon.setStyle("-fx-font-size: 20px;");
        
        // Avatar
        Circle profileCircle = new Circle(18);
        Image avatarImage = null;
        if (user != null && user.getProfilePicture() != null && !user.getProfilePicture().isEmpty()) {
            File file = new File("uploads/profiles/" + user.getProfilePicture());
            if (file.exists()) {
                avatarImage = new Image(file.toURI().toString(), 36, 36, true, true);
            }
        }
        // Fallback initials
        if (avatarImage != null) {
            profileCircle.setFill(new ImagePattern(avatarImage));
        } else {
            profileCircle.setFill(javafx.scene.paint.Color.web("#E5E7EB"));
        }

        VBox contentBox = new VBox(5);
        HBox.setHgrow(contentBox, javafx.scene.layout.Priority.ALWAYS);

        // Header: Title and Badge
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        String title = formatLogPhrase(log, user);
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #111827;");
        
        Label badgeLabel = new Label();
        badgeLabel.getStyleClass().add("badge-role");
        if (user != null && user.getRole() != null) {
            badgeLabel.setText(user.getRole().name());
            badgeLabel.getStyleClass().add("badge-role-" + user.getRole().name().toLowerCase());
        } else {
             badgeLabel.setText("SYSTEM");
             badgeLabel.getStyleClass().add("badge-role-admin");
        }
        header.getChildren().addAll(titleLabel, badgeLabel);

        // Subheader: User and IP
        HBox subheader = new HBox(5);
        subheader.setAlignment(Pos.CENTER_LEFT);
        
        String userName = (user != null) ? user.getFullName() : (log.getUserEmail() != null ? log.getUserEmail() : "System");
        Label userLabel = new Label(userName);
        userLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #4B5563;");
        
        Label dotLabel = new Label("•");
        dotLabel.setStyle("-fx-text-fill: #9CA3AF;");
        
        // Mock IP/Location representation like in the mock
        Label ipLabel = new Label("192.168.1.100");
        ipLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6B7280;");
        
        subheader.getChildren().addAll(userLabel, dotLabel, ipLabel);

        contentBox.getChildren().addAll(header, subheader);

        // Right side: Time
        Label timeLabel = new Label(log.getTimestamp() != null ? log.getTimestamp().toString().replace("T", " ").substring(0, Math.min(16, log.getTimestamp().toString().length())) : "Just now");
        timeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6B7280;");

        // Assemble
        mainLayout.getChildren().addAll(actionIcon, profileCircle, contentBox, timeLabel);
        card.getChildren().add(mainLayout);
        return card;
    }

    private String formatLogPhrase(ActivityLog log, User user) {
        String action = log.getAction();
        String name = (user != null) ? user.getFullName() : (log.getUserEmail() != null ? log.getUserEmail() : "System");
        String details = log.getDetails() != null ? log.getDetails() : "";
        return switch (action) {
            case "SIGNIN" -> "User signed in successfully";
            case "SIGNUP" -> "New user registration complete";
            case "SIGNOUT" -> "User signed out";
            case "USER_DELETE" -> "User account deleted";
            case "TFA_SETUP" -> "2FA Configuration changed";
            default -> action;
        };
    }

    private void setupTable() {
        colAvatar.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue()));
        colAvatar.setCellFactory(param -> new TableCell<>() {
            private final Circle circle = new Circle(20);
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setGraphic(null);
                } else {
                    String photoPath = user.getProfilePicture();
                    Image image = null;
                    if (photoPath != null && !photoPath.isEmpty()) {
                        File file = new File("uploads/profiles/" + photoPath);
                        if (file.exists()) {
                            image = new Image(file.toURI().toString(), 40, 40, true, true);
                        }
                    }
                    if (image == null) {
                        java.io.InputStream stream = getClass().getResourceAsStream("/com/example/pi_dev/user/default-avatar.png");
                        if (stream != null) {
                            image = new Image(stream, 40, 40, true, true);
                        }
                    }
                    if (image != null) {
                        circle.setFill(new ImagePattern(image));
                    } else {
                        circle.setFill(javafx.scene.paint.Color.web("#D1D5DB"));
                    }
                    setGraphic(circle);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        colName.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFullName()));
        colEmail.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getEmail()));
        colPhone.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getPhoneNumber()));
        colRole.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getRole().name()));
        colStatus.setCellValueFactory(cellData -> {
            boolean isActive = cellData.getValue().getIsActive() != null && cellData.getValue().getIsActive();
            return new SimpleStringProperty(isActive ? "Active" : "Inactive");
        });

        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final Button banBtn = new Button("Ban");
            private final HBox pane = new HBox(5, editBtn, deleteBtn, banBtn);

            {
                pane.setAlignment(Pos.CENTER);
                editBtn.getStyleClass().addAll("btn", "btn-secondary");
                editBtn.setStyle("-fx-font-size: 11; -fx-padding: 4 10;");
                deleteBtn.getStyleClass().addAll("btn", "btn-danger");
                deleteBtn.setStyle("-fx-font-size: 11; -fx-padding: 4 10;");
                banBtn.getStyleClass().addAll("btn", "btn-secondary");
                banBtn.setStyle("-fx-font-size: 11; -fx-padding: 4 10;");
                editBtn.setOnAction(event -> handleEditUser(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(event -> handleDeleteUser(getTableView().getItems().get(getIndex())));
                banBtn.setOnAction(event -> {
                    User u = getTableView().getItems().get(getIndex());
                    if (u.getIsActive() != null && u.getIsActive()) {
                        handleBanUser(u);
                    } else {
                        handleUnbanUser(u);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    User user = getTableView().getItems().get(getIndex());
                    banBtn.setText((user.getIsActive() != null && user.getIsActive()) ? "Ban" : "Unban");
                    setGraphic(pane);
                }
            }
        });
    }

    private void handleAlertUser(User user) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Alert User");
        alert.setHeaderText("Sending Alert to User");
        alert.setContentText("A risk alert has been sent to " + user.getEmail());
        alert.showAndWait();
        activityLoggerService.log("SECURITY", "RISK_ALERT", UserSession.getInstance().getCurrentUser().getUserId(), "Sent warning due to high risk score to user: " + user.getEmail());
        loadActivityLogs();
    }

    @FXML
    void handleRefreshRiskScores(ActionEvent event) {
        try {
            List<ActivityLog> allLogs = activityLogService.findAll();
            riskCardsContainer.getChildren().clear();

            for (User user : masterData) {
                // Determine user's explicit logs to compile Risk signals
                List<ActivityLog> userLogs = allLogs.stream()
                     .filter(l -> user.getEmail().equals(l.getUserName()) || (l.getUserId() != null && user.getUserId().toString().equals(l.getUserId())))
                     .toList();
                
                int recentCancellations = 0;
                StringBuilder toxicContent = new StringBuilder();
                int bookingHits = 0;
                int messageHits = 0;
                int reviewHits = 0;
                
                for(ActivityLog log : userLogs) {
                    String mod = log.getModule() != null ? log.getModule().toUpperCase() : "";
                    String act = log.getAction() != null ? log.getAction().toUpperCase() : "";
                    
                    if ("BOOKING".equals(mod) || act.contains("BOOK")) {
                        bookingHits++;
                        if (act.contains("CANCEL")) {
                            recentCancellations++;
                        }
                    } else if ("MESSAGING".equals(mod) || act.contains("MESSAGE")) {
                        messageHits++;
                        if (log.getContent() != null) toxicContent.append(log.getContent()).append(". ");
                    } else if ("REVIEWS".equals(mod) || act.contains("REVIEW")) {
                        reviewHits++;
                        if (log.getContent() != null) toxicContent.append(log.getContent()).append(". ");
                    }
                }
                
                java.util.Map<String, Object> metrics = new java.util.HashMap<>();
                metrics.put("time_between_actions_ms", 1000L); // Default safe MS
                metrics.put("click_speed", 0.0);
                metrics.put("recent_cancellations", recentCancellations);
                if (toxicContent.length() > 0) {
                    metrics.put("message_content", toxicContent.toString()); 
                }

                RiskScore score = riskMonitor.assessUserRisk(user, metrics);
                VBox card = createRiskCard(user, score, bookingHits, messageHits, reviewHits);
                riskCardsContainer.getChildren().add(card);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private VBox createRiskCard(User user, RiskScore score, int bookingC, int messageC, int reviewC) {
        VBox card = new VBox(15);
        card.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-min-width: 290; -fx-max-width: 320;");
        
        Label lblName = new Label(user.getEmail());
        lblName.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        
        Label lblBand = new Label(score.getRiskBand().toUpperCase());
        String bandColor;
        switch(score.getRiskBand()) {
            case "normal": bandColor = "#10B981"; break;
            case "suspicious": bandColor = "#F59E0B"; break;
            case "abusive": bandColor = "#EF4444"; break;
            case "critical": bandColor = "#991B1B"; break;
            default: bandColor = "#6B7280"; break;
        }
        lblBand.setStyle("-fx-background-color: " + bandColor + "; -fx-text-fill: white; -fx-padding: 4 8; -fx-background-radius: 4; -fx-font-size: 10px; -fx-font-weight: bold;");
        
        HBox header = new HBox(10, lblName, new Region(), lblBand);
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label lblScore = new Label("Overall Risk: " + score.getOverallRiskScore() + "/100");
        lblScore.setStyle("-fx-text-fill: " + bandColor + "; -fx-font-weight: bold; -fx-font-size: 14px;");
        
        VBox metricsBox = new VBox(5);
        metricsBox.getChildren().add(new Label(String.format("Anomaly: %.1f | Bot Behavior: %.1f", score.getAnomalyScore() != null ? score.getAnomalyScore() : 0.0, score.getClickSpeedScore() != null ? score.getClickSpeedScore() : 0.0)));
        metricsBox.getChildren().add(new Label(String.format("Toxicity: %.1f | Cancellation: %.1f", score.getMessageToxicityScore() != null ? score.getMessageToxicityScore() : 0.0, score.getCancellationAbuseScore() != null ? score.getCancellationAbuseScore() : 0.0)));
        metricsBox.setStyle("-fx-padding: 10; -fx-background-color: #F8FAFC; -fx-background-radius: 8; -fx-font-size: 11px; -fx-text-fill: #475569;");
        
        VBox activityBox = new VBox(5);
        activityBox.getChildren().add(new Label("Booking Interactions: " + bookingC));
        activityBox.getChildren().add(new Label("Messaging Interactions: " + messageC));
        activityBox.getChildren().add(new Label("Review Interactions: " + reviewC));
        activityBox.setStyle("-fx-padding: 10; -fx-background-color: #EFF6FF; -fx-background-radius: 8; -fx-font-size: 11px; -fx-text-fill: #1E40AF;");
        
        Button alertBtn = new Button("Dispatch Alert");
        alertBtn.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8; -fx-background-radius: 6;");
        alertBtn.setOnAction(e -> handleAlertUser(user));
        alertBtn.setMaxWidth(Double.MAX_VALUE);
        
        card.getChildren().addAll(header, lblScore, metricsBox, activityBox, alertBtn);
        return card;
    }

    private void setupFilters() {
        roleFilter.setItems(FXCollections.observableArrayList(RoleEnum.values()));
        filteredData = new FilteredList<>(masterData, p -> true);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> updateFilter());
        roleFilter.valueProperty().addListener((observable, oldValue, newValue) -> updateFilter());
        userTable.setItems(filteredData);
    }

    private void updateFilter() {
        filteredData.setPredicate(user -> {
            String searchText = searchField.getText().toLowerCase();
            RoleEnum selectedRole = roleFilter.getValue();
            boolean matchesSearch = searchText.isEmpty() ||
                    (user.getFullName() != null && user.getFullName().toLowerCase().contains(searchText)) ||
                    (user.getEmail() != null && user.getEmail().toLowerCase().contains(searchText));
            boolean matchesRole = selectedRole == null || user.getRole() == selectedRole;
            return matchesSearch && matchesRole;
        });
    }

    private void loadData() {
        masterData.setAll(userService.getAllUsers());
        updateDashboardDistribution();
    }

    @FXML
    void handleResetFilters(ActionEvent event) {
        searchField.clear();
        roleFilter.setValue(null);
    }

    @FXML
    void handleAddUser(ActionEvent event) {
        openUserForm(null);
    }

    private void handleEditUser(User user) {
        openUserForm(user);
    }

    private void handleDeleteUser(User user) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete User");
        alert.setHeaderText("Delete " + user.getFullName() + "?");
        alert.setContentText("Are you sure you want to delete this user?");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            userService.deleteUser(user.getUserId());
            activityLogService.log(UserSession.getInstance().getCurrentUser().getEmail(), "USER_DELETE", "Deleted user: " + user.getEmail());
            loadData();
        }
    }

    private void handleBanUser(User user) {
        if (UserSession.getInstance().getCurrentUser() != null &&
            user.getUserId() != null &&
            user.getUserId().equals(UserSession.getInstance().getCurrentUser().getUserId())) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Action Denied");
            alert.setHeaderText("Cannot Ban Self");
            alert.setContentText("You cannot ban your own admin account.");
            alert.showAndWait();
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Ban User");
        alert.setHeaderText("Ban " + user.getFullName() + "?");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                user.setIsActive(false);
                userService.updateUser(user);
                activityLogService.log(UserSession.getInstance().getCurrentUser().getEmail(), "USER_BAN", "Banned user: " + user.getEmail());
                loadData();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void handleUnbanUser(User user) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Unban User");
        alert.setHeaderText("Unban " + user.getFullName() + "?");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                user.setIsActive(true);
                userService.updateUser(user);
                activityLogService.log(UserSession.getInstance().getCurrentUser().getEmail(), "USER_UNBAN", "Unbanned user: " + user.getEmail());
                loadData();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void openUserForm(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pi_dev/user/user_form.fxml"));
            Parent root = loader.load();
            UserFormController controller = loader.getController();
            controller.setService(userService);
            controller.setUser(user);
            controller.setOnSaveCallback(this::loadData);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(user == null ? "Add User" : "Edit User");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleSettings(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pi_dev/user/settings.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleBackToHome(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/example/pi_dev/main/main_layout.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1200, 800));
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleLogout(ActionEvent event) {
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser != null) {
            activityLogService.log(currentUser.getEmail(), "SIGNOUT", "Admin logged out");
        }
        UserSession.getInstance().logout();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/example/pi_dev/user/login.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @FXML
    void showUserManagement(ActionEvent event) {
        if(adminTabPane != null && tabManageUsers != null) {
             adminTabPane.getSelectionModel().select(tabManageUsers);
        }
    }
    
    @FXML
    void handleShowActivityLogs(ActionEvent event) {
        if(adminTabPane != null && tabActivityLogs != null) {
             adminTabPane.getSelectionModel().select(tabActivityLogs);
        }
    }

    @FXML
    void exportLogsToPDF(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Activity Logs to PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Document", "*.pdf"));
        fileChooser.setInitialFileName("activity_logs.pdf");

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            try {
                Document document = new Document();
                PdfWriter.getInstance(document, new FileOutputStream(file));
                document.open();

                document.add(new Paragraph("System Activity Log"));
                document.add(new Paragraph("Generated at: " + LocalDateTime.now().toString().replace("T", " ")));
                document.add(new Paragraph(" "));

                PdfPTable table = new PdfPTable(new float[]{2, 2, 2, 4});
                table.setWidthPercentage(100);

                table.addCell(new PdfPCell(new Phrase("Time")));
                table.addCell(new PdfPCell(new Phrase("User")));
                table.addCell(new PdfPCell(new Phrase("Action")));
                table.addCell(new PdfPCell(new Phrase("Details")));

                List<ActivityLog> logs = activityLogService.findAll();
                for (ActivityLog log : logs) {
                    table.addCell(log.getTimestamp() != null ? log.getTimestamp().toString().replace("T", " ") : "N/A");
                    table.addCell(log.getUserEmail() != null ? log.getUserEmail() : "System");
                    table.addCell(log.getAction());
                    table.addCell(log.getDetails() != null ? log.getDetails() : "");
                }

                document.add(table);
                document.close();

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Export Successful");
                alert.setHeaderText(null);
                alert.setContentText("Activity logs exported to " + file.getAbsolutePath());
                alert.showAndWait();

            } catch (Exception e) {
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Export Failed");
                alert.setHeaderText("An error occurred during export.");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
            }
        }
    }
}
