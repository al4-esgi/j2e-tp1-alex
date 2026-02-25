package com.formation.springproducts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * SpringProductsApiApplication - Point d'entrée de l'application Spring Boot
 *
 * @SpringBootApplication : Annotation composite qui active :
 *   - @Configuration : Classe de configuration Spring
 *   - @EnableAutoConfiguration : Configuration automatique basée sur les dépendances
 *   - @ComponentScan : Scan automatique des composants dans ce package et sous-packages
 */
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
public class SpringProductsApiApplication {

    /**
     * Point d'entrée de l'application
     * @param args arguments de ligne de commande
     */
    public static void main(String[] args) {
        SpringApplication.run(SpringProductsApiApplication.class, args);
    }
}
