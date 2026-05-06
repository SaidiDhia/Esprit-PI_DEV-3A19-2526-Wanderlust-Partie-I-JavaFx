package com.example.pi_dev.Services.Booking;

/**
 * Holds the AI analysis result for a review.
 */
public class AiReviewResult {

    private final String sentiment; // POSITIVE / NEUTRAL / NEGATIVE
    private final String summary; // 1-sentence AI summary

    public AiReviewResult(String sentiment, String summary) {
        this.sentiment = sentiment;
        this.summary = summary;
    }

    public String getSentiment() {
        return sentiment;
    }

    public String getSummary() {
        return summary;
    }

    @Override
    public String toString() {
        return "AiReviewResult{sentiment='" + sentiment + "', summary='" + summary + "'}";
    }
}
