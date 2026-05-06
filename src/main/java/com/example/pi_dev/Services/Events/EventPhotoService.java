package com.example.pi_dev.Services.Events;

import com.example.pi_dev.Entities.Events.EventPhoto;
import com.example.pi_dev.Utils.Events.Mydatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EventPhotoService {

    private Connection cnx;

    public EventPhotoService() {
        cnx = Mydatabase.getInstance().getConnextion();
    }

    // CREATE
    public void ajouter(EventPhoto photo) throws SQLException {
        String sql = "INSERT INTO event_photos (id_event, chemin_photo, description) VALUES (?, ?, ?)";

        PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

        ps.setInt(1, photo.getIdEvent());
        ps.setString(2, photo.getCheminPhoto());
        ps.setString(3, photo.getDescription());

        ps.executeUpdate();

        ResultSet generatedKeys = ps.getGeneratedKeys();
        if (generatedKeys.next()) {
            photo.setId(generatedKeys.getInt(1));
        }
    }

    // READ
    public List<EventPhoto> getPhotosByEvent(int idEvent) throws SQLException {
        List<EventPhoto> photos = new ArrayList<>();
        String sql = "SELECT * FROM event_photos WHERE id_event = ? ORDER BY date_creation ASC";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idEvent);

        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            EventPhoto photo = new EventPhoto();
            photo.setId(rs.getInt("id"));
            photo.setIdEvent(rs.getInt("id_event"));
            photo.setCheminPhoto(rs.getString("chemin_photo"));
            photo.setDescription(rs.getString("description"));
            photo.setDateCreation(rs.getTimestamp("date_creation"));

            photos.add(photo);
        }

        return photos;
    }

    // UPDATE
    public void modifier(EventPhoto photo) throws SQLException {
        String sql = "UPDATE event_photos SET chemin_photo=?, description=? WHERE id=?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, photo.getCheminPhoto());
        ps.setString(2, photo.getDescription());
        ps.setInt(3, photo.getId());

        ps.executeUpdate();
    }

    // DELETE
    public void supprimer(int idPhoto) throws SQLException {
        String sql = "DELETE FROM event_photos WHERE id=?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idPhoto);

        ps.executeUpdate();
    }

    // DELETE toutes les photos d'un événement
    public void supprimerPhotosEvent(int idEvent) throws SQLException {
        String sql = "DELETE FROM event_photos WHERE id_event=?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idEvent);

        ps.executeUpdate();
    }

    // Méthode utilitaire pour ajouter plusieurs photos
    public void ajouterPhotos(int idEvent, List<String> cheminsPhotos) throws SQLException {
        for (String chemin : cheminsPhotos) {
            EventPhoto photo = new EventPhoto(idEvent, chemin, null);
            ajouter(photo);
        }
    }

    // Méthode pour obtenir la première photo d'un événement
    public EventPhoto getPremierePhoto(int idEvent) throws SQLException {
        String sql = "SELECT * FROM event_photos WHERE id_event = ? ORDER BY date_creation ASC LIMIT 1";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, idEvent);

        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            EventPhoto photo = new EventPhoto();
            photo.setId(rs.getInt("id"));
            photo.setIdEvent(rs.getInt("id_event"));
            photo.setCheminPhoto(rs.getString("chemin_photo"));
            photo.setDescription(rs.getString("description"));
            photo.setDateCreation(rs.getTimestamp("date_creation"));

            return photo;
        }

        return null;
    }
}