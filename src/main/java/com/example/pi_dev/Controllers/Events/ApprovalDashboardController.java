package com.example.pi_dev.Controllers.Events;

import com.example.pi_dev.Entities.Events.Event;
import com.example.pi_dev.Session.Session;
import com.example.pi_dev.Utils.Events.Mydatabase;
import com.example.pi_dev.Utils.Users.UserSession;
import com.example.pi_dev.enums.RoleEnum;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ResourceBundle;

public class ApprovalDashboardController implements Initializable {

    @FXML
    private VBox activitiesContainer;
    @FXML
    private VBox eventsContainer;
    @FXML
    private VBox reservationsContainer;
    @FXML
    private Label summaryLabel;

    private Connection connection;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        connection = Mydatabase.getInstance().getConnextion();
        if (!isAdmin()) {
            showAlert("Accès réservé aux administrateurs.");
            closeWindow();
            return;
        }
        refreshData();
    }

    @FXML
    void refreshData(ActionEvent event) {
        refreshData();
    }

    private void refreshData() {
        loadPendingActivities();
        loadPendingEvents();
        loadPendingReservations();
        if (summaryLabel != null) {
            summaryLabel.setText("Validation des contenus en attente");
        }
    }

    private void loadPendingActivities() {
        if (activitiesContainer == null) {
            return;
        }
        activitiesContainer.getChildren().clear();
        String sql = "SELECT id, titre, type_activite, status, created_by_id FROM activites WHERE LOWER(COALESCE(status, 'en_attente')) IN ('en_attente', 'refuse') ORDER BY date_creation DESC";
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            boolean any = false;
            while (rs.next()) {
                any = true;
                activitiesContainer.getChildren().add(createActivityCard(
                        rs.getInt("id"),
                        rs.getString("titre"),
                        rs.getString("type_activite"),
                        rs.getString("status"),
                        rs.getString("created_by_id")));
            }
            if (!any) {
                activitiesContainer.getChildren().add(emptyLabel("Aucune activité en attente."));
            }
        } catch (SQLException e) {
            activitiesContainer.getChildren().add(emptyLabel("Erreur de chargement des activités."));
        }
    }

    private void loadPendingEvents() {
        if (eventsContainer == null) {
            return;
        }
        eventsContainer.getChildren().clear();
        String sql = "SELECT e.id, e.lieu, e.organisateur, COALESCE(e.status, e.statut) AS effective_status, e.created_by_id, a.titre AS activite_titre "
                +
                "FROM events e LEFT JOIN activites a ON a.id = e.id_activite " +
                "WHERE LOWER(COALESCE(e.status, e.statut, 'en_attente')) = 'en_attente' " +
                "ORDER BY e.date_creation DESC";
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            boolean any = false;
            while (rs.next()) {
                any = true;
                eventsContainer.getChildren().add(createEventCard(
                        rs.getInt("id"),
                        rs.getString("lieu"),
                        rs.getString("organisateur"),
                        rs.getString("activite_titre"),
                        rs.getString("effective_status"),
                        rs.getString("created_by_id")));
            }
            if (!any) {
                eventsContainer.getChildren().add(emptyLabel("Aucun événement en attente."));
            }
        } catch (SQLException e) {
            eventsContainer.getChildren().add(emptyLabel("Erreur de chargement des événements."));
        }
    }

    private VBox createActivityCard(int id, String title, String type, String status, String ownerId) {
        VBox card = new VBox(8);
        card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 14; -fx-border-color: #E5E7EB; -fx-border-radius: 14; -fx-padding: 16; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 4);");

        Label titleLabel = new Label(title != null ? title : "Sans titre");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0F2C4F;");

        Label typeLabel = new Label("Type: " + (type != null ? type : "N/A"));
        typeLabel.setStyle("-fx-text-fill: #4B5563;");

        Label ownerLabel = new Label("Créateur: " + (ownerId != null ? ownerId : "N/A"));
        ownerLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 12px;");

        Label statusLabel = new Label("Statut: " + normalizeStatusLabel(status));
        statusLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #9A3412;");

        VBox actions = new VBox(8);
        Button approve = new Button("Approuver");
        approve.setStyle(
                "-fx-background-color: #16A34A; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;");
        approve.setOnAction(e -> updateActivityStatus(id, "accepte"));

        Button reject = new Button("Désapprouver");
        reject.setStyle(
                "-fx-background-color: #DC2626; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;");
        reject.setOnAction(e -> updateActivityStatus(id, "refuse"));

        actions.getChildren().addAll(approve, reject);
        card.getChildren().addAll(titleLabel, typeLabel, ownerLabel, statusLabel, actions);
        return card;
    }

    private VBox createEventCard(int id, String lieu, String organisateur, String activiteTitre, String status,
            String ownerId) {
        VBox card = new VBox(8);
        card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 14; -fx-border-color: #E5E7EB; -fx-border-radius: 14; -fx-padding: 16; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 4);");

        Label titleLabel = new Label(organisateur != null ? organisateur : "Sans organisateur");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0F2C4F;");

        Label activityLabel = new Label("Activité: " + (activiteTitre != null ? activiteTitre : "N/A"));
        activityLabel.setStyle("-fx-text-fill: #4B5563;");

        Label lieuLabel = new Label("Lieu: " + (lieu != null ? lieu : "N/A"));
        lieuLabel.setStyle("-fx-text-fill: #4B5563;");

        Label ownerLabel = new Label("Créateur: " + (ownerId != null ? ownerId : "N/A"));
        ownerLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 12px;");

        Label statusLabel = new Label("Statut: " + normalizeStatusLabel(status));
        statusLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #9A3412;");

        VBox actions = new VBox(8);
        Button approve = new Button("Approuver");
        approve.setStyle(
                "-fx-background-color: #16A34A; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;");
        approve.setOnAction(e -> updateEventStatus(id, "accepte"));

        Button reject = new Button("Désapprouver");
        reject.setStyle(
                "-fx-background-color: #DC2626; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;");
        reject.setOnAction(e -> updateEventStatus(id, "refuse"));

        actions.getChildren().addAll(approve, reject);
        card.getChildren().addAll(titleLabel, activityLabel, lieuLabel, ownerLabel, statusLabel, actions);
        return card;
    }

    private Label emptyLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #6B7280; -fx-font-style: italic; -fx-padding: 10 0;");
        return label;
    }

    private void loadPendingReservations() {
        if (reservationsContainer == null) {
            return;
        }
        reservationsContainer.getChildren().clear();
        String sql = "SELECT r.id, r.nom_complet, r.email, r.nombre_personnes, r.statut, r.date_creation, e.lieu, e.date_debut FROM reservations r LEFT JOIN events e ON r.id_event = e.id WHERE LOWER(COALESCE(r.statut, 'en_attente')) IN ('en_attente', 'refuse') ORDER BY r.date_creation DESC";
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            boolean any = false;
            while (rs.next()) {
                any = true;
                reservationsContainer.getChildren().add(createReservationCard(
                        rs.getInt("id"),
                        rs.getString("nom_complet"),
                        rs.getString("email"),
                        rs.getInt("nombre_personnes"),
                        rs.getString("statut"),
                        rs.getString("lieu")));
            }
            if (!any) {
                reservationsContainer.getChildren().add(emptyLabel("Aucune réservation en attente."));
            }
        } catch (SQLException e) {
            reservationsContainer.getChildren().add(emptyLabel("Erreur de chargement des réservations."));
        }
    }

    private VBox createReservationCard(int id, String nomComplet, String email, int nombrePersonnes, String status,
            String lieu) {
        VBox card = new VBox(8);
        card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 14; -fx-border-color: #E5E7EB; -fx-border-radius: 14; -fx-padding: 16; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 4);");

        Label nameLabel = new Label("Réservé par: " + (nomComplet != null ? nomComplet : "N/A"));
        nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0F2C4F;");

        Label emailLabel = new Label("Email: " + (email != null ? email : "N/A"));
        emailLabel.setStyle("-fx-text-fill: #4B5563;");

        Label personsLabel = new Label("Nombre de personnes: " + nombrePersonnes);
        personsLabel.setStyle("-fx-text-fill: #4B5563;");

        Label lieuLabel = new Label("Lieu: " + (lieu != null ? lieu : "N/A"));
        lieuLabel.setStyle("-fx-text-fill: #4B5563;");

        Label statusLabel = new Label("Statut: " + normalizeStatusLabel(status));
        statusLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #9A3412;");

        VBox actions = new VBox(8);
        Button approve = new Button("Approuver");
        approve.setStyle(
                "-fx-background-color: #16A34A; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;");
        approve.setOnAction(e -> updateReservationStatus(id, "accepte"));

        Button reject = new Button("Désapprouver");
        reject.setStyle(
                "-fx-background-color: #DC2626; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;");
        reject.setOnAction(e -> updateReservationStatus(id, "refuse"));

        actions.getChildren().addAll(approve, reject);
        card.getChildren().addAll(nameLabel, emailLabel, personsLabel, lieuLabel, statusLabel, actions);
        return card;
    }

    private void updateActivityStatus(int id, String status) {
        String sql = "UPDATE activites SET status = ?, date_modification = NOW() WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, id);
            ps.executeUpdate();
            CatalogueRefreshManager.getInstance().requestRefresh();
            refreshData();
        } catch (SQLException e) {
            showAlert("Erreur lors de la mise à jour de l'activité.");
        }
    }

    private void updateEventStatus(int id, String status) {
        String sql = "UPDATE events SET status = ?, statut = ?, date_modification = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, status);
            ps.setInt(3, id);
            ps.executeUpdate();
            CatalogueRefreshManager.getInstance().requestRefresh();
            refreshData();
        } catch (SQLException e) {
            showAlert("Erreur lors de la mise à jour de l'événement.");
        }
    }

    private void updateReservationStatus(int id, String status) {
        String sql = "UPDATE reservations SET statut = ?, date_modification = NOW() WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, id);
            ps.executeUpdate();
            refreshData();
        } catch (SQLException e) {
            showAlert("Erreur lors de la mise à jour de la réservation.");
        }
    }

    private boolean isAdmin() {
        return UserSession.getInstance().getCurrentUser() != null
                && UserSession.getInstance().getCurrentUser().getRole() == RoleEnum.ADMIN;
    }

    private String normalizeStatusLabel(String status) {
        if (status == null || status.trim().isEmpty()) {
            return "En attente";
        }
        String normalized = status.trim().toLowerCase();
        if (normalized.equals("accepte")) {
            return "Accepté";
        }
        if (normalized.equals("refuse")) {
            return "Refusé";
        }
        if (normalized.equals("en_attente") || normalized.equals("en attente")) {
            return "En attente";
        }
        return normalized;
    }

    @FXML
    void closeWindow(ActionEvent event) {
        closeWindow();
    }

    private void closeWindow() {
        if (summaryLabel != null && summaryLabel.getScene() != null) {
            Stage stage = (Stage) summaryLabel.getScene().getWindow();
            stage.close();
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
