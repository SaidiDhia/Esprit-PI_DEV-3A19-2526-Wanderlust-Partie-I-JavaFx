package com.example.pi_dev.common.services;

import com.example.pi_dev.common.models.ActivityLog;
import com.example.pi_dev.Database.Users.UserDatabaseConnection;

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
        String sql = "INSERT INTO activity_logs (user_email, action, details, created_at) VALUES (?, ?, ?, ?)";
        try {
            Connection conn = getConnection();
            if (conn == null || conn.isClosed()) return;
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(true);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, userEmail != null ? userEmail : "System");
                stmt.setString(2, action);
                stmt.setString(3, details != null ? details : "");
                stmt.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
                stmt.executeUpdate();
            } finally {
                try { conn.setAutoCommit(originalAutoCommit); } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<ActivityLog> findAll() {
        List<ActivityLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM activity_logs ORDER BY log_id DESC";
        try {
            Connection conn = getConnection();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("created_at");
                    LocalDateTime timestamp = (ts != null) ? ts.toLocalDateTime() : LocalDateTime.now();
                    logs.add(new ActivityLog(
                        rs.getInt("log_id"),
                        rs.getString("user_email"),
                        rs.getString("action"),
                        rs.getString("details"),
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
