package com.formation.springproducts.repository;

import com.formation.springproducts.dto.CategoryStats;
import com.formation.springproducts.model.Category;
import com.formation.springproducts.model.Product;
import com.formation.springproducts.model.Supplier;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * ProductRepository — Spring Data JPA
 *
 * En étendant JpaRepository<Product, Long>, on obtient gratuitement :
 *   save(), findById(), findAll(), deleteById(), count(), existsById(), etc.
 * Spring génère l'implémentation au démarrage — pas de classe concrète à écrire.
 *
 * Pour les requêtes complexes (JOIN FETCH, agrégations, sous-requêtes),
 * on utilise @Query avec du JPQL.
 *
 * Pourquoi @QueryHints HINT_PASS_DISTINCT_THROUGH = false ?
 * Quand on fait DISTINCT dans un JPQL avec JOIN FETCH sur une collection,
 * Hibernate voudrait passer DISTINCT au SQL → problème avec ORDER BY.
 * Ce hint dit à Hibernate : "applique DISTINCT côté Java uniquement,
 * ne l'envoie pas dans le SQL" → résout l'incompatibilité DISTINCT + ORDER BY.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    // -------------------------------------------------------------------------
    // findAll optimisé vs non-optimisé (démo N+1 — Partie 7.1)
    // -------------------------------------------------------------------------

    /**
     * Récupère tous les produits AVEC JOIN FETCH sur category et supplier.
     * Une seule requête SQL → évite le problème N+1.
     *
     * JPQL : JOIN FETCH (INNER) sur category → exclut les produits sans catégorie.
     *        LEFT JOIN FETCH sur supplier → inclut les produits sans fournisseur.
     *
     * findAll() hérité de JpaRepository charge les relations LAZY une par une
     * si on y accède → N+1 requêtes. Utiliser cette méthode en production.
     */
    @Query("SELECT p FROM Product p JOIN FETCH p.category LEFT JOIN FETCH p.supplier")
    List<Product> findAllOptimized();

    // -------------------------------------------------------------------------
    // Recherches métier
    // -------------------------------------------------------------------------

    /**
     * Produits d'une catégorie donnée, avec JOIN FETCH.
     * Spring Data peut dériver findByCategory(Category) sans @Query,
     * mais on ajoute JOIN FETCH pour éviter le N+1 sur supplier.
     */
    @Query(
        """
        SELECT p FROM Product p
        JOIN FETCH p.category
        LEFT JOIN FETCH p.supplier
        WHERE p.category = :category
        """
    )
    List<Product> findByCategory(@Param("category") Category category);

    /**
     * Produits d'un fournisseur donné, avec JOIN FETCH.
     */
    @Query(
        """
        SELECT p FROM Product p
        JOIN FETCH p.category
        JOIN FETCH p.supplier
        WHERE p.supplier = :supplier
        """
    )
    List<Product> findBySupplier(@Param("supplier") Supplier supplier);

    /**
     * Produits dont le prix est compris entre min et max, triés par prix croissant.
     * BETWEEN est inclusif des deux bornes.
     */
    @Query(
        """
        SELECT p FROM Product p
        JOIN FETCH p.category
        LEFT JOIN FETCH p.supplier
        WHERE p.price BETWEEN :min AND :max
        ORDER BY p.price ASC
        """
    )
    List<Product> findByPriceRange(@Param("min") BigDecimal min, @Param("max") BigDecimal max);

    /**
     * Recherche insensible à la casse par mot-clé dans le nom.
     * Le % doit être ajouté par l'appelant : "%keyword%".
     */
    @Query(
        """
        SELECT p FROM Product p
        JOIN FETCH p.category
        LEFT JOIN FETCH p.supplier
        WHERE LOWER(p.name) LIKE LOWER(:keyword)
        """
    )
    List<Product> searchByName(@Param("keyword") String keyword);

    // -------------------------------------------------------------------------
    // Agrégations (Partie 6.1)
    // -------------------------------------------------------------------------

    /**
     * Nombre de produits par catégorie.
     * Retourne Object[] { String categoryName, Long count }.
     */
    @Query(
        """
        SELECT p.category.name, COUNT(p)
        FROM Product p
        WHERE p.category IS NOT NULL
        GROUP BY p.category.name
        ORDER BY COUNT(p) DESC
        """
    )
    List<Object[]> countByCategory();

    /**
     * Prix moyen par catégorie.
     * Retourne Object[] { String categoryName, Double avgPrice }.
     */
    @Query(
        """
        SELECT p.category.name, AVG(p.price)
        FROM Product p
        WHERE p.category IS NOT NULL
        GROUP BY p.category.name
        ORDER BY AVG(p.price) DESC
        """
    )
    List<Object[]> averagePriceByCategory();

    /**
     * Top N produits les plus chers.
     * On passe un Pageable depuis le service : PageRequest.of(0, limit).
     * Spring Data applique automatiquement LIMIT (ou équivalent) en SQL.
     *
     * Attention : JOIN FETCH + Pageable génère un warning Hibernate
     * ("firstResult/maxResults specified with collection fetch")
     * car Hibernate ne peut pas faire un LIMIT en SQL avec un JOIN sur collection.
     * Ici ce sont des @ManyToOne (pas de collection) → pas de problème.
     */
    @Query(
        """
        SELECT p FROM Product p
        JOIN FETCH p.category
        LEFT JOIN FETCH p.supplier
        ORDER BY p.price DESC
        """
    )
    List<Product> findTopExpensive(Pageable pageable);

    /**
     * Statistiques par catégorie — projection DTO avec SELECT NEW (Partie 6.3).
     *
     * La syntaxe SELECT NEW instancie directement un objet Java depuis JPQL :
     * plus sûr que Object[] (typage fort, pas de cast).
     * Requiert un constructeur public dans CategoryStats avec les bons types.
     */
    @Query(
        """
        SELECT NEW com.formation.springproducts.dto.CategoryStats(
            p.category.name, COUNT(p), AVG(p.price))
        FROM Product p
        WHERE p.category IS NOT NULL
        GROUP BY p.category.name
        ORDER BY COUNT(p) DESC
        """
    )
    List<CategoryStats> getCategoryStats();

    // -------------------------------------------------------------------------
    // Sous-requêtes (Partie 6.2)
    // -------------------------------------------------------------------------

    /**
     * Produits qui n'ont jamais été commandés.
     * Sous-requête JPQL : WHERE p NOT IN (SELECT oi.product FROM OrderItem oi)
     *
     * Si aucun OrderItem n'existe, la sous-requête retourne une liste vide
     * → NOT IN liste vide → tous les produits sont inclus (comportement correct).
     */
    @Query(
        """
        SELECT p FROM Product p
        JOIN FETCH p.category
        LEFT JOIN FETCH p.supplier
        WHERE p NOT IN (SELECT oi.product FROM OrderItem oi)
        """
    )
    List<Product> findNeverOrderedProducts();

    // -------------------------------------------------------------------------
    // Sous-requêtes (Partie 6.2) — catégories avec minimum N produits
    // -------------------------------------------------------------------------

    /**
     * Vérifie si un produit existe avec un nom donné (insensible à la casse).
     * Spring Data dérive automatiquement cette requête depuis le nom de la méthode.
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Vérifie si un produit existe déjà avec ce SKU (unicité du SKU).
     * Utilisé avant la création pour détecter les doublons → DuplicateProductException.
     */
    boolean existsBySku(String sku);

    /**
     * Recherche un produit par son SKU (exact, sensible à la casse car le SKU est normalisé en majuscules).
     */
    java.util.Optional<Product> findBySku(String sku);

    /**
     * Récupère un produit par son id avec @EntityGraph "Product.full".
     * Charge category + supplier en une seule requête via le graph défini sur l'entité.
     *
     * @EntityGraph au niveau repository est équivalent au hint
     * jakarta.persistence.fetchgraph passé à em.find() dans l'approche EntityManager.
     */
    @org.springframework.data.jpa.repository.EntityGraph(value = "Product.full")
    Optional<Product> findWithGraphById(Long id);
}
