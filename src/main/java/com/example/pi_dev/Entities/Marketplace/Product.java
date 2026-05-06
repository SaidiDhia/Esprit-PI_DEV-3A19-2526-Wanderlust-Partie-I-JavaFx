package com.example.pi_dev.Entities.Marketplace;

import java.util.Date;

public class Product {
    int    id;
    String title;
    String description;
    String type;
    float  price;
    int    quantity;
    int    reservedQuantity; // NEW: reserved for cash-on-delivery orders
    String image;
    String category;
    Date   CreatedDate;
    String    userId;

    public Product() {}

    public Product(String title, String description, String type, float price,
                   int quantity, String category, String image,
                   Date createdDate, String userId) {
        this.title       = title;
        this.description = description;
        this.type        = type;
        this.price       = price;
        this.quantity    = quantity;
        this.category    = category;
        this.image       = image;
        this.CreatedDate = createdDate;
        this.userId      = userId;
    }

    public int    getId()                    { return id; }
    public void   setId(int id)              { this.id = id; }

    public String getTitle()                 { return title; }
    public void   setTitle(String title)     { this.title = title; }

    public String getDescription()                     { return description; }
    public void   setDescription(String description)   { this.description = description; }

    public String getType()                  { return type; }
    public void   setType(String type)       { this.type = type; }

    public float  getPrice()                 { return price; }
    public void   setPrice(float price)      { this.price = price; }

    public int    getQuantity()              { return quantity; }
    public void   setQuantity(int quantity)  { this.quantity = quantity; }

    // Available to buy = quantity - reservedQuantity
    public int    getReservedQuantity()                      { return reservedQuantity; }
    public void   setReservedQuantity(int reservedQuantity)  { this.reservedQuantity = reservedQuantity; }
    public int    getAvailableQuantity()                     { return Math.max(0, quantity - reservedQuantity); }

    public String getImage()                 { return image; }
    public void   setImage(String image)     { this.image = image; }

    public String getCategory()              { return category; }
    public void   setCategory(String cat)    { this.category = cat; }

    public Date   getCreatedDate()               { return CreatedDate; }
    public void   setCreatedDate(Date d)         { this.CreatedDate = d; }

    public String    getUserId()                { return userId; }
    public void   setUserId(String userId)      { this.userId = userId; }

    @Override
    public String toString() {
        return "Product{id=" + id + ", title='" + title + "', price=" + price +
               ", qty=" + quantity + ", reserved=" + reservedQuantity + "}";
    }
}
