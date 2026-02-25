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

import com.formation.springproducts.model.Supplier;
import com.formation.springproducts.service.SupplierService;

/**
 * SupplierController — Couche Présentation REST pour les fournisseurs (TP2)
 *
 * Endpoints :
 *   GET    /api/suppliers         → liste tous les fournisseurs
 *   GET    /api/suppliers/{id}    → récupère un fournisseur par id
 *   POST   /api/suppliers         → crée un fournisseur
 *   PUT    /api/suppliers/{id}    → met à jour un fournisseur
 *   DELETE /api/suppliers/{id}    → supprime un fournisseur (délie ses produits)
 *   GET    /api/suppliers/count   → nombre total de fournisseurs
 *
 * Tous les endpoints délèguent au SupplierService (couche application).
 * Le contrôleur ne contient AUCUNE logique métier.
 */
@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    // =========================================================================
    // Lecture
    // =========================================================================

    /**
     * GET /api/suppliers
     *
     * Liste tous les fournisseurs, triés par nom alphabétique.
     * Les produits liés ne sont PAS inclus (LAZY, @JsonIgnore sur Supplier.products).
     *
     * @return 200 OK avec la liste des fournisseurs
     */
    @GetMapping
    public ResponseEntity<List<Supplier>> getAllSuppliers() {
        return ResponseEntity.ok(supplierService.getAllSuppliers());
    }

    /**
     * GET /api/suppliers/{id}
     *
     * Récupère un fournisseur par son id.
     *
     * @param id identifiant Long du fournisseur
     * @return 200 OK avec le fournisseur, ou 404 NOT FOUND
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getSupplier(@PathVariable Long id) {
        return supplierService.getSupplier(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorMessage("Fournisseur non trouvé avec l'ID: " + id)));
    }

    /**
     * GET /api/suppliers/count
     *
     * Nombre total de fournisseurs.
     *
     * @return 200 OK avec le count
     */
    @GetMapping("/count")
    public ResponseEntity<CountResponse> countSuppliers() {
        return ResponseEntity.ok(new CountResponse(supplierService.countSuppliers()));
    }

    // =========================================================================
    // Création / Mise à jour / Suppression
    // =========================================================================

    /**
     * POST /api/suppliers
     *
     * Crée un nouveau fournisseur.
     *
     * Corps JSON attendu :
     * {
     *   "name": "Tech Distribution SA",
     *   "email": "contact@techdist.fr",
     *   "phone": "+33123456789"
     * }
     *
     * @return 201 CREATED avec le fournisseur créé et le header Location,
     *         ou 400 BAD REQUEST si le nom est vide ou l'email déjà utilisé
     */
    @PostMapping
    public ResponseEntity<?> createSupplier(@RequestBody SupplierRequest request) {
        try {
            Supplier created = supplierService.createSupplier(
                    request.getName(),
                    request.getEmail(),
                    request.getPhone()
            );
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
     * PUT /api/suppliers/{id}
     *
     * Met à jour un fournisseur existant.
     *
     * Corps JSON attendu :
     * {
     *   "name": "Nouveau nom",
     *   "email": "nouvel@email.fr",
     *   "phone": "+33987654321"
     * }
     *
     * @param id identifiant Long du fournisseur
     * @return 200 OK avec le fournisseur mis à jour,
     *         404 NOT FOUND si le fournisseur n'existe pas,
     *         400 BAD REQUEST si données invalides
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateSupplier(@PathVariable Long id, @RequestBody SupplierRequest request) {
        try {
            Supplier updated = supplierService.updateSupplier(
                    id,
                    request.getName(),
                    request.getEmail(),
                    request.getPhone()
            );
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("non trouvé")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorMessage(msg));
            }
            return ResponseEntity.badRequest().body(new ErrorMessage(msg));
        }
    }

    /**
     * DELETE /api/suppliers/{id}
     *
     * Supprime un fournisseur.
     *
     * Note : la suppression d'un fournisseur NE supprime PAS ses produits.
     * Le service délie automatiquement tous les produits du fournisseur
     * (product.setSupplier(null)) avant la suppression pour éviter une
     * violation de contrainte de clé étrangère.
     * Les produits restent en base sans fournisseur assigné.
     *
     * Tout se passe dans UNE seule transaction :
     * si la suppression échoue → rollback, les produits restent liés au fournisseur.
     *
     * @param id identifiant Long du fournisseur
     * @return 204 NO CONTENT si supprimé avec succès, ou 404 NOT FOUND
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSupplier(@PathVariable Long id) {
        try {
            supplierService.deleteSupplier(id);
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
     * Corps de requête pour créer ou mettre à jour un fournisseur.
     */
    public static class SupplierRequest {

        private String name;
        private String email;
        private String phone;

        public SupplierRequest() {}

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
