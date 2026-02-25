package com.formation.springproducts.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.formation.springproducts.model.Supplier;

/**
 * SupplierRepository — Spring Data JPA
 *
 * JpaRepository<Supplier, Long> fournit gratuitement :
 *   save(), findById(), findAll(), deleteById(), count(), existsById(), etc.
 *
 * Toutes les requêtes ici sont des requêtes dérivées :
 * Spring Data génère le JPQL automatiquement depuis le nom de la méthode.
 * Pas besoin de @Query pour des recherches simples sur un seul champ.
 */
@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    /**
     * Recherche un fournisseur par son email, insensible à la casse.
     * Spring Data génère : WHERE LOWER(s.email) = LOWER(?1)
     *
     * Retourne Optional.empty() si aucun fournisseur ne correspond.
     * Utilisé pour valider l'unicité de l'email avant création/mise à jour.
     */
    Optional<Supplier> findByEmailIgnoreCase(String email);

    /**
     * Recherche un fournisseur par son nom, insensible à la casse.
     * Spring Data génère : WHERE LOWER(s.name) = LOWER(?1)
     */
    Optional<Supplier> findByNameIgnoreCase(String name);

    /**
     * Vérifie si un fournisseur existe avec cet email (insensible à la casse).
     * Plus efficace que findByEmailIgnoreCase().isPresent() :
     * génère un SELECT COUNT au lieu de charger l'entité complète.
     */
    boolean existsByEmailIgnoreCase(String email);
}
