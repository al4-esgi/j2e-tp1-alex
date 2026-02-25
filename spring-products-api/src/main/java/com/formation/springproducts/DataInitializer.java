package com.formation.springproducts;

import com.formation.springproducts.model.Category;
import com.formation.springproducts.model.Product;
import com.formation.springproducts.model.Supplier;
import com.formation.springproducts.repository.CategoryRepository;
import com.formation.springproducts.repository.ProductRepository;
import com.formation.springproducts.repository.SupplierRepository;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;

    public DataInitializer(CategoryRepository categoryRepository, SupplierRepository supplierRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (categoryRepository.count() > 0) {
            log.info("DataInitializer : données déjà présentes — initialisation ignorée.");
            return;
        }

        log.info("DataInitializer : insertion des données de démonstration...");

        // =====================================================================
        // Catégories
        // =====================================================================
        Category informatique = categoryRepository.save(new Category("Informatique", "Ordinateurs, périphériques et accessoires informatiques"));

        Category telephonie = categoryRepository.save(new Category("Téléphonie", "Smartphones, tablettes et accessoires mobiles"));

        Category audio = categoryRepository.save(new Category("Audio", "Casques, écouteurs et enceintes"));

        Category mobilier = categoryRepository.save(new Category("Mobilier", "Mobilier de bureau et accessoires ergonomiques"));

        log.info("DataInitializer : {} catégories créées.", categoryRepository.count());

        // =====================================================================
        // Fournisseurs
        // =====================================================================
        Supplier techDistrib = supplierRepository.save(new Supplier("Tech Distribution SA", "contact@techdist.fr", "+33123456789"));

        Supplier mobilePro = supplierRepository.save(new Supplier("MobilePro SARL", "pro@mobilepro.fr", "+33234567890"));

        Supplier soundEquip = supplierRepository.save(new Supplier("SoundEquip SAS", "info@soundequip.fr", "+33345678901"));

        log.info("DataInitializer : {} fournisseurs créés.", supplierRepository.count());

        // =====================================================================
        // Produits
        // =====================================================================
        List<Product> products = List.of(
            product(
                "Laptop Dell XPS 15",
                "Ordinateur portable haute performance, écran 15 pouces OLED, 32 Go RAM, 1 To SSD",
                new BigDecimal("1299.99"),
                15,
                informatique,
                techDistrib
            ),
            product(
                "Clavier Mécanique Logitech MX Keys",
                "Clavier gaming RGB avec switches mécaniques rétroéclairés, compatible multi-OS",
                new BigDecimal("159.99"),
                20,
                informatique,
                techDistrib
            ),
            product(
                "Souris Ergonomique Logitech MX Master 3",
                "Souris sans fil ergonomique avec molette MagSpeed et 7 boutons programmables",
                new BigDecimal("99.99"),
                35,
                informatique,
                techDistrib
            ),
            product("iPhone 15 Pro", "Smartphone Apple avec puce A17 Pro, écran Super Retina XDR 6.1 pouces, 256 Go", new BigDecimal("1199.00"), 25, telephonie, mobilePro),
            product(
                "Samsung Galaxy S24 Ultra",
                "Smartphone Android avec S Pen intégré, écran Dynamic AMOLED 6.8 pouces, 512 Go",
                new BigDecimal("1349.00"),
                18,
                telephonie,
                mobilePro
            ),
            product("Écouteurs Sony WH-1000XM5", "Casque audio sans fil à réduction de bruit active, 30h d'autonomie", new BigDecimal("399.00"), 30, audio, soundEquip),
            // Produit sans fournisseur → teste les LEFT JOIN FETCH sur supplier
            product("Chaise de Bureau Ergonomique Herman Miller", "Chaise haute gamme avec support lombaire réglable", new BigDecimal("1450.00"), 8, mobilier, null)
        );

        productRepository.saveAll(products);

        log.info("DataInitializer : {} produits créés.", productRepository.count());
        log.info("DataInitializer : initialisation terminée. 🚀");
        log.info("DataInitializer : API disponible sur http://localhost:8081/api/");
        log.info("  GET /api/products       → liste optimisée (JOIN FETCH)");
        log.info("  GET /api/products/slow  → démo problème N+1");
        log.info("  GET /api/products/fast  → démo 1 requête SQL");
        log.info("  GET /api/categories");
        log.info("  GET /api/suppliers");
        log.info("  GET /api/orders");
    }

    private Product product(String name, String description, BigDecimal price, int stock, Category category, Supplier supplier) {
        Product p = new Product(name, description, price, stock);
        p.setCategory(category);
        p.setSupplier(supplier);
        return p;
    }
}
