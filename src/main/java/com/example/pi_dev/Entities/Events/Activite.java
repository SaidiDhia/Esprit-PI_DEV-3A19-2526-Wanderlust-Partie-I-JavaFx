package com.example.pi_dev.Entities.Events;

import java.sql.Timestamp;

public class Activite {

    private int id;
    private String titre;
    private String description;
    private String typeActivite;
    private CategorieActivite categorie;
    private String image;
    private Timestamp dateCreation;
    private Timestamp dateModification;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTypeActivite() {
        return typeActivite;
    }

    public void setTypeActivite(String typeActivite) {
        this.typeActivite = typeActivite;
    }

    public CategorieActivite getCategorie() {
        return categorie;
    }

    public void setCategorie(CategorieActivite categorie) {
        this.categorie = categorie;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Timestamp getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(Timestamp dateCreation) {
        this.dateCreation = dateCreation;
    }

    public Timestamp getDateModification() {
        return dateModification;
    }

    public void setDateModification(Timestamp dateModification) {
        this.dateModification = dateModification;
    }

    public Activite() {
    }

    public Activite(int id, String titre, String description, String typeActivite, CategorieActivite categorie, String image, Timestamp dateCreation, Timestamp dateModification) {
        this.id = id;
        this.titre = titre;
        this.description = description;
        this.typeActivite = typeActivite;
        this.categorie = categorie;
        this.image = image;
        this.dateCreation = dateCreation;
        this.dateModification = dateModification;
    }

    public Activite(int id, String titre, String typeActivite, CategorieActivite categorie, String description, String image) {
        this.id = id;
        this.titre = titre;
        this.typeActivite = typeActivite;
        this.categorie = categorie;
        this.description = description;
        this.image = image;
    }

    @Override
    public String toString() {
        return "Activite{" +
                "id=" + id +
                ", titre='" + titre + '\'' +
                ", description='" + description + '\'' +
                ", typeActivite='" + typeActivite + '\'' +
                ", image='" + image + '\'' +
                ", dateCreation=" + dateCreation +
                ", dateModification=" + dateModification +
                '}';
    }

}