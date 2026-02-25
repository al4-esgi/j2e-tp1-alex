package com.formation.springproducts.repository;

import com.formation.springproducts.model.Product;

import java.util.List;
import java.util.Optional;

/**
 * Interface IProductRepository - Couche Infrastructure
 * Définit le contrat pour la persistence des produits
 * Respecte le principe d'inversion de dépendances (DIP)
 *
 * Identique à la version Jakarta EE pour maintenir la même architecture
 */
public interface IProductRepository {

    /**
     * Sauvegarde un produit (création ou mise à jour)
     * @param product le produit à sauvegarder
     * @return le produit sauvegardé avec son ID
     */
    Product save(Product product);

    /**
     * Recherche un produit par son ID
     * @param id l'identifiant du produit
     * @return Optional contenant le produit si trouvé
     */
    Optional<Product> findById(String id);

    /**
     * Récupère tous les produits
     * @return liste de tous les produits
     */
    List<Product> findAll();

    /**
     * Recherche les produits par catégorie
     * @param category la catégorie recherchée
     * @return liste des produits de cette catégorie
     */
    List<Product> findByCategory(String category);

    /**
     * Supprime un produit par son ID
     * @param id l'identifiant du produit à supprimer
     */
    void delete(String id);

    /**
     * Vérifie si un produit existe
     * @param id l'identifiant du produit
     * @return true si le produit existe
     */
    boolean exists(String id);

    /**
     * Compte le nombre total de produits
     * @return le nombre de produits
     */
    long count();
}
