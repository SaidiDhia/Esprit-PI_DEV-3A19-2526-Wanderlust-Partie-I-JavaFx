package com.example.pi_dev.Entities.Blog;

import java.time.LocalDateTime;

public class Reaction {
    private int idReaction;
    private String type;
    private LocalDateTime date;
    private Integer idPost;
    private Integer idCommentaire;
    private String idUser; // CHANGED: int → String (UUID)

    public Reaction() { this.date = LocalDateTime.now(); }

    public Reaction(String type, int idPost, String idUser) {
        this.type = type;
        this.idPost = idPost;
        this.idCommentaire = null;
        this.idUser = idUser;
        this.date = LocalDateTime.now();
    }

    public Reaction(String type, Integer idPost, Integer idCommentaire, String idUser) {
        this.type = type;
        this.idPost = idPost;
        this.idCommentaire = idCommentaire;
        this.idUser = idUser;
        this.date = LocalDateTime.now();
    }

    public Reaction(int idReaction, String type, LocalDateTime date, Integer idPost, Integer idCommentaire, String idUser) {
        this.idReaction = idReaction;
        this.type = type;
        this.date = date;
        this.idPost = idPost;
        this.idCommentaire = idCommentaire;
        this.idUser = idUser;
    }

    public int getIdReaction() { return idReaction; }
    public void setIdReaction(int idReaction) { this.idReaction = idReaction; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }
    public Integer getIdPost() { return idPost; }
    public void setIdPost(Integer idPost) { this.idPost = idPost; }
    public Integer getIdCommentaire() { return idCommentaire; }
    public void setIdCommentaire(Integer idCommentaire) { this.idCommentaire = idCommentaire; }
    public String getIdUser() { return idUser; } // CHANGED
    public void setIdUser(String idUser) { this.idUser = idUser; } // CHANGED

    @Override
    public String toString() {
        return "Reaction{id=" + idReaction + ", type='" + type + "', idUser=" + idUser + '}';
    }
}
