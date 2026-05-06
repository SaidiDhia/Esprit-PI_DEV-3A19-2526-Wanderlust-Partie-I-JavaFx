package com.example.pi_dev.Iservices.Marketplace;

public interface IPanierService {
    void addToCart(String userId, int productId, int quantity);
    void increaseQuantity(int productId);
    void decreaseQuantity(int productId);
    void removeFromCart(int productId);
    void buyCart(String userId);
    void displayCart();
}
