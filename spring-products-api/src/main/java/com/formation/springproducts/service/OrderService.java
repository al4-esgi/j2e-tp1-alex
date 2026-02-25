package com.formation.springproducts.service;

import com.formation.springproducts.model.Order;
import com.formation.springproducts.model.OrderItem;
import com.formation.springproducts.model.OrderStatus;
import com.formation.springproducts.model.Product;
import com.formation.springproducts.repository.OrderRepository;
import com.formation.springproducts.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    // -------------------------------------------------------------------------
    // Création (Partie 5.3)
    // -------------------------------------------------------------------------

    /**
     * Crée une commande complète avec tous ses items.
     *
     * Tout dans UNE seule transaction :
     * - Si un produit n'existe pas → IllegalArgumentException → rollback complet
     * - Si le stock est insuffisant → IllegalArgumentException → rollback complet
     * - Si tout réussit → commande + items créés, stocks décrémentés
     *
     * cascade = ALL sur Order.items : save(order) persiste automatiquement
     * tous les OrderItem présents dans order.getItems().
     *
     * Le @PrePersist sur Order génère automatiquement :
     *   orderNumber, orderDate, status = PENDING
     */
    public Order createOrder(String customerName, String customerEmail, Map<Long, Integer> productsAndQuantities) {
        if (customerName == null || customerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom du client est obligatoire");
        }
        if (productsAndQuantities == null || productsAndQuantities.isEmpty()) {
            throw new IllegalArgumentException("La commande doit contenir au moins un produit");
        }

        Order order = new Order(customerName.trim(), customerEmail);

        for (Map.Entry<Long, Integer> entry : productsAndQuantities.entrySet()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();

            if (quantity == null || quantity <= 0) {
                throw new IllegalArgumentException("La quantité pour le produit ID " + productId + " doit être supérieure à 0");
            }

            Product product = productRepository.findById(productId).orElseThrow(() -> new IllegalArgumentException("Produit non trouvé avec l'ID: " + productId));

            if (product.getStock() < quantity) {
                throw new IllegalArgumentException("Stock insuffisant pour '" + product.getName() + "'. " + "Disponible: " + product.getStock() + ", demandé: " + quantity);
            }

            // Décrémenter le stock — dirty checking flush automatiquement
            product.setStock(product.getStock() - quantity);

            // Snapshot du prix au moment de la commande (immuable)
            OrderItem item = new OrderItem(product, quantity, product.getPrice());
            order.addItem(item);
        }

        order.calculateTotal();

        // save() persiste order + tous les items via cascade = ALL
        return orderRepository.save(order);
    }

    // -------------------------------------------------------------------------
    // Lecture
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Optional<Order> getOrder(Long id) {
        if (id == null || id <= 0) return Optional.empty();
        return orderRepository.findById(id);
    }

    /**
     * Récupère une commande avec ses items et produits chargés (JOIN FETCH).
     * Une seule requête SQL grâce au double JOIN FETCH défini dans OrderRepository.
     */
    @Transactional(readOnly = true)
    public Order getOrderWithItems(Long id) {
        return orderRepository.findByIdWithItems(id).orElseThrow(() -> new IllegalArgumentException("Commande non trouvée avec l'ID: " + id));
    }

    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Order> getAllOrdersWithItems() {
        return orderRepository.findAllWithItems();
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByCustomerEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("L'email du client est obligatoire");
        }
        return orderRepository.findByCustomerEmailWithItems(email.trim());
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByStatus(OrderStatus status) {
        if (status == null) throw new IllegalArgumentException("Le statut est obligatoire");
        return orderRepository.findByStatusOrderByOrderDateDesc(status);
    }

    // -------------------------------------------------------------------------
    // Mise à jour du statut
    // -------------------------------------------------------------------------

    /**
     * Met à jour le statut d'une commande.
     *
     * Transitions autorisées :
     *   PENDING → CONFIRMED → SHIPPED → DELIVERED
     * Toute autre transition lève une exception (rollback si dans une transaction parente).
     */
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus) {
        if (newStatus == null) throw new IllegalArgumentException("Le nouveau statut est obligatoire");

        Order order = orderRepository.findById(orderId).orElseThrow(() -> new IllegalArgumentException("Commande non trouvée avec l'ID: " + orderId));

        validateStatusTransition(order.getStatus(), newStatus);

        order.setStatus(newStatus);
        // Dirty checking suffit, save() explicite pour la clarté
        return orderRepository.save(order);
    }

    private void validateStatusTransition(OrderStatus current, OrderStatus next) {
        if (current == next) return;
        boolean valid = switch (current) {
            case PENDING -> next == OrderStatus.CONFIRMED;
            case CONFIRMED -> next == OrderStatus.SHIPPED;
            case SHIPPED -> next == OrderStatus.DELIVERED;
            case DELIVERED -> false;
        };
        if (!valid) {
            throw new IllegalArgumentException(
                "Transition invalide : " + current + " → " + next + ". Transitions autorisées : PENDING→CONFIRMED, CONFIRMED→SHIPPED, SHIPPED→DELIVERED"
            );
        }
    }

    // -------------------------------------------------------------------------
    // Suppression
    // -------------------------------------------------------------------------

    /**
     * Supprime une commande et tous ses items.
     * cascade = ALL + orphanRemoval = true sur Order.items
     * → les OrderItem sont supprimés automatiquement avec l'Order.
     */
    public void deleteOrder(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new IllegalArgumentException("Commande non trouvée avec l'ID: " + id);
        }
        orderRepository.deleteById(id);
    }

    // -------------------------------------------------------------------------
    // Agrégations (Partie 6.1)
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public BigDecimal getTotalRevenue() {
        return orderRepository.sumTotalAmountByStatus(OrderStatus.DELIVERED);
    }

    @Transactional(readOnly = true)
    public List<Object[]> getOrderCountByStatus() {
        return orderRepository.countGroupByStatus();
    }

    /**
     * Produits les plus commandés.
     * PageRequest.of(0, limit) → Spring Data applique LIMIT en SQL.
     */
    @Transactional(readOnly = true)
    public List<Object[]> getMostOrderedProducts(int limit) {
        if (limit <= 0) throw new IllegalArgumentException("La limite doit être supérieure à 0");
        return orderRepository.findMostOrderedProducts(PageRequest.of(0, limit));
    }

    @Transactional(readOnly = true)
    public long countOrders() {
        return orderRepository.count();
    }
}
