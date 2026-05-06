package com.example.pi_dev.Entities.Booking;

import java.time.LocalDateTime;

public class PlaceImage {

    private int id;
    private int placeId;
    private String url;
    private int sortOrder;
    private boolean isPrimary;
    private LocalDateTime createdAt;

    public PlaceImage() {
    }

    public PlaceImage(int placeId, String url, int sortOrder, boolean isPrimary) {
        this.placeId = placeId;
        this.url = url;
        this.sortOrder = sortOrder;
        this.isPrimary = isPrimary;
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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public void setPrimary(boolean primary) {
        isPrimary = primary;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "PlaceImage{id=" + id + ", placeId=" + placeId + ", isPrimary=" + isPrimary + ", url=" + url + "}";
    }
}