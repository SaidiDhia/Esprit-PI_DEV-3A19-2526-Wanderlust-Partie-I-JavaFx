package com.example.pi_dev.Utils.Booking;

import com.example.pi_dev.Database.Users.UserDatabaseConnection;
import java.sql.Connection;
import java.sql.SQLException;

public class Mydatabase {

    private static Mydatabase instance;
    private final Connection con;

    private Mydatabase() {
        try {
            con = UserDatabaseConnection.getInstance().getConnection();
            System.out.println("connexion etablie (shared UserDatabaseConnection)");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static synchronized Mydatabase getInstance() {
        if (instance == null) {
            instance = new Mydatabase();
        }
        return instance;
    }

    public Connection getConnextion() {
        return con;
    }
}