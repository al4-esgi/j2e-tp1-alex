package com.formation.springproducts.repository;

import com.formation.springproducts.model.Order;
import com.formation.springproducts.model.OrderStatus;
import jakarta.persistence.QueryHint;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * OrderRepository — Spring Data JPA
 *
 * JpaRepository<Order, Long> fournit gratuitement :
 *   save(), findById(), findAll(), deleteById(), count(), existsById(), etc.
 *
 * Particularité des requêtes sur Order :
 * Order a une relation @OneToMany vers OrderItem (collection).
 * Un JOIN FETCH sur une collection génère des doublons au niveau JPQL
 * (une ligne par item). On utilise DISTINCT pour dédupliquer côté Java.
 *
 * @QueryHints("hibernate.query.passDistinctThrough" = false) :
 * Sans ce hint, Hibernate enverrait DISTINCT dans le SQL ce qui est
 * incompatible avec ORDER BY sur des colonnes non projetées dans le SELECT.
 * Ce hint dit "applique DISTINCT uniquement en mémoire Java, pas en SQL"
 * → résout le conflit DISTINCT + ORDER BY en SQL.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    // -------------------------------------------------------------------------
    // Requêtes dérivées
    // -------------------------------------------------------------------------

    /**
     * Commandes d'un client par email, triées par date décroissante.
     * Spring Data génère : WHERE LOWER(o.customerEmail) = LOWER(?1) ORDER BY o.orderDate DESC
     *
     * Simple résumé sans items — utiliser findByCustomerEmailWithItems()
     * si les items doivent être inclus dans la réponse.
     */
    List<Order> findByCustomerEmailIgnoreCaseOrderByOrderDateDesc(String customerEmail);

    /**
     * Commandes ayant un statut donné, triées par date décroissante.
     * Spring Data génère : WHERE o.status = ?1 ORDER BY o.orderDate DESC
     */
    List<Order> findByStatusOrderByOrderDateDesc(OrderStatus status);

    /**
     * Vérifie si une commande existe avec ce numéro de commande.
     */
    boolean existsByOrderNumber(String orderNumber);

    // -------------------------------------------------------------------------
    // Chargement avec items (JOIN FETCH — Partie 5)
    // -------------------------------------------------------------------------

    /**
     * Récupère une commande avec ses items ET les produits de chaque item.
     *
     * Double JOIN FETCH : Order → items → product (+ category du product)
     * → une seule requête SQL au lieu de 1 + N + M requêtes.
     *
     * DISTINCT : évite les doublons Hibernate provoqués par le JOIN sur la
     * collection @OneToMany items (une ligne SQL par item → plusieurs lignes
     * pour le même Order).
     *
     * hibernate.query.passDistinctThrough = false :
     * Hibernate applique DISTINCT côté Java uniquement, pas en SQL
     * → évite le conflit DISTINCT + ORDER BY en SQL.
     */
    @QueryHints(@QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    @Query(
        """
        SELECT DISTINCT o FROM Order o
        LEFT JOIN FETCH o.items i
        LEFT JOIN FETCH i.product p
        LEFT JOIN FETCH p.category
        LEFT JOIN FETCH p.supplier
        WHERE o.id = :id
        """
    )
    Optional<Order> findByIdWithItems(@Param("id") Long id);

    /**
     * Toutes les commandes avec leurs items et produits chargés.
     *
     * Double JOIN FETCH pour tout charger en une seule requête SQL.
     * DISTINCT + passDistinctThrough=false pour éviter les doublons
     * sans perturber le ORDER BY en SQL.
     *
     * À utiliser pour les endpoints qui sérialisent les commandes complètes.
     * Pour les listes de résumé, utiliser findAll() (plus léger).
     */
    @QueryHints(@QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    @Query(
        """
        SELECT DISTINCT o FROM Order o
        LEFT JOIN FETCH o.items i
        LEFT JOIN FETCH i.product p
        LEFT JOIN FETCH p.category
        LEFT JOIN FETCH p.supplier
        ORDER BY o.orderDate DESC
        """
    )
    List<Order> findAllWithItems();

    /**
     * Commandes d'un client avec leurs items chargés.
     * JOIN FETCH pour éviter le N+1 lors de la sérialisation des items.
     */
    @QueryHints(@QueryHint(name = "hibernate.query.passDistinctThrough", value = "false"))
    @Query(
        """
        SELECT DISTINCT o FROM Order o
        LEFT JOIN FETCH o.items i
        LEFT JOIN FETCH i.product p
        LEFT JOIN FETCH p.supplier
        WHERE LOWER(o.customerEmail) = LOWER(:email)
        ORDER BY o.orderDate DESC
        """
    )
    List<Order> findByCustomerEmailWithItems(@Param("email") String email);

    // -------------------------------------------------------------------------
    // Agrégations (Partie 6.1)
    // -------------------------------------------------------------------------

    /**
     * Chiffre d'affaires total pour un statut donné.
     *
     * COALESCE retourne 0 si aucune commande avec ce statut n'existe
     * (SUM() retournerait null sur un ensemble vide).
     *
     * Appelé depuis OrderService.getTotalRevenue() avec OrderStatus.DELIVERED.
     */
    @Query(
        """
        SELECT COALESCE(SUM(o.totalAmount), 0)
        FROM Order o
        WHERE o.status = :status
        """
    )
    BigDecimal sumTotalAmountByStatus(@Param("status") OrderStatus status);

    /**
     * Nombre de commandes par statut.
     * Retourne Object[] { OrderStatus status, Long count }.
     *
     * Note : Object[0] sera du type OrderStatus (l'enum Java), pas une String,
     * car on groupe sur o.status (le champ Java mappé via EnumType.STRING).
     */
    @Query(
        """
        SELECT o.status, COUNT(o)
        FROM Order o
        GROUP BY o.status
        ORDER BY COUNT(o) DESC
        """
    )
    List<Object[]> countGroupByStatus();

    /**
     * Produits les plus commandés, en quantité totale commandée.
     * Retourne Object[] { String productName, Long totalQuantity }.
     *
     * On passe un Pageable depuis le service : PageRequest.of(0, limit).
     * Spring Data applique automatiquement LIMIT en SQL.
     *
     * GROUP BY oi.product.name : suffisant pour le TP.
     * En production, grouper sur oi.product.id pour éviter les collisions de noms.
     */
    @Query(
        """
        SELECT oi.product.name, SUM(oi.quantity)
        FROM OrderItem oi
        GROUP BY oi.product.name
        ORDER BY SUM(oi.quantity) DESC
        """
    )
    List<Object[]> findMostOrderedProducts(Pageable pageable);
}
