package com.formation.springproducts.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;

/**
 * Configuration Jackson — Hibernate6Module
 *
 * Sans ce module, Jackson tente de sérialiser les proxies Hibernate LAZY
 * hors session (open-in-view=false) → LazyInitializationException.
 *
 * Avec ce module :
 * - Proxies non-initialisés → sérialisés comme null
 * - Collections LAZY non-initialisées → sérialisées comme null
 * - Proxies initialisés (JOIN FETCH) → sérialisés normalement
 *
 * Exemple :
 *   GET /api/orders       → items: null  (LAZY, non chargé)
 *   GET /api/orders/full  → items: [...]  (JOIN FETCH, chargé)
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Hibernate6Module hibernate6Module() {
        Hibernate6Module module = new Hibernate6Module();
        // Ne pas forcer le chargement LAZY lors de la sérialisation
        // → les relations non initialisées apparaissent comme null dans le JSON
        module.disable(Hibernate6Module.Feature.FORCE_LAZY_LOADING);
        // Sérialise l'identifiant des proxies non-initialisés au lieu de null
        // → utile pour déboguer (ex: "category": {"id": 1} au lieu de null)
        module.enable(Hibernate6Module.Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS);
        return module;
    }
}
