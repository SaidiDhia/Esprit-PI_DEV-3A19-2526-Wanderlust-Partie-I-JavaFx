package com.example.pi_dev.Services.Events;

import com.example.pi_dev.Entities.Events.Reservation;
import com.example.pi_dev.Utils.Events.Mydatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReservationService {

    private Connection cnx;

    public ReservationService() {
        cnx = Mydatabase.getInstance().getConnextion();
    }

    // CREATE
    public void ajouter(Reservation r) throws SQLException {

        String sql = "INSERT INTO reservations (id_event, nom_complet, email, telephone, nombre_personnes, demandes_speciales, statut, prix_total, date_creation) VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())";

        PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

        ps.setInt(1, r.getIdEvent());
        ps.setString(2, r.getNomComplet());
        ps.setString(3, r.getEmail());
        ps.setString(4, r.getTelephone());
        ps.setInt(5, r.getNombrePersonnes());
        ps.setString(6, r.getDemandesSpeciales());
        ps.setString(7, r.getStatut().toString());

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

        ps.setDouble(8, prixTotal);

        ps.executeUpdate();

        ResultSet generatedKeys = ps.getGeneratedKeys();
        if (generatedKeys.next()) {
            r.setId(generatedKeys.getInt(1));
        }

        EventService es = new EventService();
        es.diminuerPlaces(r.getIdEvent(), r.getNombrePersonnes());
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
            r.setNomComplet(rs.getString("nom_complet"));
            r.setEmail(rs.getString("email"));
            r.setTelephone(rs.getString("telephone"));
            r.setNombrePersonnes(rs.getInt("nombre_personnes"));
            r.setDemandesSpeciales(rs.getString("demandes_speciales"));

            String statutStr = rs.getString("statut");
            if (statutStr != null) {
                try {
                    r.setStatut(Reservation.StatutReservation.valueOf(statutStr));
                } catch (IllegalArgumentException e) {
                    r.setStatut(Reservation.StatutReservation.EN_ATTENTE);
                }
            }

            list.add(r);
        }
        return list;
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

        String updateSql = "UPDATE reservations SET id_event=?, nom_complet=?, email=?, telephone=?, nombre_personnes=?, demandes_speciales=?, statut=?, prix_total=? WHERE id=?";

        PreparedStatement ps = cnx.prepareStatement(updateSql);

        ps.setInt(1, r.getIdEvent());
        ps.setString(2, r.getNomComplet());
        ps.setString(3, r.getEmail());
        ps.setString(4, r.getTelephone());
        ps.setInt(5, r.getNombrePersonnes());
        ps.setString(6, r.getDemandesSpeciales());
        ps.setString(7, r.getStatut().toString());

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

        ps.setDouble(8, prixTotalUpdate);
        ps.setInt(9, r.getId());

        ps.executeUpdate();

        EventService es = new EventService();

        es.diminuerPlaces(ancienEvent, -ancienNombre);
        es.diminuerPlaces(r.getIdEvent(), r.getNombrePersonnes());
    }

    // Méthode add pour compatibilité avec le controller
    public void add(Reservation r) throws SQLException {
        ajouter(r);
    }

    // DELETE
    public void supprimer(int idReservation) throws SQLException {

        String selectSql = "SELECT id_event, nombre_personnes FROM reservations WHERE id=?";
        PreparedStatement selectPs = cnx.prepareStatement(selectSql);
        selectPs.setInt(1, idReservation);
        ResultSet rs = selectPs.executeQuery();

        int idEvent = 0;
        int nombre = 0;

        if (rs.next()) {
            idEvent = rs.getInt("id_event");
            nombre = rs.getInt("nombre_personnes");
        }

        String deleteSql = "DELETE FROM reservations WHERE id=?";
        PreparedStatement deletePs = cnx.prepareStatement(deleteSql);
        deletePs.setInt(1, idReservation);
        deletePs.executeUpdate();

        EventService es = new EventService();
        es.diminuerPlaces(idEvent, -nombre);
    }
}