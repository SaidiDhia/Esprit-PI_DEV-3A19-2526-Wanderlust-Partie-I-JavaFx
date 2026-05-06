package com.example.pi_dev.Repositories.Messaging;

import com.example.pi_dev.Database.Messaging.DatabaseConnection;
import com.example.pi_dev.Entities.Messaging.Reaction;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReactionRepository {

    private static final String USER_ID_COLLATION = "utf8mb4_general_ci";

    public void addReaction(long messageId, String userId, String reaction) throws SQLException {
        String sql = "INSERT INTO message_reactions (message_id, user_id, reaction) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, messageId);
            stmt.setString(2, userId);
            stmt.setString(3, reaction);
            stmt.executeUpdate();
        }
    }

    public void removeReaction(long messageId, String userId) throws SQLException {
        String sql = "DELETE FROM message_reactions WHERE message_id = ? AND CONVERT(user_id USING utf8mb4) COLLATE "
                + USER_ID_COLLATION + " = CONVERT(? USING utf8mb4) COLLATE " + USER_ID_COLLATION;

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, messageId);
            stmt.setString(2, userId);
            stmt.executeUpdate();
        }
    }

    public List<Reaction> getReactionsForMessage(long messageId) throws SQLException {
        String sql = "SELECT r.*, u.full_name " +
                "FROM message_reactions r " +
                "LEFT JOIN users u ON CONVERT(r.user_id USING utf8mb4) COLLATE " + USER_ID_COLLATION +
                " = CONVERT(u.user_id USING utf8mb4) COLLATE " + USER_ID_COLLATION + " " +
                "WHERE r.message_id = ? " +
                "ORDER BY r.created_at";

        List<Reaction> reactions = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, messageId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Reaction r = new Reaction();
                r.setId(rs.getLong("id"));
                r.setMessageId(rs.getLong("message_id"));
                r.setUserId(rs.getString("user_id"));
                r.setReaction(rs.getString("reaction"));
                r.setCreatedAt(
                        rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
                r.setUserFullName(rs.getString("full_name"));
                reactions.add(r);
            }
        }
        return reactions;
    }

    public String getUserReaction(long messageId, String userId) throws SQLException {
        String sql = "SELECT reaction FROM message_reactions WHERE message_id = ? AND CONVERT(user_id USING utf8mb4) COLLATE "
                + USER_ID_COLLATION + " = CONVERT(? USING utf8mb4) COLLATE " + USER_ID_COLLATION;

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, messageId);
            stmt.setString(2, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getString("reaction");
            }
        }
        return null;
    }
}