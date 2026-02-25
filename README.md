# TP1 - API REST avec Architecture en Couches

**Durée estimée** : 2h05  
**Niveau** : Intermédiaire  
**Technologies** : Jakarta EE 10, Spring Boot 3.2, Java 17, Maven, Docker

---

## 👤 Auteur

[Votre Nom]  
Master ESGI - JEE  
Date : [Date du TP]

---

## 📋 Description du Projet

Ce projet implémente une **API REST de gestion de produits** en utilisant deux approches :
1. **Jakarta EE 10** avec WildFly
2. **Spring Boot 3.2** avec Tomcat embarqué

Les deux applications partagent la même **architecture en 4 couches** :
- **Presentation Layer** : Endpoints REST (Resource/Controller)
- **Application Layer** : Logique métier (Service)
- **Infrastructure Layer** : Persistence (Repository)
- **Domain Layer** : Modèle de données (Product)

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────┐
│     Presentation Layer                  │
│  ProductResource / ProductController    │
│  (REST API - HTTP/JSON)                 │
└──────────────┬──────────────────────────┘
               │ dépend de
               ↓
┌─────────────────────────────────────────┐
│     Application Layer                   │
│         ProductService                  │
│  (Logique métier + Validations)         │
└──────────────┬──────────────────────────┘
               │ dépend de
               ↓
┌─────────────────────────────────────────┐
│     Infrastructure Layer                │
│      IProductRepository (interface)     │
│           ↑ implémente                  │
│  InMemoryProductRepository              │
│  (Persistence en mémoire)               │
└──────────────┬──────────────────────────┘
               │ utilise
               ↓
┌─────────────────────────────────────────┐
│          Domain Layer                   │
│            Product                      │
│  (Modèle de données)                    │
└─────────────────────────────────────────┘
```

---

## 📁 Structure du Projet

```
tp-1/
├── jakarta-products-api/          # Application Jakarta EE
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/formation/products/
│   │   │   │   ├── model/
│   │   │   │   │   └── Product.java
│   │   │   │   ├── repository/
│   │   │   │   │   ├── IProductRepository.java
│   │   │   │   │   └── InMemoryProductRepository.java
│   │   │   │   ├── service/
│   │   │   │   │   └── ProductService.java
│   │   │   │   ├── resource/
│   │   │   │   │   └── ProductResource.java
│   │   │   │   └── config/
│   │   │   │       └── ApplicationConfig.java
│   │   │   └── webapp/WEB-INF/
│   │   │       └── beans.xml
│   ├── pom.xml
│   ├── Dockerfile
│   └── docker-compose.yml
│
├── spring-products-api/           # Application Spring Boot
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/formation/springproducts/
│   │   │   │   ├── model/
│   │   │   │   │   └── Product.java
│   │   │   │   ├── repository/
│   │   │   │   │   ├── IProductRepository.java
│   │   │   │   │   └── InMemoryProductRepository.java
│   │   │   │   ├── service/
│   │   │   │   │   └── ProductService.java
│   │   │   │   ├── controller/
│   │   │   │   │   └── ProductController.java
│   │   │   │   └── SpringProductsApiApplication.java
│   │   │   └── resources/
│   │   │       └── application.properties
│   └── pom.xml
│
├── docker-compose-db.yml          # PostgreSQL (pour BONUS JPA)
├── ANALYSE.md                     # Analyse comparative
└── README.md                      # Ce fichier
```

---

## 🚀 Prérequis

Avant de commencer, assurez-vous d'avoir installé :

- **Java 17** (JDK)
- **Maven 3.9+**
- **Docker** et **Docker Compose**
- **Git**
- **IDE** : VS Code, IntelliJ IDEA, ou Eclipse

### Installation avec SDKMAN (recommandé)

```bash
# Installer SDKMAN
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

# Installer Java 17
sdk install java 17.0.10-tem
sdk default java 17.0.10-tem

# Installer Maven
sdk install maven 3.9.6

# Vérifier les installations
java -version
mvn -version
```

---

## 🔧 Installation et Lancement

### Option 1 : Jakarta EE avec WildFly

#### 1. Compiler le projet

```bash
cd jakarta-products-api
mvn clean package
```

#### 2. Lancer avec Docker Compose

```bash
docker compose up -d
```

#### 3. Vérifier le déploiement

```bash
# Voir les logs
docker compose logs -f

# L'application est prête quand vous voyez :
# "Deployed 'products-api.war'"
```

#### 4. Tester l'API

URL de base : `http://localhost:8080/products-api/api/products`

```bash
# Lister tous les produits
curl http://localhost:8080/products-api/api/products

# Récupérer un produit par ID
curl http://localhost:8080/products-api/api/products/{id}
```

#### 5. Arrêter l'application

```bash
docker compose down
```

---

### Option 2 : Spring Boot

#### 1. Lancer l'application

```bash
cd spring-products-api

# Option A : Avec Maven
mvn spring-boot:run

# Option B : Compiler puis exécuter
mvn clean package
java -jar target/spring-products-api-1.0.0.jar
```

#### 2. Vérifier le démarrage

L'application démarre sur le port **8081** (configuré dans `application.properties`).

Vous devriez voir dans les logs :
```
Started SpringProductsApiApplication in X.XXX seconds
```

#### 3. Tester l'API

URL de base : `http://localhost:8081/api/products`

```bash
# Lister tous les produits
curl http://localhost:8081/api/products

# Récupérer un produit par ID
curl http://localhost:8081/api/products/{id}
```

#### 4. Arrêter l'application

Appuyez sur `Ctrl+C` dans le terminal.

---

## 📡 Endpoints REST

Les deux applications exposent exactement les mêmes endpoints :

| Méthode HTTP | Endpoint | Description | Code Succès |
|--------------|----------|-------------|-------------|
| `GET` | `/api/products` | Liste tous les produits | 200 OK |
| `GET` | `/api/products?category=X` | Filtre par catégorie | 200 OK |
| `GET` | `/api/products/{id}` | Récupère un produit | 200 OK / 404 |
| `POST` | `/api/products` | Crée un produit | 201 CREATED |
| `PUT` | `/api/products/{id}` | Met à jour un produit | 200 OK / 404 |
| `PATCH` | `/api/products/{id}/stock` | Ajuste le stock | 200 OK / 404 |
| `DELETE` | `/api/products/{id}` | Supprime un produit | 204 NO CONTENT |
| `GET` | `/api/products/count` | Compte les produits | 200 OK |

---

## 🧪 Tests avec curl

### 1. Lister tous les produits

```bash
# Jakarta EE
curl http://localhost:8080/products-api/api/products

# Spring Boot
curl http://localhost:8081/api/products
```

### 2. Créer un nouveau produit

```bash
curl -X POST http://localhost:8081/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "MacBook Pro M3",
    "description": "Laptop professionnel Apple",
    "price": 2499.99,
    "category": "Informatique",
    "stock": 5
  }'
```

### 3. Récupérer un produit par ID

```bash
curl http://localhost:8081/api/products/{id}
```

### 4. Mettre à jour un produit

```bash
curl -X PUT http://localhost:8081/api/products/{id} \
  -H "Content-Type: application/json" \
  -d '{
    "name": "MacBook Pro M3 Updated",
    "description": "Laptop professionnel Apple - Mis à jour",
    "price": 2399.99,
    "category": "Informatique",
    "stock": 8
  }'
```

### 5. Ajuster le stock

```bash
curl -X PATCH http://localhost:8081/api/products/{id}/stock \
  -H "Content-Type: application/json" \
  -d '{
    "quantity": -2
  }'
```

### 6. Supprimer un produit

```bash
curl -X DELETE http://localhost:8081/api/products/{id}
```

### 7. Filtrer par catégorie

```bash
curl "http://localhost:8081/api/products?category=Informatique"
```

### 8. Compter les produits

```bash
curl http://localhost:8081/api/products/count
```

---

## 🧪 Tests avec Thunder Client (VS Code)

1. Installer l'extension **Thunder Client** dans VS Code
2. Créer une nouvelle collection "Products API"
3. Ajouter les requêtes ci-dessus
4. Tester les deux applications (ports 8080 et 8081)

---

## 🎁 BONUS : Ajouter PostgreSQL et JPA

### 1. Démarrer PostgreSQL

```bash
# À la racine du projet tp-1/
docker compose -f docker-compose-db.yml up -d

# Vérifier que PostgreSQL est démarré
docker compose -f docker-compose-db.yml ps
```

### 2. Configuration de la base de données

**Informations de connexion** :
- Host : `localhost`
- Port : `5432`
- Database : `productsdb`
- Username : `products_user`
- Password : `products_pass`

### 3. Activer JPA dans Spring Boot

Décommentez les lignes dans `spring-products-api/src/main/resources/application.properties` :

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/productsdb
spring.datasource.username=products_user
spring.datasource.password=products_pass
spring.jpa.hibernate.ddl-auto=update
```

### 4. Ajouter les annotations JPA sur Product

```java
@Entity
@Table(name = "products")
public class Product {
    @Id
    private String id;
    
    @Column(nullable = false)
    private String name;
    
    // ... reste du code
}
```

### 5. Créer JpaProductRepository

```java
@Repository
public class JpaProductRepository implements IProductRepository {
    @PersistenceContext
    private EntityManager entityManager;
    
    // Implémenter les méthodes avec EntityManager ou JPQL
}
```

**Note** : Le Service et le Controller ne changent pas ! C'est la force de l'architecture en couches.

---

## 🐛 Troubleshooting

### Jakarta EE ne démarre pas

```bash
# Vérifier les logs Docker
docker compose logs -f

# Vérifier que le WAR est créé
ls -lh target/products-api.war

# Rebuild si nécessaire
mvn clean package
docker compose up --build
```

### Spring Boot ne démarre pas

```bash
# Vérifier les dépendances
mvn dependency:tree

# Nettoyer et recompiler
mvn clean install

# Vérifier le port (doit être libre)
lsof -i :8081
```

### Port déjà utilisé

```bash
# Changer le port dans application.properties (Spring Boot)
server.port=8082

# Ou tuer le processus utilisant le port
kill -9 $(lsof -t -i:8081)
```

---

## 📊 Différences Clés

| Aspect | Jakarta EE | Spring Boot |
|--------|------------|-------------|
| Annotations injection | `@Inject` | Constructeur (recommandé) |
| Annotation service | `@ApplicationScoped` | `@Service` |
| Annotation controller | `@Path` + `@GET/POST` | `@RestController` + `@GetMapping` |
| Configuration | `beans.xml` | `application.properties` |
| Démarrage | ~20-30 sec | ~3-5 sec |
| Hot reload | Non (sauf JRebel) | Oui (DevTools) |

---

## 📚 Difficultés Rencontrées

### Difficulté 1 : [À compléter]

**Problème** : [Description du problème]

**Solution** : [Comment vous l'avez résolu]

### Difficulté 2 : [À compléter]

**Problème** : [Description du problème]

**Solution** : [Comment vous l'avez résolu]

---

## 💡 Points Clés Appris

1. **Architecture en couches** : La séparation en couches rend le code maintenable et testable

2. **Injection de dépendances** : Permet le découplage et facilite les tests

3. **Interface Repository** : Abstraction essentielle pour changer d'implémentation sans impacter le reste

4. **Principes SOLID** : S'appliquent concrètement dans ce projet (voir ANALYSE.md)

5. **Jakarta EE vs Spring Boot** : Deux approches différentes pour la même architecture

---

## 📖 Ressources Utiles

- [Jakarta EE Tutorial](https://eclipse-ee4j.github.io/jakartaee-tutorial/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [RESTful API Best Practices](https://restfulapi.net/)
- [SOLID Principles](https://www.digitalocean.com/community/conceptual-articles/s-o-l-i-d-the-first-five-principles-of-object-oriented-design)
- [Docker Documentation](https://docs.docker.com/)

---

## 📝 TODO / Améliorations Futures

- [ ] Ajouter des tests unitaires (JUnit 5 + Mockito)
- [ ] Implémenter JPA avec PostgreSQL (BONUS)
- [ ] Ajouter la validation Bean Validation (`@Valid`, `@NotNull`, etc.)
- [ ] Centraliser la gestion des erreurs avec `@ExceptionHandler`
- [ ] Ajouter Swagger/OpenAPI pour documenter l'API
- [ ] Implémenter la pagination pour `GET /products`
- [ ] Ajouter Spring Security pour l'authentification
- [ ] Conteneuriser Spring Boot avec Docker

---

## ✅ Livrables du TP

- [x] Application Jakarta EE fonctionnelle
- [x] Application Spring Boot fonctionnelle
- [x] Fichier ANALYSE.md avec comparaison détaillée
- [x] Dockerfile et docker-compose.yml
- [x] README.md avec instructions complètes
- [ ] Captures d'écran des tests (à ajouter dans `captures/`)

---

## 📧 Contact

Pour toute question sur ce projet :
- **Email** : [votre.email@example.com]
- **GitHub** : [votre-username]

---

**Bon courage ! 🚀**
