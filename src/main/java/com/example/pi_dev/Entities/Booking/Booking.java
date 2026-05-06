package com.example.pi_dev.Entities.Booking;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Booking {
    private int id;
    private String userId;
    private String hostId; // The ID of the place owner
    private int placeId;
    private LocalDate startDate;
    private LocalDate endDate;
    private double totalPrice;
    private int guestsCount;
    private Status status;
    private String pdfPath;

    // Cancellation fields
    private LocalDateTime cancelledAt;
    private double refundAmount;
    private String cancelledBy;
    private String cancelReason;

    public enum Status {
        PENDING, CONFIRMED, REJECTED, CANCELLED, COMPLETED
    }

    public Booking() {
    }

    public Booking(LocalDate endDate, double totalPrice, Status status, LocalDate startDate, int placeId, int id,
                   String userId, int guestsCount) {
        this.endDate = endDate;
        this.totalPrice = totalPrice;
        this.status = status;
        this.startDate = startDate;
        this.placeId = placeId;
        this.id = id;
        this.userId = userId;
        this.guestsCount = guestsCount;
    }

    public Booking(int guestsCount, LocalDate endDate, int placeId, LocalDate startDate, Status status,
                   double totalPrice) {
        this.guestsCount = guestsCount;
        this.endDate = endDate;
        this.placeId = placeId;
        this.startDate = startDate;
        this.status = status;
        this.totalPrice = totalPrice;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getHostId() {
        return hostId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    public int getGuestsCount() {
        return guestsCount;
    }

    public void setGuestsCount(int guestsCount) {
        this.guestsCount = guestsCount;
    }

    public int getPlaceId() {
        return placeId;
    }

    public void setPlaceId(int placeId) {
        this.placeId = placeId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getPdfPath() {
        return pdfPath;
    }

    public void setPdfPath(String pdfPath) {
        this.pdfPath = pdfPath;
    }

    // --- Cancellation getters/setters ---

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public double getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(double refundAmount) {
        this.refundAmount = refundAmount;
    }

    public String getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(String cancelledBy) {
        this.cancelledBy = cancelledBy;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }
}
