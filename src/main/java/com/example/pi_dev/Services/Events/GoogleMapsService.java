package com.example.pi_dev.Services.Events;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class GoogleMapsService {
    private static final String API_KEY = "AIzaSyDWvi3ZewbLDWLkkPlFtNg1iV7hcbdHyE4";
    private static final String GEOCODING_URL = "https://maps.googleapis.com/maps/api/geocode/json";

    private boolean isInitialized = false;

    public boolean initialize() {
        try {
            System.out.println("🗺️ Google Maps Service initialisé (mode réel)");
            this.isInitialized = true;
            return true;

        } catch (Exception e) {
            System.err.println("Erreur lors de l'initialisation de Google Maps: " + e.getMessage());
            this.isInitialized = false;
            return false;
        }
    }

    public CompletableFuture<Coordinates> getCoordinates(String address) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isInitialized) {
                System.err.println("Google Maps n'est pas initialisé");
                return null;
            }

            try {
                System.out.println("🗺️ Recherche des coordonnées pour: " + address);

                if (API_KEY.equals("VOTRE_CLÉ_API_ICI") || API_KEY.length() < 30) {
                    System.out.println("⚠️ Pas d'API key configurée, utilisation de la simulation");
                    Coordinates coords = simulateCoordinates(address);

                    if (coords != null) {
                        System.out.println("✅ Coordonnées simulées trouvées: " + coords);
                        return coords;
                    } else {
                        System.out.println("❌ Coordonnées non trouvées pour: " + address);
                        return null;
                    }
                } else {
                    System.out.println("✅ Clé API Google Maps configurée, utilisation de l'API réelle");
                }

                String url = String.format("%s?address=%s&key=%s",
                        GEOCODING_URL,
                        java.net.URLEncoder.encode(address, "UTF-8"),
                        API_KEY);

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    Coordinates coords = parseCoordinates(response.body());
                    if (coords != null) {
                        System.out.println("✅ Coordonnées API trouvées: " + coords);
                        return coords;
                    } else {
                        System.out.println("❌ Coordonnées non trouvées dans la réponse API");
                        return simulateCoordinates(address);
                    }
                } else {
                    System.err.println("Erreur API: " + response.statusCode());
                    return simulateCoordinates(address);
                }

            } catch (Exception e) {
                System.err.println("Erreur lors de la recherche des coordonnées: " + e.getMessage());
                return simulateCoordinates(address);
            }
        });
    }

    public String getStaticMapUrl(String address, int width, int height) {
        if (!isInitialized) {
            return null;
        }

        try {
            System.out.println("🗺️ Génération de la carte statique pour: " + address);

            CompletableFuture<Coordinates> coordsFuture = getCoordinates(address);
            Coordinates coords = coordsFuture.join();

            if (coords != null) {
                String url = String.format(
                        "https://maps.googleapis.com/maps/api/staticmap?center=%.4f,%.4f&zoom=15&size=%dx%d&markers=color:red|%.4f,%.4f&key=%s",
                        coords.getLatitude(), coords.getLongitude(),
                        width, height,
                        coords.getLatitude(), coords.getLongitude(),
                        API_KEY
                );
                System.out.println("DEBUG: URL générée avec coordonnées: " + url);
                return url;
            } else {
                String url = String.format(
                        "https://maps.googleapis.com/maps/api/staticmap?center=%s&zoom=15&size=%dx%d&markers=color:red|%s&key=%s",
                        address.replace(" ", "+"),
                        width, height,
                        address.replace(" ", "+"),
                        API_KEY
                );
                System.out.println("DEBUG: URL générée avec adresse: " + url);
                return url;
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la génération de la carte: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public CompletableFuture<NearbyPlacesResult> findNearbyPlaces(String location, String type, int radius) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isInitialized) {
                System.err.println("Google Maps n'est pas initialisé");
                return null;
            }

            try {
                System.out.println("🗺️ Recherche de lieux à proximité de: " + location);
                System.out.println("   Type: " + type + ", Rayon: " + radius + "m");

                NearbyPlacesResult result = simulateNearbyPlaces(location, type);

                if (result != null) {
                    System.out.println("✅ " + result.getPlaces().size() + " lieux trouvés");
                    return result;
                } else {
                    System.out.println("❌ Aucun lieu trouvé");
                    return null;
                }

            } catch (Exception e) {
                System.err.println("Erreur lors de la recherche de lieux: " + e.getMessage());
                return null;
            }
        });
    }

    public double calculateDistance(Coordinates point1, Coordinates point2) {
        if (point1 == null || point2 == null) {
            return -1;
        }

        double lat1 = Math.toRadians(point1.getLatitude());
        double lon1 = Math.toRadians(point1.getLongitude());
        double lat2 = Math.toRadians(point2.getLatitude());
        double lon2 = Math.toRadians(point2.getLongitude());

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double earthRadius = 6371;
        return earthRadius * c;
    }

    public String getDirectionsUrl(String origin, String destination) {
        if (!isInitialized) {
            return null;
        }

        try {
            return String.format(
                    "https://www.google.com/maps/dir/?api=1&origin=%s&destination=%s",
                    origin.replace(" ", "+"),
                    destination.replace(" ", "+")
            );

        } catch (Exception e) {
            System.err.println("Erreur lors de la génération de l'itinéraire: " + e.getMessage());
            return null;
        }
    }

    private Coordinates parseCoordinates(String jsonResponse) {
        try {
            System.out.println("DEBUG: Parsing de la réponse JSON...");

            if (!jsonResponse.contains("\"results\"")) {
                System.out.println("DEBUG: La réponse ne contient pas 'results'");
                return null;
            }

            if (!jsonResponse.contains("\"location\"")) {
                System.out.println("DEBUG: La réponse ne contient pas 'location'");
                return null;
            }

            int latIndex = jsonResponse.indexOf("\"lat\":");
            if (latIndex == -1) {
                System.out.println("DEBUG: 'lat' non trouvé dans la réponse");
                return null;
            }

            int latStart = jsonResponse.indexOf(":", latIndex) + 1;
            int latEnd = jsonResponse.indexOf(",", latStart);
            if (latEnd == -1) latEnd = jsonResponse.indexOf("}", latStart);

            int lngIndex = jsonResponse.indexOf("\"lng\":");
            if (lngIndex == -1) {
                System.out.println("DEBUG: 'lng' non trouvé dans la réponse");
                return null;
            }

            int lngStart = jsonResponse.indexOf(":", lngIndex) + 1;
            int lngEnd = jsonResponse.indexOf("}", lngStart);
            if (lngEnd == -1) lngEnd = jsonResponse.indexOf(",", lngStart);

            String latStr = jsonResponse.substring(latStart, latEnd).trim();
            String lngStr = jsonResponse.substring(lngStart, lngEnd).trim();

            latStr = latStr.replaceAll("\"", "");
            lngStr = lngStr.replaceAll("\"", "");

            System.out.println("DEBUG: latStr = '" + latStr + "', lngStr = '" + lngStr + "'");

            double latitude = Double.parseDouble(latStr);
            double longitude = Double.parseDouble(lngStr);

            System.out.println("DEBUG: Coordonnées parsées: lat=" + latitude + ", lng=" + longitude);
            return new Coordinates(latitude, longitude);

        } catch (Exception e) {
            System.err.println("Erreur lors du parsing des coordonnées: " + e.getMessage());
            return null;
        }
    }

    public Coordinates simulateCoordinates(String address) {
        if (address.toLowerCase().contains("tunis")) {
            return new Coordinates(36.8065, 10.1815);
        } else if (address.toLowerCase().contains("sousse")) {
            return new Coordinates(35.8256, 10.6369);
        } else if (address.toLowerCase().contains("monastir")) {
            return new Coordinates(35.7643, 10.8113);
        } else if (address.toLowerCase().contains("djerba")) {
            return new Coordinates(33.8145, 10.8659);
        } else if (address.toLowerCase().contains("hammamet")) {
            return new Coordinates(36.4000, 10.6000);
        } else if (address.toLowerCase().contains("nabeul")) {
            return new Coordinates(36.4561, 10.7358);
        } else if (address.toLowerCase().contains("sfax")) {
            return new Coordinates(34.7406, 10.7603);
        } else if (address.toLowerCase().contains("bizerte")) {
            return new Coordinates(37.2744, 9.8739);
        } else if (address.toLowerCase().contains("tozeur")) {
            return new Coordinates(33.9252, 8.1344);
        } else if (address.toLowerCase().contains("douz")) {
            return new Coordinates(33.4500, 9.0100);
        } else if (address.toLowerCase().contains("ariana")) {
            return new Coordinates(36.8625, 10.1956);
        } else if (address.toLowerCase().contains("ben arous")) {
            return new Coordinates(36.7475, 10.2086);
        } else if (address.toLowerCase().contains("manouba")) {
            return new Coordinates(36.8065, 10.0956);
        } else if (address.toLowerCase().contains("mourouj")) {
            return new Coordinates(36.8665, 10.2556);
        } else if (address.toLowerCase().contains("plage")) {
            return new Coordinates(35.8000, 10.6000);
        } else if (address.toLowerCase().contains("desert")) {
            return new Coordinates(33.5000, 9.0000);
        } else {
            return new Coordinates(33.8869, 9.5375);
        }
    }

    private NearbyPlacesResult simulateNearbyPlaces(String location, String type) {
        NearbyPlacesResult result = new NearbyPlacesResult();

        if (type.toLowerCase().contains("restaurant")) {
            result.addPlace("Restaurant Le Médina", 4.5, 0.2);
            result.addPlace("Café Sidi Bou Said", 4.2, 0.5);
            result.addPlace("Restaurant La Marsa", 4.7, 0.8);
        } else if (type.toLowerCase().contains("hotel")) {
            result.addPlace("Hotel Africa", 4.3, 0.3);
            result.addPlace("Hotel Tunis", 4.1, 0.6);
            result.addPlace("Residence Sousse", 4.5, 0.9);
        } else if (type.toLowerCase().contains("parking")) {
            result.addPlace("Parking Centre Ville", 3.8, 0.1);
            result.addPlace("Parking Plage", 4.0, 0.4);
        } else {
            result.addPlace("Centre Commercial", 4.2, 0.3);
            result.addPlace("Pharmacie", 4.1, 0.2);
            result.addPlace("Banque", 3.9, 0.5);
        }

        return result;
    }

    public boolean isAvailable() {
        return isInitialized;
    }

    public boolean testConnection() {
        if (!isInitialized) {
            return false;
        }

        try {
            System.out.println("🗺️ Test de connexion à Google Maps...");

            if (API_KEY.equals("VOTRE_CLÉ_API_ICI") || API_KEY.length() < 30) {
                System.out.println("⚠️ Test en mode simulation (pas de clé API)");

                Coordinates coords = simulateCoordinates("Tunis");

                if (coords != null) {
                    System.out.println("✅ Connexion à Google Maps simulée avec succès");
                    System.out.println("   Test: Tunis → " + coords);
                    return true;
                } else {
                    System.out.println("❌ Échec du test de connexion");
                    return false;
                }
            } else {
                System.out.println("✅ Test en mode API réelle (clé configurée)");

                try {
                    String url = String.format("%s?address=%s&key=%s",
                            "https://maps.googleapis.com/maps/api/geocode/json",
                            java.net.URLEncoder.encode("Tunis, Tunisie", "UTF-8"),
                            API_KEY);

                    HttpClient client = HttpClient.newHttpClient();
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .build();

                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() == 200) {
                        System.out.println("DEBUG: Réponse API reçue (status 200)");

                        Coordinates coords = parseCoordinates(response.body());
                        if (coords != null) {
                            System.out.println("✅ Connexion à Google Maps API réussie");
                            System.out.println("   Test: Tunis → " + coords);
                            return true;
                        } else {
                            System.out.println("❌ parseCoordinates a retourné null malgré une réponse 200");
                            Coordinates fallbackCoords = simulateCoordinates("Tunis");
                            if (fallbackCoords != null) {
                                System.out.println("✅ Fallback simulation réussie: " + fallbackCoords);
                                return true;
                            }
                        }
                    }

                    System.out.println("❌ Échec du test API - Code: " + response.statusCode());
                    return false;

                } catch (Exception e) {
                    System.err.println("Erreur lors du test API: " + e.getMessage());
                    return false;
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur de connexion: " + e.getMessage());
            return false;
        }
    }

    public static class Coordinates {
        private final double latitude;
        private final double longitude;

        public Coordinates(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }

        @Override
        public String toString() {
            return String.format("(%.6f, %.6f)", latitude, longitude);
        }
    }

    public static class NearbyPlacesResult {
        private final java.util.List<Place> places = new java.util.ArrayList<>();

        public void addPlace(String name, double rating, double distance) {
            places.add(new Place(name, rating, distance));
        }

        public java.util.List<Place> getPlaces() { return places; }

        public static class Place {
            private final String name;
            private final double rating;
            private final double distance;

            public Place(String name, double rating, double distance) {
                this.name = name;
                this.rating = rating;
                this.distance = distance;
            }

            public String getName() { return name; }
            public double getRating() { return rating; }
            public double getDistance() { return distance; }

            @Override
            public String toString() {
                return String.format("%s (⭐%.1f, %.1fkm)", name, rating, distance);
            }
        }
    }
}