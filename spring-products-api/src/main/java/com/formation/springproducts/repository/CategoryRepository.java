package com.formation.springproducts.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.formation.springproducts.model.Category;

/**
 * CategoryRepository — Spring Data JPA
 *
 * JpaRepository<Category, Long> fournit gratuitement :
 *   save(), findById(), findAll(), deleteById(), count(), existsById(), etc.
 *
 * Requêtes dérivées (Spring Data génère le JPQL depuis le nom de la méthode) :
 *   findByNameIgnoreCase → WHERE LOWER(c.name) = LOWER(:name)
 *   existsByNameIgnoreCase → SELECT COUNT > 0 WHERE LOWER(c.name) = LOWER(:name)
 *
 * @Query pour les cas où on a besoin de JOIN FETCH (chargement des produits).
 *
 * @EntityGraph : alternative à @Query pour contrôler le fetch plan
 * sans écrire de JPQL. Utilise le graphe "Category.withProducts"
 * défini avec @NamedEntityGraph sur l'entité Category.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // -------------------------------------------------------------------------
    // Requêtes dérivées (Spring Data génère le JPQL automatiquement)
    // -------------------------------------------------------------------------

    /**
     * Recherche une catégorie par son nom, insensible à la casse.
     * Spring Data génère : WHERE LOWER(c.name) = LOWER(?1)
     *
     * Retourne Optional.empty() si aucune catégorie ne correspond.
     */
    Optional<Category> findByNameIgnoreCase(String name);

    /**
     * Vérifie si une catégorie existe avec ce nom (insensible à la casse).
     * Utilisé pour valider l'unicité du nom avant création/mise à jour.
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Toutes les catégories triées par nom alphabétique.
     * Spring Data génère : SELECT c FROM Category c ORDER BY c.name ASC
     */
    List<Category> findAllByOrderByNameAsc();

    // -------------------------------------------------------------------------
    // Chargement avec produits (JOIN FETCH — Partie 4.2)
    // -------------------------------------------------------------------------

    /**
     * Récupère une catégorie avec ses produits chargés via JOIN FETCH.
     *
     * Sans JOIN FETCH, accéder à category.getProducts() hors transaction
     * lèverait une LazyInitializationException (open-in-view=false).
     *
     * JPQL : LEFT JOIN FETCH car une catégorie peut n'avoir aucun produit.
     */
    @Query("""
           SELECT c FROM Category c
           LEFT JOIN FETCH c.products
           WHERE c.id = :id
           """)
    Optional<Category> findByIdWithProducts(@Param("id") Long id);

    /**
     * Alternative à findByIdWithProducts() via @EntityGraph.
     *
     * @EntityGraph(value = "Category.withProducts") utilise le graphe nommé
     * défini avec @NamedEntityGraph sur l'entité Category.
     * Produit le même SQL que le @Query ci-dessus (LEFT JOIN sur products).
     *
     * Les deux approches sont équivalentes — @EntityGraph est plus déclaratif,
     * @Query est plus explicite. On garde les deux pour illustrer la différence.
     */
    @EntityGraph(value = "Category.withProducts")
    @Query("SELECT c FROM Category c WHERE c.id = :id")
    Optional<Category> findByIdWithGraph(@Param("id") Long id);
}
