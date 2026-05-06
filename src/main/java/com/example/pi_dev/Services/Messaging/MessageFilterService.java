package com.example.pi_dev.Services.Messaging;

import java.util.*;
import java.util.regex.Pattern;

public class MessageFilterService {

    // List of inappropriate words to filter (in French, Arabic, English)
    private static final Set<String> BAD_WORDS = new HashSet<>(Arrays.asList(
            // French bad words (most common)
            "merde", "putain", "connard", "connasse", "salope", "enculé", "bâtard",
            "fils de pute", "nique", "bite", "couille", "chatte", "pd", "tapette",
            "enfoiré", "salaud", "garce", "putain de merde", "foutre", "branleur",
            "branleuse", "chiant", "chier", "débile", "abruti", "crétin", "imbécile",

            // English bad words
            "fuck", "shit", "asshole", "bitch", "dick", "cunt", "pussy", "bastard",
            "motherfucker", "damn", "piss", "crap", "twat", "wanker",
            "cock", "suck", "blowjob", "whore", "slut", "fag", "retard",

            // Arabic bad words (common ones)
            "كس", "شرموط", "عرص", "ابن الكلب", "يا كلب",
            "يا حمار", "يا غبي", "خرة",  "كسمك", "كسم",
             "خول", "لوطي", "شرموطة", "نبيل",

            // Mixed/Common internet slang
            "stfu", "wtf", "omg", "lmao", "lmfao", "gtfo", "fck", "fuk",
            "sht", "bs", "dmn", "prn", "p0rn", "s3x"
    ));

    // Stronger words that should block the message entirely
    private static final Set<String> STRONG_BAD_WORDS = new HashSet<>(Arrays.asList(
            // These words will block the message completely
            "fuck", "shit", "putain", "merde", "كس", "شرموط",  "كسمك"
    ));

    // Pattern to detect words with leetspeak (like f*ck, sh!t)
    private static final Pattern LEET_PATTERN = Pattern.compile(
            "[a-zA-Z0-9]*[\\*\\!\\@\\#\\$\\%][a-zA-Z0-9]*"
    );

    // Common letter substitutions for leetspeak
    private static final Map<Character, Character> LEET_MAP = new HashMap<>();

    public MessageFilterService() {
        // Initialize leet speak mapping


    }

    /**
     * Filter a message - replaces bad words with asterisks
     * @return The filtered message
     */
    public String filterMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return message;
        }

        String filtered = message;

        // Replace each bad word with asterisks
        for (String badWord : BAD_WORDS) {
            // Case insensitive replacement with word boundaries
            String regex = "(?i)\\b" + Pattern.quote(badWord) + "\\b";
            String replacement = "*".repeat(badWord.length());
            filtered = filtered.replaceAll(regex, replacement);
        }

        // Check for leetspeak versions
        String normalized = normalizeLeetSpeak(message);
        for (String badWord : BAD_WORDS) {
            if (normalized.contains(badWord)) {
                // Find the original word and replace it
                String[] words = message.split("\\s+");
                for (String word : words) {
                    if (normalizeLeetSpeak(word).equals(badWord)) {
                        filtered = filtered.replace(word, "*".repeat(word.length()));
                    }
                }
            }
        }

        return filtered;
    }

    /**
     * Convert leetspeak to normal text
     */
    private String normalizeLeetSpeak(String text) {
        StringBuilder normalized = new StringBuilder();
        for (char c : text.toLowerCase().toCharArray()) {
            if (LEET_MAP.containsKey(c)) {
                normalized.append(LEET_MAP.get(c));
            } else if (Character.isLetterOrDigit(c)) {
                normalized.append(c);
            }
        }
        return normalized.toString();
    }

    /**
     * Check if message contains bad words
     * @return true if bad words found
     */
    public boolean containsProfanity(String message) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }

        String lowerMessage = message.toLowerCase();

        // Check exact bad words
        for (String badWord : BAD_WORDS) {
            if (lowerMessage.contains(badWord.toLowerCase())) {
                return true;
            }
        }

        // Check for leetspeak attempts
        String normalized = normalizeLeetSpeak(message);
        for (String badWord : BAD_WORDS) {
            if (normalized.contains(badWord)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if message contains strong profanity that should block sending
     */
    public boolean containsStrongProfanity(String message) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }

        String lowerMessage = message.toLowerCase();
        String normalized = normalizeLeetSpeak(message);

        for (String strongWord : STRONG_BAD_WORDS) {
            if (lowerMessage.contains(strongWord.toLowerCase()) ||
                    normalized.contains(strongWord)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Validate message before sending
     * @return FilterResult with status and filtered message
     */
    public FilterResult validateMessage(String message) {
        if (containsStrongProfanity(message)) {
            return new FilterResult(false,
                    "❌ Message contains inappropriate language and cannot be sent.",
                    message);
        }

        if (containsProfanity(message)) {
            String filtered = filterMessage(message);
            return new FilterResult(true,
                    "⚠️ Message contains filtered words. Your message has been cleaned.",
                    filtered);
        }

        return new FilterResult(true, null, message);
    }

    /**
     * Result class for message validation
     */
    public static class FilterResult {
        private final boolean allowed;
        private final String warning;
        private final String filteredMessage;

        public FilterResult(boolean allowed, String warning, String filteredMessage) {
            this.allowed = allowed;
            this.warning = warning;
            this.filteredMessage = filteredMessage;
        }

        public boolean isAllowed() { return allowed; }
        public String getWarning() { return warning; }
        public String getFilteredMessage() { return filteredMessage; }
    }

    /**
     * Add custom bad words to the filter
     */
    public void addBadWord(String word) {
        BAD_WORDS.add(word.toLowerCase());
    }

    /**
     * Remove a word from the filter
     */
    public void removeBadWord(String word) {
        BAD_WORDS.remove(word.toLowerCase());
    }

    /**
     * Get list of filtered words (for admin panel)
     */
    public List<String> getFilteredWords() {
        return new ArrayList<>(BAD_WORDS);
    }
}