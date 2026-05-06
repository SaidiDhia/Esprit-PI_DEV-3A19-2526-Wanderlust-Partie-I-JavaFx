package com.example.pi_dev.Utils.Blog;

import com.example.pi_dev.Database.Users.UserDatabaseConnection;
import java.sql.Connection;
import java.sql.SQLException;

public class BlogDataBase {

    private static BlogDataBase instance;
    private Connection connection;

    // Constructeur privé (Singleton)
    private BlogDataBase() {
        try {
            connection = UserDatabaseConnection.getInstance().getConnection();
            System.out.println("Connexion etablie avec succes (shared UserDatabaseConnection)");
        } catch (SQLException e) {
            System.err.println("Erreur de connexion a la base de donnees !");
            e.printStackTrace();
        }
    }

    // Méthode pour obtenir l'instance unique (Singleton)
    public static BlogDataBase getInstance() {
        if (instance == null) {
            synchronized (BlogDataBase.class) {
                if (instance == null) {
                    instance = new BlogDataBase();
                }
            }
        }
        return instance;
    }

    // Méthode pour obtenir la connexion
    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = UserDatabaseConnection.getInstance().getConnection();
                System.out.println("Connexion retablie (shared UserDatabaseConnection)");
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la verification de la connexion");
            e.printStackTrace();
        }
        return connection;
    }

    // Méthode pour fermer la connexion
    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("Connexion fermee");
            } catch (SQLException e) {
                System.err.println("Erreur lors de la fermeture de la connexion");
                e.printStackTrace();
            }
        }
    }

    // Méthode pour tester la connexion
    public boolean testConnection() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}
