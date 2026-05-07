package com.example.pi_dev.Services.Events;

import com.example.pi_dev.Entities.Events.Activite;
import com.example.pi_dev.Utils.Events.Mydatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ActiviteService {

    private Connection cnx;

    public ActiviteService() {
        cnx = Mydatabase.getInstance().getConnextion();
    }

    // ================= ADD =================
    public void ajouter(Activite a) throws SQLException {

        String sql = "INSERT INTO activites (titre, description, type_activite, image, age_minimum, status, date_creation, date_modification) VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())";

        PreparedStatement ps = cnx.prepareStatement(sql);

        ps.setString(1, a.getTitre());
        ps.setString(2, a.getDescription());
        ps.setString(3, a.getTypeActivite());
        ps.setString(4, a.getImage());
        if (a.getAgeMinimum() == null) {
            ps.setNull(5, java.sql.Types.INTEGER);
        } else {
            ps.setInt(5, a.getAgeMinimum());
        }
        ps.setString(6, "en_attente");

        ps.executeUpdate();
    }

    // READ
    public List<Activite> afficher() throws SQLException {

        List<Activite> list = new ArrayList<>();
        String sql = "SELECT * FROM activites";

        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {

            Activite a = new Activite();

            a.setId(rs.getInt("id"));
            a.setTitre(rs.getString("titre"));
            a.setDescription(rs.getString("description"));
            a.setTypeActivite(rs.getString("type_activite"));
            a.setImage(rs.getString("image"));
            a.setStatus(rs.getString("status"));
            int age = rs.getInt("age_minimum");
            if (rs.wasNull()) {
                a.setAgeMinimum(null);
            } else {
                a.setAgeMinimum(age);
            }
            a.setDateCreation(rs.getTimestamp("date_creation"));
            a.setDateModification(rs.getTimestamp("date_modification"));

            list.add(a);
        }

        return list;
    }

    // UPDATE
    public void modifier(Activite a) throws SQLException {

        String sql = "UPDATE activites SET titre=?, description=?, type_activite=?, image=?, age_minimum=?, status=?, date_modification=NOW() WHERE id=?";

        PreparedStatement ps = cnx.prepareStatement(sql);

        ps.setString(1, a.getTitre());
        ps.setString(2, a.getDescription());
        ps.setString(3, a.getTypeActivite());
        ps.setString(4, a.getImage());
        if (a.getAgeMinimum() == null) {
            ps.setNull(5, java.sql.Types.INTEGER);
        } else {
            ps.setInt(5, a.getAgeMinimum());
        }
        ps.setString(6, a.getStatus() != null ? a.getStatus() : "en_attente");
        ps.setInt(7, a.getId());

        ps.executeUpdate();
    }

    // DELETE
    public void supprimer(int id) throws SQLException {

        String sql = "DELETE FROM activites WHERE id=?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);

        ps.executeUpdate();
    }
}