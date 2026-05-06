package com.example.pi_dev.Services.Marketplace;

import com.example.pi_dev.Entities.Marketplace.Facture;
import com.example.pi_dev.Entities.Marketplace.FactureProduct;
import com.example.pi_dev.Entities.Marketplace.Panier;
import com.example.pi_dev.Entities.Marketplace.Product;
import com.example.pi_dev.Iservices.Marketplace.IPanierService;

import java.util.Map;

public class PanierService implements IPanierService {

    private Panier panier;
    private ProductService productService;

    public PanierService(String userId, ProductService productService) {
        this.panier = new Panier(userId);
        this.productService = productService;
    }

    @Override
    public void addToCart(String userId, int productId, int quantity) {

        // Get product from DB
        Product p = productService.getProductById(productId);

        // ❌ Product not found
        if (p == null) {
            System.out.println("Product not found.");
            return;
        }

        // ❌ Out of stock
        if (p.getQuantity() <= 0) {
            System.out.println("Product out of stock.");
            return;
        }

        int quantityInCart = panier.getProducts().getOrDefault(productId, 0);
        int remainingStock = p.getQuantity() - quantityInCart;

        // ❌ Not enough stock
        if (quantity > remainingStock) {
            System.out.println("You cannot add this quantity. Only " + remainingStock + " items remaining in stock.");
            return;
        }
        // ❌ not selecting at least 1
        if (quantity < 1) {
            System.out.println("Quantity must be at least 1.");
            return;
        }

        // ✅ Add or increase quantity
        panier.getProducts().put(productId, quantityInCart + quantity);

        System.out.println("Added product ID=" + productId +
                " | Added quantity=" + quantity +
                " | Total in cart=" + (quantityInCart + quantity));
    }

    @Override
    public void increaseQuantity(int productId) {
        if (!panier.getProducts().containsKey(productId)) return;

        Product p = productService.getProductById(productId);
        if (p == null) return;

        int currentQty = panier.getProducts().get(productId);

        if (currentQty >= p.getQuantity()) {
            System.out.println("Cannot increase quantity. Stock limit reached.");
            return;
        }

        panier.getProducts().put(productId, currentQty + 1);
        System.out.println("Quantity increased for product ID=" + productId);
    }

    @Override
    public void decreaseQuantity(int productId) {
        if (!panier.getProducts().containsKey(productId)) return;

        int currentQty = panier.getProducts().get(productId);

        if (currentQty <= 1) {
            System.out.println("Minimum quantity is 1.");
            return;
        }

        panier.getProducts().put(productId, currentQty - 1);
        System.out.println("Quantity decreased for product ID=" + productId);
    }

    @Override
    public void removeFromCart(int productId) {
        if (panier.getProducts().remove(productId) != null) {
            System.out.println("Product removed from cart: ID=" + productId);
        }
    }

    @Override
    public void buyCart(String userId) {

        FactureService factureService = new FactureService();


        float total = 0;

        for (Map.Entry<Integer, Integer> entry : panier.getProducts().entrySet()) {
            int productId = entry.getKey();
            int qtyInCart = entry.getValue();

            Product p = productService.getProductById(productId);
            if (p == null) continue;

            int remaining = p.getQuantity() - qtyInCart;

            // ✅ KEEP PRODUCT EVEN IF 0
            if (remaining < 0) remaining = 0;

            p.setQuantity(remaining);
            productService.updateProduct(p);



            System.out.println(
                    "Product ID=" + productId +
                            " bought=" + qtyInCart +
                            " | Remaining stock=" + remaining
            );

            if (p != null) {
                total += p.getPrice() * entry.getValue();
            }


        }
        Facture facture = new Facture(userId, new java.util.Date(), total);
        int factureId = factureService.addFacture(facture); // ✅ parent

        FactureProductService factureProductService = new FactureProductService();

        for (Map.Entry<Integer, Integer> entry : panier.getProducts().entrySet()) {

            int productId = entry.getKey();
            int qtyInCart = entry.getValue();

            Product p = productService.getProductById(productId);
            if (p == null) continue;

            // update stock
            int remaining = p.getQuantity() - qtyInCart;
            if (remaining < 0) remaining = 0;

            p.setQuantity(remaining);
            productService.updateProduct(p);

            // ✅ facture_product (child)
            FactureProduct fp = new FactureProduct(
                    factureId,        // 👈 parent ID
                    p.getId(),
                    p.getTitle(),
                    qtyInCart,
                    p.getPrice()
            );

            factureProductService.addFactureProduct(fp);
        }




        panier.getProducts().clear();
        System.out.println("Purchase completed. Cart cleared.");
    }

    @Override
    public void displayCart() {
        if (panier.getProducts().isEmpty()) {
            System.out.println("Cart is empty.");
            return;
        }

        System.out.println("\n--- Cart Content ---");
        for (Map.Entry<Integer, Integer> entry : panier.getProducts().entrySet()) {
            Product p = productService.getProductById(entry.getKey());
            if (p != null) {
                System.out.println(
                        p.getId() + " | " +
                                p.getTitle() +
                                " | In cart: " + entry.getValue() +
                                " | Stock: " + p.getQuantity()
                );
            }
        }
    }
}
