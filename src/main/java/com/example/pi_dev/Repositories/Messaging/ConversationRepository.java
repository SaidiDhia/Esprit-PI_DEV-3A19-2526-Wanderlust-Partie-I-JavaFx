package com.example.pi_dev.Repositories.Messaging;

import com.example.pi_dev.Database.Messaging.DatabaseConnection;
import com.example.pi_dev.Entities.Messaging.Conversation;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ConversationRepository {

    // Updated create method to include created_by
    public long create(Conversation c, String creatorId) throws SQLException {
        String sql = """
            INSERT INTO conversation (name, type, created_at, created_by)
            VALUES (?, ?, NOW(), ?)
        """;

        Connection conn = DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, c.getName());
            stmt.setString(2, c.getType());
            stmt.setString(3, creatorId);

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
        return -1;
    }

    // Keep original create for backward compatibility
    public long create(Conversation c) throws SQLException {
        return create(c, null);
    }
    public List<Conversation> findByUser(String userId) throws SQLException {
        String sql = """
        SELECT c.*
        FROM conversation c
        JOIN conversation_user cu ON c.id = cu.conversation_id
        WHERE cu.user_id = ? 
        AND cu.is_active = true 
        AND c.is_archived = false  /* ← THIS LINE EXCLUDES ARCHIVED CONVERSATIONS */
        ORDER BY c.is_pinned DESC, c.last_activity DESC, c.id DESC
    """;

        List<Conversation> list = new ArrayList<>();

        try (PreparedStatement ps = DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Conversation c = new Conversation();
                c.setId(rs.getLong("id"));
                c.setType(rs.getString("type"));
                c.setContextType(rs.getString("context_type"));
                c.setContextId(rs.getLong("context_id"));
                c.setName(rs.getString("name"));
                c.setCreatedAt(rs.getTimestamp("created_at") != null ?
                        rs.getTimestamp("created_at").toLocalDateTime() : null);
                c.setLastActivity(rs.getTimestamp("last_activity") != null ?
                        rs.getTimestamp("last_activity").toLocalDateTime() : null);
                c.setArchived(rs.getBoolean("is_archived"));
                c.setPinned(rs.getBoolean("is_pinned"));
                c.setMuteNotifications(rs.getBoolean("mute_notifications"));
                list.add(c);
            }
        }
        return list;
    }

    public boolean isUserCreator(long conversationId, String userId) throws SQLException {
        String sql = "SELECT created_by FROM conversation WHERE id = ?";

        try (PreparedStatement ps = DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
            ps.setLong(1, conversationId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String creatorId = rs.getString("created_by");
                return creatorId != null && creatorId.equals(userId);
            }
        }
        return false;
    }
    public List<Conversation> findArchivedByUser(String userId) throws SQLException {
        String sql = """
        SELECT c.*
        FROM conversation c
        JOIN conversation_user cu ON c.id = cu.conversation_id
        WHERE cu.user_id = ? AND cu.is_active = true AND c.is_archived = true
        ORDER BY c.last_activity DESC
    """;

        List<Conversation> list = new ArrayList<>();
        try (PreparedStatement ps = DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Conversation c = new Conversation();
                c.setId(rs.getLong("id"));
                c.setType(rs.getString("type"));
                c.setContextType(rs.getString("context_type"));
                c.setContextId(rs.getLong("context_id"));
                c.setName(rs.getString("name"));
                c.setArchived(rs.getBoolean("is_archived"));
                list.add(c);
            }
        }
        return list;
    }
    public void updateLastActivity(long conversationId) throws SQLException {
        String sql = "UPDATE conversation SET last_activity = NOW() WHERE id = ?";

        try (PreparedStatement ps = DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
            ps.setLong(1, conversationId);
            ps.executeUpdate();
        }
    }

    public void updateLastMessage(long conversationId, long messageId) throws SQLException {
        String sql = "UPDATE conversation SET last_message_id = ? WHERE id = ?";

        try (PreparedStatement ps = DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
            ps.setLong(1, messageId);
            ps.setLong(2, conversationId);
            ps.executeUpdate();
        }
    }

    public void updatePinStatus(long conversationId, String userId, boolean pinned) throws SQLException {
        String sql = "UPDATE conversation SET is_pinned = ? WHERE id = ?";

        try (PreparedStatement ps = DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
            ps.setBoolean(1, pinned);
            ps.setLong(2, conversationId);
            ps.executeUpdate();
        }
    }

    public void updateArchiveStatus(long conversationId, String userId, boolean archived) throws SQLException {
        String sql = "UPDATE conversation SET is_archived = ? WHERE id = ?";

        try (PreparedStatement ps = DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
            ps.setBoolean(1, archived);
            ps.setLong(2, conversationId);
            ps.executeUpdate();
        }
    }

    public void updateMuteStatus(long conversationId, String userId, boolean muted) throws SQLException {
        String sql = "UPDATE conversation SET mute_notifications = ? WHERE id = ?";

        try (PreparedStatement ps = DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
            ps.setBoolean(1, muted);
            ps.setLong(2, conversationId);
            ps.executeUpdate();
        }
    }

    // Check if a PERSONAL conversation already exists between two users
    public Long findExistingPersonalConversation(String userId1, String userId2) throws SQLException {
        String sql = """
            SELECT c.id FROM conversation c
            JOIN conversation_user cu1 ON c.id = cu1.conversation_id
            JOIN conversation_user cu2 ON c.id = cu2.conversation_id
            WHERE c.type = 'PERSONAL'
            AND cu1.user_id = ? AND cu2.user_id = ?
            AND cu1.user_id != cu2.user_id
            GROUP BY c.id
            HAVING COUNT(DISTINCT cu1.user_id) = 2
        """;

        try (PreparedStatement ps = DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, userId1);
            ps.setString(2, userId2);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getLong("id");
            }
        }
        return null;
    }

    public void updateName(long id, String newName) throws SQLException {
        String sql = "UPDATE conversation SET name = ? WHERE id = ?";

        try (PreparedStatement ps = DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, newName);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    public void delete(long id) throws SQLException {
        Connection conn = DatabaseConnection.getInstance().getConnection();

        try {
            conn.setAutoCommit(false);

            // 1️⃣ delete messages
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM message WHERE conversation_id = ?")) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }

            // 2️⃣ delete participants
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM conversation_user WHERE conversation_id = ?")) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }

            // 3️⃣ delete conversation
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM conversation WHERE id = ?")) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }

            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }
}