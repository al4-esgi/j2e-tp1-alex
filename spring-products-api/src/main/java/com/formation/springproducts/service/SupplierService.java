package com.formation.springproducts.service;

import com.formation.springproducts.model.Product;
import com.formation.springproducts.model.Supplier;
import com.formation.springproducts.repository.ProductRepository;
import com.formation.springproducts.repository.SupplierRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;

    public SupplierService(SupplierRepository supplierRepository, ProductRepository productRepository) {
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
    }

    public Supplier createSupplier(String name, String email, String phone) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom du fournisseur est obligatoire");
        }
        if (email != null && !email.trim().isEmpty() && supplierRepository.existsByEmailIgnoreCase(email.trim())) {
            throw new IllegalArgumentException("Un fournisseur avec l'email '" + email + "' existe déjà");
        }
        return supplierRepository.save(new Supplier(name.trim(), email, phone));
    }

    @Transactional(readOnly = true)
    public Optional<Supplier> getSupplier(Long id) {
        if (id == null || id <= 0) return Optional.empty();
        return supplierRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAll();
    }

    public Supplier updateSupplier(Long id, String name, String email, String phone) {
        Supplier supplier = supplierRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Fournisseur non trouvé avec l'ID: " + id));

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom du fournisseur est obligatoire");
        }

        boolean emailChanged = email != null && !email.trim().isEmpty() && (supplier.getEmail() == null || !supplier.getEmail().equalsIgnoreCase(email.trim()));
        if (emailChanged && supplierRepository.existsByEmailIgnoreCase(email.trim())) {
            throw new IllegalArgumentException("Un fournisseur avec l'email '" + email + "' existe déjà");
        }

        supplier.setName(name.trim());
        supplier.setEmail(email);
        supplier.setPhone(phone);
        return supplierRepository.save(supplier);
    }

    /**
     * Supprime un fournisseur.
     *
     * Supplier.products n'a pas de cascade → il faut délier les produits
     * avant la suppression pour éviter une violation de contrainte FK.
     * On passe supplier à null sur chaque produit lié (dirty checking suffit).
     * Tout se passe dans une seule transaction : rollback si la suppression échoue.
     */
    public void deleteSupplier(Long id) {
        Supplier supplier = supplierRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Fournisseur non trouvé avec l'ID: " + id));

        List<Product> linked = productRepository.findBySupplier(supplier);
        linked.forEach(p -> p.setSupplier(null));
        // Dirty checking : les modifications seront flushées avant le DELETE.

        supplierRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public long countSuppliers() {
        return supplierRepository.count();
    }
}
