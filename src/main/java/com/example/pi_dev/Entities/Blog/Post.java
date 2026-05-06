package com.example.pi_dev.Entities.Blog;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Post {
    private int idPost;
    private String contenu;
    private String media;
    private LocalDateTime dateCreation;
    private String statut;
    private String idUser; // CHANGED: int → String (UUID)

    private List<Commentaire> commentaires;
    private List<Reaction> reactions;

    public Post() {
        this.commentaires = new ArrayList<>();
        this.reactions = new ArrayList<>();
        this.dateCreation = LocalDateTime.now();
    }

    public Post(String contenu, String media, String statut, String idUser) {
        this.contenu = contenu;
        this.media = media;
        this.statut = statut;
        this.idUser = idUser;
        this.dateCreation = LocalDateTime.now();
        this.commentaires = new ArrayList<>();
        this.reactions = new ArrayList<>();
    }

    public Post(int idPost, String contenu, String media, LocalDateTime dateCreation, String statut, String idUser) {
        this.idPost = idPost;
        this.contenu = contenu;
        this.media = media;
        this.dateCreation = dateCreation;
        this.statut = statut;
        this.idUser = idUser;
        this.commentaires = new ArrayList<>();
        this.reactions = new ArrayList<>();
    }

    public int getIdPost() { return idPost; }
    public void setIdPost(int idPost) { this.idPost = idPost; }
    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }
    public String getMedia() { return media; }
    public void setMedia(String media) { this.media = media; }
    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public String getIdUser() { return idUser; } // CHANGED
    public void setIdUser(String idUser) { this.idUser = idUser; } // CHANGED
    public List<Commentaire> getCommentaires() { return commentaires; }
    public void setCommentaires(List<Commentaire> commentaires) { this.commentaires = commentaires; }
    public List<Reaction> getReactions() { return reactions; }
    public void setReactions(List<Reaction> reactions) { this.reactions = new ArrayList<>(reactions); }

    @Override
    public String toString() {
        return "Post{idPost=" + idPost + ", statut='" + statut + "', idUser=" + idUser + '}';
    }
}
