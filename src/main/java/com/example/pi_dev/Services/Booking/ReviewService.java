package com.example.pi_dev.Services.Booking;

import com.example.pi_dev.Entities.Booking.Review;
import com.example.pi_dev.Utils.Booking.Mydatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC-based service for Place reviews and ratings.
 */
public class ReviewService {

    private final Connection con;
    private final AiReviewService aiReviewService = new AiReviewService();

    public ReviewService() {
        con = Mydatabase.getInstance().getConnextion();
    }

    // ─── Insert (returns generated ID) ────────────────────────────────────────

    /**
     * Inserts a new review (or updates if user already reviewed this place).
     * Returns the review ID (generated or existing).
     */
    public int addReview(int placeId, String userId, int rating, String comment) {
        if (rating < 1 || rating > 5)
            throw new IllegalArgumentException("Rating must be between 1 and 5.");

        // Try INSERT first; on duplicate, update and then fetch the existing id
        String insertSql = "INSERT INTO review (place_id, user_id, rating, comment) VALUES (?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE rating=VALUES(rating), comment=VALUES(comment)";

        try (PreparedStatement ps = con.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, placeId);
            ps.setString(2, userId);
            ps.setInt(3, rating);
            ps.setString(4, comment);
            ps.executeUpdate();

            // If a new row was inserted, LAST_INSERT_ID() > 0
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    if (id > 0) {
                        System.out
                                .println("Review saved (new): id=" + id + " placeId=" + placeId + " userId=" + userId);
                        return (int) id;
                    }
                }
            }

            // ON DUPLICATE KEY UPDATE fired — fetch the existing review id
            String selectSql = "SELECT id FROM review WHERE place_id=? AND user_id=?";
            try (PreparedStatement ps2 = con.prepareStatement(selectSql)) {
                ps2.setInt(1, placeId);
                ps2.setString(2, userId);
                try (ResultSet rs = ps2.executeQuery()) {
                    if (rs.next()) {
                        int id = rs.getInt(1);
                        System.out.println("Review updated (existing): id=" + id);
                        return id;
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur ajout review", e);
        }

        throw new RuntimeException("Could not retrieve review ID after insert/update.");
    }

    // ─── AI analysis + update ─────────────────────────────────────────────────

    /**
     * Calls the AI service to analyse the comment, then persists sentiment +
     * ai_summary.
     * Never throws: if AI or DB update fails, logs the error and moves on.
     */
    public void analyzeAndUpdateReview(int reviewId, String comment) {
        try {
            AiReviewResult result = aiReviewService.analyzeReview(comment);
            System.out.println("[AI] reviewId=" + reviewId + " → " + result);

            String sql = "UPDATE review SET sentiment=?, ai_summary=? WHERE id=?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, result.getSentiment());
                ps.setString(2, result.getSummary());
                ps.setInt(3, reviewId);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            System.err.println(
                    "[ReviewService] analyzeAndUpdateReview failed for id=" + reviewId + ": " + e.getMessage());
            // Intentional: review stays in DB, just without AI data
        }
    }

    // ─── Rating refresh ───────────────────────────────────────────────────────

    /**
     * Returns the average rating for a place (0.0 if no reviews).
     */
    public double getAverageRating(int placeId) {
        String sql = "SELECT AVG(rating) FROM review WHERE place_id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, placeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double avg = rs.getDouble(1);
                    return rs.wasNull() ? 0.0 : avg;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur getAverageRating", e);
        }
        return 0.0;
    }

    /**
     * Returns the number of reviews for a place.
     */
    public int getReviewsCount(int placeId) {
        String sql = "SELECT COUNT(*) FROM review WHERE place_id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, placeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur getReviewsCount", e);
        }
    }

    /**
     * Recalculates AVG rating and COUNT for the given place and persists them in
     * place.avg_rating / reviews_count.
     */
    public void refreshPlaceRatingStats(int placeId) {
        String sql = "UPDATE place SET avg_rating = (SELECT AVG(r.rating) FROM review r WHERE r.place_id = ?), " +
                "reviews_count = (SELECT COUNT(*) FROM review r2 WHERE r2.place_id = ?) " +
                "WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, placeId);
            ps.setInt(2, placeId);
            ps.setInt(3, placeId);
            ps.executeUpdate();
            System.out.println("Rating stats refreshed for placeId=" + placeId);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur refreshPlaceRatingStats", e);
        }
    }

    // ─── Reviews list ─────────────────────────────────────────────────────────

    /**
     * Returns all reviews for a place ordered by most recent first.
     * Includes AI fields (sentiment, ai_summary) — may be null if AI failed.
     */
    public List<Review> getReviewsForPlace(int placeId) {
        List<Review> list = new ArrayList<>();
        String sql = "SELECT id, place_id, user_id, rating, comment, sentiment, ai_summary, created_at " +
                "FROM review WHERE place_id=? ORDER BY created_at DESC";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, placeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Review r = new Review();
                    r.setId(rs.getInt("id"));
                    r.setPlaceId(rs.getInt("place_id"));
                    r.setUserId(rs.getString("user_id"));
                    r.setRating(rs.getInt("rating"));
                    r.setComment(rs.getString("comment"));
                    r.setSentiment(rs.getString("sentiment"));
                    r.setAiSummary(rs.getString("ai_summary"));
                    Timestamp ts = rs.getTimestamp("created_at");
                    if (ts != null)
                        r.setCreatedAt(ts.toLocalDateTime());
                    list.add(r);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ReviewService] getReviewsForPlace error: " + e.getMessage());
        }
        return list;
    }

    // ─── Eligibility check ────────────────────────────────────────────────────

    /**
     * Checks if a user has already reviewed a place.
     */
    public boolean hasReviewed(int placeId, String userId) {
        String sql = "SELECT COUNT(*) FROM review WHERE place_id=? AND user_id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, placeId);
            ps.setString(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur hasReviewed", e);
        }
    }
}
