package com.formation.springproducts.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.formation.springproducts.model.Category;
import com.formation.springproducts.service.CategoryService;

/**
 * CategoryController — Couche Présentation REST pour les catégories (TP2)
 *
 * Endpoints :
 *   GET    /api/categories              → liste toutes les catégories
 *   GET    /api/categories/{id}         → récupère une catégorie (sans produits)
 *   GET    /api/categories/{id}/products → récupère une catégorie avec ses produits
 *   POST   /api/categories              → crée une catégorie
 *   PUT    /api/categories/{id}         → met à jour une catégorie
 *   DELETE /api/categories/{id}         → supprime une catégorie (+ ses produits en cascade)
 *   GET    /api/categories/count        → nombre total de catégories
 *
 * Tous les endpoints délèguent au CategoryService (couche application).
 * Le contrôleur ne contient AUCUNE logique métier.
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    // =========================================================================
    // Lecture
    // =========================================================================

    /**
     * GET /api/categories
     *
     * Liste toutes les catégories, triées par nom.
     * Les produits ne sont PAS inclus dans la réponse (LAZY, @JsonIgnore).
     * Utiliser GET /api/categories/{id}/products pour obtenir les produits d'une catégorie.
     *
     * @return 200 OK avec la liste des catégories
     */
    @GetMapping
    public ResponseEntity<List<Category>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    /**
     * GET /api/categories/{id}
     *
     * Récupère une catégorie par son id, SANS ses produits.
     *
     * @param id identifiant Long de la catégorie
     * @return 200 OK avec la catégorie, ou 404 NOT FOUND
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getCategory(@PathVariable Long id) {
        return categoryService.getCategory(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorMessage("Catégorie non trouvée avec l'ID: " + id)));
    }

    /**
     * GET /api/categories/{id}/products
     *
     * Récupère une catégorie avec la liste complète de ses produits chargée.
     *
     * Utilise JOIN FETCH en interne (findByIdWithProducts) :
     * → Une seule requête SQL au lieu de 1 + N.
     * → Les produits sont inclus dans la réponse JSON.
     *
     * Note : Category.products est annoté @JsonIgnore sur l'entité pour éviter
     * la récursion infinie lors de la sérialisation standard. Ici on retourne
     * explicitement la catégorie chargée — les produits ne contiennent pas
     * de référence retour vers la catégorie dans le JSON (Product.category
     * est sérialisé mais sans ses propres products grâce à @JsonIgnore).
     *
     * @param id identifiant Long de la catégorie
     * @return 200 OK avec la catégorie + liste des produits, ou 404 NOT FOUND
     */
    @GetMapping("/{id}/products")
    public ResponseEntity<?> getCategoryWithProducts(@PathVariable Long id) {
        try {
            Category category = categoryService.getCategoryWithProducts(id);
            // Temporairement exposer les produits pour cet endpoint en les ajoutant
            // dans une réponse enveloppée
            CategoryWithProductsResponse response = new CategoryWithProductsResponse(category);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorMessage(e.getMessage()));
        }
    }

    /**
     * GET /api/categories/count
     *
     * Nombre total de catégories.
     *
     * @return 200 OK avec le count
     */
    @GetMapping("/count")
    public ResponseEntity<CountResponse> countCategories() {
        return ResponseEntity.ok(new CountResponse(categoryService.countCategories()));
    }

    // =========================================================================
    // Création / Mise à jour / Suppression
    // =========================================================================

    /**
     * POST /api/categories
     *
     * Crée une nouvelle catégorie.
     *
     * Corps JSON attendu :
     * {
     *   "name": "Informatique",
     *   "description": "Matériel et accessoires informatiques"
     * }
     *
     * @return 201 CREATED avec la catégorie créée et le header Location,
     *         ou 400 BAD REQUEST si le nom est vide / déjà utilisé
     */
    @PostMapping
    public ResponseEntity<?> createCategory(@RequestBody CategoryRequest request) {
        try {
            Category created = categoryService.createCategory(request.getName(), request.getDescription());
            URI location = ServletUriComponentsBuilder
                    .fromCurrentRequest()
                    .path("/{id}")
                    .buildAndExpand(created.getId())
                    .toUri();
            return ResponseEntity.created(location).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorMessage(e.getMessage()));
        }
    }

    /**
     * PUT /api/categories/{id}
     *
     * Met à jour une catégorie existante.
     *
     * Corps JSON attendu :
     * {
     *   "name": "Nouveau nom",
     *   "description": "Nouvelle description"
     * }
     *
     * @param id identifiant Long de la catégorie
     * @return 200 OK avec la catégorie mise à jour,
     *         404 NOT FOUND si la catégorie n'existe pas,
     *         400 BAD REQUEST si données invalides
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCategory(@PathVariable Long id, @RequestBody CategoryRequest request) {
        try {
            Category updated = categoryService.updateCategory(id, request.getName(), request.getDescription());
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("non trouvée")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorMessage(msg));
            }
            return ResponseEntity.badRequest().body(new ErrorMessage(msg));
        }
    }

    /**
     * DELETE /api/categories/{id}
     *
     * Supprime une catégorie ET tous ses produits associés.
     *
     * ATTENTION : opération destructive en cascade.
     * Grâce à cascade = CascadeType.ALL + orphanRemoval = true sur Category.products,
     * tous les produits de cette catégorie seront supprimés de la base.
     *
     * @param id identifiant Long de la catégorie
     * @return 204 NO CONTENT si supprimée avec succès, ou 404 NOT FOUND
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        try {
            categoryService.deleteCategory(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorMessage(e.getMessage()));
        }
    }

    // =========================================================================
    // Classes internes de requête / réponse
    // =========================================================================

    /**
     * Corps de requête pour créer ou mettre à jour une catégorie.
     */
    public static class CategoryRequest {

        private String name;
        private String description;

        public CategoryRequest() {}

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
    }

    /**
     * Réponse enveloppée pour GET /api/categories/{id}/products.
     * Inclut les informations de la catégorie ET la liste de ses produits.
     * Contourne le @JsonIgnore sur Category.products pour cet endpoint spécifique.
     */
    public static class CategoryWithProductsResponse {

        private Long id;
        private String name;
        private String description;
        private List<com.formation.springproducts.model.Product> products;

        public CategoryWithProductsResponse() {}

        public CategoryWithProductsResponse(Category category) {
            this.id = category.getId();
            this.name = category.getName();
            this.description = category.getDescription();
            this.products = category.getProducts();
        }

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

        public List<com.formation.springproducts.model.Product> getProducts() {
            return products;
        }

        public void setProducts(List<com.formation.springproducts.model.Product> products) {
            this.products = products;
        }
    }

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
