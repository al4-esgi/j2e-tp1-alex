package com.formation.springproducts.service;

import com.formation.springproducts.model.Category;
import com.formation.springproducts.repository.CategoryRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public Category createCategory(String name, String description) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom de la catégorie est obligatoire");
        }
        if (categoryRepository.existsByNameIgnoreCase(name.trim())) {
            throw new IllegalArgumentException("Une catégorie avec le nom '" + name + "' existe déjà");
        }
        return categoryRepository.save(new Category(name.trim(), description));
    }

    @Transactional(readOnly = true)
    public Optional<Category> getCategory(Long id) {
        if (id == null || id <= 0) return Optional.empty();
        return categoryRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    /**
     * Récupère une catégorie avec ses produits chargés via JOIN FETCH.
     * Sans JOIN FETCH, accéder à category.getProducts() hors transaction
     * lèverait une LazyInitializationException (open-in-view=false).
     */
    @Transactional(readOnly = true)
    public Category getCategoryWithProducts(Long id) {
        return categoryRepository.findByIdWithProducts(id).orElseThrow(() -> new IllegalArgumentException("Catégorie non trouvée avec l'ID: " + id));
    }

    public Category updateCategory(Long id, String name, String description) {
        Category category = categoryRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Catégorie non trouvée avec l'ID: " + id));

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom de la catégorie est obligatoire");
        }
        if (!category.getName().equalsIgnoreCase(name.trim()) && categoryRepository.existsByNameIgnoreCase(name.trim())) {
            throw new IllegalArgumentException("Une catégorie avec le nom '" + name + "' existe déjà");
        }

        category.setName(name.trim());
        category.setDescription(description);
        // Dirty checking : save() explicite pour la clarté
        return categoryRepository.save(category);
    }

    /**
     * Supprime une catégorie et TOUS ses produits associés.
     * cascade = ALL + orphanRemoval = true sur Category.products
     * → les produits sont supprimés en cascade.
     */
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new IllegalArgumentException("Catégorie non trouvée avec l'ID: " + id);
        }
        categoryRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public long countCategories() {
        return categoryRepository.count();
    }
}
