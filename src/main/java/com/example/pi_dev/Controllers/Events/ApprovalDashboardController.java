package com.example.pi_dev.Controllers.Events;

import com.example.pi_dev.Services.Events.EmailService;
import com.example.pi_dev.Services.Events.EventService;
import com.example.pi_dev.Services.Events.ReservationService;
import com.example.pi_dev.Entities.Events.Reservation;
import com.example.pi_dev.Utils.Events.CatalogueRefreshManager;
import com.example.pi_dev.Utils.Events.Mydatabase;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.Date;
import java.util.ResourceBundle;

public class ApprovalDashboardController implements Initializable {

    // ── Header Stats ───────────────────────────────────────────────────────────
    @FXML private Label summaryLabel;
    @FXML private Label statActivitesTotal, statActivitesAttente;
    @FXML private Label statEventsTotal,    statEventsAttente;
    @FXML private Label statReservationsTotal, statReservationsAttente;
    @FXML private Label statRevenuTotal;

    // ── Sidebar buttons ────────────────────────────────────────────────────────
    @FXML private Button sideHomeBtn, sideActivitesBtn, sideEventsBtn, sideReservationsBtn;
    @FXML private Label  sectionTitle;

    // ── Table containers ───────────────────────────────────────────────────────
    @FXML private VBox       homePanel;
    @FXML private VBox       homeContainer;
    @FXML private VBox       tablePanel;
    @FXML private HBox       tableHeader;
    @FXML private VBox       activitiesContainer, eventsContainer, reservationsContainer;

    // ── Charts ────────────────────────────────────────────────────────────────
    @FXML private javafx.scene.chart.LineChart<String, Number> movementChart;
    @FXML private javafx.scene.chart.PieChart statusPieChart;

    private Connection connection;
    private EmailService emailService = new EmailService();
    private ReservationService reservationService = new ReservationService();

    private static final String BTN_ACTIVE   = "-fx-background-color: #2D3E50; -fx-text-fill: #e9ecef; -fx-alignment: CENTER_LEFT; -fx-padding: 12 20; -fx-font-size: 13px; -fx-cursor: hand; -fx-background-radius: 0; -fx-border-color: transparent;";
    private static final String BTN_INACTIVE = "-fx-background-color: transparent; -fx-text-fill: #adb5bd; -fx-alignment: CENTER_LEFT; -fx-padding: 12 20; -fx-font-size: 13px; -fx-cursor: hand; -fx-background-radius: 0; -fx-border-color: transparent;";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        connection = Mydatabase.getInstance().getConnextion();
        emailService.initialize();
        refreshData();
        showHome(null); // default tab is now Home
    }

    // ── Actions ────────────────────────────────────────────────────────────────
    @FXML void refreshData(ActionEvent e) { refreshData(); }
    @FXML void closeWindow(ActionEvent e)  { closeWindow(); }

    @FXML void showHome(ActionEvent e) {
        setPanelVisible(homePanel, true);
        setPanelVisible(tablePanel, false);
        setActiveButtonStyle(sideHomeBtn);
        if (sectionTitle != null) sectionTitle.setText("🏠  Accueil");
        renderCalendar();
    }

    @FXML void showActivites(ActionEvent e) {
        setPanelVisible(homePanel, false);
        setPanelVisible(tablePanel, true);
        setVisible(activitiesContainer, true);
        setVisible(eventsContainer, false);
        setVisible(reservationsContainer, false);
        setActiveButtonStyle(sideActivitesBtn);
        if (sectionTitle != null) sectionTitle.setText("🏃  Activités");
        buildActiviteHeader();
        loadActivities();
    }

    @FXML void showEvents(ActionEvent e) {
        setPanelVisible(homePanel, false);
        setPanelVisible(tablePanel, true);
        setVisible(activitiesContainer, false);
        setVisible(eventsContainer, true);
        setVisible(reservationsContainer, false);
        setActiveButtonStyle(sideEventsBtn);
        if (sectionTitle != null) sectionTitle.setText("📅  Événements");
        buildEventHeader();
        loadEvents();
    }

    @FXML void showReservations(ActionEvent e) {
        setPanelVisible(homePanel, false);
        setPanelVisible(tablePanel, true);
        setVisible(activitiesContainer, false);
        setVisible(eventsContainer, false);
        setVisible(reservationsContainer, true);
        setActiveButtonStyle(sideReservationsBtn);
        if (sectionTitle != null) sectionTitle.setText("🎫  Réservations");
        buildReservationHeader();
        loadReservations();
    }

    private void setActiveButtonStyle(Button activeBtn) {
        sideHomeBtn.setStyle(BTN_INACTIVE);
        sideActivitesBtn.setStyle(BTN_INACTIVE);
        sideEventsBtn.setStyle(BTN_INACTIVE);
        sideReservationsBtn.setStyle(BTN_INACTIVE);
        if (activeBtn != null) activeBtn.setStyle(BTN_ACTIVE);
    }

    private void setPanelVisible(VBox panel, boolean visible) {
        if (panel != null) { panel.setVisible(visible); panel.setManaged(visible); }
    }

    private void setVisible(VBox container, boolean visible) {
        if (container != null) { container.setVisible(visible); container.setManaged(visible); }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  REFRESH GLOBAL
    // ══════════════════════════════════════════════════════════════════════════
    private void refreshData() {
        loadStats();
        renderCalendar();
        loadCharts();
        loadActivities();
        loadEvents();
        loadReservations();
        if (summaryLabel != null) summaryLabel.setText("Tableau de bord mis à jour");
    }

    private void loadCharts() {
        if (movementChart != null) {
            movementChart.getData().clear();
            javafx.scene.chart.XYChart.Series<String, Number> series = new javafx.scene.chart.XYChart.Series<>();
            series.setName("Réservations");
            
            String sql = "SELECT DATE_FORMAT(date_creation, '%M') as mois, COUNT(*) as cnt FROM reservations GROUP BY MONTH(date_creation) ORDER BY MONTH(date_creation)";
            try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    series.getData().add(new javafx.scene.chart.XYChart.Data<>(rs.getString("mois"), rs.getInt("cnt")));
                }
            } catch (SQLException e) { e.printStackTrace(); }
            movementChart.getData().add(series);
        }

        if (statusPieChart != null) {
            statusPieChart.getData().clear();
            String sql = "SELECT statut, COUNT(*) as cnt FROM reservations GROUP BY statut";
            try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    String s = rs.getString("statut");
                    statusPieChart.getData().add(new javafx.scene.chart.PieChart.Data(s != null ? s : "En attente", rs.getInt("cnt")));
                }
            } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    private void renderCalendar() {
        if (homeContainer == null) return;
        homeContainer.getChildren().clear();
        homeContainer.setAlignment(Pos.CENTER);

        LocalDate today = LocalDate.now();
        YearMonth yearMonth = YearMonth.from(today);
        
        // Month Title
        Label monthTitle = new Label(yearMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH).toUpperCase() + " " + yearMonth.getYear());
        monthTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1E293B; -fx-padding: 0 0 20 0;");
        homeContainer.getChildren().add(monthTitle);

        // Fetch events for the current month to show dots
        Set<Integer> eventDays = new HashSet<>();
        String sql = "SELECT date_debut FROM events WHERE MONTH(date_debut) = ? AND YEAR(date_debut) = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, yearMonth.getMonthValue());
            ps.setInt(2, yearMonth.getYear());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("date_debut");
                if (ts != null) {
                    eventDays.add(ts.toLocalDateTime().getDayOfMonth());
                }
            }
        } catch (SQLException e) {
            System.err.println("Calendar DB error: " + e.getMessage());
        }

        GridPane grid = new GridPane();
        grid.setHgap(30);
        grid.setVgap(25);
        grid.setAlignment(Pos.CENTER);
        grid.setStyle("-fx-background-color: white; -fx-padding: 40; -fx-background-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 20, 0, 0, 10);");

        // Weekday Headers
        String[] days = {"LUN", "MAR", "MER", "JEU", "VEN", "SAM", "DIM"};
        for (int i = 0; i < days.length; i++) {
            Label dayHead = new Label(days[i]);
            dayHead.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #94A3B8;");
            GridPane.setHalignment(dayHead, javafx.geometry.HPos.CENTER);
            grid.add(dayHead, i, 0);
        }

        // Days
        LocalDate firstOfMonth = yearMonth.atDay(1);
        int dayOfWeekValue = firstOfMonth.getDayOfWeek().getValue(); // 1 (Mon) to 7 (Sun)
        int daysInMonth = yearMonth.lengthOfMonth();

        int row = 1;
        int col = dayOfWeekValue - 1;

        // Previous month padding days (grey)
        LocalDate prevMonth = firstOfMonth.minusMonths(1);
        int daysInPrevMonth = YearMonth.from(prevMonth).lengthOfMonth();
        for (int i = 0; i < col; i++) {
            Label pDay = new Label(String.valueOf(daysInPrevMonth - col + i + 1));
            pDay.setStyle("-fx-text-fill: #CBD5E1; -fx-font-size: 13px;");
            GridPane.setHalignment(pDay, javafx.geometry.HPos.CENTER);
            grid.add(pDay, i, row);
        }

        for (int day = 1; day <= daysInMonth; day++) {
            VBox dayBox = new VBox(2);
            dayBox.setAlignment(Pos.CENTER);
            dayBox.setMinSize(45, 45);

            Label dayLbl = new Label(String.valueOf(day));
            dayLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #1E293B;");

            // Today highlight
            if (day == today.getDayOfMonth() && yearMonth.equals(YearMonth.from(today))) {
                dayBox.setStyle("-fx-background-color: #2D70B3; -fx-background-radius: 12;");
                dayLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");
            }

            dayBox.getChildren().add(dayLbl);

            // Event indicator (blue dot)
            if (eventDays.contains(day)) {
                Circle dot = new Circle(2, javafx.scene.paint.Color.web("#2D70B3"));
                dayBox.getChildren().add(dot);
            }

            GridPane.setHalignment(dayBox, javafx.geometry.HPos.CENTER);
            grid.add(dayBox, col, row);

            col++;
            if (col > 6) {
                col = 0;
                row++;
            }
        }
        
        homeContainer.getChildren().add(grid);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  STATS
    // ══════════════════════════════════════════════════════════════════════════
    private void loadStats() {
        try (Statement st = connection.createStatement()) {
            ResultSet rs = st.executeQuery("SELECT COUNT(*) AS tot, SUM(CASE WHEN LOWER(COALESCE(status,'en_attente'))='en_attente' THEN 1 ELSE 0 END) AS att FROM activites");
            if (rs.next()) { set(statActivitesTotal, rs.getString("tot")); set(statActivitesAttente, rs.getString("att")); }

            rs = st.executeQuery("SELECT COUNT(*) AS tot, SUM(CASE WHEN LOWER(COALESCE(status,statut,'en_attente'))='en_attente' THEN 1 ELSE 0 END) AS att FROM events");
            if (rs.next()) { set(statEventsTotal, rs.getString("tot")); set(statEventsAttente, rs.getString("att")); }

            rs = st.executeQuery("SELECT COUNT(*) AS tot, SUM(CASE WHEN LOWER(COALESCE(statut,'en_attente'))='en_attente' THEN 1 ELSE 0 END) AS att FROM reservations");
            if (rs.next()) { set(statReservationsTotal, rs.getString("tot")); set(statReservationsAttente, rs.getString("att")); }

            rs = st.executeQuery("SELECT COALESCE(SUM(prix_total),0) AS rev FROM reservations WHERE LOWER(COALESCE(statut,'')) IN ('accepte','confirmee')");
            if (rs.next()) set(statRevenuTotal, String.format("%.0f", rs.getDouble("rev")));
        } catch (SQLException e) { System.err.println("Stats: " + e.getMessage()); }
    }

    private void set(Label l, String v) { if (l != null) l.setText(v != null ? v : "0"); }

    // ══════════════════════════════════════════════════════════════════════════
    //  TABLE HEADERS
    // ══════════════════════════════════════════════════════════════════════════
    private void buildActiviteHeader()    { buildHeader("ID","Titre","Type","Statut","Créateur","Actions"); }
    private void buildEventHeader()       { buildHeader("ID","Organisateur","Lieu","Date début","Statut","Actions"); }
    private void buildReservationHeader() { buildHeader("ID","Client","Événement","Personnes","Statut","Actions"); }

    private void buildHeader(String... cols) {
        if (tableHeader == null) return;
        tableHeader.getChildren().clear();
        for (String col : cols) {
            Label l = new Label(col);
            l.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #64748B;");
            l.setPrefWidth(colWidth(col));
            if (col.equals("Actions")) HBox.setHgrow(l, Priority.ALWAYS);
            tableHeader.getChildren().add(l);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  ACTIVITÉS
    // ══════════════════════════════════════════════════════════════════════════
    private void loadActivities() {
        if (activitiesContainer == null) return;
        activitiesContainer.getChildren().clear();
        String sql = "SELECT id, titre, type_activite, status, created_by_id FROM activites ORDER BY date_creation DESC";
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            int row = 0;
            while (rs.next()) {
                activitiesContainer.getChildren().add(buildActiviteRow(
                    rs.getInt("id"), rs.getString("titre"),
                    rs.getString("type_activite"), rs.getString("status"),
                    rs.getString("created_by_id"), row++ % 2 == 0));
            }
            if (row == 0) activitiesContainer.getChildren().add(emptyRow("Aucune activité."));
        } catch (SQLException e) { activitiesContainer.getChildren().add(emptyRow("Erreur: " + e.getMessage())); }
    }

    private HBox buildActiviteRow(int id, String titre, String type, String status, String owner, boolean even) {
        HBox row = baseRow(even);
        row.getChildren().addAll(
            cell(String.valueOf(id), "ID"), cell(titre, "Titre"),
            cell(type, "Type"), statusBadge(status), cell(truncate(owner, 20), "Créateur")
        );
        HBox acts = actionsBox();
        Button ok = actionBtn("✓ Approuver", "#16A34A", "#F0FDF4");
        ok.setOnAction(e -> { updateActivityStatus(id, "accepte"); showActivites(null); });
        Button no = actionBtn("✗ Refuser", "#DC2626", "#FEF2F2");
        no.setOnAction(e -> { updateActivityStatus(id, "refuse"); showActivites(null); });
        Button del = actionBtn("🗑", "#64748B", "#F1F5F9");
        del.setOnAction(e -> { deleteActivity(id); showActivites(null); });
        acts.getChildren().addAll(ok, no, del);
        row.getChildren().add(acts);
        return row;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  ÉVÉNEMENTS
    // ══════════════════════════════════════════════════════════════════════════
    private void loadEvents() {
        if (eventsContainer == null) return;
        eventsContainer.getChildren().clear();
        String sql = "SELECT id, organisateur, lieu, date_debut, COALESCE(status, statut) AS eff FROM events ORDER BY date_creation DESC";
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            int row = 0;
            while (rs.next()) {
                String date = rs.getTimestamp("date_debut") != null ? rs.getTimestamp("date_debut").toString().substring(0, 10) : "—";
                eventsContainer.getChildren().add(buildEventRow(
                    rs.getInt("id"), rs.getString("organisateur"),
                    rs.getString("lieu"), date, rs.getString("eff"), row++ % 2 == 0));
            }
            if (row == 0) eventsContainer.getChildren().add(emptyRow("Aucun événement."));
        } catch (SQLException e) { eventsContainer.getChildren().add(emptyRow("Erreur: " + e.getMessage())); }
    }

    private HBox buildEventRow(int id, String org, String lieu, String date, String status, boolean even) {
        HBox row = baseRow(even);
        row.getChildren().addAll(
            cell(String.valueOf(id), "ID"), cell(org, "Titre"),
            cell(lieu, "Type"), cell(date, "Type"), statusBadge(status)
        );
        HBox acts = actionsBox();
        Button ok = actionBtn("✓ Approuver", "#16A34A", "#F0FDF4");
        ok.setOnAction(e -> { updateEventStatus(id, "accepte"); showEvents(null); });
        Button no = actionBtn("✗ Refuser", "#DC2626", "#FEF2F2");
        no.setOnAction(e -> { updateEventStatus(id, "refuse"); showEvents(null); });
        Button del = actionBtn("🗑", "#64748B", "#F1F5F9");
        del.setOnAction(e -> { deleteEvent(id); showEvents(null); });
        acts.getChildren().addAll(ok, no, del);
        row.getChildren().add(acts);
        return row;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  RÉSERVATIONS
    // ══════════════════════════════════════════════════════════════════════════
    private void loadReservations() {
        if (reservationsContainer == null) return;
        reservationsContainer.getChildren().clear();
        String sql = "SELECT r.id, r.nom_complet, r.nombre_personnes, r.statut, e.organisateur " +
                     "FROM reservations r LEFT JOIN events e ON r.id_event = e.id ORDER BY r.date_creation DESC";
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            int row = 0;
            while (rs.next()) {
                reservationsContainer.getChildren().add(buildReservationRow(
                    rs.getInt("id"), rs.getString("nom_complet"),
                    rs.getString("organisateur"), rs.getInt("nombre_personnes"),
                    rs.getString("statut"), row++ % 2 == 0));
            }
            if (row == 0) reservationsContainer.getChildren().add(emptyRow("Aucune réservation."));
        } catch (SQLException e) { reservationsContainer.getChildren().add(emptyRow("Erreur: " + e.getMessage())); }
    }

    private HBox buildReservationRow(int id, String client, String eventNom, int pers, String status, boolean even) {
        HBox row = baseRow(even);
        row.getChildren().addAll(
            cell(String.valueOf(id), "ID"), cell(client, "Titre"),
            cell(eventNom, "Titre"), cell(pers + " pers.", "Type"), statusBadge(status)
        );
        HBox acts = actionsBox();
        Button ok = actionBtn("✓ Confirmer", "#16A34A", "#F0FDF4");
        ok.setOnAction(e -> { updateReservationStatus(id, "confirmee"); showReservations(null); });
        Button no = actionBtn("✗ Refuser", "#DC2626", "#FEF2F2");
        no.setOnAction(e -> { updateReservationStatus(id, "refuse"); showReservations(null); });
        Button del = actionBtn("🗑", "#64748B", "#F1F5F9");
        del.setOnAction(e -> { deleteReservation(id); showReservations(null); });
        acts.getChildren().addAll(ok, no, del);
        row.getChildren().add(acts);
        return row;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  DB UPDATES & DELETES
    // ══════════════════════════════════════════════════════════════════════════
    private void updateActivityStatus(int id, String s) {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE activites SET status=?, date_modification=? WHERE id=?")) {
            ps.setString(1, s); ps.setTimestamp(2, new Timestamp(System.currentTimeMillis())); ps.setInt(3, id);
            ps.executeUpdate(); CatalogueRefreshManager.getInstance().requestRefresh();
        } catch (SQLException e) { showAlert(e.getMessage()); }
    }

    private void deleteActivity(int id) {
        if (!confirm("Supprimer cette activité ?")) return;
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM activites WHERE id=?")) {
            ps.setInt(1, id); ps.executeUpdate(); CatalogueRefreshManager.getInstance().requestRefresh();
        } catch (SQLException e) { showAlert(e.getMessage()); }
    }

    private void updateEventStatus(int id, String s) {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE events SET status=?, statut=?, date_modification=CURRENT_TIMESTAMP WHERE id=?")) {
            ps.setString(1, s); ps.setString(2, s); ps.setInt(3, id);
            ps.executeUpdate(); CatalogueRefreshManager.getInstance().requestRefresh();
        } catch (SQLException e) { showAlert(e.getMessage()); }
    }

    private void deleteEvent(int id) {
        if (!confirm("Supprimer cet événement et ses réservations ?")) return;
        try {
            connection.prepareStatement("DELETE FROM reservations WHERE id_event=" + id).executeUpdate();
            connection.prepareStatement("DELETE FROM events WHERE id=" + id).executeUpdate();
            CatalogueRefreshManager.getInstance().requestRefresh();
        } catch (SQLException e) { showAlert(e.getMessage()); }
    }

    private void updateReservationStatus(int id, String s) {
        try {
            Reservation res = reservationService.findById(id);
            if (res == null) return;
            String old = res.getStatut() != null ? res.getStatut().name().toLowerCase() : "en_attente";
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE reservations SET statut=?, date_modification=? WHERE id=?")) {
                ps.setString(1, s); ps.setTimestamp(2, new Timestamp(System.currentTimeMillis())); ps.setInt(3, id);
                ps.executeUpdate();
                EventService es = new EventService();
                boolean nowAcc = s.equalsIgnoreCase("accepte") || s.equalsIgnoreCase("confirmee");
                boolean wasAcc = old.equals("accepte") || old.equals("confirmee");
                if (nowAcc && !wasAcc) {
                    es.diminuerPlaces(res.getIdEvent(), res.getNombrePersonnes());
                    if (emailService.isAvailable()) emailService.sendReservationConfirmation(res);
                } else if (!nowAcc && wasAcc) {
                    es.diminuerPlaces(res.getIdEvent(), -res.getNombrePersonnes());
                }
                CatalogueRefreshManager.getInstance().requestRefresh();
            }
        } catch (SQLException e) { showAlert(e.getMessage()); }
    }

    private void deleteReservation(int id) {
        if (!confirm("Supprimer cette réservation ?")) return;
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM reservations WHERE id=?")) {
            ps.setInt(1, id); ps.executeUpdate(); CatalogueRefreshManager.getInstance().requestRefresh();
        } catch (SQLException e) { showAlert(e.getMessage()); }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  UI HELPERS
    // ══════════════════════════════════════════════════════════════════════════
    private HBox baseRow(boolean even) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        String bg = even ? "#FFFFFF" : "#F8FAFC";
        row.setStyle("-fx-background-color: " + bg + "; -fx-padding: 11 18; -fx-border-color: #F1F5F9; -fx-border-width: 0 0 1 0;");
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: #EFF6FF; -fx-padding: 11 18; -fx-border-color: #F1F5F9; -fx-border-width: 0 0 1 0;"));
        row.setOnMouseExited(ev -> row.setStyle("-fx-background-color: " + bg + "; -fx-padding: 11 18; -fx-border-color: #F1F5F9; -fx-border-width: 0 0 1 0;"));
        return row;
    }

    private double colWidth(String col) {
        return switch (col) { case "ID" -> 50; case "Personnes" -> 100; case "Actions" -> -1; default -> 180; };
    }

    private Label cell(String text, String col) {
        Label l = new Label(text != null ? text : "—");
        l.setStyle("-fx-font-size: 13px; -fx-text-fill: #1E293B;");
        l.setPrefWidth(colWidth(col));
        return l;
    }

    private Label statusBadge(String status) {
        String norm = status != null ? status.trim().toLowerCase() : "en_attente";
        String text, bg, fg;
        switch (norm) {
            case "accepte", "confirmee" -> { text = "Confirmé";   bg = "#DCFCE7"; fg = "#15803D"; }
            case "refuse"               -> { text = "Refusé";     bg = "#FEE2E2"; fg = "#B91C1C"; }
            default                     -> { text = "En attente"; bg = "#FEF9C3"; fg = "#92400E"; }
        }
        Label l = new Label(text);
        l.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg +
                "; -fx-background-radius: 20; -fx-padding: 3 12; -fx-font-size: 11px; -fx-font-weight: bold;");
        l.setPrefWidth(110);
        return l;
    }

    private HBox actionsBox() {
        HBox box = new HBox(6);
        box.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private Button actionBtn(String text, String textColor, String bgColor) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: " + bgColor + "; -fx-text-fill: " + textColor +
                "; -fx-background-radius: 6; -fx-padding: 5 14; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand; -fx-border-color: " + textColor + "; -fx-border-radius: 6; -fx-border-width: 1;");
        return b;
    }

    private HBox emptyRow(String text) {
        HBox row = new HBox();
        row.setStyle("-fx-padding: 24; -fx-background-color: white;");
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #94A3B8; -fx-font-style: italic; -fx-font-size: 13px;");
        row.getChildren().add(l);
        return row;
    }

    private String truncate(String s, int max) {
        if (s == null) return "—";
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }

    private boolean confirm(String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        a.setHeaderText(null);
        return a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    private void closeWindow() {
        if (summaryLabel != null && summaryLabel.getScene() != null)
            ((Stage) summaryLabel.getScene().getWindow()).close();
    }

    private void showAlert(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }
}
