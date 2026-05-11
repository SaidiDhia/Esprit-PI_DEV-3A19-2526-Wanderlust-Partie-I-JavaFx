package com.example.pi_dev.Repositories.Messaging;

import com.example.pi_dev.Database.Messaging.DatabaseConnection;
import com.example.pi_dev.Entities.Messaging.Message;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {

    public void create(Message m) throws SQLException {
        String sql = """
            INSERT INTO message (
                conversation_id, sender_id, content, message_type, 
                file_url, thumbnail_url, file_size, file_name, mime_type, 
                duration, status, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
        """;

        try (PreparedStatement ps = DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
            ps.setLong(1, m.getConversationId());
            ps.setString(2, m.getSenderId());
            ps.setString(3, m.getContent());
            ps.setString(4, m.getMessageType());
            ps.setString(5, m.getFileUrl());
            ps.setString(6, m.getThumbnailUrl());
            ps.setLong(7, m.getFileSize());
            ps.setString(8, m.getFileName());
            ps.setString(9, m.getMimeType());

            // Handle duration (can be null)
            if (m.getDuration() != null) {
                ps.setInt(10, m.getDuration());
            } else {
                ps.setNull(10, Types.INTEGER);
            }

            // Handle status
            if (m.getStatus() != null) {
                ps.setString(11, m.getStatus().name());
            } else {
                ps.setString(11, "SENT");
            }

            // --- Pre-insert Security & Risk Checks ---
            try {
                if (m.getContent() != null && !m.getContent().isEmpty()) {
                    double toxScore = new com.example.pi_dev.Services.Users.MessageToxicityService(com.example.pi_dev.common.ApiConfiguration.TOXICITY_API_URL).score(m.getContent());
                    if (toxScore > 80.0) {
                        m.setContent("[Filtered by Toxicity AI]");
                        ps.setString(3, m.getContent()); // Override SQL param value
                        
                        String uEmail = m.getSenderId();
                        if (uEmail != null && !uEmail.contains("@")) {
                            try { uEmail = com.example.pi_dev.Utils.Users.UserSession.getInstance().getCurrentUser().getEmail(); } catch(Exception ignored) {}
                        }
                        if (uEmail != null && uEmail.contains("@")) {
                            new com.example.pi_dev.Services.Users.EmailService().sendEmail(
                                uEmail, 
                                "Toxicity Alert on WonderLust", 
                                "A recent message you sent was flagged by our Toxicity AI (Score: " + String.format("%.1f", toxScore) + "). The message was filtered out. Warning: Repeated violations may result in a ban!"
                            );
                        }
                    }
                }
                String senderName = "User";
                try {
                	senderName = com.example.pi_dev.Utils.Users.UserSession.getInstance().getCurrentUser().getFullName();
                } catch(Exception e) {}
                String receiverName = getReceiverName(m.getConversationId(), m.getSenderId());
                
                new com.example.pi_dev.common.services.ActivityLogService().log(
                    m.getSenderId(), 
                    "SENDMESSAGE", 
                    senderName + " sent: '" + m.getContent() + "' to " + receiverName
                );
            } catch (Exception x) {
                x.printStackTrace();
            }

            ps.executeUpdate();
        }
    }

    private String getReceiverName(long conversationId, String senderId) {
        String sql = "SELECT u.first_name, u.last_name FROM conversation_user cu JOIN user u ON cu.user_id = u.id WHERE cu.conversation_id = ? AND cu.user_id != ?";
        try (PreparedStatement ps = DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
            ps.setLong(1, conversationId);
            ps.setString(2, senderId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("first_name") + " " + rs.getString("last_name");
            }
        } catch (Exception e) {}
        return "Unknown User";
    }

    public List<Message> findByConversation(long conversationId, String userId) throws SQLException {
        List<Message> list = new ArrayList<>();

        String sql = """
            SELECT m.*
            FROM message m
            JOIN conversation_user cu ON cu.conversation_id = m.conversation_id
            WHERE m.conversation_id = ? AND cu.user_id = ?
            ORDER BY m.created_at ASC
        """;

        try (PreparedStatement ps = DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
            ps.setLong(1, conversationId);
            ps.setString(2, userId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Message message = new Message();

                // Basic fields
                message.setId(rs.getLong("id"));
                message.setConversationId(rs.getLong("conversation_id"));
                message.setSenderId(rs.getString("sender_id"));
                message.setContent(rs.getString("content"));
                message.setCreatedAt(rs.getTimestamp("created_at") != null ?
                        rs.getTimestamp("created_at").toLocalDateTime() : null);

                // Media fields
                message.setMessageType(rs.getString("message_type"));
                message.setFileUrl(rs.getString("file_url"));
                message.setThumbnailUrl(rs.getString("thumbnail_url"));
                message.setFileSize(rs.getLong("file_size"));
                message.setFileName(rs.getString("file_name"));
                message.setMimeType(rs.getString("mime_type"));

                // Duration (can be null)
                if (rs.getObject("duration") != null) {
                    message.setDuration(rs.getInt("duration"));
                }

                // Status
                String statusStr = rs.getString("status");
                if (statusStr != null) {
                    message.setStatus(Message.Status.valueOf(statusStr));
                }

                list.add(message);
            }
        }
        return list;
    }

    public void delete(long messageId, String userId) throws SQLException {
        String sql = """
            DELETE FROM message
            WHERE id = ? AND sender_id = ?
        """;

        try (PreparedStatement ps = DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
            ps.setLong(1, messageId);
            ps.setString(2, userId);
            ps.executeUpdate();
        }
    }

    public void update(long messageId, String userId, String newContent) throws SQLException {
        String sql = """
            UPDATE message
            SET content = ?, edited_at = NOW()
            WHERE id = ? AND sender_id = ?
        """;

        try (PreparedStatement ps = DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, newContent);
            ps.setLong(2, messageId);
            ps.setString(3, userId);
            ps.executeUpdate();
        }
    }

    // Optional: Get message by ID
    public Message findById(long messageId) throws SQLException {
        String sql = "SELECT * FROM message WHERE id = ?";

        try (PreparedStatement ps = DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
            ps.setLong(1, messageId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Message message = new Message();
                message.setId(rs.getLong("id"));
                message.setConversationId(rs.getLong("conversation_id"));
                message.setSenderId(rs.getString("sender_id"));
                message.setContent(rs.getString("content"));
                message.setCreatedAt(rs.getTimestamp("created_at") != null ?
                        rs.getTimestamp("created_at").toLocalDateTime() : null);
                message.setMessageType(rs.getString("message_type"));
                message.setFileUrl(rs.getString("file_url"));
                message.setThumbnailUrl(rs.getString("thumbnail_url"));
                message.setFileSize(rs.getLong("file_size"));
                message.setFileName(rs.getString("file_name"));
                message.setMimeType(rs.getString("mime_type"));

                if (rs.getObject("duration") != null) {
                    message.setDuration(rs.getInt("duration"));
                }

                String statusStr = rs.getString("status");
                if (statusStr != null) {
                    message.setStatus(Message.Status.valueOf(statusStr));
                }

                return message;
            }
        }
        return null;
    }
}