package com.example.pi_dev.Utils.Marketplace;

import java.sql.Connection;
import java.sql.SQLException;
import com.example.pi_dev.Database.Users.UserDatabaseConnection;

public class Mydatabase {

    public static Mydatabase instance;
    private Connection con;
    public static Mydatabase getInstance() {
        if (instance == null) {
            instance = new Mydatabase();
        }
        return instance;
    }
    private Mydatabase() {

        try {
            con = UserDatabaseConnection.getInstance().getConnection();
            System.out.println("connected (shared UserDatabaseConnection)");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public Connection getConnection() {
        return con;
    }

}
