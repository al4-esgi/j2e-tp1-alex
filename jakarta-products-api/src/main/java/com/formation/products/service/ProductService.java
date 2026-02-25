package com.formation.products.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.formation.products.model.Product;
import com.formation.products.repository.IProductRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * ProductService - Couche Application (Business Logic)
 * Contient la logique métier et les validations
 * Dépend de l'interface IProductRepository (principe DIP)
 *
 * @ApplicationScoped : Bean CDI avec cycle de vie application
 */
@ApplicationScoped
public class ProductService {

    @Inject
    private IProductRepository productRepository;

    /**
     * Crée un nouveau produit avec validation
     * @param product le produit à créer
     * @return le produit créé avec son ID
     * @throws IllegalArgumentException si les données sont invalides
     */
    public Product createProduct(Product product) {
        validateProduct(product);

        // S'assurer que l'ID est null pour une création
        product.setId(null);

        return productRepository.save(product);
    }

    /**
     * Récupère un produit par son ID
     * @param id l'identifiant du produit
     * @return Optional contenant le produit si trouvé
     */
    public Optional<Product> getProduct(String id) {
        if (id == null || id.trim().isEmpty()) {
            return Optional.empty();
        }
        return productRepository.findById(id);
    }

    /**
     * Récupère tous les produits
     * @return liste de tous les produits
     */
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    /**
     * Récupère les produits par catégorie
     * @param category la catégorie recherchée
     * @return liste des produits de cette catégorie
     */
    public List<Product> getProductsByCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return getAllProducts();
        }
        return productRepository.findByCategory(category);
    }

    /**
     * Met à jour un produit existant
     * @param id l'identifiant du produit
     * @param updatedProduct les nouvelles données
     * @return le produit mis à jour
     * @throws IllegalArgumentException si le produit n'existe pas ou données invalides
     */
    public Product updateProduct(String id, Product updatedProduct) {
        if (!productRepository.exists(id)) {
            throw new IllegalArgumentException("Produit non trouvé avec l'ID: " + id);
        }

        validateProduct(updatedProduct);

        // Conserver l'ID et la date de création originale
        Optional<Product> existingProduct = productRepository.findById(id);
        if (existingProduct.isPresent()) {
            updatedProduct.setId(id);
            updatedProduct.setCreatedAt(existingProduct.get().getCreatedAt());
        }

        return productRepository.save(updatedProduct);
    }

    /**
     * Supprime un produit
     * @param id l'identifiant du produit à supprimer
     * @throws IllegalArgumentException si le produit n'existe pas
     */
    public void deleteProduct(String id) {
        if (!productRepository.exists(id)) {
            throw new IllegalArgumentException("Produit non trouvé avec l'ID: " + id);
        }
        productRepository.delete(id);
    }

    /**
     * Ajuste le stock d'un produit
     * @param id l'identifiant du produit
     * @param quantity la quantité à ajouter (peut être négative)
     * @return le produit avec le stock mis à jour
     * @throws IllegalArgumentException si le produit n'existe pas ou stock insuffisant
     */
    public Product updateStock(String id, int quantity) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Produit non trouvé avec l'ID: " + id));

        int newStock = product.getStock() + quantity;

        if (newStock < 0) {
            throw new IllegalArgumentException(
                "Stock insuffisant. Stock actuel: " + product.getStock() +
                ", quantité demandée: " + quantity
            );
        }

        product.setStock(newStock);
        return productRepository.save(product);
    }

    /**
     * Compte le nombre total de produits
     * @return le nombre de produits
     */
    public long countProducts() {
        return productRepository.count();
    }

    /**
     * Valide les données d'un produit
     * @param product le produit à valider
     * @throws IllegalArgumentException si les données sont invalides
     */
    private void validateProduct(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("Le produit ne peut pas être null");
        }

        // Validation du nom
        if (product.getName() == null || product.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom du produit est obligatoire");
        }

        // Validation du prix
        if (product.getPrice() == null) {
            throw new IllegalArgumentException("Le prix est obligatoire");
        }

        if (product.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le prix doit être supérieur à zéro");
        }

        // Validation de la catégorie
        if (product.getCategory() == null || product.getCategory().trim().isEmpty()) {
            throw new IllegalArgumentException("La catégorie est obligatoire");
        }

        // Validation du stock
        if (product.getStock() < 0) {
            throw new IllegalArgumentException("Le stock ne peut pas être négatif");
        }
    }
}
