package com.example.pi_dev.Services.Blog;

import com.example.pi_dev.Entities.Blog.Post;
import com.example.pi_dev.Iservices.Blog.IpostServices;
import com.example.pi_dev.Utils.Blog.BlogDataBase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Posting_Services implements IpostServices {

    private Connection connection;

    public Posting_Services() {
        connection = BlogDataBase.getInstance().getConnection();
    }

    @Override
    public void ajouterPost(Post post) {
        String requete = "INSERT INTO posts (contenu, media, statut, id_user) VALUES (?, ?, ?, ?)";
        try {
            PreparedStatement pst = connection.prepareStatement(requete, Statement.RETURN_GENERATED_KEYS);
            pst.setString(1, post.getContenu());
            pst.setString(2, post.getMedia());
            pst.setString(3, post.getStatut());
            pst.setString(4, post.getIdUser()); // FIXED: setString
            int rowsAffected = pst.executeUpdate();
            if (rowsAffected > 0) {
                ResultSet generatedKeys = pst.getGeneratedKeys();
                if (generatedKeys.next()) post.setIdPost(generatedKeys.getInt(1));
                System.out.println("Post ajoute! ID: " + post.getIdPost());
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public List<Post> afficherPosts() {
        List<Post> posts = new ArrayList<>();
        String requete = "SELECT * FROM posts ORDER BY date_creation DESC";
        try {
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(requete);
            while (rs.next()) posts.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return posts;
    }

    @Override
    public Post getPostById(int idPost) {
        String requete = "SELECT * FROM posts WHERE id_post = ?";
        try {
            PreparedStatement pst = connection.prepareStatement(requete);
            pst.setInt(1, idPost);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    @Override
    public void modifierPost(Post post) {
        String requete = "UPDATE posts SET contenu = ?, media = ?, statut = ? WHERE id_post = ?";
        try {
            PreparedStatement pst = connection.prepareStatement(requete);
            pst.setString(1, post.getContenu());
            pst.setString(2, post.getMedia());
            pst.setString(3, post.getStatut());
            pst.setInt(4, post.getIdPost());
            pst.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public void supprimerPost(int idPost) {
        String requete = "DELETE FROM posts WHERE id_post = ?";
        try {
            PreparedStatement pst = connection.prepareStatement(requete);
            pst.setInt(1, idPost);
            pst.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public List<Post> getPostsByStatut(String statut) {
        List<Post> posts = new ArrayList<>();
        String requete = "SELECT * FROM posts WHERE statut = ? ORDER BY date_creation DESC";
        try {
            PreparedStatement pst = connection.prepareStatement(requete);
            pst.setString(1, statut);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) posts.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return posts;
    }

    private Post mapRow(ResultSet rs) throws SQLException {
        String idUser = null;
        try { idUser = rs.getString("id_user"); } catch (Exception ignored) {} // FIXED: getString
        return new Post(
                rs.getInt("id_post"),
                rs.getString("contenu"),
                rs.getString("media"),
                rs.getTimestamp("date_creation").toLocalDateTime(),
                rs.getString("statut"),
                idUser // FIXED: String
        );
    }
}
