package com.example.pi_dev.Services.Booking;

import com.example.pi_dev.Entities.Booking.GeoPoint;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Geocoding service using the Nominatim OpenStreetMap API.
 * Returns lat/lng for a given address + city.
 */
public class GeoService {

    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search";
    private final HttpClient client;

    public GeoService() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Geocodes an address + city via Nominatim.
     *
     * @param address street address
     * @param city    city name
     * @return GeoPoint(lat, lng) for the first result, or null if not found / error
     */
    public GeoPoint geocode(String address, String city) {
        if (address == null || address.isBlank() || city == null || city.isBlank())
            return null;

        String query = URLEncoder.encode(address + ", " + city, StandardCharsets.UTF_8);
        String url = NOMINATIM_URL + "?q=" + query + "&format=json&limit=1";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "WanderlustApp/1.0")
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body().trim();

            if (body.equals("[]") || body.isEmpty())
                return null;

            // Simple manual JSON parse — extract first "lat" and "lon" values
            double lat = extractDouble(body, "\"lat\"");
            double lon = extractDouble(body, "\"lon\"");

            if (Double.isNaN(lat) || Double.isNaN(lon))
                return null;

            return new GeoPoint(lat, lon);
        } catch (IOException | InterruptedException e) {
            System.err.println("GeoService geocoding error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Extracts the first numeric value after the given key in a JSON string.
     */
    private double extractDouble(String json, String key) {
        int idx = json.indexOf(key);
        if (idx < 0)
            return Double.NaN;
        int colon = json.indexOf(':', idx);
        if (colon < 0)
            return Double.NaN;
        int start = colon + 1;
        // skip whitespace and quotes
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '"'))
            start++;
        int end = start;
        while (end < json.length()
                && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.' || json.charAt(end) == '-'))
            end++;
        try {
            return Double.parseDouble(json.substring(start, end));
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }
}
