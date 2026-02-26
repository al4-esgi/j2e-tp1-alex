# Rapport de Projet - Spring Products API

## 1. Présentation

Spring Products API est une API REST complète de gestion de produits développée avec Spring Boot 3.2.2 dans le cadre du projet final TP4. L'application permet de gérer l'ensemble du cycle de vie des produits, catégories, fournisseurs et commandes via des endpoints REST documentés et sécurisés.

Le projet couvre les concepts vus en cours : architecture en couches, persistence JPA avec optimisation des requêtes, validation métier avec contraintes custom, gestion centralisée des erreurs, pagination, documentation OpenAPI et authentification HTTP Basic avec gestion des rôles.

L'API est conteneurisée avec Docker et s'appuie sur PostgreSQL comme base de données. Elle est accessible localement sur `http://localhost:8081` et dispose d'une interface Swagger UI interactive sur `/swagger-ui.html`.

## 2. Architecture Technique

### 2.1 Framework Utilisé

**Spring Boot 3.2.2** a été choisi pour sa convention over configuration qui permet de démarrer rapidement sans XML de configuration. Son écosystème intégré (Spring Data JPA, Spring Security, SpringDoc) évite de gérer manuellement les dépendances entre composants. La génération d'un JAR exécutable simplifie également le déploiement et la conteneurisation Docker.

Par rapport à Jakarta EE (WildFly/Quarkus), Spring Boot offre un temps de démarrage plus rapide en développement, une meilleure intégration avec les outils modernes (Docker, CI/CD) et une documentation communautaire très fournie.

### 2.2 Couches de l'Application

L'application respecte une séparation stricte en **4 couches** :

**Couche Présentation (`controller/`)** — Reçoit les requêtes HTTP, valide les entrées avec `@Valid`, délègue au service et retourne les `ResponseEntity` appropriées. Ne contient aucune logique métier. Annotée `@RestController`.

**Couche Application (`service/`)** — Contient toute la logique métier : validation des règles, orchestration des repositories, gestion des transactions avec `@Transactional`. Chaque méthode de lecture est annotée `@Transactional(readOnly = true)` pour optimiser les performances Hibernate.

**Couche Domaine (`model/` + `repository/` + `validation/`)** — Contient les entités JPA annotées (`@Entity`, Bean Validation), les interfaces repository (`JpaRepository`) et les contraintes de validation custom. Les repositories sont de simples interfaces : Spring Data génère les implémentations au démarrage.

**Couche Infrastructure (PostgreSQL via Spring Data JPA)** — La base de données est isolée derrière les interfaces repository. Le reste de l'application n'a aucune connaissance des détails SQL ou de la configuration Hibernate.

### 2.3 Technologies Utilisées

- **Spring Boot 3.2.2** — framework principal (Spring MVC, Spring Data JPA, Spring Security)
- **Hibernate 6.4** — ORM, génération du schéma SQL, dirty checking
- **Spring Data JPA** — génération automatique des implémentations repository
- **Spring Security 6** — authentification HTTP Basic, autorisation par rôles, BCrypt
- **SpringDoc OpenAPI 2.3.0** — documentation Swagger UI auto-générée
- **Bean Validation (Jakarta Validation)** — contraintes déclaratives sur les entités
- **PostgreSQL 16** — base de données relationnelle
- **Docker / Docker Compose** — conteneurisation de l'application et de la base
- **Java 17** — LTS avec records, text blocks, pattern matching

### 2.4 Modèle de Données

L'application gère 5 entités JPA reliées entre elles :

```
categories              products                    suppliers
──────────              ────────                    ─────────
id (PK)         ◄──     id (PK)              ──►    id (PK)
name                    name                        name
description             description                 email
                        price                       phone
                        stock
                        sku (unique, @ValidSKU)
                        category_id (FK, NOT NULL)
                        supplier_id (FK, nullable)
                        created_at
                        updated_at

orders                  order_items
──────                  ───────────
id (PK)         ◄──     id (PK)
order_number            order_id (FK)
customer_name           product_id (FK)      ──►    products
customer_email          quantity
status (enum)           unit_price
total_amount            subtotal
order_date
```

**Relations JPA :**

- `Product` → `Category` : `@ManyToOne(fetch = LAZY)` — un produit appartient à une catégorie
- `Product` → `Supplier` : `@ManyToOne(fetch = LAZY)` — un produit peut avoir un fournisseur (nullable)
- `Order` → `OrderItem` : `@OneToMany(cascade = ALL, orphanRemoval = true)` — une commande contient plusieurs lignes
- `OrderItem` → `Product` : `@ManyToOne` — chaque ligne référence un produit

Toutes les relations `@ManyToOne` sont `LAZY` pour éviter le problème N+1. Les chargements sont gérés explicitement via `JOIN FETCH` dans les requêtes JPQL.

## 3. Fonctionnalités Implémentées

### 3.1 CRUD

CRUD complet sur les 4 entités principales. Exemple sur les produits :

| Méthode | URL                       | Description                              |
| ------- | ------------------------- | ---------------------------------------- |
| GET     | /api/products?page=0&size=10 | Liste paginée                         |
| GET     | /api/products/{id}        | Produit par ID (404 si absent)           |
| POST    | /api/products             | Créer (201 + header Location)            |
| PUT     | /api/products/{id}        | Mettre à jour complet                    |
| DELETE  | /api/products/{id}        | Supprimer (204, ou 404 si absent)        |
| PATCH   | /api/products/{id}/stock  | Ajustement partiel du stock              |

Exemple de création (POST /api/products) :

```json
{
  "name": "MacBook Pro 14",
  "description": "Ordinateur portable Apple",
  "price": 1999.99,
  "stock": 10,
  "sku": "MBP001",
  "category": { "id": 1 },
  "supplier": { "id": 2 }
}
```

Réponse 201 Created avec header `Location: /api/products/42`.

### 3.2 Validation

**Bean Validation sur les entités :**

| Champ       | Contraintes                                                     |
| ----------- | --------------------------------------------------------------- |
| `name`      | `@NotBlank`, `@Size(min=2, max=200)`                            |
| `price`     | `@NotNull`, `@DecimalMin("0.01")`, `@Digits(integer=8, fraction=2)` |
| `stock`     | `@Min(0)`                                                       |
| `sku`       | `@ValidSKU` (contrainte custom)                                 |
| `category`  | `@NotNull`                                                      |
| `description` | `@Size(max=1000)`                                             |

**3 contraintes custom implémentées :**

- **`@ValidSKU`** — valide le format `ABC123` (3 lettres majuscules + 3 chiffres) via regex `[A-Z]{3}[0-9]{3}`. Utilisée sur `Product.sku`.
- **`@ValidPrice`** — vérifie des règles métier sur le prix (cohérence avec le type de produit). Utilisée sur `Product.price`.
- **`@ValidDateRange`** — vérifie que la date de fin est postérieure à la date de début. Utilisée sur les entités avec des plages de dates.

Chaque contrainte custom est composée de :
- une annotation (`@interface`) avec `@Constraint(validatedBy = ...)`
- un validateur qui implémente `ConstraintValidator<A, T>`

### 3.3 Gestion des Erreurs

`GlobalExceptionHandler` (`@ControllerAdvice`) intercepte toutes les exceptions avant qu'elles ne remontent au client et retourne des réponses JSON structurées avec le bon code HTTP.

**Codes HTTP retournés :**

| Exception                         | HTTP | Cas d'usage                              |
| --------------------------------- | ---- | ---------------------------------------- |
| `MethodArgumentNotValidException` | 400  | `@Valid` sur `@RequestBody` échoue       |
| `ConstraintViolationException`    | 400  | Violation au niveau service/Hibernate    |
| `IllegalArgumentException`        | 400  | Validation métier manuelle               |
| `ProductNotFoundException`        | 404  | Produit introuvable par ID               |
| `CategoryNotFoundException`       | 404  | Catégorie introuvable par ID             |
| `DuplicateProductException`       | 409  | SKU déjà utilisé                         |
| `CategoryNotEmptyException`       | 409  | Suppression d'une catégorie non vide     |
| `InsufficientStockException`      | 422  | Stock insuffisant pour la demande        |
| `Exception` (fallback)            | 500  | Toute erreur non prévue                  |

**Exemple de réponse 400 avec détails de validation :**

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation échouée : 2 erreur(s) détectée(s)",
  "timestamp": "2024-01-15T10:30:00",
  "path": "uri=/api/products",
  "errors": [
    {
      "field": "name",
      "message": "Le nom du produit est obligatoire",
      "rejectedValue": ""
    },
    {
      "field": "sku",
      "message": "SKU invalide. Format attendu: ABC123 (3 lettres majuscules + 3 chiffres)",
      "rejectedValue": "abc-123"
    }
  ]
}
```

**Exemple de réponse 404 :**

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Produit non trouvé avec l'ID : 99",
  "timestamp": "2024-01-15T10:31:00",
  "path": "uri=/api/products/99"
}
```

### 3.4 Fonctionnalités Avancées

**Pagination**

`GET /api/products` supporte la pagination via les paramètres `page` et `size` :

```
GET /api/products?page=0&size=10   → première page de 10 produits
GET /api/products?page=2&size=5    → troisième page de 5 produits
```

L'objet `Page<Product>` retourné contient les métadonnées nécessaires au client pour naviguer :

```json
{
  "content": [ ... ],
  "totalElements": 42,
  "totalPages": 5,
  "number": 0,
  "size": 10,
  "first": true,
  "last": false
}
```

La validation métier limite `size` entre 1 et 100 pour éviter les requêtes abusives.

**Documentation Swagger UI**

Accessible sur `http://localhost:8081/swagger-ui.html`. Générée automatiquement par SpringDoc à partir des annotations Spring MVC existantes. Le bouton **Authorize** permet de saisir les credentials HTTP Basic directement dans l'interface pour tester les endpoints protégés.

**Authentification et Autorisation (Spring Security)**

HTTP Basic Auth avec deux rôles :

| Compte  | Mot de passe | Rôle  | Droits                              |
| ------- | ------------ | ----- | ----------------------------------- |
| `admin` | `admin123`   | ADMIN | GET + POST + PUT + DELETE + PATCH   |
| `user`  | `user123`    | USER  | GET uniquement                      |

Les mots de passe sont hashés avec **BCrypt** avant d'être stockés (même en mémoire). Les sessions sont `STATELESS` : chaque requête doit porter ses credentials en header `Authorization: Basic <base64>`. Le CSRF est désactivé car l'API REST ne repose pas sur les cookies de session.

**Optimisation N+1**

Deux endpoints de démonstration :
- `GET /api/products/slow` — charge les produits sans `JOIN FETCH`, déclenche N+1 requêtes SQL (visible dans les logs Hibernate)
- `GET /api/products/fast` — `JOIN FETCH` sur category et supplier, 1 seule requête SQL

Par défaut, tous les endpoints de lecture utilisent la version optimisée.

**Statistiques et agrégations JPQL**

- `GET /api/products/stats/by-category` — `COUNT` par catégorie
- `GET /api/products/stats/avg-price` — `AVG(price)` par catégorie
- `GET /api/products/stats/category-stats` — projection DTO via `SELECT NEW`
- `GET /api/products/never-ordered` — sous-requête `NOT IN (SELECT oi.product FROM OrderItem oi)`

## 4. Difficultés Rencontrées et Solutions

### Difficulté 1 : LazyInitializationException hors transaction

**Problème** : Avec `spring.jpa.open-in-view=false`, les relations `LAZY` ne peuvent pas être chargées en dehors d'une transaction. Jackson tente de sérialiser les proxies Hibernate après la fermeture de la session → `LazyInitializationException: no Session`.

**Solution** : Deux approches combinées. Pour les listes, utilisation de `JOIN FETCH` dans les requêtes JPQL pour charger les relations en une seule requête SQL dans la transaction. Pour les cas où `JOIN FETCH` n'est pas applicable, utilisation du module `jackson-datatype-hibernate6` qui sérialise les proxies non initialisés comme `null` au lieu de lever une exception.

### Difficulté 2 : JOIN FETCH incompatible avec Pageable

**Problème** : Combiner `JOIN FETCH` et `Pageable` dans une requête `@Query` Spring Data provoque une erreur car Spring Data ne peut pas dériver automatiquement la requête `COUNT` depuis une requête `JOIN FETCH`. Sans `COUNT`, il est impossible de calculer `totalPages` et `totalElements` dans l'objet `Page`.

**Solution** : Ajout d'une `countQuery` explicite dans l'annotation `@Query` :

```java
@Query(
  value = "SELECT p FROM Product p JOIN FETCH p.category LEFT JOIN FETCH p.supplier",
  countQuery = "SELECT COUNT(p) FROM Product p"
)
Page<Product> findAllPaginated(Pageable pageable);
```

Les deux requêtes sont exécutées séparément : une pour les données de la page, une pour le comptage total.

### Difficulté 3 : Sérialisation circulaire entre entités liées

**Problème** : Les entités JPA avec relations bidirectionnelles (`Order` ↔ `OrderItem`) provoquent des boucles infinies lors de la sérialisation JSON : Jackson suit les références en boucle jusqu'à un `StackOverflowError`.

**Solution** : Annotation `@JsonManagedReference` du côté parent (`Order.items`) et `@JsonBackReference` du côté enfant (`OrderItem.order`). Le côté `@JsonBackReference` est exclu de la sérialisation JSON, ce qui brise la boucle.

### Difficulté 4 : Unicité du SKU et gestion des conflits

**Problème** : La contrainte `UNIQUE` en base sur la colonne `sku` génère une `DataIntegrityViolationException` de bas niveau si un doublon est inséré, ce qui expose un message technique peu clair au client.

**Solution** : Vérification préventive dans le service avec `productRepository.existsBySku(sku)` avant le `save()`, et levée d'une `DuplicateProductException` custom interceptée par le `GlobalExceptionHandler` qui retourne un 409 avec un message métier lisible.

## 5. Points d'Amélioration

**Avec plus de temps, j'ajouterais :**

- **JWT à la place du Basic Auth** — le Basic Auth envoie les credentials en clair (encodés Base64, non chiffrés) à chaque requête. JWT permettrait des tokens à durée de vie limitée, un refresh token et une meilleure scalabilité (pas besoin de vérifier les credentials en base à chaque requête).

- **Pagination sur tous les endpoints de liste** — actuellement seul `GET /api/products` est paginé. Les endpoints `GET /api/orders`, `GET /api/categories`, `GET /api/suppliers` retournent toujours des listes complètes, ce qui peut poser des problèmes de performance avec un grand volume de données.

- **Tests automatisés** — les tests ont été réalisés manuellement via Thunder Client. Des tests d'intégration avec `@SpringBootTest` + `@Transactional` et une base H2 en mémoire permettraient de valider automatiquement chaque endpoint à chaque build.

- **Versioning d'API** — préfixer les URLs avec `/api/v1/` pour permettre des évolutions de l'API sans casser les clients existants.

- **Gestion des utilisateurs en base** — remplacer l'`InMemoryUserDetailsManager` par un `UserDetailsService` custom qui charge les utilisateurs depuis une table `users` en PostgreSQL, avec inscription et changement de mot de passe.

## 6. Conclusion

Ce projet m'a permis de mettre en pratique l'ensemble des concepts du cours sur une application cohérente de bout en bout. Les points les plus importants que j'ai retenus :

**L'architecture en couches est un investissement** : le temps passé à bien séparer Controller, Service et Repository au début se récupère largement quand il faut modifier ou tester une partie de l'application sans toucher aux autres.

**JPA demande de comprendre ce qu'il génère** : le problème N+1 est invisible jusqu'à ce qu'on active les logs SQL. `spring.jpa.open-in-view=false` oblige à réfléchir au chargement des relations dès la conception des requêtes, ce qui est une bonne discipline.

**La gestion des erreurs est une fonctionnalité à part entière** : un `@ControllerAdvice` bien conçu avec des réponses JSON structurées fait la différence entre une API professionnelle et une API qui expose des stack traces au client.

**Spring Security s'intègre proprement** : le fait de pouvoir configurer toute la sécurité dans une seule classe `SecurityConfig` sans modifier les controllers existants illustre bien le principe Open/Closed — l'application est ouverte à l'extension (ajout de sécurité) sans modification du code existant.
