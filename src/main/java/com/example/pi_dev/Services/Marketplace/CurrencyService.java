package com.example.pi_dev.Services.Marketplace;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * CurrencyService — converts TND to EUR/USD using ExchangeRate-API (FREE, no key needed)
 * API: https://api.exchangerate-api.com/v4/latest/TND
 */
public class CurrencyService {

    private static final String API_URL = "https://api.exchangerate-api.com/v4/latest/TND";

    private static double rateEUR = 0;
    private static double rateUSD = 0;
    private static long lastFetched = 0;
    private static final long CACHE_DURATION = 60 * 60 * 1000; // 1 hour cache

    /**
     * Fetch rates from API (cached for 1 hour)
     */
    private static void fetchRates() {
        long now = System.currentTimeMillis();
        if (rateEUR != 0 && (now - lastFetched) < CACHE_DURATION) return; // use cache

        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream())
            );
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
            reader.close();

            String json = response.toString();
            rateEUR = extractRate(json, "EUR");
            rateUSD = extractRate(json, "USD");
            lastFetched = now;

        } catch (Exception e) {
            System.err.println("Currency API failed: " + e.getMessage());
            // fallback rates if API is down
            rateEUR = 0.30;
            rateUSD = 0.32;
        }
    }

    /**
     * Convert TND amount to EUR
     */
    public static double toEUR(double tnd) {
        fetchRates();
        return tnd * rateEUR;
    }

    /**
     * Convert TND amount to USD
     */
    public static double toUSD(double tnd) {
        fetchRates();
        return tnd * rateUSD;
    }

    /**
     * Get formatted string: "32.50 TND = 9.75 EUR | 10.40 USD"
     */
    public static String getConvertedLabel(double tnd) {
        fetchRates();
        return String.format("%.2f TND  =  %.2f EUR  |  %.2f USD",
                tnd, toEUR(tnd), toUSD(tnd));
    }

    /**
     * Extract a rate value from JSON string
     */
    private static double extractRate(String json, String currency) {
        String key = "\"" + currency + "\":";
        int idx = json.indexOf(key);
        if (idx == -1) return 0;
        int start = idx + key.length();
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        try {
            return Double.parseDouble(json.substring(start, end).trim());
        } catch (Exception e) {
            return 0;
        }
    }
}
