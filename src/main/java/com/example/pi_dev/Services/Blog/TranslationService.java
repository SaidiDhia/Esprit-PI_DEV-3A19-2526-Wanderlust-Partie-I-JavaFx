package com.example.pi_dev.Services.Blog;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * TranslationService — uses MyMemory FREE API
 *
 * ✅ No API key needed!
 * ✅ Free tier: 5,000 words/day
 * ✅ Supports FR ↔ EN (and many other languages)
 *
 * API docs: https://mymemory.translated.net/doc/spec.php
 *
 * Place at: src/main/java/Services/TranslationService.java
 */
public class TranslationService {

    private static final String API_URL  = "https://api.mymemory.translated.net/get";
    private static final int    TIMEOUT  = 10; // seconds

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(TIMEOUT))
            .build();

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Detects the language of the text and translates:
     *   - French  → English
     *   - English → French
     *   - Other   → English (fallback)
     *
     * Never throws — returns the original text on any error.
     */
    public TranslationResult translateAuto(String text) {
        if (text == null || text.isBlank()) {
            return new TranslationResult(text, "en", "en", false);
        }

        try {
            // Detect language first with a short sample (max 50 chars for speed)
            String sample = text.length() > 50 ? text.substring(0, 50) : text;
            String detectedLang = detectLanguage(sample);

            String sourceLang;
            String targetLang;

            if ("fr".equals(detectedLang)) {
                sourceLang = "fr";
                targetLang = "en";
            } else if ("en".equals(detectedLang)) {
                sourceLang = "en";
                targetLang = "fr";
            } else {
                // Unknown language — translate to English
                sourceLang = detectedLang != null ? detectedLang : "auto";
                targetLang = "en";
            }

            String translated = callMyMemory(text, sourceLang, targetLang);
            boolean success = translated != null && !translated.equals(text);

            return new TranslationResult(
                    success ? translated : text,
                    sourceLang,
                    targetLang,
                    success
            );

        } catch (Exception e) {
            System.err.println("[Translation] Error: " + e.getMessage());
            return new TranslationResult(text, "?", "?", false);
        }
    }

    /**
     * Translates directly from source to target language.
     * Example: translate("Bonjour", "fr", "en")
     */
    public TranslationResult translate(String text, String sourceLang, String targetLang) {
        if (text == null || text.isBlank()) {
            return new TranslationResult(text, sourceLang, targetLang, false);
        }
        try {
            String translated = callMyMemory(text, sourceLang, targetLang);
            boolean success = translated != null && !translated.equals(text);
            return new TranslationResult(
                    success ? translated : text,
                    sourceLang,
                    targetLang,
                    success
            );
        } catch (Exception e) {
            System.err.println("[Translation] Error: " + e.getMessage());
            return new TranslationResult(text, sourceLang, targetLang, false);
        }
    }

    // =========================================================================
    // Language Detection (lightweight — checks common French patterns)
    // =========================================================================

    /**
     * Simple heuristic language detection.
     * MyMemory also returns the detected language in its response,
     * but doing a pre-check avoids a round-trip for common cases.
     */
    private String detectLanguage(String sample) {
        String lower = sample.toLowerCase();

        // French indicators: common words and accented characters
        int frScore = 0;
        String[] frWords = {"le", "la", "les", "de", "du", "des", "un", "une",
                "est", "et", "en", "je", "tu", "il", "nous", "vous", "ils",
                "pour", "dans", "sur", "avec", "par", "que", "qui", "au"};
        for (String w : frWords) {
            if (lower.contains(" " + w + " ") || lower.startsWith(w + " ")
                    || lower.endsWith(" " + w)) {
                frScore++;
            }
        }

        // Accented characters are strong French indicators
        for (char c : sample.toCharArray()) {
            if ("àâäéèêëîïôùûüçœæ".indexOf(c) >= 0) frScore += 3;
        }

        // English indicators
        int enScore = 0;
        String[] enWords = {"the", "a", "an", "is", "are", "was", "were",
                "i", "you", "he", "she", "we", "they", "this", "that",
                "with", "for", "on", "at", "to", "of", "in", "it"};
        for (String w : enWords) {
            if (lower.contains(" " + w + " ") || lower.startsWith(w + " ")
                    || lower.endsWith(" " + w)) {
                enScore++;
            }
        }

        if (frScore > enScore) return "fr";
        if (enScore > frScore) return "en";

        // Tie — use MyMemory's built-in detection by calling with "autodetect"
        return tryAutoDetect(sample);
    }

    private String tryAutoDetect(String sample) {
        try {
            // Call with "autodetect|en" — MyMemory will tell us the source lang
            String result = callMyMemoryRaw(sample, "autodetect", "en");
            if (result != null) {
                // MyMemory returns detected lang in responseData.detectedLanguage
                String detectedField = extractJsonString(result, "detectedLanguage");
                if (detectedField != null && detectedField.contains("-")) {
                    return detectedField.split("-")[0].toLowerCase();
                }
            }
        } catch (Exception ignored) {}
        return "en"; // safe default
    }

    // =========================================================================
    // MyMemory API Call
    // =========================================================================

    /**
     * Calls MyMemory and returns translated text, or null on failure.
     */
    private String callMyMemory(String text, String sourceLang, String targetLang)
            throws Exception {
        String raw = callMyMemoryRaw(text, sourceLang, targetLang);
        if (raw == null) return null;

        // Extract translatedText from response JSON
        // Response: {"responseData":{"translatedText":"...","match":0.8},...}
        String translated = extractJsonString(raw, "translatedText");
        if (translated == null || translated.isBlank()) return null;

        // MyMemory sometimes returns "MYMEMORY WARNING" on quota exceeded
        if (translated.toUpperCase().startsWith("MYMEMORY WARNING")) {
            System.err.println("[Translation] MyMemory quota warning: " + translated);
            return null;
        }

        System.out.println("[Translation] " + sourceLang + " → " + targetLang
                + " | \"" + text.substring(0, Math.min(30, text.length()))
                + "…\" → \"" + translated.substring(0, Math.min(30, translated.length())) + "…\"");

        return translated;
    }

    private String callMyMemoryRaw(String text, String sourceLang, String targetLang)
            throws Exception {
        String langPair = sourceLang + "|" + targetLang;
        String encodedText     = URLEncoder.encode(text,     StandardCharsets.UTF_8);
        String encodedLangPair = URLEncoder.encode(langPair, StandardCharsets.UTF_8);

        String url = API_URL + "?q=" + encodedText + "&langpair=" + encodedLangPair;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(TIMEOUT))
                .header("User-Agent", "Mozilla/5.0 BlogApp/1.0")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            System.err.println("[Translation] HTTP " + response.statusCode());
            return null;
        }

        return response.body();
    }

    // =========================================================================
    // Minimal JSON helper
    // =========================================================================

    /**
     * Extracts a string value from flat JSON by key name.
     * No external library needed.
     */
    private String extractJsonString(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIdx = json.indexOf(searchKey);
        if (keyIdx < 0) return null;

        int colonIdx = json.indexOf(':', keyIdx + searchKey.length());
        if (colonIdx < 0) return null;

        int valueStart = colonIdx + 1;
        while (valueStart < json.length()
                && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }
        if (valueStart >= json.length()) return null;
        if (json.charAt(valueStart) != '"') return null;

        StringBuilder sb = new StringBuilder();
        int i = valueStart + 1;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                switch (next) {
                    case '"'  -> { sb.append('"');  i += 2; }
                    case '\\' -> { sb.append('\\'); i += 2; }
                    case '/'  -> { sb.append('/');  i += 2; }
                    case 'n'  -> { sb.append('\n'); i += 2; }
                    case 'r'  -> { sb.append('\r'); i += 2; }
                    case 't'  -> { sb.append('\t'); i += 2; }
                    case 'b'  -> { sb.append('\b'); i += 2; }
                    case 'f'  -> { sb.append('\f'); i += 2; }
                    case 'u'  -> {
                        // Unicode escape: 4 hex digits (e.g. 00a0 = non-breaking space)
                        if (i + 5 < json.length()) {
                            String hex = json.substring(i + 2, i + 6);
                            try {
                                int codePoint = Integer.parseInt(hex, 16);
                                sb.append((char) codePoint);
                                i += 6;
                            } catch (NumberFormatException ex) {
                                sb.append('u');
                                i += 2;
                            }
                        } else {
                            sb.append('u');
                            i += 2;
                        }
                    }
                    default -> { sb.append(next); i += 2; }
                }
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    // =========================================================================
    // Result class
    // =========================================================================

    public static class TranslationResult {
        public final String  translatedText;
        public final String  sourceLang;
        public final String  targetLang;
        public final boolean success;

        public TranslationResult(String translatedText, String sourceLang,
                                 String targetLang, boolean success) {
            this.translatedText = translatedText;
            this.sourceLang     = sourceLang;
            this.targetLang     = targetLang;
            this.success        = success;
        }
    }
}
