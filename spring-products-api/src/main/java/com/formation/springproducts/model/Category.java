package com.formation.springproducts.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Entité Category
 *
 * Représente une catégorie de produits.
 * Une catégorie peut contenir plusieurs produits (@OneToMany).
 *
 * Questions TP :
 * - mappedBy = "category" → le côté propriétaire de la relation est Product.category
 * - orphanRemoval = true → si un Product est retiré de la liste, il est supprimé en base
 * - CascadeType.ALL → toutes les opérations (persist, merge, remove…) se propagent aux produits
 */
@Entity
@Table(name = "categories")
@NamedEntityGraph(name = "Category.withProducts", attributeNodes = @NamedAttributeNode("products"))
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le nom de la catégorie est obligatoire")
    @Size(max = 100, message = "Le nom ne peut pas dépasser 100 caractères")
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Size(max = 500, message = "La description ne peut pas dépasser 500 caractères")
    @Column(length = 500)
    private String description;

    /**
     * Relation bidirectionnelle : une catégorie possède plusieurs produits.
     *
     * - mappedBy = "category" : c'est Product.category_id qui porte la clé étrangère
     * - cascade = ALL : persist/merge/remove se propagent aux produits enfants
     * - orphanRemoval = true : un produit retiré de cette liste est supprimé en base
     * - @JsonIgnore : évite la récursion infinie lors de la sérialisation JSON
     * - @BatchSize(size = 10) : charge les produits par lots de 10 (optimisation N+1)
     */
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    @org.hibernate.annotations.BatchSize(size = 10)
    @JsonIgnore
    private List<Product> products = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Constructeurs
    // -------------------------------------------------------------------------

    public Category() {}

    public Category(String name, String description) {
        this.name = name;
        this.description = description;
    }

    // -------------------------------------------------------------------------
    // Méthodes helper pour maintenir la relation bidirectionnelle
    // -------------------------------------------------------------------------

    /**
     * Ajoute un produit à cette catégorie.
     * Maintient la cohérence des deux côtés de la relation.
     */
    public void addProduct(Product product) {
        products.add(product);
        product.setCategory(this);
    }

    /**
     * Retire un produit de cette catégorie.
     * Maintient la cohérence des deux côtés de la relation.
     */
    public void removeProduct(Product product) {
        products.remove(product);
        product.setCategory(null);
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

    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }

    // -------------------------------------------------------------------------
    // equals / hashCode / toString
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return Objects.equals(id, category.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Category{id=" + id + ", name='" + name + "'}";
    }
}
