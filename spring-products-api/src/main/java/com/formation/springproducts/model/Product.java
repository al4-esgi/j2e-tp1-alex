package com.formation.springproducts.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedEntityGraphs;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entité Product - Version JPA complète (TP2)
 *
 * Changements par rapport au TP1 :
 * - id : String -> Long (généré par la base)
 * - category : String -> @ManyToOne Category (clé étrangère category_id)
 * - supplier : @ManyToOne Supplier (clé étrangère supplier_id)
 * - createdAt / updatedAt gérés par @PrePersist / @PreUpdate
 *
 * Fetch strategies :
 * - LAZY sur category et supplier → la relation n'est pas chargée automatiquement,
 *   il faut un JOIN FETCH explicite dans les requêtes JPQL pour éviter le N+1.
 *
 * @NamedEntityGraph : profils de chargement nommés utilisables depuis les repositories.
 */
@Entity
@Table(name = "products")
@NamedEntityGraphs(
    {
        @NamedEntityGraph(name = "Product.withCategory", attributeNodes = @NamedAttributeNode("category")),
        @NamedEntityGraph(name = "Product.full", attributeNodes = { @NamedAttributeNode("category"), @NamedAttributeNode("supplier") }),
    }
)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le nom du produit est obligatoire")
    @Size(max = 200, message = "Le nom ne peut pas dépasser 200 caractères")
    @Column(nullable = false, length = 200)
    private String name;

    @Size(max = 1000, message = "La description ne peut pas dépasser 1000 caractères")
    @Column(length = 1000)
    private String description;

    @NotNull(message = "Le prix est obligatoire")
    @DecimalMin(value = "0.01", message = "Le prix doit être supérieur à zéro")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Min(value = 0, message = "Le stock ne peut pas être négatif")
    @Column(nullable = false)
    private int stock;

    /**
     * Relation ManyToOne vers Category.
     *
     * LAZY : la catégorie n'est pas chargée automatiquement avec le produit.
     * Pour la charger, utiliser JOIN FETCH dans une requête JPQL.
     * Cela évite les requêtes inutiles quand on n'a pas besoin de la catégorie.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    /**
     * Relation ManyToOne vers Supplier.
     *
     * LAZY : même raison que pour category.
     * LEFT JOIN FETCH dans les requêtes car le supplier peut être null.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // -------------------------------------------------------------------------
    // Constructeurs
    // -------------------------------------------------------------------------

    public Product() {}

    public Product(String name, String description, BigDecimal price, int stock) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
    }

    public Product(String name, String description, BigDecimal price, int stock, Category category, Supplier supplier) {
        this(name, description, price, stock);
        this.category = category;
        this.supplier = supplier;
    }

    // -------------------------------------------------------------------------
    // Callbacks JPA
    // -------------------------------------------------------------------------

    /**
     * Appelé automatiquement par JPA avant le premier INSERT.
     * Initialise les timestamps.
     */
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Appelé automatiquement par JPA avant chaque UPDATE.
     * Met à jour le timestamp de modification.
     */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // -------------------------------------------------------------------------
    // Méthodes utilitaires
    // -------------------------------------------------------------------------

    public void adjustStock(int quantity) {
        this.stock += quantity;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // -------------------------------------------------------------------------
    // equals / hashCode / toString
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return Objects.equals(id, product.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return (
            "Product{" +
            "id=" +
            id +
            ", name='" +
            name +
            '\'' +
            ", price=" +
            price +
            ", stock=" +
            stock +
            ", category=" +
            (category != null ? category.getName() : "null") +
            ", supplier=" +
            (supplier != null ? supplier.getName() : "null") +
            '}'
        );
    }
}
