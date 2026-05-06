package com.example.pi_dev.Services.Marketplace;

import com.example.pi_dev.Entities.Marketplace.FactureProduct;
import com.example.pi_dev.Utils.Marketplace.Mydatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FactureProductService {

    private Connection con;

    public FactureProductService() {
        con = Mydatabase.getInstance().getConnection();
    }

    public void addFactureProduct(FactureProduct fp) {
        String sql = "INSERT INTO facture_product (facture_id, product_id, product_title, quantity, price, product_image) VALUES (?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, fp.getFactureId());
            ps.setInt(2, fp.getProductId());
            ps.setString(3, fp.getProductTitle());
            ps.setInt(4, fp.getQuantity());
            ps.setFloat(5, fp.getPrice());
            ps.setString(6, fp.getProductImage());  // ADD THIS
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<FactureProduct> getProductsByFacture(int factureId) {
        List<FactureProduct> list = new ArrayList<>();
        String sql = "SELECT * FROM facture_product WHERE facture_id = ?";

        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, factureId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                FactureProduct fp = new FactureProduct(
                        rs.getInt("facture_id"),
                        rs.getInt("product_id"),
                        rs.getString("product_title"),
                        rs.getInt("quantity"),
                        rs.getFloat("price"),
                        rs.getString("product_image")  // ADD THIS
                );
                list.add(fp);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}