package com.example.pi_dev.Repositories.Messaging;

import com.example.pi_dev.Database.Messaging.DatabaseConnection;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class UserRepository {

    public String getUserFullName(String userId) throws SQLException {
        String sql = "SELECT full_name FROM users WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getString("full_name");
            }
        }
        return null;
    }

    public String getUserEmail(String userId) throws SQLException {
        String sql = "SELECT email FROM users WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getString("email");
            }
        }
        return null;
    }

    public Map<String, String> getUserDetails(String userId) throws SQLException {
        String sql = "SELECT full_name, email FROM users WHERE user_id = ?";
        Map<String, String> details = new HashMap<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                details.put("full_name", rs.getString("full_name"));
                details.put("email", rs.getString("email"));
            }
        }
        return details;
    }
}