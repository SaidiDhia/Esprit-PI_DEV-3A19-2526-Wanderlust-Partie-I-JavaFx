package com.example.pi_dev.Utils.Events;

import java.sql.*;

public class DatabaseUtils {

    public static void ensureSchemaCorrect(Connection connection) {
        // 1. Check/Add ALL columns to 'events' table
        ensureColumnExists(connection, "events", "lieu", "VARCHAR(255) NULL");
        ensureColumnExists(connection, "events", "organisateur", "VARCHAR(255) NULL");
        ensureColumnExists(connection, "events", "email", "VARCHAR(255) NULL");
        ensureColumnExists(connection, "events", "telephone", "VARCHAR(50) NULL");
        ensureColumnExists(connection, "events", "description", "LONGTEXT NULL");
        ensureColumnExists(connection, "events", "materiels_necessaires", "LONGTEXT NULL");
        ensureColumnExists(connection, "events", "prix", "DECIMAL(10,2) DEFAULT 0.00");
        ensureColumnExists(connection, "events", "capacite_max", "INT DEFAULT 0");
        ensureColumnExists(connection, "events", "places_disponibles", "INT DEFAULT 0");
        ensureColumnExists(connection, "events", "date_debut", "DATETIME NULL");
        ensureColumnExists(connection, "events", "date_fin", "DATETIME NULL");
        ensureColumnExists(connection, "events", "image", "VARCHAR(255) NULL");
        ensureColumnExists(connection, "events", "video_youtube", "VARCHAR(255) NULL");
        ensureColumnExists(connection, "events", "status", "VARCHAR(50) DEFAULT 'en_attente'");
        ensureColumnExists(connection, "events", "statut", "VARCHAR(50) DEFAULT 'en_attente'");
        ensureColumnExists(connection, "events", "date_limite_inscription", "DATETIME NULL");
        ensureColumnExists(connection, "events", "date_creation", "DATETIME NULL");
        ensureColumnExists(connection, "events", "date_modification", "DATETIME NULL");
        ensureColumnExists(connection, "events", "created_by_id", "VARCHAR(36) NULL");

        // 2. Ensure 'events_activities' join table exists
        executeQuietly(connection, "CREATE TABLE IF NOT EXISTS events_activities (" +
                "events_id INT NOT NULL, " +
                "activities_id INT NOT NULL, " +
                "PRIMARY KEY (events_id, activities_id))");
        
        handleColumnNaming(connection, "events_activities", "id_event", "events_id", "INT NOT NULL");
        handleColumnNaming(connection, "events_activities", "event_id", "events_id", "INT NOT NULL");
        handleColumnNaming(connection, "events_activities", "id_activite", "activities_id", "INT NOT NULL");
        handleColumnNaming(connection, "events_activities", "activite_id", "activities_id", "INT NOT NULL");
        
        ensureColumnExists(connection, "events_activities", "events_id", "INT NOT NULL");
        ensureColumnExists(connection, "events_activities", "activities_id", "INT NOT NULL");
        dropColumnIfExists(connection, "events_activities", "event_id");
        dropColumnIfExists(connection, "events_activities", "activite_id");

        // 3. Ensure 'events_images' table exists
        executeQuietly(connection, "CREATE TABLE IF NOT EXISTS events_images (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "events_id INT NOT NULL, " +
                "chemin_photo VARCHAR(255) NOT NULL, " +
                "description VARCHAR(255) NULL)");
        
        handleColumnNaming(connection, "events_images", "id_event", "events_id", "INT NOT NULL");
        ensureColumnExists(connection, "events_images", "events_id", "INT NOT NULL");
        dropColumnIfExists(connection, "events_images", "id_event");

        // 4. Ensure 'activity_log' table exists
        executeQuietly(connection, "CREATE TABLE IF NOT EXISTS activity_log (" +
                "id bigint(20) NOT NULL AUTO_INCREMENT, " +
                "module varchar(50) NOT NULL, " +
                "action varchar(120) NOT NULL, " +
                "user_id varchar(36) DEFAULT NULL, " +
                "user_name varchar(255) DEFAULT NULL, " +
                "content longtext, " +
                "created_at datetime NOT NULL, " +
                "PRIMARY KEY (id))");
        
        // 5. Ensure columns in reservations
        ensureColumnExists(connection, "reservations", "user_id", "VARCHAR(36) NULL");
        ensureColumnExists(connection, "reservations", "statut", "VARCHAR(50) DEFAULT 'en_attente'");
        ensureColumnExists(connection, "reservations", "date_creation", "DATETIME NULL");
        ensureColumnExists(connection, "reservations", "date_modification", "DATETIME NULL");

        // 6. Ensure 'event_reviews' table exists
        executeQuietly(connection, "CREATE TABLE IF NOT EXISTS event_reviews (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "events_id INT NOT NULL, " +
                "user_id VARCHAR(36) NULL, " +
                "user_name VARCHAR(255) NULL, " +
                "comment LONGTEXT NULL, " +
                "rating INT DEFAULT 0, " +
                "date_creation DATETIME DEFAULT CURRENT_TIMESTAMP)");
    }

    private static void handleColumnNaming(Connection conn, String table, String oldName, String newName, String definition) {
        try {
            boolean oldExists = false;
            boolean newExists = false;
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SHOW COLUMNS FROM " + table)) {
                while (rs.next()) {
                    String col = rs.getString("Field");
                    if (col.equalsIgnoreCase(oldName)) oldExists = true;
                    if (col.equalsIgnoreCase(newName)) newExists = true;
                }
            }
            if (oldExists && !newExists) {
                try (Statement st = conn.createStatement()) {
                    st.executeUpdate("ALTER TABLE " + table + " CHANGE " + oldName + " " + newName + " " + definition);
                    System.out.println("Column " + oldName + " renamed to " + newName + " in " + table);
                }
            }
        } catch (SQLException ignored) {}
    }

    private static void ensureColumnExists(Connection conn, String table, String column, String definition) {
        try {
            boolean exists = false;
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SHOW COLUMNS FROM " + table + " LIKE '" + column + "'")) {
                if (rs.next()) exists = true;
            }
            if (!exists) {
                try (Statement st = conn.createStatement()) {
                    st.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
                    System.out.println("Column " + column + " added to " + table);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error ensuring column " + column + " in table " + table + ": " + e.getMessage());
        }
    }

    private static void dropColumnIfExists(Connection conn, String table, String column) {
        try {
            boolean exists = false;
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SHOW COLUMNS FROM " + table + " LIKE '" + column + "'")) {
                if (rs.next()) exists = true;
            }
            if (exists) {
                try (Statement st = conn.createStatement()) {
                    st.executeUpdate("ALTER TABLE " + table + " DROP COLUMN " + column);
                    System.out.println("Redundant column " + column + " removed from " + table);
                }
            }
        } catch (SQLException ignored) {}
    }

    private static void executeQuietly(Connection conn, String sql) {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate(sql);
        } catch (SQLException e) {
            System.err.println("Error executing SQL: " + sql + " -> " + e.getMessage());
        }
    }
}
