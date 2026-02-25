# TP2 - Persistence avec JPA

## Auteur
Alex

## Description
API REST avec persistence JPA complète incluant :
- 5 entités JPA (Product, Category, Supplier, Order, OrderItem)
- Relations complexes (@ManyToOne, @OneToMany)
- Requêtes JPQL optimisées (JOIN FETCH, agrégations, sous-requêtes)
- Transactions gérées avec @Transactional
- Tests complets via Thunder Client

## Stack Technique
- Java 17
- Spring Boot 3.2.2
- Spring Data JPA / Hibernate 6.4
- PostgreSQL 16
- Docker

## Modèle de Données

```
categories         products              suppliers
----------         --------              ---------
id (PK)     <──   id (PK)         ┌──>  id (PK)
name               name            │     name
description        description     │     email
                   price           │     phone
                   stock           │
                   category_id (FK)│
                   supplier_id (FK)┘

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

## Lancement

### Avec Docker Compose (recommandé)
```bash
docker-compose up -d
```

### En local (PostgreSQL requis)
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

L'API est disponible sur `http://localhost:8081/api/`

## Endpoints

### Products
| Méthode | URL | Description |
|---------|-----|-------------|
| GET | /api/products | Liste optimisée (JOIN FETCH) |
| GET | /api/products/slow | Démo problème N+1 |
| GET | /api/products/fast | Démo 1 requête SQL |
| GET | /api/products/{id} | Produit par ID |
| POST | /api/products | Créer un produit |
| PUT | /api/products/{id} | Mettre à jour un produit |
| DELETE | /api/products/{id} | Supprimer un produit |
| PATCH | /api/products/{id}/stock | Ajuster le stock |
| GET | /api/products/stats/count-by-category | Nb produits par catégorie |
| GET | /api/products/stats/avg-price-by-category | Prix moyen par catégorie |
| GET | /api/products/stats/category-stats | Stats DTO projetées |
| GET | /api/products/stats/top-expensive | Top produits chers |
| GET | /api/products/never-ordered | Produits jamais commandés |
| POST | /api/products/transfer | Transférer vers une catégorie |

### Categories
| Méthode | URL | Description |
|---------|-----|-------------|
| GET | /api/categories | Liste toutes les catégories |
| GET | /api/categories/{id} | Catégorie par ID |
| GET | /api/categories/{id}/products | Catégorie avec ses produits |
| POST | /api/categories | Créer une catégorie |
| PUT | /api/categories/{id} | Mettre à jour une catégorie |
| DELETE | /api/categories/{id} | Supprimer (+ produits en cascade) |
| GET | /api/categories/count | Nombre de catégories |

### Suppliers
| Méthode | URL | Description |
|---------|-----|-------------|
| GET | /api/suppliers | Liste tous les fournisseurs |
| GET | /api/suppliers/{id} | Fournisseur par ID |
| POST | /api/suppliers | Créer un fournisseur |
| PUT | /api/suppliers/{id} | Mettre à jour un fournisseur |
| DELETE | /api/suppliers/{id} | Supprimer (délie les produits) |
| GET | /api/suppliers/count | Nombre de fournisseurs |

### Orders
| Méthode | URL | Description |
|---------|-----|-------------|
| GET | /api/orders | Liste toutes les commandes |
| GET | /api/orders/full | Commandes avec items (JOIN FETCH) |
| GET | /api/orders/{id} | Commande par ID |
| GET | /api/orders/{id}/items | Commande avec ses items |
| POST | /api/orders | Créer une commande complète |
| PATCH | /api/orders/{id}/status | Mettre à jour le statut |
| DELETE | /api/orders/{id} | Supprimer une commande |
| GET | /api/orders/count | Nombre de commandes |
| GET | /api/orders/stats/revenue | Chiffre d'affaires (DELIVERED) |
| GET | /api/orders/stats/by-status | Commandes par statut |
| GET | /api/orders/stats/top-products | Produits les plus commandés |

## Tests Effectués

- [x] CRUD complet sur toutes les entités
- [x] Relations bidirectionnelles fonctionnelles
- [x] Transactions avec rollback (stock insuffisant, produit inexistant)
- [x] Requêtes d'agrégation (COUNT, AVG, SUM, GROUP BY)
- [x] Optimisation N+1 (comparaison /slow vs /fast)
- [x] DTOs pour projections (CategoryStats avec SELECT NEW)
- [x] Cycle de vie des commandes (PENDING → CONFIRMED → SHIPPED → DELIVERED)
- [x] Sous-requêtes JPQL (produits jamais commandés)

## Captures

| Fichier | Contenu |
|---------|---------|
| tables-postgresql.png | Tables générées par Hibernate dans PostgreSQL |
| hibernate-logs.png | Logs SQL au démarrage (création des tables) |
| n+1-problem.png | Comparaison requêtes /slow vs /fast dans les logs |
| requetes-agregation.png | Réponses des endpoints de statistiques |
| tests-thunder-client.png | Tests CRUD complets via Thunder Client |

## Difficultés Rencontrées

- **LazyInitializationException** : résolue en ajoutant `JOIN FETCH` dans les requêtes JPQL et en configurant `spring.jpa.open-in-view=false` pour rendre le problème explicite.
- **Doublons avec JOIN FETCH sur collection** : résolue avec `DISTINCT` + `hibernate.query.passDistinctThrough=false` pour ne pas propager le DISTINCT au SQL (incompatible avec ORDER BY).
- **Suppression de fournisseur avec produits liés** : pas de cascade sur `Supplier.products`, il faut délier manuellement les produits avant la suppression pour éviter une violation de contrainte FK.
- **Prix immuable dans les commandes** : `unitPrice` copié depuis `product.getPrice()` au moment de la commande pour conserver l'historique même si le prix du produit change ensuite.

## Points Clés Appris

1. **Fetch LAZY par défaut** : toutes les relations `@ManyToOne` et `@OneToMany` sont LAZY. Il faut `JOIN FETCH` explicite dans les requêtes JPQL pour les charger sans N+1.
2. **Dirty checking** : modifier une entité dans une transaction `@Transactional` suffit pour que Hibernate génère l'UPDATE au flush. `save()` n'est pas toujours nécessaire.
3. **Côté propriétaire de la relation** : la clé étrangère est toujours du côté qui n'a pas `mappedBy`. C'est ce côté qui doit être mis à jour pour que la relation soit persistée.
4. **CASCADE vs orphanRemoval** : `cascade = ALL` propage les opérations JPA aux enfants ; `orphanRemoval = true` supprime en base un enfant retiré de la collection parente.
5. **SELECT NEW en JPQL** : permet de projeter directement dans un DTO typé plutôt que de manipuler des `Object[]` non typés.
