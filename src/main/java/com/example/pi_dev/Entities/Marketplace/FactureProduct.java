package com.example.pi_dev.Entities.Marketplace;

public class FactureProduct {
    private int factureId;
    private int productId;
    private String productTitle;
    private int quantity;
    private float price;
    private String productImage;  // ADD THIS

    public FactureProduct(int factureId, int productId, String productTitle, int quantity, float price, String productImage) {
        this.factureId = factureId;
        this.productId = productId;
        this.productTitle = productTitle;
        this.quantity = quantity;
        this.price = price;
        this.productImage = productImage;  // ADD THIS
    }

    // Keep the old constructor for backward compatibility
    public FactureProduct(int factureId, int productId, String productTitle, int quantity, float price) {
        this(factureId, productId, productTitle, quantity, price, null);
    }

    public FactureProduct() {

    }

    public int getFactureId() { return factureId; }
    public int getProductId() { return productId; }
    public String getProductTitle() { return productTitle; }
    public int getQuantity() { return quantity; }
    public float getPrice() { return price; }
    public String getProductImage() { return productImage; }  // ADD THIS

    public void setFactureId(int factureId) {
        this.factureId = factureId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public void setProductTitle(String productTitle) {
        this.productTitle = productTitle;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void setPrice(float price) {
        this.price = price;
    }

    public void setProductImage(String productImage) {  // ADD THIS
        this.productImage = productImage;
    }
}