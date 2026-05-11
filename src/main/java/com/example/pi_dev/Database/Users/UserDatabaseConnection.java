package com.example.pi_dev.Database.Users;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.SQLException;

public class UserDatabaseConnection {

    private static UserDatabaseConnection instance;
    private Connection connection;

    private static final String URL = "jdbc:mysql://localhost:3306/wonderlust_db?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private UserDatabaseConnection() throws SQLException {
        connection = DriverManager.getConnection(URL, USER, PASSWORD);
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET NAMES utf8mb4 COLLATE utf8mb4_general_ci");
        }
    }

    public static UserDatabaseConnection getInstance() throws SQLException {
        if (instance == null) {
            instance = new UserDatabaseConnection();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }
}