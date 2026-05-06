package com.example.pi_dev.Repositories.Messaging;

import com.example.pi_dev.Database.Messaging.DatabaseConnection;
import com.example.pi_dev.Entities.Messaging.ConversationParticipant;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ConversationUserRepository {

    // Updated method to include role and added_by
    public void addUserToConversation(long conversationId, String userId, String role, String addedBy) {
        String sql = """
            INSERT INTO conversation_user (conversation_id, user_id, role, added_by, joined_at, is_active)
            VALUES (?, ?, ?, ?, NOW(), true)
        """;

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, conversationId);
            stmt.setString(2, userId);
            stmt.setString(3, role);
            stmt.setString(4, addedBy);
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Keep old method for backward compatibility
    public void addUserToConversation(long conversationId, String userId) {
        addUserToConversation(conversationId, userId, "MEMBER", null);
    }

    public void removeUserFromConversation(long conversationId, String userId) throws SQLException {
        String sql = "DELETE FROM conversation_user WHERE conversation_id = ? AND user_id = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, conversationId);
            stmt.setString(2, userId);
            stmt.executeUpdate();
        }
    }

    public void softRemoveUserFromConversation(long conversationId, String userId) throws SQLException {
        String sql = "UPDATE conversation_user SET is_active = false WHERE conversation_id = ? AND user_id = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, conversationId);
            stmt.setString(2, userId);
            stmt.executeUpdate();
        }
    }

    public List<ConversationParticipant> getConversationParticipants(long conversationId) throws SQLException {
        String sql = """
        SELECT cu.*, u.full_name, u.email 
        FROM conversation_user cu
        LEFT JOIN users u ON cu.user_id = u.user_id
        WHERE cu.conversation_id = ? AND cu.is_active = true
        ORDER BY 
            CASE cu.role 
                WHEN 'CREATOR' THEN 1
                WHEN 'ADMIN' THEN 2
                ELSE 3
            END, u.full_name
    """;

        List<ConversationParticipant> participants = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, conversationId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                ConversationParticipant p = new ConversationParticipant();
                p.setId(rs.getLong("id"));
                p.setConversationId(rs.getLong("conversation_id"));
                p.setUserId(rs.getString("user_id"));
                p.setFullName(rs.getString("full_name"));
                p.setEmail(rs.getString("email"));
                p.setRole(rs.getString("role"));
                p.setJoinedAt(rs.getTimestamp("joined_at") != null ?
                        rs.getTimestamp("joined_at").toLocalDateTime() : null);
                p.setAddedBy(rs.getString("added_by"));
                p.setActive(rs.getBoolean("is_active"));
                participants.add(p);
            }
        }
        return participants;
    }

    public String getUserRoleInConversation(long conversationId, String userId) throws SQLException {
        String sql = "SELECT role FROM conversation_user WHERE conversation_id = ? AND user_id = ? AND is_active = true";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, conversationId);
            stmt.setString(2, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getString("role");
            }
        }
        return null;
    }

    public boolean isUserInConversation(long conversationId, String userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM conversation_user WHERE conversation_id = ? AND user_id = ? AND is_active = true";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, conversationId);
            stmt.setString(2, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    public void updateUserRole(long conversationId, String userId, String newRole) throws SQLException {
        String sql = "UPDATE conversation_user SET role = ? WHERE conversation_id = ? AND user_id = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newRole);
            stmt.setLong(2, conversationId);
            stmt.setString(3, userId);
            stmt.executeUpdate();
        }
    }

    public String findUserIdByEmail(String email) throws SQLException {
        String sql = "SELECT user_id FROM users WHERE email = ?";

        try (PreparedStatement ps = DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getString("user_id");
            }
        }
        return null;
    }

    // Get all contacts (users you've had PERSONAL conversations with)
    public List<String> getUserContacts(String userId) throws SQLException {
        String sql = """
            SELECT DISTINCT u.user_id, u.full_name, u.email
            FROM users u
            JOIN conversation_user cu1 ON u.user_id = cu1.user_id
            JOIN conversation c ON cu1.conversation_id = c.id
            JOIN conversation_user cu2 ON c.id = cu2.conversation_id
            WHERE c.type = 'PERSONAL'
            AND cu2.user_id = ?
            AND cu1.user_id != ?
            AND cu1.is_active = true
            ORDER BY u.full_name
        """;

        List<String> contacts = new ArrayList<>();

        try (PreparedStatement ps = DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, userId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                contacts.add(rs.getString("full_name"));
            }
        }
        return contacts;
    }
}