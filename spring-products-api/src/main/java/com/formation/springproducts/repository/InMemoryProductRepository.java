package com.formation.springproducts.repository;

import com.formation.springproducts.model.Product;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * InMemoryProductRepository - Couche Infrastructure
 * Implémentation en mémoire du repository (sans base de données)
 * Utilise ConcurrentHashMap pour la sécurité thread-safe
 *
 * @Repository : Annotation Spring qui marque cette classe comme un composant Repository
 * Spring la détectera automatiquement lors du scan des composants
 */
@Repository
public class InMemoryProductRepository implements IProductRepository {

    private final ConcurrentHashMap<String, Product> products = new ConcurrentHashMap<>();

    /**
     * Constructeur - Initialise quelques produits de test
     */
    public InMemoryProductRepository() {
        initializeTestData();
    }

    /**
     * Initialise des données de test
     */
    private void initializeTestData() {
        Product p1 = new Product(
            "Laptop Dell XPS 15",
            "Ordinateur portable haute performance avec écran 15 pouces",
            new BigDecimal("1299.99"),
            "Informatique",
            15
        );

        Product p2 = new Product(
            "iPhone 15 Pro",
            "Smartphone Apple dernière génération",
            new BigDecimal("1199.00"),
            "Téléphonie",
            25
        );

        Product p3 = new Product(
            "Chaise de Bureau Ergonomique",
            "Chaise confortable pour longues sessions de travail",
            new BigDecimal("349.99"),
            "Mobilier",
            10
        );

        Product p4 = new Product(
            "Écouteurs Sony WH-1000XM5",
            "Casque audio à réduction de bruit active",
            new BigDecimal("399.00"),
            "Audio",
            30
        );

        Product p5 = new Product(
            "Clavier Mécanique Logitech",
            "Clavier gaming RGB avec switches mécaniques",
            new BigDecimal("159.99"),
            "Informatique",
            20
        );

        products.put(p1.getId(), p1);
        products.put(p2.getId(), p2);
        products.put(p3.getId(), p3);
        products.put(p4.getId(), p4);
        products.put(p5.getId(), p5);
    }

    @Override
    public Product save(Product product) {
        // Si l'ID est null ou vide, c'est une création
        if (product.getId() == null || product.getId().isEmpty()) {
            product.setId(UUID.randomUUID().toString());
        }

        // Sauvegarde dans la map
        products.put(product.getId(), product);
        return product;
    }

    @Override
    public Optional<Product> findById(String id) {
        return Optional.ofNullable(products.get(id));
    }

    @Override
    public List<Product> findAll() {
        return products.values().stream()
                .collect(Collectors.toList());
    }

    @Override
    public List<Product> findByCategory(String category) {
        return products.values().stream()
                .filter(p -> p.getCategory().equalsIgnoreCase(category))
                .collect(Collectors.toList());
    }

    @Override
    public void delete(String id) {
        products.remove(id);
    }

    @Override
    public boolean exists(String id) {
        return products.containsKey(id);
    }

    @Override
    public long count() {
        return products.size();
    }
}
