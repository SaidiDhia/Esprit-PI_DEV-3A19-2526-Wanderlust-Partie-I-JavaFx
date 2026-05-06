package com.example.pi_dev.Services.Blog;

import com.example.pi_dev.Entities.Blog.Reaction;
import com.example.pi_dev.Iservices.Blog.IreactionServices;
import com.example.pi_dev.Utils.Blog.BlogDataBase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Reaction_Services implements IreactionServices {

    private Connection connection;

    public Reaction_Services() {
        connection = BlogDataBase.getInstance().getConnection();
        ensureSchema();
    }

    private void ensureSchema() {
        try (Statement st = connection.createStatement()) {
            try (ResultSet rs = st.executeQuery("SHOW COLUMNS FROM reactions LIKE 'id_user'")) {
                if (rs.next()) {
                    String type = rs.getString("Type");
                    if (type == null || !type.toLowerCase().startsWith("varchar")) {
                        try (Statement alter = connection.createStatement()) {
                            alter.executeUpdate("ALTER TABLE reactions MODIFY id_user VARCHAR(36) NOT NULL");
                        }
                    }
                }
            }
            // Optional FK; ignore errors if it already exists or data prevents adding it now
            try {
                st.executeUpdate("ALTER TABLE reactions ADD CONSTRAINT fk_reactions_user FOREIGN KEY (id_user) REFERENCES users(user_id)");
            } catch (SQLException ignored) {}
        } catch (SQLException ignored) {}
    }

    public boolean toggleReaction(Reaction reaction) {
        Reaction existing = getUserReaction(reaction.getIdPost(), reaction.getIdCommentaire(), reaction.getIdUser());
        if (existing != null) {
            supprimerReaction(existing.getIdReaction());
            return false;
        } else {
            ajouterReaction(reaction);
            return true;
        }
    }

    public Reaction getUserReaction(Integer idPost, Integer idCommentaire, String idUser) { // FIXED: String
        String requete;
        if (idPost != null)
            requete = "SELECT * FROM reactions WHERE id_post = ? AND id_user = ? AND id_commentaire IS NULL";
        else
            requete = "SELECT * FROM reactions WHERE id_commentaire = ? AND id_user = ?";
        try {
            PreparedStatement pst = connection.prepareStatement(requete);
            pst.setInt(1, idPost != null ? idPost : idCommentaire);
            pst.setString(2, idUser); // FIXED: setString
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    @Override
    public void ajouterReaction(Reaction reaction) {
        String requete = "INSERT INTO reactions (type, id_post, id_commentaire, id_user) VALUES (?, ?, ?, ?)";
        try {
            PreparedStatement pst = connection.prepareStatement(requete, Statement.RETURN_GENERATED_KEYS);
            pst.setString(1, reaction.getType());
            if (reaction.getIdPost() != null) pst.setInt(2, reaction.getIdPost());
            else pst.setNull(2, Types.INTEGER);
            if (reaction.getIdCommentaire() != null) pst.setInt(3, reaction.getIdCommentaire());
            else pst.setNull(3, Types.INTEGER);
            pst.setString(4, reaction.getIdUser()); // FIXED: setString
            int rowsAffected = pst.executeUpdate();
            if (rowsAffected > 0) {
                ResultSet generatedKeys = pst.getGeneratedKeys();
                if (generatedKeys.next()) reaction.setIdReaction(generatedKeys.getInt(1));
            }
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("incorrect integer value")) {
                ensureSchema();
                try {
                    PreparedStatement pst = connection.prepareStatement(requete, Statement.RETURN_GENERATED_KEYS);
                    pst.setString(1, reaction.getType());
                    if (reaction.getIdPost() != null) pst.setInt(2, reaction.getIdPost());
                    else pst.setNull(2, Types.INTEGER);
                    if (reaction.getIdCommentaire() != null) pst.setInt(3, reaction.getIdCommentaire());
                    else pst.setNull(3, Types.INTEGER);
                    pst.setString(4, reaction.getIdUser());
                    int rowsAffected = pst.executeUpdate();
                    if (rowsAffected > 0) {
                        ResultSet generatedKeys = pst.getGeneratedKeys();
                        if (generatedKeys.next()) reaction.setIdReaction(generatedKeys.getInt(1));
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            } else {
                e.printStackTrace();
            }
        }
    }

    @Override
    public List<Reaction> afficherReactions() {
        List<Reaction> list = new ArrayList<>();
        try {
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM reactions ORDER BY date DESC");
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    @Override
    public Reaction getReactionById(int idReaction) {
        try {
            PreparedStatement pst = connection.prepareStatement("SELECT * FROM reactions WHERE id_reaction = ?");
            pst.setInt(1, idReaction);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    @Override
    public List<Reaction> getReactionsByCommentaire(int idCommentaire) {
        List<Reaction> list = new ArrayList<>();
        try {
            PreparedStatement pst = connection.prepareStatement(
                    "SELECT * FROM reactions WHERE id_commentaire = ? ORDER BY date DESC");
            pst.setInt(1, idCommentaire);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    @Override
    public List<Reaction> getReactionsByPost(int idPost) {
        List<Reaction> list = new ArrayList<>();
        try {
            PreparedStatement pst = connection.prepareStatement(
                    "SELECT * FROM reactions WHERE id_post = ? ORDER BY date DESC");
            pst.setInt(1, idPost);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    @Override
    public void modifierReaction(Reaction reaction) {
        try {
            PreparedStatement pst = connection.prepareStatement(
                    "UPDATE reactions SET type = ? WHERE id_reaction = ?");
            pst.setString(1, reaction.getType());
            pst.setInt(2, reaction.getIdReaction());
            pst.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public void supprimerReaction(int idReaction) {
        try {
            PreparedStatement pst = connection.prepareStatement(
                    "DELETE FROM reactions WHERE id_reaction = ?");
            pst.setInt(1, idReaction);
            pst.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public int compterReactionsByType(String type, Integer idPost, Integer idCommentaire) {
        String requete;
        if (idPost != null) requete = "SELECT COUNT(*) as total FROM reactions WHERE id_post = ? AND type = ?";
        else if (idCommentaire != null) requete = "SELECT COUNT(*) as total FROM reactions WHERE id_commentaire = ? AND type = ?";
        else return 0;
        try {
            PreparedStatement pst = connection.prepareStatement(requete);
            pst.setInt(1, idPost != null ? idPost : idCommentaire);
            pst.setString(2, type);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return rs.getInt("total");
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    private Reaction mapRow(ResultSet rs) throws SQLException {
        Integer idPost = rs.getObject("id_post") != null ? rs.getInt("id_post") : null;
        Integer idCommentaire = rs.getObject("id_commentaire") != null ? rs.getInt("id_commentaire") : null;
        String idUser = null;
        try { idUser = rs.getString("id_user"); } catch (Exception ignored) {} // FIXED: getString
        return new Reaction(
                rs.getInt("id_reaction"),
                rs.getString("type"),
                rs.getTimestamp("date").toLocalDateTime(),
                idPost,
                idCommentaire,
                idUser // FIXED: String
        );
    }
}
