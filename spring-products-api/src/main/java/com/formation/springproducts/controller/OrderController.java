package com.formation.springproducts.controller;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.formation.springproducts.model.Order;
import com.formation.springproducts.model.OrderStatus;
import com.formation.springproducts.service.OrderService;

/**
 * OrderController — Couche Présentation REST pour les commandes (TP2 Partie 5)
 *
 * Endpoints :
 *   GET    /api/orders                    → liste toutes les commandes (résumés)
 *   GET    /api/orders/full               → liste avec items chargés (JOIN FETCH)
 *   GET    /api/orders/{id}               → récupère une commande (sans items)
 *   GET    /api/orders/{id}/items         → récupère une commande avec ses items
 *   POST   /api/orders                    → crée une commande complète
 *   PATCH  /api/orders/{id}/status        → met à jour le statut
 *   DELETE /api/orders/{id}               → supprime une commande (+ items en cascade)
 *   GET    /api/orders/count              → nombre total de commandes
 *   GET    /api/orders/stats/revenue      → chiffre d'affaires total (DELIVERED)
 *   GET    /api/orders/stats/by-status    → nombre de commandes par statut
 *   GET    /api/orders/stats/top-products → produits les plus commandés
 *
 * Tous les endpoints délèguent au OrderService (couche application).
 * Le contrôleur ne contient AUCUNE logique métier.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // =========================================================================
    // Lecture
    // =========================================================================

    /**
     * GET /api/orders
     *
     * Liste toutes les commandes, SANS charger les items (résumés).
     * Triées par date de commande décroissante.
     *
     * Utiliser GET /api/orders/full si les items sont nécessaires.
     *
     * @param email  (optionnel) filtre par email client
     * @param status (optionnel) filtre par statut (PENDING, CONFIRMED, SHIPPED, DELIVERED)
     * @return 200 OK avec la liste des commandes
     */
    @GetMapping
    public ResponseEntity<?> getAllOrders(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) OrderStatus status) {
        try {
            List<Order> orders;
            if (email != null && !email.trim().isEmpty()) {
                orders = orderService.getOrdersByCustomerEmail(email);
            } else if (status != null) {
                orders = orderService.getOrdersByStatus(status);
            } else {
                orders = orderService.getAllOrders();
            }
            return ResponseEntity.ok(orders);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorMessage(e.getMessage()));
        }
    }

    /**
     * GET /api/orders/full
     *
     * Liste toutes les commandes avec leurs items et produits chargés.
     *
     * Utilise un double JOIN FETCH (Order → items → product) :
     * → Une seule requête SQL au lieu de 1 + N + M requêtes.
     *
     * À préférer pour les endpoints qui sérialisent les commandes complètes.
     * Pour les listes de résumé, utiliser GET /api/orders (plus léger).
     *
     * @return 200 OK avec la liste complète des commandes + items
     */
    @GetMapping("/full")
    public ResponseEntity<List<Order>> getAllOrdersWithItems() {
        return ResponseEntity.ok(orderService.getAllOrdersWithItems());
    }

    /**
     * GET /api/orders/{id}
     *
     * Récupère une commande par son id, SANS ses items (chargement LAZY).
     * Retourne uniquement les informations de base de la commande.
     *
     * Pour récupérer les items, utiliser GET /api/orders/{id}/items.
     *
     * @param id identifiant Long de la commande
     * @return 200 OK avec la commande, ou 404 NOT FOUND
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrder(@PathVariable Long id) {
        return orderService.getOrder(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorMessage("Commande non trouvée avec l'ID: " + id)));
    }

    /**
     * GET /api/orders/{id}/items
     *
     * Récupère une commande avec tous ses items et les produits associés.
     *
     * Utilise un double JOIN FETCH :
     * Order → items → product (+ category du product)
     * → Une seule requête SQL.
     *
     * @param id identifiant Long de la commande
     * @return 200 OK avec la commande complète, ou 404 NOT FOUND
     */
    @GetMapping("/{id}/items")
    public ResponseEntity<?> getOrderWithItems(@PathVariable Long id) {
        try {
            Order order = orderService.getOrderWithItems(id);
            return ResponseEntity.ok(order);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorMessage(e.getMessage()));
        }
    }

    /**
     * GET /api/orders/count
     *
     * Nombre total de commandes.
     *
     * @return 200 OK avec le count
     */
    @GetMapping("/count")
    public ResponseEntity<CountResponse> countOrders() {
        return ResponseEntity.ok(new CountResponse(orderService.countOrders()));
    }

    // =========================================================================
    // Création (Partie 5.3)
    // =========================================================================

    /**
     * POST /api/orders
     *
     * Crée une commande complète avec tous ses items.
     *
     * Corps JSON attendu :
     * {
     *   "customerName": "Jean Dupont",
     *   "customerEmail": "jean.dupont@email.fr",
     *   "productsAndQuantities": {
     *     "1": 2,
     *     "3": 1,
     *     "5": 3
     *   }
     * }
     *
     * Comportement transactionnel (Partie 4) :
     * - Tout se passe dans une seule transaction
     * - Si un produit n'existe pas → 400 BAD REQUEST + rollback complet
     * - Si le stock est insuffisant → 400 BAD REQUEST + rollback complet
     * - Si tout réussit → la commande + les items sont créés, les stocks décrémentés
     *
     * Le @PrePersist sur Order génère automatiquement :
     * - orderNumber : "ORD-YYYYMMDD-XXXXXXXX"
     * - orderDate : timestamp de la création
     * - status : PENDING
     *
     * @return 201 CREATED avec la commande créée et le header Location,
     *         ou 400 BAD REQUEST si données invalides / stock insuffisant
     */
    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody CreateOrderRequest request) {
        try {
            Order created = orderService.createOrder(
                    request.getCustomerName(),
                    request.getCustomerEmail(),
                    request.getProductsAndQuantities()
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

    // =========================================================================
    // Mise à jour du statut (Partie 5.3)
    // =========================================================================

    /**
     * PATCH /api/orders/{id}/status
     *
     * Met à jour le statut d'une commande.
     *
     * Corps JSON attendu :
     * { "status": "CONFIRMED" }
     *
     * Cycle de vie du statut (Partie 5.1) :
     * PENDING → CONFIRMED → SHIPPED → DELIVERED
     *
     * Les transitions inversées sont rejetées :
     * ex. DELIVERED → PENDING → 400 BAD REQUEST
     *
     * @param id identifiant Long de la commande
     * @return 200 OK avec la commande mise à jour,
     *         404 NOT FOUND si la commande n'existe pas,
     *         400 BAD REQUEST si la transition est invalide
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody StatusUpdateRequest request) {
        try {
            Order updated = orderService.updateOrderStatus(id, request.getStatus());
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("non trouvée")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorMessage(msg));
            }
            return ResponseEntity.badRequest().body(new ErrorMessage(msg));
        }
    }

    // =========================================================================
    // Suppression
    // =========================================================================

    /**
     * DELETE /api/orders/{id}
     *
     * Supprime une commande et tous ses items.
     *
     * Grâce à cascade = ALL + orphanRemoval = true sur Order.items,
     * tous les OrderItem sont automatiquement supprimés avec la commande.
     *
     * Note : les stocks des produits commandés ne sont PAS recrédités.
     *
     * @param id identifiant Long de la commande
     * @return 204 NO CONTENT si supprimée avec succès, ou 404 NOT FOUND
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOrder(@PathVariable Long id) {
        try {
            orderService.deleteOrder(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorMessage(e.getMessage()));
        }
    }

    // =========================================================================
    // Agrégations et statistiques (Partie 6.1)
    // =========================================================================

    /**
     * GET /api/orders/stats/revenue
     *
     * Chiffre d'affaires total sur les commandes DELIVERED.
     *
     * JPQL : SELECT SUM(o.totalAmount) FROM Order o WHERE o.status = 'DELIVERED'
     *
     * Retourne 0.00 si aucune commande livrée n'existe.
     *
     * @return 200 OK avec { "totalRevenue": 12345.67 }
     */
    @GetMapping("/stats/revenue")
    public ResponseEntity<RevenueResponse> getTotalRevenue() {
        return ResponseEntity.ok(new RevenueResponse(orderService.getTotalRevenue()));
    }

    /**
     * GET /api/orders/stats/by-status
     *
     * Nombre de commandes par statut.
     *
     * JPQL : SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status
     *
     * Retourne des paires { status, count } sous forme de tableau.
     *
     * @return 200 OK avec la liste des statistiques par statut
     */
    @GetMapping("/stats/by-status")
    public ResponseEntity<List<Object[]>> countByStatus() {
        return ResponseEntity.ok(orderService.getOrderCountByStatus());
    }

    /**
     * GET /api/orders/stats/top-products?limit=5
     *
     * Produits les plus commandés (en quantité totale commandée).
     *
     * JPQL : SELECT oi.product.name, SUM(oi.quantity) FROM OrderItem oi
     *        GROUP BY oi.product.name ORDER BY SUM(oi.quantity) DESC
     *
     * @param limit nombre maximum de résultats (défaut : 5)
     * @return 200 OK avec la liste des produits les plus commandés
     */
    @GetMapping("/stats/top-products")
    public ResponseEntity<?> getMostOrderedProducts(
            @RequestParam(defaultValue = "5") int limit) {
        try {
            return ResponseEntity.ok(orderService.getMostOrderedProducts(limit));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorMessage(e.getMessage()));
        }
    }

    // =========================================================================
    // Classes internes de requête / réponse
    // =========================================================================

    /**
     * Corps de requête pour créer une commande.
     */
    public static class CreateOrderRequest {

        private String customerName;
        private String customerEmail;

        /**
         * Map<productId, quantity> :
         * les clés sont les ids des produits à commander,
         * les valeurs sont les quantités souhaitées.
         *
         * Exemple JSON : { "1": 2, "3": 1, "5": 3 }
         */
        private Map<Long, Integer> productsAndQuantities;

        public CreateOrderRequest() {}

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

        public Map<Long, Integer> getProductsAndQuantities() {
            return productsAndQuantities;
        }

        public void setProductsAndQuantities(Map<Long, Integer> productsAndQuantities) {
            this.productsAndQuantities = productsAndQuantities;
        }
    }

    /**
     * Corps de requête pour mettre à jour le statut d'une commande.
     */
    public static class StatusUpdateRequest {

        private OrderStatus status;

        public StatusUpdateRequest() {}

        public OrderStatus getStatus() {
            return status;
        }

        public void setStatus(OrderStatus status) {
            this.status = status;
        }
    }

    /**
     * Réponse pour le chiffre d'affaires total.
     */
    public static class RevenueResponse {

        private java.math.BigDecimal totalRevenue;

        public RevenueResponse() {}

        public RevenueResponse(java.math.BigDecimal totalRevenue) {
            this.totalRevenue = totalRevenue;
        }

        public java.math.BigDecimal getTotalRevenue() {
            return totalRevenue;
        }

        public void setTotalRevenue(java.math.BigDecimal totalRevenue) {
            this.totalRevenue = totalRevenue;
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
