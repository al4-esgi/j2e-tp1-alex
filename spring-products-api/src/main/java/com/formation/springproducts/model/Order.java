package com.formation.springproducts.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Entité Order (Commande)
 *
 * Une commande appartient à un client et contient plusieurs OrderItem.
 *
 * Note : "order" est un mot réservé SQL → table nommée "orders".
 *
 * Relations :
 * - @OneToMany vers OrderItem avec cascade = ALL et orphanRemoval = true
 *   → ajouter / retirer un item de la liste suffit : JPA gère la persistence
 *   → supprimer l'Order supprime aussi tous ses items (cascade)
 *   → retirer un item de la liste le supprime en base (orphanRemoval)
 *
 * Cycle de vie du statut : PENDING → CONFIRMED → SHIPPED → DELIVERED
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Numéro de commande lisible, généré automatiquement dans @PrePersist.
     * Format : ORD-YYYYMMDD-XXXXXXXX (8 hex aléatoires)
     */
    @Column(name = "order_number", unique = true, nullable = false, length = 30)
    private String orderNumber;

    @NotBlank(message = "Le nom du client est obligatoire")
    @Column(name = "customer_name", nullable = false, length = 200)
    private String customerName;

    @Email(message = "L'email du client doit être valide")
    @Column(name = "customer_email", length = 200)
    private String customerEmail;

    /**
     * Statut stocké en tant que chaîne (EnumType.STRING) pour la lisibilité en base.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    /**
     * Montant total de la commande — calculé à partir des items via calculateTotal().
     * Stocké en base pour éviter de recalculer à chaque lecture.
     */
    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "order_date", updatable = false)
    private LocalDateTime orderDate;

    /**
     * Liste des lignes de commande.
     *
     * - cascade = ALL : persist/merge/remove se propagent automatiquement aux items
     * - orphanRemoval = true : un item retiré de cette liste est supprimé en base
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Constructeurs
    // -------------------------------------------------------------------------

    public Order() {}

    public Order(String customerName, String customerEmail) {
        this.customerName = customerName;
        this.customerEmail = customerEmail;
    }

    // -------------------------------------------------------------------------
    // Callback JPA
    // -------------------------------------------------------------------------

    /**
     * Appelé automatiquement par JPA avant le premier INSERT.
     * - Génère un numéro de commande unique et lisible
     * - Initialise la date de commande
     * - Positionne le statut initial à PENDING
     */
    @PrePersist
    public void prePersist() {
        if (this.orderDate == null) {
            this.orderDate = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = OrderStatus.PENDING;
        }
        if (this.orderNumber == null) {
            String datePart = this.orderDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String randomPart = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
            this.orderNumber = "ORD-" + datePart + "-" + randomPart;
        }
    }

    // -------------------------------------------------------------------------
    // Méthodes métier
    // -------------------------------------------------------------------------

    /**
     * Ajoute un item à la commande.
     * Maintient la relation bidirectionnelle Order ↔ OrderItem.
     */
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    /**
     * Retire un item de la commande.
     * Grâce à orphanRemoval = true, l'item sera supprimé en base.
     */
    public void removeItem(OrderItem item) {
        items.remove(item);
        item.setOrder(null);
    }

    /**
     * Recalcule et met à jour totalAmount à partir de tous les items.
     * À appeler après chaque ajout / modification / suppression d'item.
     */
    public void calculateTotal() {
        this.totalAmount = items.stream()
                .map(OrderItem::getSubtotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
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

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    // -------------------------------------------------------------------------
    // equals / hashCode / toString
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(id, order.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Order{id=" + id
                + ", orderNumber='" + orderNumber + '\''
                + ", customerName='" + customerName + '\''
                + ", status=" + status
                + ", totalAmount=" + totalAmount
                + ", items=" + items.size()
                + '}';
    }
}
