package com.example.pi_dev.Entities.Booking;

import java.time.LocalDateTime;

/**
 * Represents a user review for a place.
 */
public class Review {
    private int id;
    private int placeId;
    private String userId;
    private int rating; // 1..5
    private String comment;
    private LocalDateTime createdAt;

    // ── AI Review Insights fields ─────────────────────────────────────────────
    private String sentiment; // POSITIVE / NEUTRAL / NEGATIVE
    private String aiSummary; // AI-generated 1-sentence summary

    public Review() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPlaceId() {
        return placeId;
    }

    public void setPlaceId(int placeId) {
        this.placeId = placeId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getSentiment() {
        return sentiment;
    }

    public void setSentiment(String sentiment) {
        this.sentiment = sentiment;
    }

    public String getAiSummary() {
        return aiSummary;
    }

    public void setAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
    }
}
