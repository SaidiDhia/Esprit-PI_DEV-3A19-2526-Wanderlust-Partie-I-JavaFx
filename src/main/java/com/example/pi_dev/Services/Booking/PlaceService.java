package com.example.pi_dev.Services.Booking;

import com.example.pi_dev.Entities.Booking.Place;
import com.example.pi_dev.Iservices.Booking.IPlaceService;
import com.example.pi_dev.Utils.Booking.Mydatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PlaceService implements IPlaceService {

    private final Connection con;

    public PlaceService() {
        con = Mydatabase.getInstance().getConnextion();
    }

    @Override
    public int ajouterPlace(Place p) {
        String sql = "INSERT INTO place " +
                "(host_id, title, description, price_per_day, capacity, max_guests, address, city, category, status, image_url, lat, lng) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getHostId());
            ps.setString(2, p.getTitle());
            ps.setString(3, p.getDescription());
            ps.setDouble(4, p.getPricePerDay());
            ps.setInt(5, p.getCapacity());
            ps.setInt(6, p.getMaxGuests());
            ps.setString(7, p.getAddress());
            ps.setString(8, p.getCity());
            ps.setString(9, p.getCategory());
            ps.setString(10, Place.Status.PENDING.name());
            ps.setString(11, p.getImageUrl());
            if (p.getLat() != null)
                ps.setDouble(12, p.getLat());
            else
                ps.setNull(12, Types.DOUBLE);
            if (p.getLng() != null)
                ps.setDouble(13, p.getLng());
            else
                ps.setNull(13, Types.DOUBLE);

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int newId = keys.getInt(1);
                    p.setId(newId);
                    System.out.println("Place ajoutée id=" + newId);
                    return newId;
                }
            }
            throw new RuntimeException("Place insérée mais ID non récupéré.");
        } catch (SQLException e) {
            throw new RuntimeException("Erreur ajout place", e);
        }
    }

    @Override
    public void modifierPlace(Place p) {
        String sql = "UPDATE place SET " +
                "host_id=?, title=?, description=?, price_per_day=?, capacity=?, max_guests=?, address=?, city=?, category=?, status=?, image_url=?, lat=?, lng=? "
                +
                "WHERE id=?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, p.getHostId());
            ps.setString(2, p.getTitle());
            ps.setString(3, p.getDescription());
            ps.setDouble(4, p.getPricePerDay());
            ps.setInt(5, p.getCapacity());
            ps.setInt(6, p.getMaxGuests());
            ps.setString(7, p.getAddress());
            ps.setString(8, p.getCity());
            ps.setString(9, p.getCategory());
            ps.setString(10, p.getStatus().name());
            ps.setString(11, p.getImageUrl());
            if (p.getLat() != null)
                ps.setDouble(12, p.getLat());
            else
                ps.setNull(12, Types.DOUBLE);
            if (p.getLng() != null)
                ps.setDouble(13, p.getLng());
            else
                ps.setNull(13, Types.DOUBLE);
            ps.setInt(14, p.getId());

            ps.executeUpdate();
            System.out.println("Place modifiée");
        } catch (SQLException e) {
            throw new RuntimeException("Erreur modification place", e);
        }
    }

    /**
     * Updates only the lat/lng columns for an existing place.
     */
    public void updateLatLng(int placeId, double lat, double lng) {
        String sql = "UPDATE place SET lat=?, lng=? WHERE id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDouble(1, lat);
            ps.setDouble(2, lng);
            ps.setInt(3, placeId);
            ps.executeUpdate();
            System.out.println("lat/lng updated for placeId=" + placeId);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur updateLatLng", e);
        }
    }

    @Override
    public void supprimerPlace(int id) {
        String sql = "DELETE FROM place WHERE id=?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("Place supprimée id=" + id);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur suppression place", e);
        }
    }

    @Override
    public List<Place> afficherPlaces() {
        List<Place> list = new ArrayList<>();
        String sql = "SELECT * FROM place ORDER BY id DESC";

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapPlace(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur affichage places", e);
        }

        return list;
    }

    @Override
    public Place getPlaceById(int id) {
        String sql = "SELECT * FROM place WHERE id=?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return mapPlace(rs);
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur getPlaceById", e);
        }
    }

    @Override
    public List<Place> afficherParStatus(Place.Status status) {
        return findByStatus(status);
    }

    @Override
    public List<Place> findApproved() {
        return findByStatus(Place.Status.APPROVED);
    }

    @Override
    public List<Place> findPending() {
        return findByStatus(Place.Status.PENDING);
    }

    @Override
    public List<Place> findByHost(String hostId) {
        List<Place> list = new ArrayList<>();
        String sql = "SELECT * FROM place WHERE host_id=? ORDER BY id DESC";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, hostId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapPlace(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur findByHost", e);
        }

        return list;
    }

    @Override
    public void updateStatus(int placeId, Place.Status status) {
        String sql = "UPDATE place SET status=? WHERE id=?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, placeId);
            ps.executeUpdate();
            System.out.println("Place status updated: id=" + placeId + " -> " + status);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur updateStatus place", e);
        }
    }

    // --- Private helpers ---
    private List<Place> findByStatus(Place.Status status) {
        List<Place> list = new ArrayList<>();
        String sql = "SELECT * FROM place WHERE status=? ORDER BY id DESC";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapPlace(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur findByStatus", e);
        }

        return list;
    }

    private Place mapPlace(ResultSet rs) throws SQLException {
        Place p = new Place();
        p.setId(rs.getInt("id"));
        p.setHostId(rs.getString("host_id"));
        p.setTitle(rs.getString("title"));
        p.setDescription(rs.getString("description"));
        p.setPricePerDay(rs.getDouble("price_per_day"));
        p.setCapacity(rs.getInt("capacity"));
        p.setMaxGuests(rs.getInt("max_guests"));
        p.setAddress(rs.getString("address"));
        p.setCity(rs.getString("city"));
        p.setCategory(rs.getString("category"));
        String placeStatus = rs.getString("status");
        if (placeStatus == null || placeStatus.trim().isEmpty()) {
            p.setStatus(Place.Status.PENDING);
        } else {
            try {
                p.setStatus(Place.Status.valueOf(placeStatus.trim().toUpperCase()));
            } catch (IllegalArgumentException ex) {
                p.setStatus(Place.Status.PENDING);
            }
        }
        p.setImageUrl(rs.getString("image_url"));

        // Geolocation columns (may be null if columns don't exist yet)
        try {
            double lat = rs.getDouble("lat");
            p.setLat(rs.wasNull() ? null : lat);
            double lng = rs.getDouble("lng");
            p.setLng(rs.wasNull() ? null : lng);
        } catch (SQLException ignored) {
            /* column not yet in DB */ }

        // Rating columns (may be null)
        try {
            double avg = rs.getDouble("avg_rating");
            p.setAvgRating(rs.wasNull() ? null : avg);
            p.setReviewsCount(rs.getInt("reviews_count"));
        } catch (SQLException ignored) {
            /* column not yet in DB */ }

        return p;
    }
}
