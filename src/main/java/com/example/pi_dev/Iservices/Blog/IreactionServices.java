package com.example.pi_dev.Iservices.Blog;

import com.example.pi_dev.Entities.Blog.Reaction;
import java.util.List;

public interface IreactionServices {
    // Create
    void ajouterReaction(Reaction reaction);

    // Read
    List<Reaction> afficherReactions();
    Reaction getReactionById(int idReaction);
    List<Reaction> getReactionsByCommentaire(int idCommentaire);
    List<Reaction> getReactionsByPost(int idPost);

    // Update
    void modifierReaction(Reaction reaction);

    // Delete
    void supprimerReaction(int idReaction);

    // Méthodes supplémentaires
    int compterReactionsByType(String type, Integer idPost, Integer idCommentaire);

    // Toggle like/unlike — returns true if added, false if removed
    boolean toggleReaction(Reaction reaction);

    // Get existing reaction of a user on a post or comment
    Reaction getUserReaction(Integer idPost, Integer idCommentaire, String idUser); // FIXED: String
}
