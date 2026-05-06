package com.example.pi_dev.Utils.Events;

import com.example.pi_dev.Database.Users.UserDatabaseConnection;
import java.sql.Connection;
import java.sql.SQLException;

public class Mydatabase {

    Connection con;
    public static Mydatabase instance;

    private Mydatabase(){
        try {
            con = UserDatabaseConnection.getInstance().getConnection();
            System.out.println("connexion etablie (shared UserDatabaseConnection)");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static Mydatabase getInstance(){
        if(instance == null){
            instance = new Mydatabase();
        }
        return instance;
    }

    public Connection getConnextion(){
        return con;
    }
}