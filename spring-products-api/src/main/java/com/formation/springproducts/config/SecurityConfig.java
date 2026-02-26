package com.formation.springproducts.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SecurityConfig — Configuration Spring Security (TP4 — Cours 5)
 *
 * Implémente une authentification HTTP Basic avec deux rôles :
 *   - ADMIN : accès complet (lecture + écriture)
 *   - USER  : lecture seule (GET uniquement)
 *
 * Stratégie d'autorisation :
 *   GET  /* → ouvert à tous les utilisateurs authentifiés (USER ou ADMIN)
 *   POST / PUT / DELETE / PATCH → réservé au rôle ADMIN
 *
 * Endpoints publics (sans authentification) :
 *   /swagger-ui/**  → documentation de l'API
 *   /v3/api-docs/** → spec OpenAPI JSON
 *   /actuator/health → health check (si activé)
 *
 * Session : STATELESS — chaque requête doit porter ses credentials.
 * Passwords : hashés avec BCrypt (facteur de coût 12 par défaut).
 *
 * Comptes de test :
 *   admin / admin123  → rôle ADMIN
 *   user  / user123   → rôle USER
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * PasswordEncoder BCrypt — utilisé pour hasher les mots de passe.
     * BCrypt est résistant aux attaques par rainbow table et force brute
     * grâce à son algorithme adaptatif (salt intégré).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * UserDetailsService in-memory — pour la démonstration.
     *
     * En production : remplacer par JdbcUserDetailsManager (base de données)
     * ou une implémentation custom qui charge les utilisateurs depuis la BDD.
     *
     * Les mots de passe sont hashés via BCrypt avant stockage (même en mémoire).
     */
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        var admin = User.builder()
                .username("admin")
                .password(encoder.encode("admin123"))
                .roles("ADMIN", "USER")
                .build();

        var user = User.builder()
                .username("user")
                .password(encoder.encode("user123"))
                .roles("USER")
                .build();

        return new InMemoryUserDetailsManager(admin, user);
    }

    /**
     * Chaîne de filtres de sécurité principale.
     *
     * Règles d'autorisation (dans l'ordre — la première règle qui correspond s'applique) :
     *
     * 1. Swagger UI et spec OpenAPI → publics (sans authentification)
     * 2. GET sur /api/** → accessible aux rôles USER et ADMIN
     * 3. POST / PUT / DELETE / PATCH sur /api/** → réservé à ADMIN
     * 4. Tout le reste → authentification requise
     *
     * CSRF désactivé car l'API est STATELESS (pas de session, pas de cookie de session).
     * Pour une API REST consommée par des clients JS/mobiles, CSRF n'est pas pertinent.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Désactiver CSRF pour une API REST stateless
            .csrf(csrf -> csrf.disable())

            // Pas de session HTTP — chaque requête est autonome
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Règles d'autorisation
            .authorizeHttpRequests(auth -> auth
                // Swagger UI accessible sans authentification
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/v3/api-docs"
                ).permitAll()

                // Lecture : USER et ADMIN
                .requestMatchers(HttpMethod.GET, "/api/**").hasAnyRole("USER", "ADMIN")

                // Écriture : ADMIN uniquement
                .requestMatchers(HttpMethod.POST,   "/api/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH,  "/api/**").hasRole("ADMIN")

                // Tout autre endpoint → authentification requise
                .anyRequest().authenticated()
            )

            // Activer HTTP Basic Auth
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
