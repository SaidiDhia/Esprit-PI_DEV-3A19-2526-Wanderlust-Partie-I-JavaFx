package com.example.pi_dev.Services.Blog;

import com.example.pi_dev.Utils.Blog.BlogDataBase;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;

public class SaveService {

    private final Connection connection;
    private final String idUser; // CHANGED: int → String (UUID)

    public SaveService(String idUser) { // CHANGED
        this.connection = BlogDataBase.getInstance().getConnection();
        this.idUser = idUser;
        ensureTableExists();
    }

    private void ensureTableExists() {
        String sql =
                "CREATE TABLE IF NOT EXISTS posts_sauvegardes (" +
                        "  id_user VARCHAR(36) NOT NULL," + // CHANGED
                        "  id_post INT NOT NULL," +
                        "  PRIMARY KEY (id_user, id_post)," +
                        "  FOREIGN KEY (id_post) REFERENCES posts(id_post) ON DELETE CASCADE," +
                        "  FOREIGN KEY (id_user) REFERENCES users(user_id) ON DELETE CASCADE" + // ADDED
                        ")";
        try {
            connection.createStatement().executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Set<Integer> loadSavedPostIds() {
        Set<Integer> ids = new HashSet<>();
        String sql = "SELECT id_post FROM posts_sauvegardes WHERE id_user = ?";
        try {
            PreparedStatement pst = connection.prepareStatement(sql);
            pst.setString(1, idUser); // CHANGED: setString
            ResultSet rs = pst.executeQuery();
            while (rs.next()) ids.add(rs.getInt("id_post"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ids;
    }

    public void savePost(int idPost) {
        String sql = "INSERT IGNORE INTO posts_sauvegardes (id_user, id_post) VALUES (?, ?)";
        try {
            PreparedStatement pst = connection.prepareStatement(sql);
            pst.setString(1, idUser); // CHANGED: setString
            pst.setInt(2, idPost);
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void unsavePost(int idPost) {
        String sql = "DELETE FROM posts_sauvegardes WHERE id_user = ? AND id_post = ?";
        try {
            PreparedStatement pst = connection.prepareStatement(sql);
            pst.setString(1, idUser); // CHANGED: setString
            pst.setInt(2, idPost);
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean toggle(int idPost) {
        if (loadSavedPostIds().contains(idPost)) {
            unsavePost(idPost);
            return false;
        } else {
            savePost(idPost);
            return true;
        }
    }
}
