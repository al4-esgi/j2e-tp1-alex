package com.formation.springproducts.controller;

import com.formation.springproducts.model.Product;
import com.formation.springproducts.service.ProductService;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * ProductController - Couche Presentation (REST API)
 * Expose les endpoints REST pour la gestion des produits
 * Délègue la logique métier au ProductService
 *
 * @RestController : Combine @Controller et @ResponseBody
 *                   Toutes les méthodes retournent directement des données JSON
 * @RequestMapping : Définit le chemin de base pour tous les endpoints de ce contrôleur
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    /**
     * Constructeur avec injection de dépendances
     * Spring injecte automatiquement ProductService
     * @param productService le service à injecter
     */
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * GET /api/products - Liste tous les produits ou filtre par catégorie
     * @param category paramètre optionnel pour filtrer par catégorie
     * @return ResponseEntity avec la liste des produits (200 OK)
     */
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts(@RequestParam(required = false) String category) {
        List<Product> products;

        if (category != null && !category.trim().isEmpty()) {
            products = productService.getProductsByCategory(category);
        } else {
            products = productService.getAllProducts();
        }

        return ResponseEntity.ok(products);
    }

    /**
     * GET /api/products/{id} - Récupère un produit par son ID
     * @param id l'identifiant du produit
     * @return ResponseEntity avec le produit (200 OK) ou erreur (404 NOT FOUND)
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getProduct(@PathVariable String id) {
        return productService
            .getProduct(id)
            .<ResponseEntity<?>>map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorMessage("Produit non trouvé avec l'ID: " + id)));
    }

    /**
     * POST /api/products - Crée un nouveau produit
     * @param product le produit à créer
     * @return ResponseEntity avec le produit créé (201 CREATED) et header Location
     */
    @PostMapping
    public ResponseEntity<?> createProduct(@RequestBody Product product) {
        try {
            Product created = productService.createProduct(product);

            // Construire l'URI du produit créé
            URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.getId()).toUri();

            return ResponseEntity.created(location).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorMessage(e.getMessage()));
        }
    }

    /**
     * PUT /api/products/{id} - Met à jour un produit existant
     * @param id l'identifiant du produit
     * @param product les nouvelles données
     * @return ResponseEntity avec le produit mis à jour (200 OK) ou erreur
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable String id, @RequestBody Product product) {
        try {
            Product updated = productService.updateProduct(id, product);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorMessage(e.getMessage()));
        }
    }

    /**
     * DELETE /api/products/{id} - Supprime un produit
     * @param id l'identifiant du produit à supprimer
     * @return ResponseEntity vide (204 NO CONTENT) ou erreur (404 NOT FOUND)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable String id) {
        try {
            productService.deleteProduct(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorMessage(e.getMessage()));
        }
    }

    /**
     * PATCH /api/products/{id}/stock - Ajuste le stock d'un produit
     * @param id l'identifiant du produit
     * @param stockUpdate objet contenant la quantité à ajouter
     * @return ResponseEntity avec le produit mis à jour (200 OK) ou erreur
     */
    @PatchMapping("/{id}/stock")
    public ResponseEntity<?> updateStock(@PathVariable String id, @RequestBody StockUpdate stockUpdate) {
        try {
            Product updated = productService.updateStock(id, stockUpdate.getQuantity());
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorMessage(e.getMessage()));
        }
    }

    /**
     * GET /api/products/count - Compte le nombre total de produits
     * @return ResponseEntity avec le count (200 OK)
     */
    @GetMapping("/count")
    public ResponseEntity<CountResponse> countProducts() {
        long count = productService.countProducts();
        return ResponseEntity.ok(new CountResponse(count));
    }

    /**
     * Classe interne pour les messages d'erreur
     */
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

    /**
     * Classe interne pour les mises à jour de stock
     */
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

    /**
     * Classe interne pour les réponses de comptage
     */
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
}
