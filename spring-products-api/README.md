# Spring Products API — Projet Final TP4

## Auteur

Alex

## Description

API REST complète de gestion de produits développée avec **Spring Boot 3.2.2** et **JPA/Hibernate**.

L'API couvre l'ensemble du cycle de vie des produits, catégories, fournisseurs et commandes, avec une architecture en couches, une validation poussée, une gestion centralisée des erreurs, une pagination, une documentation Swagger et une authentification HTTP Basic.

## Framework

**Spring Boot** — choisi pour sa convention over configuration, son écosystème mature (Spring Data JPA, Spring Security, SpringDoc) et sa facilité de déploiement via un JAR exécutable.

## Stack Technique

- Java 17
- Spring Boot 3.2.2
- Spring Data JPA / Hibernate 6.4
- Spring Security 6 (HTTP Basic + BCrypt)
- SpringDoc OpenAPI 2.3.0 (Swagger UI)
- PostgreSQL 16
- Docker / Docker Compose

## Architecture

L'application respecte une séparation stricte en **4 couches** :

```
Requête HTTP
     │
     ▼
┌─────────────────────┐
│   Presentation      │  controller/   @RestController, @RequestMapping
│   (Couche REST)     │  Reçoit les requêtes HTTP, délègue au service,
│                     │  retourne les ResponseEntity
└────────┬────────────┘
         │
         ▼
┌─────────────────────┐
│   Application       │  service/      @Service, @Transactional
│   (Couche Service)  │  Logique métier, validation, orchestration
│                     │  des repositories
└────────┬────────────┘
         │
         ▼
┌─────────────────────┐
│   Domain            │  model/        @Entity, Bean Validation
│   (Couche Domaine)  │  repository/   interfaces JpaRepository
│                     │  validation/   contraintes custom
└────────┬────────────┘
         │
         ▼
┌─────────────────────┐
│   Infrastructure    │  PostgreSQL via Spring Data JPA
│   (Couche Données)  │  Implémentations générées automatiquement
└─────────────────────┘
```

**Principes SOLID appliqués :**

- **S** — chaque classe a une responsabilité unique (Controller ≠ Service ≠ Repository)
- **D** — les services dépendent des interfaces `JpaRepository`, pas des implémentations
- **O** — les contraintes de validation sont extensibles sans modifier les entités

## Modèle de Données

```
categories         products              suppliers
----------         --------              ---------
id (PK)     <──   id (PK)         ┌──>  id (PK)
name               name            │     name
description        description     │     email
                   price           │     phone
                   stock           │
                   sku (unique)    │
                   category_id (FK)│
                   supplier_id (FK)┘
                   created_at
                   updated_at

orders             order_items
------             -----------
id (PK)     <──   id (PK)
order_number       order_id (FK)
customer_name      product_id (FK)
customer_email     quantity
status             unit_price
total_amount       subtotal
order_date
```

**Relations JPA :**

- `Product` → `Category` : `@ManyToOne` LAZY
- `Product` → `Supplier` : `@ManyToOne` LAZY
- `Order` → `OrderItem` : `@OneToMany` avec `cascade = ALL, orphanRemoval = true`
- `OrderItem` → `Product` : `@ManyToOne`

## Fonctionnalités Implémentées

### CRUD Complet

Les 4 entités principales (Product, Category, Supplier, Order) exposent chacune les opérations GET, POST, PUT, DELETE avec validation et gestion d'erreurs.

### Validation (Bean Validation)

- `@NotBlank`, `@Size`, `@Min`, `@DecimalMin`, `@Digits` sur toutes les entités
- **3 contraintes custom** :
  - `@ValidSKU` — format `ABC123` (3 majuscules + 3 chiffres)
  - `@ValidPrice` — prix cohérent avec les règles métier
  - `@ValidDateRange` — cohérence des dates sur les commandes

### Gestion des Erreurs

`GlobalExceptionHandler` (`@ControllerAdvice`) centralise toutes les exceptions :

| Exception                         | Code HTTP                          |
| --------------------------------- | ---------------------------------- |
| `MethodArgumentNotValidException` | 400 — détails des champs en erreur |
| `ConstraintViolationException`    | 400                                |
| `IllegalArgumentException`        | 400                                |
| `ProductNotFoundException`        | 404                                |
| `CategoryNotFoundException`       | 404                                |
| `DuplicateProductException`       | 409                                |
| `CategoryNotEmptyException`       | 409                                |
| `InsufficientStockException`      | 422                                |
| `Exception` (fallback)            | 500                                |

Réponse JSON structurée :

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation échouée : 2 erreur(s) détectée(s)",
  "path": "uri=/api/products",
  "errors": [
    { "field": "name", "message": "Le nom du produit est obligatoire", "rejectedValue": "" },
    { "field": "sku", "message": "Format SKU invalide (ex: ABC123)", "rejectedValue": "abc" }
  ]
}
```

### Pagination

`GET /api/products` supporte la pagination :

```
GET /api/products?page=0&size=10
GET /api/products?page=1&size=5
```

Réponse avec métadonnées :

```json
{
  "content": [...],
  "totalElements": 42,
  "totalPages": 5,
  "number": 0,
  "size": 10
}
```

### Optimisation N+1

- `GET /api/products/slow` — démo du problème N+1 (1 + N requêtes SQL)
- `GET /api/products/fast` — version optimisée avec `JOIN FETCH` (1 seule requête)
- Par défaut, tous les endpoints utilisent `JOIN FETCH`

### Sécurité (Spring Security — HTTP Basic)

| Compte  | Mot de passe | Rôle  | Droits                                    |
| ------- | ------------ | ----- | ----------------------------------------- |
| `admin` | `admin123`   | ADMIN | Lecture + écriture (tous les verbes HTTP) |
| `user`  | `user123`    | USER  | Lecture seule (GET uniquement)            |

Règles :

- `GET /api/**` → authentification requise, rôles USER ou ADMIN
- `POST / PUT / DELETE / PATCH /api/**` → rôle ADMIN uniquement
- `GET /swagger-ui/**` et `/v3/api-docs/**` → **public** (sans auth)
- Sessions **STATELESS** — credentials à envoyer à chaque requête
- Mots de passe hashés avec **BCrypt**

### Documentation Swagger UI

Accessible sur : `http://localhost:8081/swagger-ui.html`

Cliquer sur **Authorize** et saisir `admin` / `admin123` pour tester les endpoints d'écriture directement depuis l'UI.

## Lancement

### Avec Docker Compose (recommandé)

```bash
# Depuis le dossier spring-products-api/
docker-compose up -d
```

L'application démarre sur `http://localhost:8081`  
La base de données PostgreSQL démarre sur `localhost:5432`

### Sans Docker (PostgreSQL requis localement)

```bash
# Démarrer PostgreSQL
docker run -d \
  --name products-postgres \
  -e POSTGRES_DB=productsdb \
  -e POSTGRES_USER=products_user \
  -e POSTGRES_PASSWORD=products_pass \
  -p 5432:5432 \
  postgres:16-alpine

# Lancer l'application
mvn spring-boot:run
```

### URLs utiles

| URL                                     | Description               |
| --------------------------------------- | ------------------------- |
| `http://localhost:8081/api/products`    | Endpoint principal        |
| `http://localhost:8081/swagger-ui.html` | Documentation interactive |
| `http://localhost:8081/v3/api-docs`     | Spec OpenAPI JSON         |

## Endpoints

### Products

| Méthode | URL                                | Auth requise | Description                                  |
| ------- | ---------------------------------- | ------------ | -------------------------------------------- |
| GET     | /api/products                      | USER         | Liste paginée (`?page=0&size=10`) ou filtrée |
| GET     | /api/products/{id}                 | USER         | Produit par ID                               |
| GET     | /api/products/slow                 | USER         | Démo problème N+1                            |
| GET     | /api/products/fast                 | USER         | Liste optimisée JOIN FETCH                   |
| POST    | /api/products                      | ADMIN        | Créer un produit                             |
| PUT     | /api/products/{id}                 | ADMIN        | Mettre à jour un produit                     |
| DELETE  | /api/products/{id}                 | ADMIN        | Supprimer un produit                         |
| PATCH   | /api/products/{id}/stock           | ADMIN        | Ajuster le stock                             |
| PATCH   | /api/products/{id}/stock/decrease  | ADMIN        | Diminuer le stock                            |
| GET     | /api/products/stats/by-category    | USER         | Nb produits par catégorie                    |
| GET     | /api/products/stats/avg-price      | USER         | Prix moyen par catégorie                     |
| GET     | /api/products/stats/category-stats | USER         | Stats DTO projetées                          |
| GET     | /api/products/top                  | USER         | Top N produits chers                         |
| GET     | /api/products/never-ordered        | USER         | Produits jamais commandés                    |
| POST    | /api/products/transfer             | ADMIN        | Transférer vers une catégorie                |

### Categories

| Méthode | URL                           | Auth requise | Description                 |
| ------- | ----------------------------- | ------------ | --------------------------- |
| GET     | /api/categories               | USER         | Liste toutes les catégories |
| GET     | /api/categories/{id}          | USER         | Catégorie par ID            |
| GET     | /api/categories/{id}/products | USER         | Catégorie avec ses produits |
| POST    | /api/categories               | ADMIN        | Créer une catégorie         |
| PUT     | /api/categories/{id}          | ADMIN        | Mettre à jour une catégorie |
| DELETE  | /api/categories/{id}          | ADMIN        | Supprimer une catégorie     |

### Suppliers

| Méthode | URL                 | Auth requise | Description                  |
| ------- | ------------------- | ------------ | ---------------------------- |
| GET     | /api/suppliers      | USER         | Liste tous les fournisseurs  |
| GET     | /api/suppliers/{id} | USER         | Fournisseur par ID           |
| POST    | /api/suppliers      | ADMIN        | Créer un fournisseur         |
| PUT     | /api/suppliers/{id} | ADMIN        | Mettre à jour un fournisseur |
| DELETE  | /api/suppliers/{id} | ADMIN        | Supprimer un fournisseur     |

### Orders

| Méthode | URL                            | Auth requise | Description                       |
| ------- | ------------------------------ | ------------ | --------------------------------- |
| GET     | /api/orders                    | USER         | Liste toutes les commandes        |
| GET     | /api/orders/full               | USER         | Commandes avec items (JOIN FETCH) |
| GET     | /api/orders/{id}               | USER         | Commande par ID                   |
| GET     | /api/orders/{id}/items         | USER         | Commande avec ses items           |
| POST    | /api/orders                    | ADMIN        | Créer une commande                |
| PATCH   | /api/orders/{id}/status        | ADMIN        | Mettre à jour le statut           |
| DELETE  | /api/orders/{id}               | ADMIN        | Supprimer une commande            |
| GET     | /api/orders/stats/revenue      | USER         | Chiffre d'affaires (DELIVERED)    |
| GET     | /api/orders/stats/by-status    | USER         | Commandes par statut              |
| GET     | /api/orders/stats/top-products | USER         | Produits les plus commandés       |

## Difficultés Rencontrées

- **LazyInitializationException** : résolue avec `JOIN FETCH` dans les requêtes JPQL et `spring.jpa.open-in-view=false` pour rendre le problème explicite dès le développement.
- **JOIN FETCH + Pageable** : Spring Data ne peut pas dériver un `COUNT` depuis une requête avec `JOIN FETCH`. Résolu en ajoutant une `countQuery` séparée dans l'annotation `@Query`.
- **Doublons avec DISTINCT + ORDER BY** : `DISTINCT` en JPQL sur un `JOIN FETCH` de collection est incompatible avec `ORDER BY` en SQL. Résolu avec le hint `passDistinctThrough=false` (filtre côté Java uniquement).
- **Sérialisation des proxies LAZY** : hors transaction, Jackson tente de sérialiser les proxies Hibernate non initialisés → `LazyInitializationException`. Résolu avec le module `jackson-datatype-hibernate6` qui sérialise les proxies non initialisés comme `null`.
