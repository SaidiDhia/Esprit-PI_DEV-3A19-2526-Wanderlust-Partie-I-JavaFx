package com.example.pi_dev.Services.Events;

import com.example.pi_dev.Entities.Events.Event;
import com.example.pi_dev.Entities.Events.Reservation;
import com.example.pi_dev.Utils.Events.Mydatabase;
import com.example.pi_dev.Services.Events.EventService;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReservationService {

    private Connection cnx;

    public ReservationService() {
        cnx = Mydatabase.getInstance().getConnextion();
        ensureUserColumnExists();
    }

    private void ensureUserColumnExists() {
        try {
            boolean exists = false;
            try (Statement st = cnx.createStatement();
                 ResultSet rs = st.executeQuery("SHOW COLUMNS FROM reservations LIKE 'user_id'")) {
                if (rs.next()) exists = true;
            }
            if (!exists) {
                try (Statement st = cnx.createStatement()) {
                    st.executeUpdate("ALTER TABLE reservations ADD COLUMN user_id VARCHAR(36) NULL AFTER id_event");
                }
            }
        } catch (SQLException e) {
            System.err.println("Impossible de vérifier la colonne user_id des réservations: " + e.getMessage());
        }
    }

    // CREATE
    public void ajouter(Reservation r) throws SQLException {

        String sql = "INSERT INTO reservations (id_event, user_id, nom_complet, email, telephone, nombre_personnes, demandes_speciales, statut, prix_total, date_creation) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";

        PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

        ps.setInt(1, r.getIdEvent());
        ps.setString(2, r.getUserId());
        ps.setString(3, r.getNomComplet());
        ps.setString(4, r.getEmail());
        ps.setString(5, r.getTelephone());
        ps.setInt(6, r.getNombrePersonnes());
        ps.setString(7, r.getDemandesSpeciales());
        ps.setString(8, r.getStatut() != null ? r.getStatut().name().toLowerCase() : "en_attente");

        // Compute prix_total: prefer the value in Reservation, otherwise compute from
        // Event price
        Double prixTotal = r.getPrixTotal();
        if (prixTotal == null) {
            try {
                EventService es = new EventService();
                com.example.pi_dev.Entities.Events.Event ev = es.findById(r.getIdEvent());
                if (ev != null && ev.getPrix() != null) {
                    prixTotal = ev.getPrix().doubleValue()
                            * (r.getNombrePersonnes() != null ? r.getNombrePersonnes() : 1);
                } else {
                    prixTotal = 0.0;
                }
            } catch (Exception ex) {
                prixTotal = 0.0;
            }
        }

        ps.setDouble(9, prixTotal);

        ps.executeUpdate();

        ResultSet generatedKeys = ps.getGeneratedKeys();
        if (generatedKeys.next()) {
            r.setId(generatedKeys.getInt(1));
        }
    }

    // READ
    public List<Reservation> afficher() throws SQLException {
        List<Reservation> list = new ArrayList<>();
        String sql = "SELECT * FROM reservations ORDER BY id DESC";

        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            Reservation r = new Reservation();
            r.setId(rs.getInt("id"));
            r.setIdEvent(rs.getInt("id_event"));
            r.setUserId(rs.getString("user_id"));
            r.setNomComplet(rs.getString("nom_complet"));
            r.setEmail(rs.getString("email"));
            r.setTelephone(rs.getString("telephone"));
            r.setNombrePersonnes(rs.getInt("nombre_personnes"));
            r.setDemandesSpeciales(rs.getString("demandes_speciales"));

            String statutStr = rs.getString("statut");
            if (statutStr != null) {
                try {
                    r.setStatut(Reservation.StatutReservation.valueOf(statutStr.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    r.setStatut(Reservation.StatutReservation.EN_ATTENTE);
                }
            }

            list.add(r);
        }
        return list;
    }

    public Reservation findById(int id) throws SQLException {
        String sql = "SELECT r.*, e.organisateur, e.lieu, e.date_debut, e.date_fin, e.prix AS event_prix " +
                "FROM reservations r LEFT JOIN events e ON e.id = r.id_event " +
                "WHERE r.id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Reservation r = mapReservation(rs);
                    Event event = new Event();
                    event.setId(rs.getInt("id_event"));
                    event.setOrganisateur(rs.getString("organisateur"));
                    event.setLieu(rs.getString("lieu"));
                    if (rs.getTimestamp("date_debut") != null)
                        event.setDateDebut(rs.getTimestamp("date_debut").toLocalDateTime());
                    if (rs.getTimestamp("date_fin") != null)
                        event.setDateFin(rs.getTimestamp("date_fin").toLocalDateTime());
                    if (rs.getBigDecimal("event_prix") != null)
                        event.setPrix(rs.getBigDecimal("event_prix"));
                    r.setEvent(event);
                    return r;
                }
            }
        }
        return null;
    }

    // UPDATE
    public void modifier(Reservation r) throws SQLException {

        String selectSql = "SELECT nombre_personnes, id_event FROM reservations WHERE id = ?";
        PreparedStatement selectPs = cnx.prepareStatement(selectSql);
        selectPs.setInt(1, r.getId());
        ResultSet rs = selectPs.executeQuery();

        int ancienNombre = 0;
        int ancienEvent = 0;

        if (rs.next()) {
            ancienNombre = rs.getInt("nombre_personnes");
            ancienEvent = rs.getInt("id_event");
        }

        String updateSql = "UPDATE reservations SET id_event=?, user_id=?, nom_complet=?, email=?, telephone=?, nombre_personnes=?, demandes_speciales=?, statut=?, prix_total=? WHERE id=?";

        PreparedStatement ps = cnx.prepareStatement(updateSql);

        ps.setInt(1, r.getIdEvent());
        ps.setString(2, r.getUserId());
        ps.setString(3, r.getNomComplet());
        ps.setString(4, r.getEmail());
        ps.setString(5, r.getTelephone());
        ps.setInt(6, r.getNombrePersonnes());
        ps.setString(7, r.getDemandesSpeciales());
        ps.setString(8, r.getStatut() != null ? r.getStatut().name().toLowerCase() : "en_attente");

        Double prixTotalUpdate = r.getPrixTotal();
        if (prixTotalUpdate == null) {
            try {
                EventService es = new EventService();
                com.example.pi_dev.Entities.Events.Event ev = es.findById(r.getIdEvent());
                if (ev != null && ev.getPrix() != null) {
                    prixTotalUpdate = ev.getPrix().doubleValue()
                            * (r.getNombrePersonnes() != null ? r.getNombrePersonnes() : 1);
                } else {
                    prixTotalUpdate = 0.0;
                }
            } catch (Exception ex) {
                prixTotalUpdate = 0.0;
            }
        }

        ps.setDouble(9, prixTotalUpdate);
        ps.setInt(10, r.getId());

        ps.executeUpdate();

        EventService es = new EventService();

        es.diminuerPlaces(ancienEvent, -ancienNombre);
        es.diminuerPlaces(r.getIdEvent(), r.getNombrePersonnes());
    }

    // Méthode add pour compatibilité avec le controller
    public void add(Reservation r) throws SQLException {
        ajouter(r);
    }

    public List<Reservation> getReservationsByUser(String userId) throws SQLException {
        List<Reservation> list = new ArrayList<>();
        if (userId == null || userId.trim().isEmpty()) {
            return list;
        }

        // Fallback: Use email if available to find reservations made without a valid session ID
        String userEmail = "";
        if (com.example.pi_dev.Utils.Users.UserSession.getInstance().getCurrentUser() != null) {
            userEmail = com.example.pi_dev.Utils.Users.UserSession.getInstance().getCurrentUser().getEmail();
        }

        String sql = "SELECT r.*, e.organisateur, e.lieu, e.date_debut, e.date_fin, e.prix AS event_prix " +
                "FROM reservations r LEFT JOIN events e ON e.id = r.id_event " +
                "WHERE (r.user_id = ? OR (r.email = ? AND r.email IS NOT NULL AND r.email != '')) ORDER BY r.id DESC";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, userEmail);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Reservation r = mapReservation(rs);
                    // Force ensure status is set even if NULL in DB
                    if (r.getStatut() == null) r.setStatut(Reservation.StatutReservation.EN_ATTENTE);
                    Event event = new Event();
                    event.setId(rs.getInt("id_event"));
                    event.setOrganisateur(rs.getString("organisateur"));
                    event.setLieu(rs.getString("lieu"));
                    event.setDateDebut(rs.getTimestamp("date_debut") != null ? rs.getTimestamp("date_debut").toLocalDateTime() : null);
                    event.setDateFin(rs.getTimestamp("date_fin") != null ? rs.getTimestamp("date_fin").toLocalDateTime() : null);
                    if (rs.getBigDecimal("event_prix") != null) {
                        event.setPrix(rs.getBigDecimal("event_prix"));
                    }
                    r.setEvent(event);
                    list.add(r);
                }
            }
        }

        return list;
    }

    // DELETE
    public void supprimer(int idReservation) throws SQLException {

        String selectSql = "SELECT id_event, nombre_personnes, statut FROM reservations WHERE id=?";
        PreparedStatement selectPs = cnx.prepareStatement(selectSql);
        selectPs.setInt(1, idReservation);
        ResultSet rs = selectPs.executeQuery();

        if (rs.next()) {
            int idEvent = rs.getInt("id_event");
            int nombre = rs.getInt("nombre_personnes");
            String statut = rs.getString("statut");

            String deleteSql = "DELETE FROM reservations WHERE id=?";
            PreparedStatement deletePs = cnx.prepareStatement(deleteSql);
            deletePs.setInt(1, idReservation);
            deletePs.executeUpdate();

            // Only release places if the reservation was already accepted
            if (statut != null && statut.equalsIgnoreCase("accepte")) {
                EventService es = new EventService();
                es.diminuerPlaces(idEvent, -nombre);
            }
        }
    }

    private Reservation mapReservation(ResultSet rs) throws SQLException {
        Reservation r = new Reservation();
        r.setId(rs.getInt("id"));
        r.setIdEvent(rs.getInt("id_event"));
        r.setUserId(rs.getString("user_id"));
        r.setNomComplet(rs.getString("nom_complet"));
        r.setEmail(rs.getString("email"));
        r.setTelephone(rs.getString("telephone"));
        r.setNombrePersonnes(rs.getInt("nombre_personnes"));
        r.setDemandesSpeciales(rs.getString("demandes_speciales"));
        r.setPrixTotal(rs.getDouble("prix_total"));

        String statutStr = rs.getString("statut");
        if (statutStr != null) {
            String normalized = statutStr.trim().toUpperCase().replace(" ", "_");
            
            // Handle synonyms and common variations (especially from Symfony/External)
            if (normalized.contains("ACCEPT") || normalized.contains("APPROV") || normalized.equals("VALIDATED") || 
                normalized.equals("1") || normalized.contains("CONFIRME")) {
                r.setStatut(Reservation.StatutReservation.ACCEPTE);
            } else if (normalized.contains("REFUS") || normalized.contains("REJECT") || normalized.equals("CANCELLED") || 
                       normalized.equals("2") || normalized.contains("ANNULE")) {
                r.setStatut(Reservation.StatutReservation.REFUSE);
            } else if (normalized.contains("ATTENTE") || normalized.contains("PENDING") || normalized.equals("0")) {
                r.setStatut(Reservation.StatutReservation.EN_ATTENTE);
            } else {
                try {
                    r.setStatut(Reservation.StatutReservation.valueOf(normalized));
                } catch (IllegalArgumentException e) {
                    r.setStatut(Reservation.StatutReservation.EN_ATTENTE);
                }
            }
        } else {
            r.setStatut(Reservation.StatutReservation.EN_ATTENTE);
        }

        r.setDateCreation(getSafeTimestamp(rs, "date_creation"));
        r.setDateModification(getSafeTimestamp(rs, "date_modification"));
        return r;
    }

    private java.sql.Timestamp getSafeTimestamp(ResultSet rs, String column) {
        try {
            return rs.getTimestamp(column);
        } catch (SQLException e) {
            return null;
        }
    }
}