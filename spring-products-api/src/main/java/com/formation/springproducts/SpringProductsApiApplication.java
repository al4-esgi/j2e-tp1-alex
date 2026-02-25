package com.formation.springproducts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SpringProductsApiApplication — Point d'entrée de l'application Spring Boot (TP2)
 *
 * @SpringBootApplication active :
 *   - @Configuration          : classe de configuration Spring
 *   - @EnableAutoConfiguration : auto-configure Spring Data JPA, DataSource, Hibernate
 *                                grâce aux dépendances présentes dans le classpath
 *   - @ComponentScan          : détecte automatiquement @Entity, @Repository,
 *                               @Service, @RestController dans ce package et ses sous-packages
 *
 * Changement TP2 :
 * - Suppression de exclude = { DataSourceAutoConfiguration.class }
 *   → Spring Boot peut maintenant auto-configurer la DataSource PostgreSQL
 *     et l'EntityManagerFactory à partir de application.properties.
 */
@SpringBootApplication
public class SpringProductsApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringProductsApiApplication.class, args);
    }
}
