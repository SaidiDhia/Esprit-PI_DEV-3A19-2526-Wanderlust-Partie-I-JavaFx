package com.example.pi_dev.Controllers.Users;

import com.example.pi_dev.enums.RoleEnum;
import com.example.pi_dev.Entities.Users.User;
import com.example.pi_dev.Services.Users.UserService;
import com.example.pi_dev.Utils.Users.UserSession;
import com.example.pi_dev.common.services.ActivityLogService;
import com.example.pi_dev.common.models.ActivityLog;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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

    private final UserService userService = new UserService();
    private final ActivityLogService activityLogService = new ActivityLogService();
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
        loadData();
        loadActivityLogs();
        updateDashboardDistribution();
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
