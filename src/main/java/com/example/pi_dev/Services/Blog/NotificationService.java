package com.example.pi_dev.Services.Blog;

import com.example.pi_dev.Utils.Blog.BlogDataBase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists and loads notifications in the DB.
 * A notification is always stored for the RECIPIENT (owner of the
 * post/comment),
 * not the actor who triggered the action.
 */
public class NotificationService {

    public enum NotifType {
        COMMENT, REPLY_TO_COMMENT, REACTION_POST, REACTION_COMMENT
    }

    public static class NotifRecord {
        public int id;
        public NotifType type;
        public String recipientUserId; // who RECEIVES the notification
        public String actorUsername; // who performed the action
        public String postPreview;
        public String contentPreview;
        public LocalDateTime date;
        public boolean read;
    }

    private Connection getConn() {
        return BlogDataBase.getInstance().getConnection();
    }

    /** Ensure the notifications table exists (call once at startup). */
    public void ensureTable() {
        String sql = """
                    CREATE TABLE IF NOT EXISTS blog_notifications (
                        id               INT AUTO_INCREMENT PRIMARY KEY,
                        recipient_id     VARCHAR(36) NOT NULL,
                        actor_username   VARCHAR(100) NOT NULL,
                        type             VARCHAR(40)  NOT NULL,
                        post_preview     TEXT,
                        content_preview  TEXT,
                        created_at       DATETIME DEFAULT CURRENT_TIMESTAMP,
                        is_read          TINYINT(1) DEFAULT 0,
                        INDEX (recipient_id)
                    )
                """;
        try (Statement st = getConn().createStatement()) {
            st.executeUpdate(sql);
        } catch (SQLException e) {
            System.err.println("⚠ ensureTable: " + e.getMessage());
        }
    }

    /**
     * Saves a new notification for the RECIPIENT.
     * Call this from the controller whenever someone comments/reacts on another
     * user's content.
     */
    public void push(NotifType type, String recipientUserId,
            String actorUsername, String postPreview, String contentPreview) {
        String sql = """
                    INSERT INTO blog_notifications
                        (recipient_id, actor_username, type, post_preview, content_preview)
                    VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, recipientUserId);
            ps.setString(2, actorUsername);
            ps.setString(3, type.name());
            ps.setString(4, postPreview);
            ps.setString(5, contentPreview);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("⚠ push notif: " + e.getMessage());
        }
    }

    /** Loads all notifications for a given user, newest first. */
    public List<NotifRecord> getForUser(String recipientUserId) {
        String sql = """
                    SELECT id, type, actor_username, post_preview, content_preview, created_at, is_read
                    FROM blog_notifications
                    WHERE recipient_id = ?
                    ORDER BY created_at DESC
                    LIMIT 100
                """;
        List<NotifRecord> list = new ArrayList<>();
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, recipientUserId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                NotifRecord r = new NotifRecord();
                r.id = rs.getInt("id");
                String notifType = rs.getString("type");
                if (notifType == null || notifType.trim().isEmpty()) {
                    r.type = NotifType.COMMENT;
                } else {
                    try {
                        r.type = NotifType.valueOf(notifType.trim().toUpperCase());
                    } catch (IllegalArgumentException ex) {
                        r.type = NotifType.COMMENT;
                    }
                }
                r.actorUsername = rs.getString("actor_username");
                r.postPreview = rs.getString("post_preview");
                r.contentPreview = rs.getString("content_preview");
                r.date = rs.getTimestamp("created_at").toLocalDateTime();
                r.read = rs.getInt("is_read") == 1;
                list.add(r);
            }
        } catch (SQLException e) {
            System.err.println("⚠ getForUser: " + e.getMessage());
        }
        return list;
    }

    /** Marks a single notification as read. */
    public void markRead(int notifId) {
        try (PreparedStatement ps = getConn().prepareStatement(
                "UPDATE blog_notifications SET is_read=1 WHERE id=?")) {
            ps.setInt(1, notifId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("⚠ markRead: " + e.getMessage());
        }
    }

    /** Marks all notifications for a user as read. */
    public void markAllRead(String recipientUserId) {
        try (PreparedStatement ps = getConn().prepareStatement(
                "UPDATE blog_notifications SET is_read=1 WHERE recipient_id=?")) {
            ps.setString(1, recipientUserId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("⚠ markAllRead: " + e.getMessage());
        }
    }

    /** Count of unread notifications for a user. */
    public int countUnread(String recipientUserId) {
        try (PreparedStatement ps = getConn().prepareStatement(
                "SELECT COUNT(*) FROM blog_notifications WHERE recipient_id=? AND is_read=0")) {
            ps.setString(1, recipientUserId);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("⚠ countUnread: " + e.getMessage());
        }
        return 0;
    }
}
