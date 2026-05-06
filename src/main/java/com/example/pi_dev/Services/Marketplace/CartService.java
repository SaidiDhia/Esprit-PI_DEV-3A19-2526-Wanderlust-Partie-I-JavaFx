package com.example.pi_dev.Services.Marketplace;

import com.example.pi_dev.Entities.Marketplace.DeliveryAddress;
import com.example.pi_dev.Entities.Marketplace.Facture;
import com.example.pi_dev.Entities.Marketplace.FactureProduct;
import com.example.pi_dev.Entities.Marketplace.Product;
import com.example.pi_dev.Utils.Marketplace.Mydatabase;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class CartService {

    private Connection     con;
    private ProductService productService;

    public CartService() {
        con            = Mydatabase.getInstance().getConnection();
        productService = new ProductService();
    }

    // ─── Get or create cart ───────────────────────────────────────────────────

    public int getOrCreateCart(String userId) {
        try {
            String checkSql = "SELECT id FROM cart WHERE user_id = ?";
            PreparedStatement checkStmt = con.prepareStatement(checkSql);
            checkStmt.setString(1, userId);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) return rs.getInt("id");

            String insertSql = "INSERT INTO cart (user_id, total_price) VALUES (?, 0.00)";
            PreparedStatement insertStmt = con.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
            insertStmt.setString(1, userId);
            insertStmt.executeUpdate();
            ResultSet keys = insertStmt.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    // ─── Get cart items ───────────────────────────────────────────────────────
    //
    // FIX 1: Alias ci.quantity AS cart_qty so it doesn't collide with p.quantity.
    //         Before this fix, rs.getInt("quantity") at the bottom of the loop
    //         was reading the PRODUCT's stock count, not the cart item count.
    //
    // FIX 2: Load reserved_quantity into the Product object so that
    //         getAvailableQuantity() returns the correct number everywhere.

    public Map<Product, Integer> getCartItems(String userId) {
        Map<Product, Integer> cartItems = new LinkedHashMap<>();
        try {
            int cartId = getOrCreateCart(userId);
            if (cartId == -1) return cartItems;

            // FIX 1: alias ci.quantity to avoid column name collision with p.quantity
            String sql = "SELECT ci.quantity AS cart_qty, p.* " +
                         "FROM cart_item ci " +
                         "JOIN products p ON ci.product_id = p.id " +
                         "WHERE ci.cart_id = ?";
            PreparedStatement stmt = con.prepareStatement(sql);
            stmt.setInt(1, cartId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Product product = new Product(
                    rs.getString("title"),
                    rs.getString("description"),
                    rs.getString("type"),
                    rs.getFloat("price"),
                    rs.getInt("quantity"),          // product's physical stock
                    rs.getString("category"),
                    rs.getString("image"),
                    rs.getTimestamp("created_date"),
                    rs.getString("userId")
                );
                product.setId(rs.getInt("id"));

                // FIX 2: load reserved_quantity so getAvailableQuantity() is correct
                try {
                    product.setReservedQuantity(rs.getInt("reserved_quantity"));
                } catch (Exception ex) {
                    // column might not exist in older DB schema — safe to ignore
                }

                // FIX 1: use the aliased column for cart item quantity
                int cartQty = rs.getInt("cart_qty");
                cartItems.put(product, cartQty);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return cartItems;
    }

    // ─── Add to cart ──────────────────────────────────────────────────────────

    public boolean addToCart(String userId, int productId, int quantity) {
        try {
            Product product = productService.getProductById(productId);
            if (product == null || quantity < 1) return false;

            // Available = physical stock - reserved
            int available = product.getAvailableQuantity();
            if (available < quantity) {
                System.out.println("Not enough stock. Available: " + available);
                return false;
            }

            int cartId = getOrCreateCart(userId);
            if (cartId == -1) return false;

            String checkSql = "SELECT quantity FROM cart_item WHERE cart_id = ? AND product_id = ?";
            PreparedStatement checkStmt = con.prepareStatement(checkSql);
            checkStmt.setInt(1, cartId);
            checkStmt.setInt(2, productId);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                int newQty = rs.getInt("quantity") + quantity;
                if (newQty > available) return false;
                PreparedStatement upd = con.prepareStatement(
                    "UPDATE cart_item SET quantity = ? WHERE cart_id = ? AND product_id = ?");
                upd.setInt(1, newQty); upd.setInt(2, cartId); upd.setInt(3, productId);
                upd.executeUpdate();
            } else {
                PreparedStatement ins = con.prepareStatement(
                    "INSERT INTO cart_item (cart_id, product_id, quantity) VALUES (?, ?, ?)");
                ins.setInt(1, cartId); ins.setInt(2, productId); ins.setInt(3, quantity);
                ins.executeUpdate();
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ─── Update quantity ──────────────────────────────────────────────────────

    public boolean updateCartItemQuantity(String userId, int productId, int newQuantity) {
        try {
            if (newQuantity < 1) return removeFromCart(userId, productId);
            Product product = productService.getProductById(productId);
            if (product == null) return false;
            int available = product.getAvailableQuantity();
            if (newQuantity > available) return false;
            int cartId = getOrCreateCart(userId);
            PreparedStatement stmt = con.prepareStatement(
                "UPDATE cart_item SET quantity = ? WHERE cart_id = ? AND product_id = ?");
            stmt.setInt(1, newQuantity); stmt.setInt(2, cartId); stmt.setInt(3, productId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ─── Remove from cart ─────────────────────────────────────────────────────

    public boolean removeFromCart(String userId, int productId) {
        try {
            int cartId = getOrCreateCart(userId);
            PreparedStatement stmt = con.prepareStatement(
                "DELETE FROM cart_item WHERE cart_id = ? AND product_id = ?");
            stmt.setInt(1, cartId); stmt.setInt(2, productId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ─── Calculate total ──────────────────────────────────────────────────────

    public double calculateTotal(String userId) {
        double total = 0.0;
        for (Map.Entry<Product, Integer> e : getCartItems(userId).entrySet())
            total += e.getKey().getPrice() * e.getValue();
        return total;
    }

    // ─── BUY ONLINE (Stripe confirmed) → deduct stock immediately ────────────
    //
    // quantity -= qty   (item physically leaves warehouse right now)
    // reserved unchanged (online orders never touch reserved)

    public boolean buyCartOnline(String userId, DeliveryAddress deliveryAddress) {
        return processPurchase(userId, "online", "confirmed", deliveryAddress);
    }

    // ─── BUY CASH → reserve stock, wait for delivery confirmation ────────────
    //
    // reserved += qty   (lock the stock — item still in warehouse)
    // quantity unchanged (item hasn't shipped yet)

    public boolean buyCartCash(String userId, DeliveryAddress deliveryAddress) {
        return processPurchase(userId, "cash", "pending", deliveryAddress);
    }

    // ─── Core purchase logic ──────────────────────────────────────────────────

    private boolean processPurchase(String userId, String paymentMethod,
                                    String deliveryStatus, DeliveryAddress deliveryAddress) {
        FactureService         factureService = new FactureService();
        FactureProductService  fpService      = new FactureProductService();
        DeliveryAddressService daService      = new DeliveryAddressService();

        try {
            con.setAutoCommit(false);

            Map<Product, Integer> items = getCartItems(userId);
            if (items.isEmpty()) { con.rollback(); return false; }

            // Re-check stock from DB (fresh reads, not stale cart data)
            for (Map.Entry<Product, Integer> entry : items.entrySet()) {
                Product fresh = productService.getProductById(entry.getKey().getId());
                int     reqQty = entry.getValue();
                if (fresh == null) { con.rollback(); return false; }
                int available = fresh.getAvailableQuantity();
                if (available < reqQty) {
                    System.out.println("Stock issue: " + entry.getKey().getTitle()
                        + " — need " + reqQty + ", available " + available);
                    con.rollback();
                    return false;
                }
            }

            // Calculate total
            double total = 0.0;
            for (Map.Entry<Product, Integer> e : items.entrySet())
                total += e.getKey().getPrice() * e.getValue();

            // Create facture
            Facture facture = new Facture();
            facture.setUserId(userId);
            facture.setDate(new java.util.Date());
            facture.setTotal((float) total);
            facture.setPaymentMethod(paymentMethod);
            facture.setDeliveryStatus(deliveryStatus);

            int factureId = factureService.addFacture(facture);
            if (factureId == -1) { con.rollback(); return false; }

            // Save delivery address
            deliveryAddress.setFactureId(factureId);
            daService.save(deliveryAddress);

            // Update stock
            for (Map.Entry<Product, Integer> entry : items.entrySet()) {
                Product product  = entry.getKey();
                int     quantity = entry.getValue();

                if ("online".equals(paymentMethod)) {
                    // Online (Stripe): deduct stock immediately — item is sold & will ship
                    PreparedStatement ps = con.prepareStatement(
                        "UPDATE products SET quantity = quantity - ? WHERE id = ?");
                    ps.setInt(1, quantity);
                    ps.setInt(2, product.getId());
                    ps.executeUpdate();
                    System.out.println("buyOnline: product " + product.getId()
                        + " qty-=" + quantity);

                } else {
                    // Cash (COD): lock/reserve stock — item stays in warehouse until confirmed
                    PreparedStatement ps = con.prepareStatement(
                        "UPDATE products SET reserved_quantity = reserved_quantity + ? WHERE id = ?");
                    ps.setInt(1, quantity);
                    ps.setInt(2, product.getId());
                    ps.executeUpdate();
                    System.out.println("buyCash: product " + product.getId()
                        + " reserved+=" + quantity);
                }

                // Save facture_product snapshot
                FactureProduct fp = new FactureProduct();
                fp.setFactureId(factureId);
                fp.setProductId(product.getId());
                fp.setProductTitle(product.getTitle());
                fp.setQuantity(quantity);
                fp.setPrice(product.getPrice());
                fp.setProductImage(product.getImage());
                fpService.addFactureProduct(fp);
            }

            clearCart(userId);
            con.commit();
            con.setAutoCommit(true);
            return true;

        } catch (SQLException e) {
            try { con.rollback(); con.setAutoCommit(true); } catch (SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace();
            return false;
        }
    }

    public void clearCart(String userId) {
        try {
            int cartId = getOrCreateCart(userId);
            PreparedStatement stmt = con.prepareStatement("DELETE FROM cart_item WHERE cart_id = ?");
            stmt.setInt(1, cartId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
