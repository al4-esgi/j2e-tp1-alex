package com.formation.springproducts.controller;

import com.formation.springproducts.dto.CategoryStats;
import com.formation.springproducts.exception.ProductNotFoundException;
import com.formation.springproducts.model.Product;
import com.formation.springproducts.service.ProductService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * ProductController — Couche Présentation REST (TP2)
 *
 * Changements par rapport au TP1 :
 * - id : String → Long dans les @PathVariable
 * - GET /api/products/slow  : liste sans JOIN FETCH  (démo problème N+1)
 * - GET /api/products/fast  : liste avec JOIN FETCH   (version optimisée)
 * - GET /api/products       : version optimisée par défaut
 * - Nouveaux endpoints : price-range, search, stats, top, never-ordered
 *
 * Démonstration N+1 (Partie 7.1) :
 *   /api/products/slow → observe les logs Hibernate : 1 requête products + N requêtes category
 *   /api/products/fast → 1 seule requête SQL avec JOIN FETCH
 *
 * Tous les endpoints délèguent au ProductService (couche application).
 * Le contrôleur ne contient AUCUNE logique métier.
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // =========================================================================
    // CRUD de base
    // =========================================================================

    /**
     * GET /api/products
     *
     * Liste les produits avec support de la pagination et des filtres.
     *
     * Pagination (activée quand aucun filtre n'est passé) :
     *   ?page=0&size=10  → première page de 10 produits
     *   ?page=1&size=5   → deuxième page de 5 produits
     *
     * Filtres (retournent une liste complète, sans pagination) :
     *   ?categoryId=1    → produits d'une catégorie
     *   ?supplierId=2    → produits d'un fournisseur
     *   ?search=laptop   → recherche par mot-clé dans le nom
     *   ?minPrice=10&maxPrice=100 → filtre par fourchette de prix
     *
     * @param categoryId  (optionnel) filtre par catégorie
     * @param supplierId  (optionnel) filtre par fournisseur
     * @param search      (optionnel) mot-clé dans le nom
     * @param minPrice    (optionnel) prix minimum
     * @param maxPrice    (optionnel) prix maximum
     * @param page        numéro de page (0-based, défaut : 0)
     * @param size        taille de page (défaut : 10, max : 100)
     */
    @GetMapping
    public ResponseEntity<?> getAllProducts(
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) Long supplierId,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        try {
            // Filtres → liste complète (la pagination n'a pas de sens sur un sous-ensemble filtré sans countQuery dédiée)
            if (categoryId != null) {
                return ResponseEntity.ok(productService.getProductsByCategory(categoryId));
            } else if (supplierId != null) {
                return ResponseEntity.ok(productService.getProductsBySupplier(supplierId));
            } else if (search != null && !search.trim().isEmpty()) {
                return ResponseEntity.ok(productService.searchProducts(search));
            } else if (minPrice != null && maxPrice != null) {
                return ResponseEntity.ok(productService.getProductsByPriceRange(minPrice, maxPrice));
            }

            // Pas de filtre → pagination
            Page<Product> pageResult = productService.getProductsPaginated(page, size);
            return ResponseEntity.ok(pageResult);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorMessage(e.getMessage()));
        }
    }

    /**
     * GET /api/products/slow
     *
     * Liste tous les produits SANS JOIN FETCH.
     * Démo du problème N+1 (Partie 7.1) :
     * Avec N produits et spring.jpa.open-in-view=false, accéder à chaque
     * product.getCategory() déclenche N requêtes SQL supplémentaires.
     *
     * Observe les logs Hibernate : tu verras 1 + N requêtes SQL.
     * Comparer avec /api/products/fast pour voir la différence.
     */
    @GetMapping("/slow")
    public ResponseEntity<List<Product>> getProductsSlow() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    /**
     * GET /api/products/fast
     *
     * Liste tous les produits AVEC JOIN FETCH sur category et supplier.
     * Optimisation N+1 (Partie 7.1) : une seule requête SQL avec JOIN.
     *
     * Observe les logs Hibernate : tu verras 1 seule requête SQL avec JOIN.
     */
    @GetMapping("/fast")
    public ResponseEntity<List<Product>> getProductsFast() {
        return ResponseEntity.ok(productService.getAllProductsOptimized());
    }

    /**
     * GET /api/products/{id}
     *
     * Récupère un produit par son id.
     * Utilise le @NamedEntityGraph "Product.full" pour charger category + supplier.
     *
     * @param id identifiant Long du produit
     * @return 200 OK avec le produit, ou 404 NOT FOUND
     */
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable Long id) {
        return productService
            .getProduct(id)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new ProductNotFoundException(id));
    }

    /**
     * POST /api/products
     *
     * Crée un nouveau produit.
     *
     * Corps JSON attendu :
     * {
     *   "name": "...",
     *   "description": "...",
     *   "price": 99.99,
     *   "stock": 10,
     *   "category": { "id": 1 },
     *   "supplier": { "id": 1 }   (optionnel)
     * }
     *
     * Utiliser POST /api/products/with-category pour créer avec une catégorie
     * par nom (cherche ou crée la catégorie automatiquement).
     *
     * @return 201 CREATED avec le produit créé et le header Location
     */
    @PostMapping
    public ResponseEntity<Product> createProduct(@Valid @RequestBody Product product) {
        Product created = productService.createProduct(product);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    /**
     * POST /api/products/with-category
     *
     * Crée un produit en cherchant ou créant sa catégorie par nom.
     * Tout dans une seule transaction (Partie 4.1).
     *
     * Corps JSON attendu :
     * {
     *   "product": {
     *     "name": "...",
     *     "price": 99.99,
     *     "stock": 10
     *   },
     *   "categoryName": "Informatique",
     *   "supplierId": 1   (optionnel)
     * }
     *
     * @return 201 CREATED avec le produit créé
     */
    @PostMapping("/with-category")
    public ResponseEntity<Product> createProductWithCategory(@RequestBody CreateWithCategoryRequest request) {
        Product created = productService.createProductWithCategory(request.getProduct(), request.getCategoryName(), request.getSupplierId());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().replacePath("/api/products/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    /**
     * PUT /api/products/{id}
     *
     * Met à jour un produit existant.
     *
     * Corps JSON attendu :
     * {
     *   "name": "...",
     *   "description": "...",
     *   "price": 99.99,
     *   "stock": 10,
     *   "category": { "id": 1 },   (optionnel)
     *   "supplier": { "id": 1 }    (optionnel)
     * }
     *
     * @param id identifiant Long du produit
     * @return 200 OK avec le produit mis à jour, ou 404 NOT FOUND
     */
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @Valid @RequestBody Product product) {
        Product updated = productService.updateProduct(id, product);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/products/{id}
     *
     * Supprime un produit.
     *
     * @param id identifiant Long du produit
     * @return 204 NO CONTENT, ou 404 NOT FOUND
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // Stock (Partie 4.1)
    // =========================================================================

    /**
     * PATCH /api/products/{id}/stock
     *
     * Ajuste le stock d'un produit (ajouter ou retirer des unités).
     *
     * Corps JSON :
     * { "quantity": 10 }   → ajoute 10 au stock
     * { "quantity": -5 }   → retire 5 du stock
     *
     * @param id identifiant Long du produit
     * @return 200 OK avec le produit mis à jour
     */
    @PatchMapping("/{id}/stock")
    public ResponseEntity<Product> updateStock(@PathVariable Long id, @RequestBody StockUpdate stockUpdate) {
        Product updated = productService.updateStock(id, stockUpdate.getQuantity());
        return ResponseEntity.ok(updated);
    }

    /**
     * PATCH /api/products/{id}/stock/decrease
     *
     * Diminue le stock d'un produit d'une quantité donnée.
     * Lève InsufficientStockException (422) si le stock est insuffisant.
     *
     * Corps JSON :
     * { "quantity": 5 }  → retire 5 unités du stock
     *
     * @param id identifiant Long du produit
     * @return 200 OK avec le produit mis à jour
     */
    @PatchMapping("/{id}/stock/decrease")
    public ResponseEntity<Product> decreaseStock(@PathVariable Long id, @RequestBody StockUpdate stockUpdate) {
        productService.decreaseStock(id, stockUpdate.getQuantity());
        // Rechargement du produit pour retourner le stock mis à jour
        return productService
            .getProduct(id)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new ProductNotFoundException(id));
    }

    // =========================================================================
    // Agrégations et statistiques (Partie 6)
    // =========================================================================

    /**
     * GET /api/products/stats/by-category
     *
     * Nombre de produits par catégorie.
     * Retourne des paires { categoryName, count } sous forme de listes.
     *
     * JPQL : SELECT p.category.name, COUNT(p) FROM Product p GROUP BY p.category.name
     */
    @GetMapping("/stats/by-category")
    public ResponseEntity<List<Object[]>> countByCategory() {
        return ResponseEntity.ok(productService.getProductCountByCategory());
    }

    /**
     * GET /api/products/stats/avg-price
     *
     * Prix moyen par catégorie.
     * Retourne des paires { categoryName, avgPrice }.
     *
     * JPQL : SELECT p.category.name, AVG(p.price) FROM Product p GROUP BY p.category.name
     */
    @GetMapping("/stats/avg-price")
    public ResponseEntity<List<Object[]>> avgPriceByCategory() {
        return ResponseEntity.ok(productService.getAveragePriceByCategory());
    }

    /**
     * GET /api/products/stats/category-stats
     *
     * Statistiques complètes par catégorie via projection DTO (Partie 6.3).
     * Retourne des CategoryStats typés : { categoryName, productCount, averagePrice }.
     *
     * JPQL : SELECT NEW com.formation.springproducts.dto.CategoryStats(...)
     */
    @GetMapping("/stats/category-stats")
    public ResponseEntity<List<CategoryStats>> getCategoryStats() {
        return ResponseEntity.ok(productService.getCategoryStats());
    }

    /**
     * GET /api/products/top?limit=10
     *
     * Top N produits les plus chers.
     *
     * @param limit nombre de résultats (défaut : 10)
     */
    @GetMapping("/top")
    public ResponseEntity<?> getTopExpensive(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(productService.getTopExpensiveProducts(limit));
    }

    /**
     * GET /api/products/never-ordered
     *
     * Produits qui n'ont jamais été commandés (Partie 6.2).
     *
     * JPQL : WHERE p NOT IN (SELECT oi.product FROM OrderItem oi)
     */
    @GetMapping("/never-ordered")
    public ResponseEntity<List<Product>> getNeverOrderedProducts() {
        return ResponseEntity.ok(productService.getNeverOrderedProducts());
    }

    /**
     * POST /api/products/transfer
     *
     * Transfère tous les produits d'une catégorie vers une autre (Partie 4.1).
     * Transaction atomique : tout ou rien.
     *
     * Corps JSON :
     * { "fromCategoryId": 1, "toCategoryId": 2 }
     */
    @PostMapping("/transfer")
    public ResponseEntity<?> transferProducts(@RequestBody TransferRequest request) {
        productService.transferProducts(request.getFromCategoryId(), request.getToCategoryId());
        return ResponseEntity.ok(new ErrorMessage("Produits transférés avec succès de la catégorie " + request.getFromCategoryId() + " vers " + request.getToCategoryId()));
    }

    /**
     * GET /api/products/count
     *
     * Nombre total de produits.
     */
    @GetMapping("/count")
    public ResponseEntity<CountResponse> countProducts() {
        return ResponseEntity.ok(new CountResponse(productService.countProducts()));
    }

    // =========================================================================
    // Classes internes de requête / réponse
    // =========================================================================

    public static class ErrorMessage {

        private String message;

        public ErrorMessage() {}

        public ErrorMessage(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public static class StockUpdate {

        private int quantity;

        public StockUpdate() {}

        public StockUpdate(int quantity) {
            this.quantity = quantity;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }
    }

    public static class CountResponse {

        private long count;

        public CountResponse() {}

        public CountResponse(long count) {
            this.count = count;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }

    public static class CreateWithCategoryRequest {

        private Product product;
        private String categoryName;
        private Long supplierId;

        public CreateWithCategoryRequest() {}

        public Product getProduct() {
            return product;
        }

        public void setProduct(Product product) {
            this.product = product;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public void setCategoryName(String categoryName) {
            this.categoryName = categoryName;
        }

        public Long getSupplierId() {
            return supplierId;
        }

        public void setSupplierId(Long supplierId) {
            this.supplierId = supplierId;
        }
    }

    public static class TransferRequest {

        private Long fromCategoryId;
        private Long toCategoryId;

        public TransferRequest() {}

        public Long getFromCategoryId() {
            return fromCategoryId;
        }

        public void setFromCategoryId(Long fromCategoryId) {
            this.fromCategoryId = fromCategoryId;
        }

        public Long getToCategoryId() {
            return toCategoryId;
        }

        public void setToCategoryId(Long toCategoryId) {
            this.toCategoryId = toCategoryId;
        }
    }
}
