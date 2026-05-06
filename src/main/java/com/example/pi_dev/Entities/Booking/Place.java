package com.example.pi_dev.Entities.Booking;

public class Place {

    private int id;
    private String hostId;
    private String title;
    private String description;
    private double pricePerDay;
    private int capacity;
    private int maxGuests; // Maximum number of guests allowed
    private String address;
    private String city;
    private String category;
    private Status status;
    private String imageUrl;
    private Double lat;
    private Double lng;
    private Double avgRating;
    private int reviewsCount;

    public enum Status {
        PENDING, APPROVED, DENIED
    }

    public Place() {
    }

    public Place(String address, int capacity, String category, String city, String description, int id, String hostId,
                 String imageUrl, int maxGuests, double pricePerDay, String title, Status status) {
        this.address = address;
        this.capacity = capacity;
        this.category = category;
        this.city = city;
        this.description = description;
        this.id = id;
        this.hostId = hostId;
        this.imageUrl = imageUrl;
        this.maxGuests = maxGuests;
        this.pricePerDay = pricePerDay;
        this.title = title;
        this.status = status;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getPricePerDay() {
        return pricePerDay;
    }

    public void setPricePerDay(double pricePerDay) {
        this.pricePerDay = pricePerDay;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getMaxGuests() {
        return maxGuests;
    }

    public void setMaxGuests(int maxGuests) {
        this.maxGuests = maxGuests;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getHostId() {
        return hostId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLng() {
        return lng;
    }

    public void setLng(Double lng) {
        this.lng = lng;
    }

    public Double getAvgRating() {
        return avgRating;
    }

    public void setAvgRating(Double avgRating) {
        this.avgRating = avgRating;
    }

    public int getReviewsCount() {
        return reviewsCount;
    }

    public void setReviewsCount(int reviewsCount) {
        this.reviewsCount = reviewsCount;
    }
}
