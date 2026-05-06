package com.example.pi_dev.Entities.Marketplace;

import java.sql.Timestamp;

/**
 * Cart Entity - Represents user shopping cart
 */
public class Cart {
    private int id;
    private String userId;
    private double totalPrice;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Constructors
    public Cart() {}

    public Cart(String userId) {
        this.userId = userId;
        this.totalPrice = 0.0;
    }

    public Cart(int id, String userId, double totalPrice, Timestamp createdAt, Timestamp updatedAt) {
        this.id = id;
        this.userId = userId;
        this.totalPrice = totalPrice;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
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

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Cart{" +
                "id=" + id +
                ", userId=" + userId +
                ", totalPrice=" + totalPrice +
                ", createdAt=" + createdAt +
                '}';
    }
}