package com.example.pi_dev.Iservices.Blog;

import com.example.pi_dev.Entities.Blog.Post;
import java.util.List;

public interface IpostServices {
    // Create
    void ajouterPost(Post post);

    // Read
    List<Post> afficherPosts();
    Post getPostById(int idPost);

    // Update
    void modifierPost(Post post);

    // Delete
    void supprimerPost(int idPost);

    // Méthodes supplémentaires
    List<Post> getPostsByStatut(String statut);
}
