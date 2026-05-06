package com.example.pi_dev.Entities.Marketplace;

import java.util.HashMap;
import java.util.Map;

public class Panier {
    private String userId;
    // Map<ProductId, QuantityInCart>
    private Map<Integer, Integer> products;

    public Panier(String userId) {
        this.userId = userId;
        this.products = new HashMap<>();
    }

    public String getUserId() {
        return userId;
    }

    public Map<Integer, Integer> getProducts() {
        return products;
    }
}
