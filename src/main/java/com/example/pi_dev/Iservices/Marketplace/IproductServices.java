package com.example.pi_dev.Iservices.Marketplace;
import com.example.pi_dev.Entities.Marketplace.Product;

import java.util.List;

public interface IproductServices {
    void ajouterProduct();
    void modifierProduct();
    void supprimerProduct();
    List<Product> afficherProducts();
}
