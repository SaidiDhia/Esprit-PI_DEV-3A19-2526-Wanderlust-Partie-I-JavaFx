package com.example.pi_dev.Entities.Blog;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Commentaire {
    private int idCommentaire;
    private String contenu;
    private LocalDateTime date;
    private int idPost;
    private String idUser; // CHANGED: int → String (UUID)
    private Integer idParent;

    private List<Reaction> reactions;

    public Commentaire() {
        this.reactions = new ArrayList<>();
        this.date = LocalDateTime.now();
    }

    public Commentaire(String contenu, int idPost, String idUser) {
        this.contenu = contenu;
        this.idPost = idPost;
        this.idUser = idUser;
        this.idParent = null;
        this.date = LocalDateTime.now();
        this.reactions = new ArrayList<>();
    }

    public Commentaire(String contenu, int idPost, String idUser, int idParent) {
        this.contenu = contenu;
        this.idPost = idPost;
        this.idUser = idUser;
        this.idParent = idParent;
        this.date = LocalDateTime.now();
        this.reactions = new ArrayList<>();
    }

    public Commentaire(int idCommentaire, String contenu, LocalDateTime date, int idPost, String idUser, Integer idParent) {
        this.idCommentaire = idCommentaire;
        this.contenu = contenu;
        this.date = date;
        this.idPost = idPost;
        this.idUser = idUser;
        this.idParent = idParent;
        this.reactions = new ArrayList<>();
    }

    public int getIdCommentaire() { return idCommentaire; }
    public void setIdCommentaire(int idCommentaire) { this.idCommentaire = idCommentaire; }
    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }
    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }
    public int getIdPost() { return idPost; }
    public void setIdPost(int idPost) { this.idPost = idPost; }
    public String getIdUser() { return idUser; } // CHANGED
    public void setIdUser(String idUser) { this.idUser = idUser; } // CHANGED
    public Integer getIdParent() { return idParent; }
    public void setIdParent(Integer idParent) { this.idParent = idParent; }
    public List<Reaction> getReactions() { return reactions; }
    public void setReactions(List<Reaction> reactions) { this.reactions = reactions; }
    public boolean isReply() { return idParent != null; }

    @Override
    public String toString() {
        return "Commentaire{id=" + idCommentaire + ", idUser=" + idUser + ", idParent=" + idParent + '}';
    }
}
