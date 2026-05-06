package com.example.pi_dev.Services.Booking;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service IA qui analyse le texte d'une review et retourne un sentiment +
 * résumé.
 * <p>
 * Utilise Gemini 1.5 Flash si la variable système GEMINI_API_KEY est définie.
 * Sinon, applique une heuristique locale (fallback garanti, ne fait jamais
 * crasher l'UI).
 */
public class AiReviewService {

    // ─── Gemini REST endpoint ──────────────────────────────────────────────────
    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=";

    // ─── Timeout ───────────────────────────────────────────────────────────────
    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    // ─── Mots-clés pour l'heuristique de fallback ─────────────────────────────
    private static final String[] POSITIVE_WORDS = {
            "great", "excellent", "wonderful", "amazing", "fantastic", "perfect",
            "love", "best", "clean", "comfortable", "nice", "good", "superb",
            "friendly", "beautiful", "recommend", "awesome", "cozy", "charming",
            "magnifique", "parfait", "excellent", "bien", "super", "propre", "genial"
    };

    private static final String[] NEGATIVE_WORDS = {
            "bad", "terrible", "awful", "horrible", "dirty", "noisy", "worst",
            "disappointed", "poor", "broken", "rude", "ugly", "problems",
            "disgusting", "smelly", "overpriced", "waste", "never", "avoid",
            "mauvais", "terrible", "horrible", "sale", "bruyant", "decevant", "nul"
    };

    // ─── Entry point ──────────────────────────────────────────────────────────

    /**
     * Analyse le texte d'une review (sans note — utilise le texte uniquement).
     */
    public AiReviewResult analyzeReview(String reviewText) {
        return analyzeReview(reviewText, -1);
    }

    /**
     * Analyse le texte d'une review en tenant compte aussi de la note (1..5).
     * Retourne toujours un résultat (jamais null).
     *
     * @param reviewText texte du commentaire
     * @param rating     note 1..5, ou -1 si inconnue
     */
    public AiReviewResult analyzeReview(String reviewText, int rating) {
        // If comment is blank but we have a rating, use rating alone
        if (reviewText == null || reviewText.isBlank()) {
            return heuristicFallback("", rating);
        }

        String apiKey = System.getProperty("GEMINI_API_KEY",
                System.getenv("GEMINI_API_KEY") != null ? System.getenv("GEMINI_API_KEY") : "");

        if (!apiKey.isBlank()) {
            try {
                return callGeminiApi(apiKey, reviewText);
            } catch (Exception e) {
                System.err.println("[AiReviewService] Gemini API failed, using fallback: " + e.getMessage());
            }
        }

        // Fallback heuristique (texte + note)
        return heuristicFallback(reviewText, rating);
    }

    // ─── Gemini API call ──────────────────────────────────────────────────────

    private AiReviewResult callGeminiApi(String apiKey, String reviewText) throws IOException, InterruptedException {

        String escaped = reviewText.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");

        String prompt = "Analyse this accommodation review and respond ONLY with valid JSON on a single line, "
                + "no markdown, no explanation. Format: {\"sentiment\":\"POSITIVE\",\"summary\":\"One sentence summary.\"} "
                + "Sentiment must be exactly POSITIVE, NEUTRAL, or NEGATIVE. Review: \\\"" + escaped + "\\\"";

        String body = "{"
                + "\"contents\":[{\"parts\":[{\"text\":\"" + prompt + "\"}]}],"
                + "\"generationConfig\":{\"temperature\":0.2,\"maxOutputTokens\":120}"
                + "}";

        HttpClient client = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GEMINI_URL + apiKey))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Gemini API returned HTTP " + response.statusCode() + ": " + response.body());
        }

        return parseGeminiResponse(response.body());
    }

    /**
     * Extrait le JSON renvoyé par Gemini.
     * Gemini wraps the answer in
     * {"candidates":[{"content":{"parts":[{"text":"..."}]}}]}.
     */
    private AiReviewResult parseGeminiResponse(String responseBody) {
        // Extract "text" field from the Gemini wrapper
        Pattern textPat = Pattern.compile("\"text\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
        Matcher textMatcher = textPat.matcher(responseBody);

        String text = textMatcher.group(1)
                .replace("\\n", " ")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .trim();

        // Parse the inner JSON object
        Pattern sentPat = Pattern.compile("\"sentiment\"\\s*:\\s*\"(POSITIVE|NEUTRAL|NEGATIVE)\"",
                Pattern.CASE_INSENSITIVE);
        Pattern sumPat = Pattern.compile("\"summary\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

        Matcher sentMatcher = sentPat.matcher(text);
        Matcher sumMatcher = sumPat.matcher(text);

        String sentiment = sentMatcher.find() ? sentMatcher.group(1).toUpperCase(Locale.ROOT) : "NEUTRAL";
        String summary = sumMatcher.find() ? sumMatcher.group(1).trim() : "Résumé non disponible.";

        return new AiReviewResult(sentiment, summary);
    }

    // ─── Heuristic fallback ───────────────────────────────────────────────────

    /**
     * Analyse locale combinant mots-clés du texte ET la note étoiles.
     * La note a plus de poids que les mots-clés.
     */
    private AiReviewResult heuristicFallback(String text, int rating) {

        int positiveScore = 0;
        int negativeScore = 0;

        // ── Rating contributes heavily ──────────────────────────────────────────
        if (rating >= 4) {
            positiveScore += 3; // 4-5 stars strongly positive
        } else if (rating == 3) {
            // neutral by default, keywords will decide
        } else if (rating >= 1) {
            negativeScore += 3; // 1-2 stars strongly negative
        }

        // ── Keyword scan ───────────────────────────────────────────────────────
        if (text != null && !text.isBlank()) {
            String lower = text.toLowerCase(Locale.ROOT);
            for (String w : POSITIVE_WORDS) {
                if (lower.contains(w))
                    positiveScore++;
            }
            for (String w : NEGATIVE_WORDS) {
                if (lower.contains(w))
                    negativeScore++;
            }
        }

        // ── Decision (threshold = 1, no +1 padding) ────────────────────────────
        String sentiment;
        String summary;

        if (positiveScore > negativeScore) {
            sentiment = "POSITIVE";
            summary = rating >= 4
                    ? "The reviewer had an excellent experience at this place."
                    : "The reviewer had a positive experience at this place.";
        } else if (negativeScore > positiveScore) {
            sentiment = "NEGATIVE";
            summary = "The reviewer noted several issues with this place.";
        } else {
            sentiment = "NEUTRAL";
            summary = "The reviewer had a mixed or neutral experience.";
        }

        return new AiReviewResult(sentiment, summary);
    }
}
