package com.example.pi_dev.common.services;

import com.example.pi_dev.common.models.ActivityLog;
import com.example.pi_dev.Database.Users.UserDatabaseConnection;
import com.example.pi_dev.Session.Session;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogService {

    public ActivityLogService() {
    }

    private Connection getConnection() throws SQLException {
        return UserDatabaseConnection.getInstance().getConnection();
    }

    public void log(String userEmail, String action, String details) {
        // Updated to match the existing phpMyAdmin table 'activity_log'
        String sql = "INSERT INTO activity_log (module, action, user_id, user_name, content, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try {
            Connection conn = getConnection();
            if (conn == null || conn.isClosed()) return;
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, "java_app"); // module
                stmt.setString(2, action);
                stmt.setString(3, Session.getCurrentUserId());
                stmt.setString(4, userEmail != null ? userEmail : "Unknown");
                stmt.setString(5, details != null ? details : "");
                stmt.setTimestamp(6, new Timestamp(System.currentTimeMillis()));
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            System.err.println("Error logging activity: " + e.getMessage());
            // Don't fail the whole operation if logging fails
        }
    }

    public List<ActivityLog> findAll() {
        List<ActivityLog> logs = new ArrayList<>();
        // Updated to match 'activity_log'
        String sql = "SELECT * FROM activity_log ORDER BY id DESC";
        try {
            Connection conn = getConnection();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("created_at");
                    LocalDateTime timestamp = (ts != null) ? ts.toLocalDateTime() : LocalDateTime.now();
                    
                    // We use id since log_id doesn't exist in user's table
                    logs.add(new ActivityLog(
                        rs.getInt("id"),
                        rs.getString("user_name"), // mapping user_name to userEmail for compatibility
                        rs.getString("action"),
                        rs.getString("content"), // mapping content to details
                        timestamp
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return logs;
    }
}
