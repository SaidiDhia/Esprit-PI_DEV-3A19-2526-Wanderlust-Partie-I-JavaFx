package com.example.pi_dev.Services.Marketplace;

import com.example.pi_dev.Entities.Marketplace.Product;
import com.example.pi_dev.Utils.Marketplace.Mydatabase;
import com.example.pi_dev.Utils.Users.UserSession;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProductService {
    private Connection con;
    public static String CURRENT_USER_ID; // Not hardcoded anymore, gets from session

    public ProductService() {
        con = Mydatabase.getInstance().getConnection();
        // Initialize CURRENT_USER_ID from UserSession
        if (UserSession.getInstance().isLoggedIn()) {
            UUID userId = UserSession.getInstance().getCurrentUser().getUserId();
            CURRENT_USER_ID = userId != null ? userId.toString() : null;
        } else {
            CURRENT_USER_ID = null;
        }
    }

    // ─── Add a new product ────────────────────────────────────────────────────

    public void addProduct(Product p) {
        if (CURRENT_USER_ID == null) {
            throw new RuntimeException("No user logged in");
        }

        String req = """
        INSERT INTO products 
        (title, description, type, price, quantity, category, image, created_date, userId) 
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
    """;
        try {
            PreparedStatement ps = con.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, p.getTitle());
            ps.setString(2, p.getDescription());
            ps.setString(3, p.getType());
            ps.setFloat(4, p.getPrice());
            ps.setInt(5, p.getQuantity());
            ps.setString(6, p.getCategory());
            ps.setString(7, p.getImage());
            ps.setTimestamp(8, new Timestamp(p.getCreatedDate().getTime()));
            ps.setString(9, CURRENT_USER_ID);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) p.setId(rs.getInt(1));
            System.out.println("Product added for user ID=" + CURRENT_USER_ID);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // ─── Update product ───────────────────────────────────────────────────────

    public void updateProduct(Product p) {
        if (CURRENT_USER_ID == null) {
            throw new RuntimeException("No user logged in");
        }

        String req = """
    UPDATE products 
    SET title=?, description=?, type=?, price=?, quantity=?, category=?, image=?, created_date=?
    WHERE id=? AND userId=?
""";
        try {
            PreparedStatement ps = con.prepareStatement(req);
            ps.setString(1, p.getTitle());
            ps.setString(2, p.getDescription());
            ps.setString(3, p.getType());
            ps.setFloat(4, p.getPrice());
            ps.setInt(5, p.getQuantity());
            ps.setString(6, p.getCategory());
            ps.setString(7, p.getImage());
            ps.setTimestamp(8, new Timestamp(p.getCreatedDate().getTime()));
            ps.setInt(9, p.getId());
            ps.setString(10, CURRENT_USER_ID);
            ps.executeUpdate();
            System.out.println("Product updated successfully: ID=" + p.getId());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // ─── Delete product ───────────────────────────────────────────────────────

    public void deleteProduct(int id) {
        if (CURRENT_USER_ID == null) {
            throw new RuntimeException("No user logged in");
        }

        try {
            PreparedStatement ps = con.prepareStatement(
                    "DELETE FROM products WHERE id=? AND userId=?");
            ps.setInt(1, id);
            ps.setString(2, CURRENT_USER_ID);
            ps.executeUpdate();
            System.out.println("Product deleted: ID=" + id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteAllProducts() {
        String req = "DELETE FROM products";
        try {
            Statement st = con.createStatement();
            st.executeUpdate(req);
            System.out.println("All products deleted.");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // ─── Get product by ID ────────────────────────────────────────────────────

    public Product getProductById(int id) {
        try {
            String sql = "SELECT * FROM products WHERE id = ?";
            PreparedStatement stmt = con.prepareStatement(sql);
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapProduct(rs);
        } catch (SQLException e) {
            System.err.println("Error getting product: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    // ─── Get all available products (buyer view) ──────────────────────────────

    public List<Product> getAllAvailableProducts(String currentUserId) {
        if (CURRENT_USER_ID == null) {
            return new ArrayList<>(); // Return empty list if not logged in
        }

        List<Product> products = new ArrayList<>();
        try {
            String sql = "SELECT * FROM products " +
                    "WHERE userId != ? " +
                    "AND (quantity - COALESCE(reserved_quantity, 0)) > 0 " +
                    "ORDER BY created_date DESC";
            PreparedStatement stmt = con.prepareStatement(sql);
            stmt.setString(1, CURRENT_USER_ID);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) products.add(mapProduct(rs));
        } catch (SQLException e) {
            System.err.println("Error getting available products: " + e.getMessage());
            e.printStackTrace();
        }
        return products;
    }

    // ─── Get all products (seller/management view) ────────────────────────────

    public List<Product> getAllProducts(String currentUserId) {
        if (CURRENT_USER_ID == null) {
            return new ArrayList<>(); // Return empty list if not logged in
        }

        List<Product> products = new ArrayList<>();
        try {
            String sql = "SELECT * FROM products WHERE userId = ? ORDER BY created_date DESC";
            PreparedStatement stmt = con.prepareStatement(sql);
            stmt.setString(1, CURRENT_USER_ID);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) products.add(mapProduct(rs));
        } catch (SQLException e) {
            System.err.println("Error getting all products: " + e.getMessage());
            e.printStackTrace();
        }
        return products;
    }

    // ─── Helper: map ResultSet row → Product ──────────────────────────────────

    private Product mapProduct(ResultSet rs) throws SQLException {
        Product product = new Product(
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("type"),
                rs.getFloat("price"),
                rs.getInt("quantity"),
                rs.getString("category"),
                rs.getString("image"),
                rs.getTimestamp("created_date"),
                rs.getString("userId")
        );
        product.setId(rs.getInt("id"));
        try {
            product.setReservedQuantity(rs.getInt("reserved_quantity"));
        } catch (Exception ex) {
            // safe: column might not exist in older schema
        }
        return product;
    }
}