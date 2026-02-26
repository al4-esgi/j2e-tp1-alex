package com.formation.springproducts.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * OpenApiConfig — Configuration Swagger / SpringDoc (TP4 — Cours 6)
 *
 * Expose la documentation OpenAPI 3 de l'API REST et configure
 * le schéma de sécurité HTTP Basic pour Swagger UI.
 *
 * Accès :
 *   Swagger UI  → http://localhost:8081/swagger-ui.html
 *   Spec JSON   → http://localhost:8081/v3/api-docs
 *
 * Le schéma "basicAuth" est déclaré globalement :
 * tous les endpoints affichent le cadenas dans Swagger UI.
 * Le bouton "Authorize" permet de saisir les credentials
 * (admin/admin123 ou user/user123) directement depuis la doc.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "basicAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("Spring Products API")
                        .version("1.0.0")
                        .description("""
                                API REST de gestion de produits — TP4 Projet Final

                                **Authentification HTTP Basic requise** pour les opérations d'écriture.

                                Comptes disponibles :
                                - `admin` / `admin123` → rôle ADMIN (lecture + écriture)
                                - `user` / `user123`   → rôle USER (lecture seule)

                                Cliquer sur **Authorize** pour saisir vos credentials.
                                """)
                        .contact(new Contact()
                                .name("Spring Products API")
                                .email("contact@formation.com")))

                // Déclare le schéma de sécurité HTTP Basic
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("basic")
                                        .description("Authentification HTTP Basic — format : username:password en Base64")))

                // Applique le schéma de sécurité à tous les endpoints par défaut
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName));
    }
}
