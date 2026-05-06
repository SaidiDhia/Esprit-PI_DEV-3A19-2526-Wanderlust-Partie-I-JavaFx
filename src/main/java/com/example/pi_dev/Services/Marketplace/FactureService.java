package com.example.pi_dev.Services.Marketplace;

import com.example.pi_dev.Entities.Marketplace.Facture;
import com.example.pi_dev.Entities.Marketplace.FactureProduct;
import com.example.pi_dev.Utils.Marketplace.Mydatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FactureService {

    private Connection con;

    public FactureService() {
        con = Mydatabase.getInstance().getConnection();
    }

    // ─── Add facture ──────────────────────────────────────────────────────────

    public int addFacture(Facture f) {
        String sql = "INSERT INTO facture (user_id, date_facture, total_price, payment_method, delivery_status) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, f.getUserId());
            ps.setTimestamp(2, new java.sql.Timestamp(f.getDate().getTime()));
            ps.setFloat(3, f.getTotal());
            ps.setString(4, f.getPaymentMethod());
            ps.setString(5, f.getDeliveryStatus());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    // ─── Get factures by user ─────────────────────────────────────────────────

    public List<Facture> getFacturesByUser(String userId) {
        List<Facture> list = new ArrayList<>();
        String sql = "SELECT * FROM facture WHERE user_id = ? ORDER BY date_facture DESC";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapFacture(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ─── Get ALL factures for products owned by a seller ─────────────────────

    public List<Facture> getFacturesByProductOwner(String ownerUserId) {
        List<Facture> list = new ArrayList<>();
        String sql = "SELECT DISTINCT f.* FROM facture f " +
                     "JOIN facture_product fp ON f.id_facture = fp.facture_id " +
                     "JOIN products p ON fp.product_id = p.id " +
                     "WHERE p.userId = ? " +
                     "ORDER BY f.date_facture DESC";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, ownerUserId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapFacture(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ─── Get facture by ID ────────────────────────────────────────────────────

    public Facture getFactureById(int factureId) {
        String sql = "SELECT * FROM facture WHERE id_facture = ?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, factureId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapFacture(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ─── Update delivery status ───────────────────────────────────────────────

    public boolean updateDeliveryStatus(int factureId, String status) {
        String sql = "UPDATE facture SET delivery_status = ? WHERE id_facture = ?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, status);
            ps.setInt(2, factureId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ─── Confirm cash delivery ────────────────────────────────────────────────
    //
    // BEFORE (broken):
    //   reserved_quantity = reserved_quantity - qty    ← only released reservation
    //   quantity was NEVER touched → stayed at 10 forever!
    //
    // AFTER (correct):
    //   quantity          = quantity - qty             ← item physically left warehouse
    //   reserved_quantity = reserved_quantity - qty    ← reservation fulfilled & released
    //
    // Net effect on available (qty - reserved): unchanged ✓
    // Because the item was already "locked" from available when the order was placed.

    public boolean confirmCashDelivery(int factureId) {
        try {
            con.setAutoCommit(false);

            List<FactureProduct> products = getFactureProducts(factureId);
            for (FactureProduct fp : products) {

                // FIX: deduct real stock AND release reservation in one atomic update
                String sql = "UPDATE products " +
                             "SET quantity = quantity - ?, " +
                             "    reserved_quantity = reserved_quantity - ? " +
                             "WHERE id = ?";
                PreparedStatement ps = con.prepareStatement(sql);
                ps.setInt(1, fp.getQuantity());  // quantity   10 → 9
                ps.setInt(2, fp.getQuantity());  // reserved    1 → 0
                ps.setInt(3, fp.getProductId());
                ps.executeUpdate();

                System.out.println("confirmCashDelivery: product " + fp.getProductId()
                    + " qty-=" + fp.getQuantity() + " reserved-=" + fp.getQuantity());
            }

            updateDeliveryStatus(factureId, "confirmed");

            con.commit();
            con.setAutoCommit(true);
            return true;

        } catch (SQLException e) {
            try { con.rollback(); con.setAutoCommit(true); } catch (SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace();
            return false;
        }
    }

    // ─── Cancel order → restore stock ────────────────────────────────────────
    //
    // The cancel logic depends on BOTH payment method AND current status.
    //
    // CASH + PENDING   → item was only reserved, never left warehouse
    //                    → just release reservation (reserved - qty)
    //                    → quantity stays the same
    //
    // CASH + CONFIRMED → confirmCashDelivery already ran:
    //                       quantity was deducted, reserved was released
    //                    → restore quantity (quantity + qty)
    //                    → reserved is already 0, don't touch it
    //
    // ONLINE + any     → stock was deducted immediately at purchase time
    //                    → restore quantity (quantity + qty)
    //                    → reserved was never touched for online orders
    //
    // BEFORE (broken):
    //   cash cancel always did: quantity + qty AND reserved - qty
    //   → cancelling a PENDING cash order wrongly bumped quantity from 10 to 11!
    //     (the item was still in the warehouse the whole time)

    public boolean cancelOrder(int factureId) {
        try {
            con.setAutoCommit(false);

            Facture f = getFactureById(factureId);
            if (f == null) { con.rollback(); return false; }

            List<FactureProduct> products = getFactureProducts(factureId);
            String paymentMethod   = f.getPaymentMethod();
            String deliveryStatus  = f.getDeliveryStatus();

            for (FactureProduct fp : products) {
                PreparedStatement ps;

                if ("cash".equals(paymentMethod) && "pending".equals(deliveryStatus)) {
                    // Item was reserved but never physically left → only release reservation
                    ps = con.prepareStatement(
                        "UPDATE products SET reserved_quantity = reserved_quantity - ? WHERE id = ?");
                    ps.setInt(1, fp.getQuantity());  // reserved 1 → 0
                    ps.setInt(2, fp.getProductId()); // quantity unchanged ✓
                    System.out.println("cancelOrder [cash+pending]: product " + fp.getProductId()
                        + " reserved-=" + fp.getQuantity());

                } else if ("cash".equals(paymentMethod) && "confirmed".equals(deliveryStatus)) {
                    // confirmCashDelivery already ran: stock was deducted, reserved already 0
                    // → just give the stock back
                    ps = con.prepareStatement(
                        "UPDATE products SET quantity = quantity + ? WHERE id = ?");
                    ps.setInt(1, fp.getQuantity());  // quantity 9 → 10
                    ps.setInt(2, fp.getProductId()); // reserved unchanged (already 0) ✓
                    System.out.println("cancelOrder [cash+confirmed]: product " + fp.getProductId()
                        + " qty+=" + fp.getQuantity());

                } else {
                    // Online: stock was deducted immediately at purchase → restore it
                    ps = con.prepareStatement(
                        "UPDATE products SET quantity = quantity + ? WHERE id = ?");
                    ps.setInt(1, fp.getQuantity());  // quantity 9 → 10
                    ps.setInt(2, fp.getProductId());
                    System.out.println("cancelOrder [online]: product " + fp.getProductId()
                        + " qty+=" + fp.getQuantity());
                }

                ps.executeUpdate();
            }

            updateDeliveryStatus(factureId, "cancelled");
            con.commit();
            con.setAutoCommit(true);
            return true;

        } catch (SQLException e) {
            try { con.rollback(); con.setAutoCommit(true); } catch (SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace();
            return false;
        }
    }

    // ─── Get facture products ─────────────────────────────────────────────────

    public List<FactureProduct> getFactureProducts(int factureId) {
        List<FactureProduct> list = new ArrayList<>();
        String sql = "SELECT * FROM facture_product WHERE facture_id = ?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, factureId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                FactureProduct fp = new FactureProduct();
                fp.setFactureId(rs.getInt("facture_id"));
                fp.setProductId(rs.getInt("product_id"));
                fp.setProductTitle(rs.getString("product_title"));
                fp.setQuantity(rs.getInt("quantity"));
                fp.setPrice(rs.getFloat("price"));
                fp.setProductImage(rs.getString("product_image"));
                list.add(fp);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private Facture mapFacture(ResultSet rs) throws SQLException {
        Facture f = new Facture();
        f.setId(rs.getInt("id_facture"));
        f.setUserId(rs.getString("user_id"));
        f.setDate(rs.getTimestamp("date_facture"));
        f.setTotal(rs.getFloat("total_price"));
        f.setPaymentMethod(rs.getString("payment_method"));
        f.setDeliveryStatus(rs.getString("delivery_status"));
        return f;
    }
}
