package com.example.pi_dev.common.services;

import com.example.pi_dev.Database.Users.UserDatabaseConnection;
import com.example.pi_dev.common.models.ActivityLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ActivityLogService {

    private static final String TABLE = "activity_log";

    public ActivityLogService() {
    }

    private Connection getConnection() throws SQLException {
        return UserDatabaseConnection.getInstance().getConnection();
    }

    private String resolveModule(String action) {
        if (action == null) {
            return "GENERAL";
        }
        String normalized = action.toUpperCase(Locale.ROOT);
        if (normalized.startsWith("SIGN") || normalized.contains("PASSWORD") || normalized.contains("TFA")) {
            return "AUTH";
        }
        if (normalized.contains("PROFILE") || normalized.contains("USER")) {
            return "USER";
        }
        if (normalized.contains("BOOK")) {
            return "BOOKING";
        }
        if (normalized.contains("EVENT")) {
            return "EVENTS";
        }
        return "GENERAL";
    }

    private boolean hasColumn(Connection conn, String columnName) throws SQLException {
        try (ResultSet rs = conn.getMetaData().getColumns(conn.getCatalog(), null, TABLE, columnName)) {
            return rs.next();
        }
    }

    public void log(String userEmail, String action, String details) {
        try {
            Connection conn = getConnection();
            if (conn == null || conn.isClosed()) {
                return;
            }

            boolean hasModule = hasColumn(conn, "module");
            boolean hasUserName = hasColumn(conn, "user_name");
            boolean hasContent = hasColumn(conn, "content");
            boolean hasUserEmail = hasColumn(conn, "user_email");
            boolean hasDetails = hasColumn(conn, "details");
            boolean hasCreatedAt = hasColumn(conn, "created_at");

            String sql;
            if (hasModule && hasUserName && hasContent) {
                sql = hasCreatedAt
                    ? "INSERT INTO " + TABLE + " (module, action, user_name, content, created_at) VALUES (?, ?, ?, ?, ?)"
                    : "INSERT INTO " + TABLE + " (module, action, user_name, content) VALUES (?, ?, ?, ?)";
            } else if (hasUserEmail && hasDetails) {
                sql = hasCreatedAt
                    ? "INSERT INTO " + TABLE + " (user_email, action, details, created_at) VALUES (?, ?, ?, ?)"
                    : "INSERT INTO " + TABLE + " (user_email, action, details) VALUES (?, ?, ?)";
            } else {
                sql = "INSERT INTO " + TABLE + " (action) VALUES (?)";
            }

            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(true);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                int idx = 1;
                if (hasModule && hasUserName && hasContent) {
                    stmt.setString(idx++, resolveModule(action));
                    stmt.setString(idx++, action);
                    stmt.setString(idx++, userEmail != null ? userEmail : "System");
                    stmt.setString(idx++, details != null ? details : "");
                    if (hasCreatedAt) {
                        stmt.setTimestamp(idx, Timestamp.valueOf(LocalDateTime.now()));
                    }
                } else if (hasUserEmail && hasDetails) {
                    stmt.setString(idx++, userEmail != null ? userEmail : "System");
                    stmt.setString(idx++, action);
                    stmt.setString(idx++, details != null ? details : "");
                    if (hasCreatedAt) {
                        stmt.setTimestamp(idx, Timestamp.valueOf(LocalDateTime.now()));
                    }
                } else {
                    stmt.setString(1, action);
                }
                stmt.executeUpdate();
            } finally {
                try {
                    conn.setAutoCommit(originalAutoCommit);
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<ActivityLog> findAll() {
        List<ActivityLog> logs = new ArrayList<>();
        try {
            Connection conn = getConnection();
            if (conn == null || conn.isClosed()) {
                return logs;
            }

            String orderColumn;
            if (hasColumn(conn, "id")) {
                orderColumn = "id";
            } else if (hasColumn(conn, "log_id")) {
                orderColumn = "log_id";
            } else if (hasColumn(conn, "created_at")) {
                orderColumn = "created_at";
            } else {
                orderColumn = "action";
            }

            String sql = "SELECT * FROM " + TABLE + " ORDER BY " + orderColumn + " DESC";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                Set<String> cols = extractColumns(rs.getMetaData());
                while (rs.next()) {
                    int id = cols.contains("id") ? rs.getInt("id") : (cols.contains("log_id") ? rs.getInt("log_id") : 0);
                    String action = readString(rs, cols, "action");
                    String module = cols.contains("module") ? readString(rs, cols, "module") : resolveModule(action);
                    String userId = readString(rs, cols, "user_id");
                    String userName = cols.contains("user_name")
                        ? readString(rs, cols, "user_name")
                        : readString(rs, cols, "user_email");
                    String userAvatar = readString(rs, cols, "user_avatar");
                    String targetType = readString(rs, cols, "target_type");
                    String targetId = readString(rs, cols, "target_id");
                    String targetName = readString(rs, cols, "target_name");
                    String targetImage = readString(rs, cols, "target_image");
                    String content = cols.contains("content")
                        ? readString(rs, cols, "content")
                        : readString(rs, cols, "details");
                    String destination = readString(rs, cols, "destination");
                    String metadata = cols.contains("metadata_json")
                        ? readString(rs, cols, "metadata_json")
                        : readString(rs, cols, "metadata");

                    Timestamp ts = cols.contains("created_at")
                        ? rs.getTimestamp("created_at")
                        : (cols.contains("createdAt") ? rs.getTimestamp("createdAt") : null);
                    LocalDateTime createdAt = (ts != null) ? ts.toLocalDateTime() : LocalDateTime.now();

                    logs.add(new ActivityLog(
                        id,
                        module,
                        action,
                        userId,
                        userName,
                        userAvatar,
                        targetType,
                        targetId,
                        targetName,
                        targetImage,
                        content,
                        destination,
                        metadata,
                        createdAt
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return logs;
    }

    private Set<String> extractColumns(ResultSetMetaData metaData) throws SQLException {
        Set<String> cols = new HashSet<>();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            cols.add(metaData.getColumnLabel(i).toLowerCase(Locale.ROOT));
        }
        return cols;
    }

    private String readString(ResultSet rs, Set<String> cols, String column) throws SQLException {
        return cols.contains(column) ? rs.getString(column) : null;
    }
}
