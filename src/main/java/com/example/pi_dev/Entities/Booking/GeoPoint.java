package com.example.pi_dev.Entities.Booking;

/**
 * Simple value object holding a geographic coordinate pair.
 */
public class GeoPoint {
    private final double lat;
    private final double lng;

    public GeoPoint(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    @Override
    public String toString() {
        return "GeoPoint{lat=" + lat + ", lng=" + lng + "}";
    }
}
