package com.example.pi_dev.Services.Booking;

import com.example.pi_dev.Entities.Booking.Review;
import com.example.pi_dev.Utils.Booking.Mydatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

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

        // --- Pre-insert Security & Risk Checks ---
        try {
            if (comment != null && !comment.isEmpty()) {
                double toxScore = new com.example.pi_dev.Services.Users.MessageToxicityService(com.example.pi_dev.common.ApiConfiguration.TOXICITY_API_URL).score(comment);
                if (toxScore > 80.0) {
                    comment = "[Review removed for violating community guidelines]";
                    
                    String uEmail = userId;
                    if (uEmail != null && !uEmail.contains("@")) {
                        try { uEmail = com.example.pi_dev.Utils.Users.UserSession.getInstance().getCurrentUser().getEmail(); } catch(Exception ignored) {}
                    }
                    if (uEmail != null && uEmail.contains("@")) {
                        new com.example.pi_dev.Services.Users.EmailService().sendEmail(
                            uEmail, 
                            "Toxicity Alert on WonderLust", 
                            "A recent review you posted was flagged by our Toxicity AI (Score: " + String.format("%.1f", toxScore) + "). It was filtered out. Warning: Repeated violations may result in a ban!"
                        );
                    }
                }
            }
            new com.example.pi_dev.common.services.ActivityLogService().log(
                userId, 
                "ADDREVIEW", 
                "User rated place ID " + placeId + " with " + rating + " stars."
            );
        } catch (Exception x) {
            x.printStackTrace();
        }

        // Try INSERT first; on duplicate, update and then fetch the existing id
        String insertSql = "INSERT INTO review (place_id, user_id, rating, comment, created_at) VALUES (?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE rating=VALUES(rating), comment=VALUES(comment)";

        try (PreparedStatement ps = con.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, placeId);
            ps.setString(2, userId);
            ps.setInt(3, rating);
            ps.setString(4, comment);
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            System.out.println("[ReviewService] Executing INSERT: placeId=" + placeId + ", userId=" + userId + ", rating=" + rating + ", comment=" + (comment != null ? comment.substring(0, Math.min(50, comment.length())) : "null"));
            ps.executeUpdate();

            // If a new row was inserted, LAST_INSERT_ID() > 0
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    if (id > 0) {
                        System.out.println("Review saved (new): id=" + id + " placeId=" + placeId + " userId=" + userId);
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
            System.err.println("[ReviewService] SQLException while adding review: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur ajout review: " + e.getMessage(), e);
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
