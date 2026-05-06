package com.example.pi_dev.Services.Booking;

import com.example.pi_dev.Entities.Booking.PlaceImage;
import com.example.pi_dev.Utils.Booking.Mydatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PlaceImageService {

    private final Connection con;

    public PlaceImageService() {
        con = Mydatabase.getInstance().getConnextion();
    }

    // =========================================================================
    // 4.1 — Add an image for a place
    // =========================================================================

    /**
     * Adds a new image to a place.
     * If isPrimary=true, first resets all other images to is_primary=0 for this
     * place.
     */
    public void addImage(int placeId, String url, int sortOrder, boolean isPrimary) {
        try {
            if (isPrimary) {
                resetPrimary(placeId);
            }
            String sql = "INSERT INTO place_images (place_id, url, sort_order, is_primary) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, placeId);
                ps.setString(2, url);
                ps.setInt(3, sortOrder);
                ps.setInt(4, isPrimary ? 1 : 0);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur addImage pour placeId=" + placeId, e);
        }
    }

    // =========================================================================
    // 4.2 — Get all images for a place
    // =========================================================================

    /**
     * Returns all images for a place, ordered: primary first, then by sort_order,
     * then by id.
     */
    public List<PlaceImage> getImagesForPlace(int placeId) {
        String sql = "SELECT * FROM place_images WHERE place_id=? " +
                "ORDER BY is_primary DESC, sort_order ASC, id ASC";
        List<PlaceImage> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, placeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapImage(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur getImagesForPlace placeId=" + placeId, e);
        }
        return list;
    }

    // =========================================================================
    // 4.3 — Get primary image URL
    // =========================================================================

    /**
     * Returns the URL of the primary image for a place.
     * Fallback: first image by sort_order. Returns null if no images exist.
     */
    public String getPrimaryImageUrl(int placeId) {
        // Try explicit primary first
        String sqlPrimary = "SELECT url FROM place_images WHERE place_id=? AND is_primary=1 LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sqlPrimary)) {
            ps.setInt(1, placeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("url");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur getPrimaryImageUrl (primary) placeId=" + placeId, e);
        }

        // Fallback: first image by sort_order
        String sqlFirst = "SELECT url FROM place_images WHERE place_id=? ORDER BY sort_order ASC, id ASC LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sqlFirst)) {
            ps.setInt(1, placeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("url");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur getPrimaryImageUrl (fallback) placeId=" + placeId, e);
        }

        return null; // no images at all
    }

    // =========================================================================
    // 4.4 — Set an image as primary
    // =========================================================================

    /**
     * Sets a specific image as primary for its place (resets all others first).
     */
    public void setPrimary(int placeId, int imageId) {
        try {
            resetPrimary(placeId);
            String sql = "UPDATE place_images SET is_primary=1 WHERE id=? AND place_id=?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, imageId);
                ps.setInt(2, placeId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur setPrimary imageId=" + imageId, e);
        }
    }

    // =========================================================================
    // 4.5 — Delete an image
    // =========================================================================

    /**
     * Deletes an image. If it was the primary, auto-promotes the next first image.
     */
    public void deleteImage(int imageId, int placeId) {
        // Check if this image is primary before deleting
        boolean wasPrimary = false;
        String checkSql = "SELECT is_primary FROM place_images WHERE id=?";
        try (PreparedStatement ps = con.prepareStatement(checkSql)) {
            ps.setInt(1, imageId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    wasPrimary = rs.getInt("is_primary") == 1;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur check isPrimary avant suppression", e);
        }

        // Delete it
        String delSql = "DELETE FROM place_images WHERE id=?";
        try (PreparedStatement ps = con.prepareStatement(delSql)) {
            ps.setInt(1, imageId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur deleteImage imageId=" + imageId, e);
        }

        // If it was primary, promote the first remaining image
        if (wasPrimary) {
            String promoteSql = "UPDATE place_images SET is_primary=1 " +
                    "WHERE place_id=? ORDER BY sort_order ASC, id ASC LIMIT 1";
            try (PreparedStatement ps = con.prepareStatement(promoteSql)) {
                ps.setInt(1, placeId);
                ps.executeUpdate();
            } catch (SQLException e) {
                // Non-fatal: no images left
                System.err.println("Warn: could not promote primary after delete: " + e.getMessage());
            }
        }
    }

    // =========================================================================
    // Utilities
    // =========================================================================

    /** Returns the number of images for a place. */
    public int getImageCount(int placeId) {
        String sql = "SELECT COUNT(*) FROM place_images WHERE place_id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, placeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            return 0;
        }
    }

    /** Resets all is_primary flags for a place to 0. */
    private void resetPrimary(int placeId) throws SQLException {
        String sql = "UPDATE place_images SET is_primary=0 WHERE place_id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, placeId);
            ps.executeUpdate();
        }
    }

    private PlaceImage mapImage(ResultSet rs) throws SQLException {
        PlaceImage img = new PlaceImage();
        img.setId(rs.getInt("id"));
        img.setPlaceId(rs.getInt("place_id"));
        img.setUrl(rs.getString("url"));
        img.setSortOrder(rs.getInt("sort_order"));
        img.setPrimary(rs.getInt("is_primary") == 1);
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            img.setCreatedAt(ts.toLocalDateTime());
        }
        return img;
    }
}
