package com.formation.springproducts.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Entité Supplier (Fournisseur)
 *
 * Un fournisseur peut fournir plusieurs produits (@OneToMany).
 *
 * Note : pas de cascade ni orphanRemoval ici car supprimer un fournisseur
 * ne doit pas supprimer ses produits — les produits ont une existence propre
 * indépendante du fournisseur.
 */
@Entity
@Table(name = "suppliers")
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le nom du fournisseur est obligatoire")
    @Size(max = 200, message = "Le nom ne peut pas dépasser 200 caractères")
    @Column(nullable = false, length = 200)
    private String name;

    @Email(message = "L'email doit être valide")
    @Column(unique = true)
    private String email;

    @Column(length = 20)
    private String phone;

    /**
     * Relation bidirectionnelle : un fournisseur fournit plusieurs produits.
     *
     * - mappedBy = "supplier" : c'est Product.supplier_id qui porte la clé étrangère
     * - Pas de cascade : la suppression d'un fournisseur ne supprime pas ses produits
     * - Pas d'orphanRemoval : un produit peut exister sans fournisseur
     * - @JsonIgnore : évite la récursion infinie lors de la sérialisation JSON
     */
    @OneToMany(mappedBy = "supplier")
    @JsonIgnore
    private List<Product> products = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Constructeurs
    // -------------------------------------------------------------------------

    public Supplier() {
    }

    public Supplier(String name, String email, String phone) {
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    // -------------------------------------------------------------------------
    // Méthodes helper
    // -------------------------------------------------------------------------

    public void addProduct(Product product) {
        products.add(product);
        product.setSupplier(this);
    }

    public void removeProduct(Product product) {
        products.remove(product);
        product.setSupplier(null);
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
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
        Supplier supplier = (Supplier) o;
        return Objects.equals(id, supplier.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Supplier{id=" + id + ", name='" + name + "', email='" + email + "'}";
    }
}
