package com.formation.products.config;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * ApplicationConfig - Configuration JAX-RS
 * Active JAX-RS et définit le chemin de base de l'API
 *
 * @ApplicationPath : Définit le préfixe pour tous les endpoints REST
 * Exemple : avec "/api", ProductResource sera accessible via /api/products
 */
@ApplicationPath("/api")
public class ApplicationConfig extends Application {
    // Classe vide suffisante pour activer JAX-RS
    // Jakarta EE scannera automatiquement les classes avec @Path
}
