package com.example.pi_dev.Services.Events;

import com.example.pi_dev.Entities.Events.Activite;
import com.example.pi_dev.Entities.Events.CategorieActivite;
import com.example.pi_dev.Entities.Events.Event;
import com.example.pi_dev.Entities.Events.EventPhoto;
import com.example.pi_dev.Utils.Events.Mydatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EventService {

    private Connection cnx;
    private EventPhotoService photoService;

    public EventService() {
        cnx = Mydatabase.getInstance().getConnextion();
        photoService = new EventPhotoService();
    }

    // CREATE
    public void ajouter(Event e) throws SQLException {
        String sql = "INSERT INTO events (lieu, date_debut, date_fin, prix, capacite_max, places_disponibles, organisateur, materiels_necessaires, image, telephone, email, status, statut, date_limite_inscription, date_creation, date_modification, video_youtube, created_by_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

        ps.setString(1, e.getLieu());
        ps.setTimestamp(2, e.getDateDebut() != null ? Timestamp.valueOf(e.getDateDebut()) : null);
        ps.setTimestamp(3, e.getDateFin() != null ? Timestamp.valueOf(e.getDateFin()) : null);
        ps.setBigDecimal(4, e.getPrix());
        ps.setInt(5, e.getCapaciteMax());
        ps.setInt(6, e.getPlacesDisponibles());
        ps.setString(7, e.getOrganisateur());
        ps.setString(8, e.getMaterielsNecessaires());
        ps.setString(9, e.getImage());
        ps.setString(10, e.getTelephone() != null ? String.valueOf(e.getTelephone()) : "");
        ps.setString(11, e.getEmail());
        String statusValue = e.getStatut() != null ? e.getStatut().name().toLowerCase() : "en_attente";
        ps.setString(12, statusValue);
        ps.setString(13, statusValue);

        Timestamp dateLimiteInscription;
        if (e.getDateDebut() != null) {
            java.time.LocalDate limite = e.getDateDebut().toLocalDate().minusDays(1);
            if (limite.isBefore(java.time.LocalDate.now())) {
                limite = java.time.LocalDate.now();
            }
            dateLimiteInscription = Timestamp.valueOf(limite.atStartOfDay());
        } else if (e.getDateFin() != null) {
            dateLimiteInscription = Timestamp.valueOf(e.getDateFin().toLocalDate().atStartOfDay());
        } else {
            dateLimiteInscription = new Timestamp(System.currentTimeMillis());
        }

        ps.setTimestamp(14, dateLimiteInscription);
        ps.setTimestamp(15,
                e.getDateCreation() != null ? e.getDateCreation() : new Timestamp(System.currentTimeMillis()));
        ps.setTimestamp(16, new Timestamp(System.currentTimeMillis()));
        ps.setString(17, e.getVideoYoutube());
        ps.setString(18, com.example.pi_dev.Session.Session.getCurrentUserId());

        ps.executeUpdate();

        ResultSet generatedKeys = ps.getGeneratedKeys();
        if (generatedKeys.next()) {
            e.setId(generatedKeys.getInt(1));

            // Link activity (using events_id and activities_id to match constraints)
            if (e.getIdActivite() > 0) {
                PreparedStatement psLink = cnx.prepareStatement("INSERT IGNORE INTO events_activities (events_id, activities_id) VALUES (?, ?)");
                psLink.setInt(1, e.getId());
                psLink.setInt(2, e.getIdActivite());
                psLink.executeUpdate();
            }

            if (e.getPhotos() != null && !e.getPhotos().isEmpty()) {
                photoService.ajouterPhotos(e.getId(), e.getPhotos());
            }
        }
    }

    // READ
    public List<Event> afficher() throws SQLException {
        List<Event> list = new ArrayList<>();
        String sql = "SELECT *, COALESCE(status, statut) AS effective_status FROM events ORDER BY date_creation DESC";

        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            Event e = new Event();
            e.setId(rs.getInt("id"));
            
            try (PreparedStatement psAct = cnx.prepareStatement("SELECT activities_id FROM events_activities WHERE events_id = ? LIMIT 1")) {
                psAct.setInt(1, e.getId());
                try (ResultSet rsAct = psAct.executeQuery()) {
                    if (rsAct.next()) e.setIdActivite(rsAct.getInt("activities_id"));
                }
            } catch (SQLException ignore) {}

            e.setLieu(rs.getString("lieu"));
            e.setPrix(rs.getBigDecimal("prix"));
            e.setCapaciteMax(rs.getInt("capacite_max"));
            e.setPlacesDisponibles(rs.getInt("places_disponibles"));
            e.setOrganisateur(rs.getString("organisateur"));
            e.setMaterielsNecessaires(rs.getString("materiels_necessaires"));
            e.setImage(rs.getString("image"));
            e.setVideoYoutube(rs.getString("video_youtube"));

            List<EventPhoto> photos = photoService.getPhotosByEvent(e.getId());
            List<String> cheminsPhotos = new ArrayList<>();
            for (EventPhoto photo : photos) {
                cheminsPhotos.add(photo.getCheminPhoto());
            }
            e.setPhotos(cheminsPhotos);

            list.add(e);
        }
        return list;
    }

    // UPDATE places
    public void diminuerPlaces(int idEvent, int nombre) throws SQLException {
        String sql = "UPDATE events SET places_disponibles = places_disponibles - ? WHERE id = ?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, nombre);
        ps.setInt(2, idEvent);

        ps.executeUpdate();
    }

    // DELETE PAR ID
    public void supprimer(int idEvent) throws SQLException {
        photoService.supprimerPhotosEvent(idEvent);

        String sql = "DELETE FROM events WHERE id = ?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idEvent);

        ps.executeUpdate();
    }

    // DELETE PAR ACTIVITE
    public void supprimerParActivite(int idActivite) throws SQLException {
        List<Event> events = getEventsByActivite(idActivite);

        for (Event event : events) {
            photoService.supprimerPhotosEvent(event.getId());
            // Also delete link
            PreparedStatement psDelLink = cnx.prepareStatement("DELETE FROM events_activities WHERE events_id = ?");
            psDelLink.setInt(1, event.getId());
            psDelLink.executeUpdate();
        }

        String sql = "DELETE FROM events WHERE id IN (SELECT events_id FROM events_activities WHERE activities_id = ?)";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idActivite);

        ps.executeUpdate();
    }

    public List<Event> getEventsByActivite(int idActivite) throws SQLException {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT e.* FROM events e JOIN events_activities ea ON e.id = ea.events_id WHERE ea.activities_id = ?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idActivite);

        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            Event e = new Event();
            e.setId(rs.getInt("id"));
            e.setIdActivite(idActivite);
            e.setPrix(rs.getBigDecimal("prix"));
            e.setCapaciteMax(rs.getInt("capacite_max"));
            e.setPlacesDisponibles(rs.getInt("places_disponibles"));
            e.setOrganisateur(rs.getString("organisateur"));
            e.setMaterielsNecessaires(rs.getString("materiels_necessaires"));
            e.setImage(rs.getString("image"));
            e.setVideoYoutube(rs.getString("video_youtube"));

            events.add(e);
        }

        return events;
    }

    public Event getEventById(int idEvent) throws SQLException {
        String sql = "SELECT *, COALESCE(status, statut) AS effective_status FROM events WHERE id = ?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idEvent);

        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            Event e = new Event();
            e.setId(rs.getInt("id"));
            
            try (PreparedStatement psAct = cnx.prepareStatement("SELECT activities_id FROM events_activities WHERE events_id = ? LIMIT 1")) {
                psAct.setInt(1, e.getId());
                try (ResultSet rsAct = psAct.executeQuery()) {
                    if (rsAct.next()) e.setIdActivite(rsAct.getInt("activities_id"));
                }
            } catch (SQLException ignore) {}

            e.setLieu(rs.getString("lieu"));
            e.setPrix(rs.getBigDecimal("prix"));
            e.setCapaciteMax(rs.getInt("capacite_max"));
            e.setPlacesDisponibles(rs.getInt("places_disponibles"));
            e.setOrganisateur(rs.getString("organisateur"));
            e.setMaterielsNecessaires(rs.getString("materiels_necessaires"));
            e.setImage(rs.getString("image"));
            e.setVideoYoutube(rs.getString("video_youtube"));

            List<EventPhoto> photos = photoService.getPhotosByEvent(e.getId());
            List<String> cheminsPhotos = new ArrayList<>();
            for (EventPhoto photo : photos) {
                cheminsPhotos.add(photo.getCheminPhoto());
            }
            e.setPhotos(cheminsPhotos);

            return e;
        }

        return null;
    }

    public Event findById(int id) throws SQLException {

        String sql = "SELECT e.*, COALESCE(e.status, e.statut) AS effective_status, a.titre, a.description as activite_description, a.type_activite, a.categorie, a.date_creation as activite_date_creation, ea.activities_id as activite_id "
                +
                "FROM events e " +
                "LEFT JOIN events_activities ea ON e.id = ea.events_id " +
                "LEFT JOIN activites a ON ea.activities_id = a.id " +
                "WHERE e.id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            Event e = new Event();
            e.setId(rs.getInt("id"));
            try { e.setIdActivite(rs.getInt("activite_id")); } catch (Exception ignore) {}
            e.setPrix(rs.getBigDecimal("prix"));
            e.setPlacesDisponibles(rs.getInt("places_disponibles"));
            e.setDateDebut(
                    rs.getTimestamp("date_debut") != null ? rs.getTimestamp("date_debut").toLocalDateTime() : null);
            e.setDateFin(rs.getTimestamp("date_fin") != null ? rs.getTimestamp("date_fin").toLocalDateTime() : null);
            e.setCapaciteMax(rs.getInt("capacite_max"));
            e.setOrganisateur(rs.getString("organisateur"));
            e.setLieu(rs.getString("lieu"));
            e.setDescription(rs.getString("description"));
            e.setEmail(rs.getString("email"));
            e.setTelephone(rs.getInt("telephone"));
            e.setMaterielsNecessaires(rs.getString("materiels_necessaires"));
            e.setImage(rs.getString("image"));
            e.setVideoYoutube(rs.getString("video_youtube"));
            e.setDateCreation(rs.getTimestamp("date_creation"));
            e.setDateModification(rs.getTimestamp("date_modification"));

            String statutStr = rs.getString("effective_status");
            if (statutStr != null) {
                try {
                    e.setStatut(Event.StatutEvent.valueOf(statutStr.toUpperCase()));
                } catch (IllegalArgumentException ex) {
                    e.setStatut(Event.StatutEvent.EN_ATTENTE);
                }
            }

            if (rs.getString("titre") != null) {
                Activite activite = new Activite();
                activite.setId(rs.getInt("activite_id"));
                activite.setTitre(rs.getString("titre"));
                activite.setDescription(rs.getString("activite_description"));
                activite.setTypeActivite(rs.getString("type_activite"));

                String categorieStr = rs.getString("categorie");
                if (categorieStr != null && !categorieStr.trim().isEmpty()) {
                    activite.setCategorie(CategorieActivite.fromDbValue(categorieStr));
                } else {
                    activite.setCategorie(CategorieActivite.NATURE);
                }
                activite.setDateCreation(rs.getTimestamp("activite_date_creation") != null
                        ? Timestamp.valueOf(rs.getTimestamp("activite_date_creation").toLocalDateTime())
                        : null);
                e.setActivite(activite);
            }

            return e;
        }
        return null;
    }

}