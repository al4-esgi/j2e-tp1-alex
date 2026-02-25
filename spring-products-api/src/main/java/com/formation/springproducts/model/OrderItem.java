package com.formation.springproducts.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Entité OrderItem — Ligne de commande
 *
 * Représente un produit commandé à une certaine quantité dans le cadre d'une Order.
 *
 * Points importants :
 * - unitPrice est copié depuis product.getPrice() au moment de la commande.
 *   Pourquoi ? Si le prix du produit change plus tard, le prix historique de la
 *   commande doit rester intact. C'est une donnée immuable de la vente.
 *
 * - subtotal = quantity * unitPrice, calculé automatiquement par @PrePersist / @PreUpdate.
 *
 * - @JsonIgnore sur order : évite la récursion infinie JSON
 *   (Order → items → OrderItem → order → items → …)
 */
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Commande parente. Côté propriétaire de la relation bidirectionnelle.
     * @JsonIgnore casse la boucle de sérialisation JSON.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore
    private Order order;

    /**
     * Produit commandé. LAZY : chargé uniquement si accédé explicitement.
     */
    @NotNull(message = "Le produit est obligatoire")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * Quantité commandée — minimum 1.
     */
    @Min(value = 1, message = "La quantité doit être au moins 1")
    @Column(nullable = false)
    private int quantity;

    /**
     * Prix unitaire au moment de la commande.
     * Copié depuis product.getPrice() lors de la création de l'item.
     * Ne change pas si le prix du produit évolue ensuite.
     */
    @NotNull(message = "Le prix unitaire est obligatoire")
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    /**
     * Sous-total calculé automatiquement : quantity * unitPrice.
     * Mis à jour par @PrePersist et @PreUpdate.
     */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    // -------------------------------------------------------------------------
    // Constructeurs
    // -------------------------------------------------------------------------

    public OrderItem() {}

    /**
     * Constructeur pratique.
     * unitPrice est passé explicitement (copié depuis product.getPrice() par le service).
     */
    public OrderItem(Product product, int quantity, BigDecimal unitPrice) {
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        calculateSubtotal();
    }

    // -------------------------------------------------------------------------
    // Callbacks JPA
    // -------------------------------------------------------------------------

    /**
     * Calcule le sous-total avant la première insertion en base.
     */
    @PrePersist
    public void prePersist() {
        calculateSubtotal();
    }

    /**
     * Recalcule le sous-total avant chaque mise à jour (ex : changement de quantité).
     */
    @PreUpdate
    public void preUpdate() {
        calculateSubtotal();
    }

    // -------------------------------------------------------------------------
    // Méthodes utilitaires
    // -------------------------------------------------------------------------

    /**
     * Calcule subtotal = quantity * unitPrice.
     * Robuste aux valeurs null pendant la construction de l'objet.
     */
    public void calculateSubtotal() {
        if (unitPrice != null) {
            this.subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        } else {
            this.subtotal = BigDecimal.ZERO;
        }
    }

    // -------------------------------------------------------------------------
    // Getters / Setters
    // -------------------------------------------------------------------------

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
        calculateSubtotal();
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
        calculateSubtotal();
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    // -------------------------------------------------------------------------
    // equals / hashCode / toString
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderItem orderItem = (OrderItem) o;
        return Objects.equals(id, orderItem.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return (
            "OrderItem{id=" +
            id +
            ", product=" +
            (product != null ? product.getName() : "null") +
            ", quantity=" +
            quantity +
            ", unitPrice=" +
            unitPrice +
            ", subtotal=" +
            subtotal +
            '}'
        );
    }
}
