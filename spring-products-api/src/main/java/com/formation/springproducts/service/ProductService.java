package com.formation.springproducts.service;

import com.formation.springproducts.dto.CategoryStats;
import com.formation.springproducts.model.Category;
import com.formation.springproducts.model.Product;
import com.formation.springproducts.model.Supplier;
import com.formation.springproducts.repository.CategoryRepository;
import com.formation.springproducts.repository.ProductRepository;
import com.formation.springproducts.repository.SupplierRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.hibernate.Hibernate;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ProductService — Couche Application (TP2)
 *
 * Utilise les repositories Spring Data JPA directement.
 * Plus de classes Jpa*Repository manuelles : Spring génère les implémentations
 * à partir des interfaces au démarrage.
 *
 * @Transactional sur la classe :
 * - Toutes les méthodes publiques s'exécutent dans une transaction.
 * - RuntimeException → rollback automatique.
 * - Dirty checking : modifier une entité chargée dans la transaction
 *   suffit, pas besoin d'appeler save() (mais on le fait pour la clarté).
 *
 * Méthodes en readOnly = true :
 * - Hibernate désactive le dirty checking et le flush → meilleures performances.
 * - À utiliser systématiquement pour toutes les lectures.
 */
@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository, SupplierRepository supplierRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.supplierRepository = supplierRepository;
    }

    // -------------------------------------------------------------------------
    // CRUD de base
    // -------------------------------------------------------------------------

    /**
     * Crée un nouveau produit.
     * Le produit doit avoir sa catégorie assignée (entité avec id valide).
     *
     * @throws IllegalArgumentException si les données sont invalides
     */
    public Product createProduct(Product product) {
        validateProduct(product);
        product.setId(null); // force INSERT
        return productRepository.save(product);
    }

    /**
     * Récupère un produit par son id.
     * Utilise le @NamedEntityGraph "Product.full" pour charger
     * category + supplier en une seule requête.
     */
    @Transactional(readOnly = true)
    public Optional<Product> getProduct(Long id) {
        if (id == null || id <= 0) return Optional.empty();
        return productRepository.findWithGraphById(id);
    }

    /**
     * Tous les produits SANS JOIN FETCH — déclenche le problème N+1.
     * Démo Partie 7.1 : observe les logs Hibernate, tu verras 1+N requêtes.
     *
     * Hibernate.initialize() force l'initialisation du proxy DANS la session courante.
     * → 1 requête SELECT products
     * → 1 requête par catégorie unique non encore en cache L1 (N+1 sur les catégories)
     * → 1 requête par fournisseur unique non encore en cache L1 (N+1 sur les fournisseurs)
     *
     * Sans Hibernate.initialize(), les proxies LAZY seraient retournés non-initialisés.
     * Après fermeture de la transaction, Jackson tenterait de les sérialiser
     * sans session Hibernate → LazyInitializationException ("no Session").
     *
     * Comparer avec getAllProductsOptimized() : une seule requête SQL avec JOIN FETCH.
     */
    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        List<Product> products = productRepository.findAll();
        // N+1 : Hibernate.initialize() déclenche 1 SELECT par proxy non encore chargé.
        // Avec le cache L1, les catégories déjà vues ne génèrent pas de requête supplémentaire.
        // → Toujours nettement moins efficace que le JOIN FETCH de getAllProductsOptimized().
        products.forEach(p -> {
            if (p.getCategory() != null) {
                // Hibernate.unproxy() initialise le proxy ET retourne l'entité réelle.
                // Sans ça, Jackson tente de sérialiser le proxy Hibernate hors session → no Session.
                p.setCategory((Category) Hibernate.unproxy(p.getCategory()));
            }
            if (p.getSupplier() != null) {
                p.setSupplier((Supplier) Hibernate.unproxy(p.getSupplier()));
            }
        });
        return products;
    }

    /**
     * Tous les produits AVEC JOIN FETCH — version optimisée.
     * Une seule requête SQL. À utiliser en production.
     */
    @Transactional(readOnly = true)
    public List<Product> getAllProductsOptimized() {
        return productRepository.findAllOptimized();
    }

    /**
     * Produits d'une catégorie.
     *
     * @throws IllegalArgumentException si la catégorie n'existe pas
     */
    @Transactional(readOnly = true)
    public List<Product> getProductsByCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId).orElseThrow(() -> new IllegalArgumentException("Catégorie non trouvée avec l'ID: " + categoryId));
        return productRepository.findByCategory(category);
    }

    /**
     * Produits d'un fournisseur.
     *
     * @throws IllegalArgumentException si le fournisseur n'existe pas
     */
    @Transactional(readOnly = true)
    public List<Product> getProductsBySupplier(Long supplierId) {
        Supplier supplier = supplierRepository.findById(supplierId).orElseThrow(() -> new IllegalArgumentException("Fournisseur non trouvé avec l'ID: " + supplierId));
        return productRepository.findBySupplier(supplier);
    }

    /**
     * Produits dont le prix est compris entre min et max.
     */
    @Transactional(readOnly = true)
    public List<Product> getProductsByPriceRange(BigDecimal min, BigDecimal max) {
        if (min == null || max == null) {
            throw new IllegalArgumentException("Les bornes de prix min et max sont obligatoires");
        }
        if (min.compareTo(max) > 0) {
            throw new IllegalArgumentException("Le prix minimum ne peut pas être supérieur au prix maximum");
        }
        return productRepository.findByPriceRange(min, max);
    }

    /**
     * Recherche par mot-clé dans le nom (insensible à la casse).
     */
    @Transactional(readOnly = true)
    public List<Product> searchProducts(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllProductsOptimized();
        }
        String pattern = "%" + keyword.trim().toLowerCase() + "%";
        return productRepository.searchByName(pattern);
    }

    /**
     * Met à jour un produit existant.
     *
     * @throws IllegalArgumentException si le produit n'existe pas ou données invalides
     */
    public Product updateProduct(Long id, Product updatedProduct) {
        Product existing = productRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Produit non trouvé avec l'ID: " + id));

        validateProductForUpdate(updatedProduct);

        existing.setName(updatedProduct.getName());
        existing.setDescription(updatedProduct.getDescription());
        existing.setPrice(updatedProduct.getPrice());
        existing.setStock(updatedProduct.getStock());

        if (updatedProduct.getCategory() != null && updatedProduct.getCategory().getId() != null) {
            Category newCategory = categoryRepository
                .findById(updatedProduct.getCategory().getId())
                .orElseThrow(() -> new IllegalArgumentException("Catégorie non trouvée avec l'ID: " + updatedProduct.getCategory().getId()));
            existing.setCategory(newCategory);
        }

        if (updatedProduct.getSupplier() != null && updatedProduct.getSupplier().getId() != null) {
            Supplier newSupplier = supplierRepository
                .findById(updatedProduct.getSupplier().getId())
                .orElseThrow(() -> new IllegalArgumentException("Fournisseur non trouvé avec l'ID: " + updatedProduct.getSupplier().getId()));
            existing.setSupplier(newSupplier);
        }

        // Dirty checking : la modification de 'existing' (entité managed) suffit.
        // save() est explicite pour la lisibilité.
        return productRepository.save(existing);
    }

    /**
     * Supprime un produit.
     *
     * @throws IllegalArgumentException si le produit n'existe pas
     */
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new IllegalArgumentException("Produit non trouvé avec l'ID: " + id);
        }
        productRepository.deleteById(id);
    }

    // -------------------------------------------------------------------------
    // Méthodes métier avancées (Partie 4.1)
    // -------------------------------------------------------------------------

    /**
     * Crée un produit en cherchant ou créant sa catégorie par nom.
     * Tout dans une seule transaction : si une étape échoue → rollback complet.
     */
    public Product createProductWithCategory(Product product, String categoryName, Long supplierId) {
        validateProductForUpdate(product);

        // Chercher ou créer la catégorie
        Category category = categoryRepository.findByNameIgnoreCase(categoryName).orElseGet(() -> categoryRepository.save(new Category(categoryName, null)));

        product.setCategory(category);

        if (supplierId != null) {
            Supplier supplier = supplierRepository.findById(supplierId).orElseThrow(() -> new IllegalArgumentException("Fournisseur non trouvé avec l'ID: " + supplierId));
            product.setSupplier(supplier);
        }

        product.setId(null);
        return productRepository.save(product);
    }

    /**
     * Ajuste le stock d'un produit (quantité positive = entrée, négative = sortie).
     *
     * @throws IllegalArgumentException si le produit n'existe pas ou stock insuffisant
     */
    public Product updateStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId).orElseThrow(() -> new IllegalArgumentException("Produit non trouvé avec l'ID: " + productId));

        int newStock = product.getStock() + quantity;
        if (newStock < 0) {
            throw new IllegalArgumentException("Stock insuffisant. Stock actuel: " + product.getStock() + ", quantité demandée: " + quantity);
        }

        product.setStock(newStock);
        return productRepository.save(product);
    }

    /**
     * Transfère tous les produits d'une catégorie vers une autre.
     * Transaction atomique : tout ou rien.
     *
     * Dirty checking : on modifie chaque entité managed sans appeler save()
     * → Hibernate détecte les changements et les flush en fin de transaction.
     */
    public void transferProducts(Long fromCategoryId, Long toCategoryId) {
        Category from = categoryRepository.findById(fromCategoryId).orElseThrow(() -> new IllegalArgumentException("Catégorie source non trouvée avec l'ID: " + fromCategoryId));
        Category to = categoryRepository.findById(toCategoryId).orElseThrow(() -> new IllegalArgumentException("Catégorie destination non trouvée avec l'ID: " + toCategoryId));

        List<Product> products = productRepository.findByCategory(from);
        products.forEach(p -> p.setCategory(to));
        // Pas besoin de saveAll() : dirty checking flush automatiquement les changements.
    }

    // -------------------------------------------------------------------------
    // Agrégations et statistiques (Partie 6)
    // -------------------------------------------------------------------------

    /** Nombre de produits par catégorie. */
    @Transactional(readOnly = true)
    public List<Object[]> getProductCountByCategory() {
        return productRepository.countByCategory();
    }

    /** Prix moyen par catégorie. */
    @Transactional(readOnly = true)
    public List<Object[]> getAveragePriceByCategory() {
        return productRepository.averagePriceByCategory();
    }

    /**
     * Top N produits les plus chers.
     * PageRequest.of(0, limit) = page 0 avec `limit` éléments → équivalent LIMIT en SQL.
     */
    @Transactional(readOnly = true)
    public List<Product> getTopExpensiveProducts(int limit) {
        if (limit <= 0) throw new IllegalArgumentException("La limite doit être supérieure à 0");
        return productRepository.findTopExpensive(PageRequest.of(0, limit));
    }

    /** Statistiques par catégorie via projection DTO SELECT NEW (Partie 6.3). */
    @Transactional(readOnly = true)
    public List<CategoryStats> getCategoryStats() {
        return productRepository.getCategoryStats();
    }

    /** Produits jamais commandés (Partie 6.2). */
    @Transactional(readOnly = true)
    public List<Product> getNeverOrderedProducts() {
        return productRepository.findNeverOrderedProducts();
    }

    /** Nombre total de produits. */
    @Transactional(readOnly = true)
    public long countProducts() {
        return productRepository.count();
    }

    // -------------------------------------------------------------------------
    // Validation
    // -------------------------------------------------------------------------

    private void validateProduct(Product product) {
        validateProductForUpdate(product);
        if (product.getCategory() == null) {
            throw new IllegalArgumentException("La catégorie est obligatoire");
        }
    }

    private void validateProductForUpdate(Product product) {
        if (product == null) throw new IllegalArgumentException("Le produit ne peut pas être null");
        if (product.getName() == null || product.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom du produit est obligatoire");
        }
        if (product.getPrice() == null) throw new IllegalArgumentException("Le prix est obligatoire");
        if (product.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le prix doit être supérieur à zéro");
        }
        if (product.getStock() < 0) {
            throw new IllegalArgumentException("Le stock ne peut pas être négatif");
        }
    }
}
