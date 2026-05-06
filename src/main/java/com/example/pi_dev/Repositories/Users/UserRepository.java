package com.example.pi_dev.Repositories.Users;

import com.example.pi_dev.Database.Users.UserDatabaseConnection;
import com.example.pi_dev.enums.RoleEnum;
import com.example.pi_dev.enums.TFAMethod;
import com.example.pi_dev.Entities.Users.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class UserRepository {

    private static final String USERS_TABLE = "users";
    private static final String PASSWORD_HASH_COLUMN = "password_hash";
    private static final String LEGACY_PASSWORD_COLUMN = "password";

    public void create(User user) throws SQLException {
        Connection connection = UserDatabaseConnection.getInstance().getConnection();
        String passwordColumns = getPasswordColumnsForInsert(connection);
        boolean hasIdColumn = hasColumn(connection, "id");

        StringBuilder cols = new StringBuilder();
        StringBuilder vals = new StringBuilder();
        if (hasIdColumn) {
            cols.append("id, ");
            vals.append("?, ");
        }
        cols.append("user_id, email, ").append(passwordColumns)
                .append(", full_name, phone_number, is_active, role, tfa_method, created_at, profile_picture");
        vals.append("?, ?, ").append(getPasswordPlaceholders(passwordColumns)).append(", ?, ?, ?, ?, ?, ?, ?");

        String sql = "INSERT INTO users (" + cols.toString() + ") VALUES (" + vals.toString() + ")";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int idx = 1;
            if (hasIdColumn) {
                ps.setString(idx++, user.getUserId().toString());
            }
            ps.setString(idx++, user.getUserId().toString());
            ps.setString(idx++, user.getEmail());
            idx = bindPasswordValues(ps, idx, connection, user.getPasswordHash());
            ps.setString(idx++, user.getFullName());
            ps.setString(idx++, user.getPhoneNumber());
            ps.setBoolean(idx++, user.getIsActive());
            ps.setString(idx++, user.getRole().name());
            ps.setString(idx++, user.getTfaMethod() != null ? user.getTfaMethod().name() : null);
            ps.setTimestamp(idx++, Timestamp.valueOf(user.getCreatedAt()));
            ps.setString(idx, user.getProfilePicture());

            ps.executeUpdate();
        }
    }

    public Optional<User> findById(UUID userId) throws SQLException {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try (PreparedStatement ps = UserDatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToUser(rs));
            }
        }
        return Optional.empty();
    }

    public Optional<User> findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (PreparedStatement ps = UserDatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToUser(rs));
            }
        }
        return Optional.empty();
    }

    public List<User> findAll() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";
        try (Statement st = UserDatabaseConnection.getInstance().getConnection().createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        }
        return users;
    }

    public void update(User user) throws SQLException {
        Connection connection = UserDatabaseConnection.getInstance().getConnection();
        String passwordAssignments = getPasswordAssignmentsForUpdate(connection);
        String sql = "UPDATE users SET email = ?, " + passwordAssignments
                + ", full_name = ?, phone_number = ?, is_active = ?, role = ?, tfa_method = ?, profile_picture = ? WHERE user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, user.getEmail());
            int index = 2;
            index = bindPasswordValues(ps, index, connection, user.getPasswordHash());
            ps.setString(index++, user.getFullName());
            ps.setString(index++, user.getPhoneNumber());
            ps.setBoolean(index++, user.getIsActive());
            ps.setString(index++, user.getRole().name());
            ps.setString(index++, user.getTfaMethod() != null ? user.getTfaMethod().name() : null);
            ps.setString(index++, user.getProfilePicture());
            ps.setString(index, user.getUserId().toString());

            ps.executeUpdate();
        }
    }

    public void delete(UUID userId) throws SQLException {
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (PreparedStatement ps = UserDatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            ps.executeUpdate();
        }
    }

    public void updateRole(UUID userId, RoleEnum role) throws SQLException {
        String sql = "UPDATE users SET role=? WHERE user_id = ?";
        try (PreparedStatement ps = UserDatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, role.name());
            ps.setString(2, userId.toString());
            ps.executeUpdate();
        }
    }

    // TFA Methods
    public void saveTfaSecret(UUID userId, String secretKey) throws SQLException {
        String sql = "INSERT INTO tfa_secrets (user_id, secret_key) VALUES (?, ?) ON DUPLICATE KEY UPDATE secret_key = ?";
        try (PreparedStatement ps = UserDatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            ps.setString(2, secretKey);
            ps.setString(3, secretKey);
            ps.executeUpdate();
        }
    }

    public String getTfaSecret(UUID userId) throws SQLException {
        String sql = "SELECT secret_key FROM tfa_secrets WHERE user_id = ?";
        try (PreparedStatement ps = UserDatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("secret_key");
            }
        }
        return null;
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        String userIdStr = rs.getString("user_id");
        UUID userId = null;
        try {
            if (userIdStr != null)
                userId = UUID.fromString(userIdStr);
        } catch (IllegalArgumentException ignored) {
        }

        String roleStr = rs.getString("role");
        RoleEnum role = roleStr != null ? RoleEnum.valueOf(roleStr) : RoleEnum.PARTICIPANT;

        String tfaStr = rs.getString("tfa_method");
        TFAMethod tfa = null;
        if (tfaStr != null) {
            try {
                tfa = TFAMethod.valueOf(tfaStr);
            } catch (IllegalArgumentException ignored) {
                // Unknown or legacy value (like "NONE"); treat as no TFA
                tfa = null;
            }
        }

        Timestamp createdTs = rs.getTimestamp("created_at");
        java.time.LocalDateTime createdAt = createdTs != null ? createdTs.toLocalDateTime()
                : java.time.LocalDateTime.now();

        return new User(
                userId,
                rs.getString("email"),
                getPasswordValue(rs),
                rs.getString("full_name"),
                rs.getString("phone_number"),
                rs.getBoolean("is_active"),
                role,
                tfa,
                createdAt,
                rs.getString("profile_picture"));
    }

    private String getPasswordColumnsForInsert(Connection connection) throws SQLException {
        boolean hasPasswordHash = hasColumn(connection, PASSWORD_HASH_COLUMN);
        boolean hasLegacyPassword = hasColumn(connection, LEGACY_PASSWORD_COLUMN);

        if (hasPasswordHash && hasLegacyPassword) {
            return PASSWORD_HASH_COLUMN + ", " + LEGACY_PASSWORD_COLUMN;
        }
        if (hasPasswordHash) {
            return PASSWORD_HASH_COLUMN;
        }
        if (hasLegacyPassword) {
            return LEGACY_PASSWORD_COLUMN;
        }
        throw new SQLException("No password column found in users table");
    }

    private String getPasswordAssignmentsForUpdate(Connection connection) throws SQLException {
        boolean hasPasswordHash = hasColumn(connection, PASSWORD_HASH_COLUMN);
        boolean hasLegacyPassword = hasColumn(connection, LEGACY_PASSWORD_COLUMN);

        if (hasPasswordHash && hasLegacyPassword) {
            return PASSWORD_HASH_COLUMN + " = ?, " + LEGACY_PASSWORD_COLUMN + " = ?";
        }
        if (hasPasswordHash) {
            return PASSWORD_HASH_COLUMN + " = ?";
        }
        if (hasLegacyPassword) {
            return LEGACY_PASSWORD_COLUMN + " = ?";
        }
        throw new SQLException("No password column found in users table");
    }

    private String getPasswordPlaceholders(String passwordColumns) {
        return passwordColumns.contains(",") ? "?, ?" : "?";
    }

    private int bindPasswordValues(PreparedStatement ps, int index, Connection connection, String passwordHash)
            throws SQLException {
        boolean hasPasswordHash = hasColumn(connection, PASSWORD_HASH_COLUMN);
        boolean hasLegacyPassword = hasColumn(connection, LEGACY_PASSWORD_COLUMN);

        if (hasPasswordHash) {
            ps.setString(index++, passwordHash);
        }
        if (hasLegacyPassword) {
            ps.setString(index++, passwordHash);
        }
        return index;
    }

    private String getPasswordValue(ResultSet rs) throws SQLException {
        try {
            return rs.getString(PASSWORD_HASH_COLUMN);
        } catch (SQLException ignored) {
            return rs.getString(LEGACY_PASSWORD_COLUMN);
        }
    }

    private boolean hasColumn(Connection connection, String columnName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet rs = metaData.getColumns(null, null, USERS_TABLE, columnName)) {
            return rs.next();
        }
    }
}
