package com.example.pi_dev.Test.Marketplace;

import com.example.pi_dev.Entities.Marketplace.Facture;
import com.example.pi_dev.Entities.Marketplace.FactureProduct;
import com.example.pi_dev.Services.Marketplace.FactureProductService;
import com.example.pi_dev.Services.Marketplace.FactureService;
import com.example.pi_dev.Utils.Marketplace.Mydatabase;

import com.example.pi_dev.Services.Marketplace.ProductService;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        Mydatabase.getInstance();
        ProductService ps = new ProductService();


       //         PRODUCT TESTS

         /*
        // 1️⃣ Add a product (if you want to add a new one for testing)
        Product p = new Product(
                "Camping Tent 3-Person",
                "Lightweight, waterproof 4-person tent perfect for summer camping trips.",
                "For Sale",
                200.0f,
                10,
                "Camping Equipment",
                "tent.jpg",
                new Date()
        );
        ps.addProduct(p); // add product */




        // 2️⃣ Modify product by ID

        /*
        int productId = 15; // the ID of the product in the database

        Product p2 = ps.getProductById(productId);
        p2.setTitle("Camping Tent Deluxe");
        p2.setPrice(400f);
        p2.setQuantity(20);

        ps.updateProduct(p2);

         */




        // 3️⃣ Delete product by ID
        //int productId = 10; // the ID of the product in the database
        //ps.deleteProduct(productId); // delete product



        // 4️⃣Display all products
        //displayProducts(ps);


        //    PANIER TESTS


        // ps = new ProductService();
       // PanierService panier = new PanierService(1, ps); // userId = 1

        // Add products to cart
       // panier.addToCart(1, 13, 1); // productId=6, quantity=2
       // panier.addToCart(1, 14, 1); // productId=7, quantity=3
       // panier.addToCart(1, 15, 1); // should print "Product already in cart"

        // Display cart
       // panier.displayCart();

        // Increase quantity
       // panier.increaseQuantity(12); // +1, up to stock limit
       // panier.displayCart();

        // Decrease quantity
       // panier.decreaseQuantity(14); // -1
       // panier.displayCart();

        // Remove a product
       // panier.removeFromCart(13);
       // panier.displayCart();

        // Buy cart
       // panier.buyCart(1);
       // panier.displayCart();

        //affiche facture

        // afficher factures user 1
        displayFacturesByUser("123e4567-e89b-12d3-a456-426614174000");

        // afficher contenu du facture 3
        displayFactureDetails(3);



    }

    public static void displayFacturesByUser(String userId) {
        FactureService fs = new FactureService();
        List<Facture> factures = fs.getFacturesByUser(userId);

        if (factures.isEmpty()) {
            System.out.println("No factures found for user " + userId);
        } else {
            System.out.println("\n--- Factures of user " + userId + " ---");
            for (Facture f : factures) {
                System.out.println(
                        "Facture ID: " + f.getId() +
                                " | Date: " + f.getDate() +
                                " | Total: " + f.getTotal()
                );
            }
        }
    }


    public static void displayFactureDetails(int factureId) {
        FactureProductService fps = new FactureProductService();
        List<FactureProduct> products = fps.getProductsByFacture(factureId);

        if (products.isEmpty()) {
            System.out.println("No products found for facture " + factureId);
        } else {
            System.out.println("\n--- Products of facture " + factureId + " ---");
            for (FactureProduct fp : products) {
                System.out.println(
                        "Product ID: " + fp.getProductId() +
                                " | Title: " + fp.getProductTitle() +
                                " | Qty: " + fp.getQuantity() +
                                " | Price: " + fp.getPrice()
                );
            }
        }
    }


}
