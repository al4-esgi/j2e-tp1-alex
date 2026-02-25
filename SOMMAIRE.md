# 📋 SOMMAIRE - TP1 API REST avec Architecture en Couches

**Projet** : Gestion de Produits avec Jakarta EE et Spring Boot  
**Architecture** : 4 couches (Presentation, Application, Infrastructure, Domain)  
**Durée** : 2h05

---

## 📂 Structure du Projet

```
tp-1/
├── 📄 README.md                    # Guide principal (instructions complètes)
├── 📄 QUICKSTART.md                # Démarrage rapide (2 minutes)
├── 📄 ANALYSE.md                   # Analyse comparative Jakarta EE vs Spring Boot
├── 📄 API_TESTS.http               # Collection de tests HTTP
├── 📄 SOMMAIRE.md                  # Ce fichier
├── 🔧 run.sh                       # Script d'automatisation
├── 🐳 docker-compose-db.yml        # PostgreSQL (BONUS)
├── 📸 captures/                    # Captures d'écran pour le rendu
│   └── README.md
├── 📦 jakarta-products-api/        # Application Jakarta EE
│   ├── pom.xml
│   ├── Dockerfile
│   ├── docker-compose.yml
│   └── src/main/
│       ├── java/com/formation/products/
│       │   ├── model/Product.java
│       │   ├── repository/
│       │   │   ├── IProductRepository.java
│       │   │   └── InMemoryProductRepository.java
│       │   ├── service/ProductService.java
│       │   ├── resource/ProductResource.java
│       │   └── config/ApplicationConfig.java
│       └── webapp/WEB-INF/beans.xml
└── 📦 spring-products-api/         # Application Spring Boot
    ├── pom.xml
    └── src/main/
        ├── java/com/formation/springproducts/
        │   ├── model/Product.java
        │   ├── repository/
        │   │   ├── IProductRepository.java
        │   │   └── InMemoryProductRepository.java
        │   ├── service/ProductService.java
        │   ├── controller/ProductController.java
        │   └── SpringProductsApiApplication.java
        └── resources/application.properties
```

---

## 🚀 Démarrage Rapide

### Jakarta EE (WildFly + Docker)
```bash
cd jakarta-products-api
mvn clean package
docker compose up -d
# API: http://localhost:8080/products-api/api/products
```

### Spring Boot (Tomcat embarqué)
```bash
cd spring-products-api
mvn spring-boot:run
# API: http://localhost:8081/api/products
```

### Avec le script automatique
```bash
chmod +x run.sh
./run.sh jakarta-start    # Jakarta EE
./run.sh spring-start     # Spring Boot
./run.sh test-all         # Tester les deux
```

---

## 🏗️ Architecture des Applications

Les deux applications partagent **exactement la même architecture** :

```
┌─────────────────────────────────────┐
│  Presentation Layer                 │
│  - ProductResource (Jakarta EE)     │
│  - ProductController (Spring Boot)  │
│  Rôle: Gérer HTTP/REST              │
└──────────────┬──────────────────────┘
               │ @Inject / Constructeur
               ↓
┌─────────────────────────────────────┐
│  Application Layer                  │
│  - ProductService                   │
│  Rôle: Logique métier + Validations │
└──────────────┬──────────────────────┘
               │ @Inject / Constructeur
               ↓
┌─────────────────────────────────────┐
│  Infrastructure Layer               │
│  - IProductRepository (Interface)   │
│  - InMemoryProductRepository        │
│  Rôle: Persistence (en mémoire)     │
└──────────────┬──────────────────────┘
               │ utilise
               ↓
┌─────────────────────────────────────┐
│  Domain Layer                       │
│  - Product                          │
│  Rôle: Modèle de données            │
└─────────────────────────────────────┘
```

### Principes Appliqués
- ✅ **SRP** : Chaque classe a une responsabilité unique
- ✅ **OCP** : Ouvert à l'extension, fermé à la modification
- ✅ **LSP** : Toute implémentation de IProductRepository est substituable
- ✅ **ISP** : Interface Repository bien découpée
- ✅ **DIP** : Service dépend de l'interface, pas de l'implémentation

---

## 📡 API REST - Endpoints

| Méthode | Endpoint | Description | Code Réussite |
|---------|----------|-------------|---------------|
| GET | `/api/products` | Liste tous les produits | 200 |
| GET | `/api/products?category=X` | Filtre par catégorie | 200 |
| GET | `/api/products/{id}` | Récupère un produit | 200 / 404 |
| POST | `/api/products` | Crée un produit | 201 + Location |
| PUT | `/api/products/{id}` | Met à jour un produit | 200 / 404 |
| PATCH | `/api/products/{id}/stock` | Ajuste le stock | 200 / 404 |
| DELETE | `/api/products/{id}` | Supprime un produit | 204 |
| GET | `/api/products/count` | Compte les produits | 200 |

**URLs complètes :**
- Jakarta EE : `http://localhost:8080/products-api/api/products`
- Spring Boot : `http://localhost:8081/api/products`

---

## 🔑 Différences Clés Jakarta EE vs Spring Boot

| Aspect | Jakarta EE | Spring Boot |
|--------|------------|-------------|
| **Annotations Service** | `@ApplicationScoped` | `@Service` |
| **Annotations Repository** | `@ApplicationScoped` | `@Repository` |
| **Injection** | `@Inject` | Constructeur (recommandé) |
| **Controller** | `@Path` + `@GET/POST` | `@RestController` + `@GetMapping` |
| **Configuration** | `beans.xml` | `application.properties` |
| **Packaging** | WAR (10 KB) | JAR (20-30 MB) |
| **Serveur** | WildFly externe | Tomcat embarqué |
| **Démarrage** | ~20-30 sec | ~3-5 sec |
| **Hot Reload** | Non (sauf JRebel) | Oui (DevTools) |

---

## 🧪 Tests Essentiels

### 1. Lister les produits
```bash
curl http://localhost:8081/api/products
```

### 2. Créer un produit
```bash
curl -X POST http://localhost:8081/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Nouveau Produit",
    "description": "Test de création",
    "price": 149.99,
    "category": "Test",
    "stock": 20
  }'
```

### 3. Filtrer par catégorie
```bash
curl "http://localhost:8081/api/products?category=Informatique"
```

### 4. Ajuster le stock
```bash
curl -X PATCH http://localhost:8081/api/products/{id}/stock \
  -H "Content-Type: application/json" \
  -d '{"quantity": -5}'
```

---

## 📸 Captures d'écran Requises

### Livrable 1 : Environnement
- Screenshot de `java -version`, `mvn -version`, `docker --version`

### Livrable 2 : Jakarta EE (5 captures minimum)
- GET /products (liste)
- GET /products/{id} (récupération)
- POST /products (création)
- PUT /products/{id} (mise à jour)
- DELETE /products/{id} (suppression)

### Livrable 3 : Spring Boot (5 captures minimum)
- GET /products (liste)
- GET /products?category=X (filtre)
- POST /products (création)
- PATCH /products/{id}/stock (stock)
- GET /products/count (comptage)

### Livrable 4 : Erreurs (optionnel)
- 404 Not Found
- 400 Bad Request (validation)

**Emplacement** : Dossier `captures/`

---

## 📝 Livrables du TP

### Fichiers à Rendre

```
TP1_NOM_Prenom.zip
├── jakarta-products-api/
│   ├── src/
│   ├── pom.xml
│   ├── Dockerfile
│   └── docker-compose.yml
├── spring-products-api/
│   ├── src/
│   └── pom.xml
├── ANALYSE.md                  # ⚠️ IMPORTANT - À compléter
├── README.md
└── captures/
    ├── environnement.png
    ├── jakarta-*.png (5 minimum)
    └── spring-*.png (5 minimum)
```

### Sections à Compléter dans ANALYSE.md

1. **Tableau comparatif** : Observations personnelles
2. **Architecture** : Les deux apps ont-elles la même structure ?
3. **Injection** : Interface vs classe concrète
4. **SOLID** : Exemples concrets dans votre code
5. **Tests** : Comment tester sans DB ?
6. **Évolution JPA** : Quelles classes modifier/créer/ne pas toucher ?
7. **Difficultés rencontrées** : Vos problèmes et solutions
8. **Points clés appris** : 3-5 points essentiels

---

## 🎯 Commandes Utiles

### Compilation
```bash
# Jakarta EE
cd jakarta-products-api && mvn clean package

# Spring Boot
cd spring-products-api && mvn clean package
```

### Démarrage
```bash
# Jakarta EE
./run.sh jakarta-start

# Spring Boot
./run.sh spring-start
```

### Tests
```bash
# Automatique
./run.sh test-all

# Manuel Jakarta EE
curl http://localhost:8080/products-api/api/products

# Manuel Spring Boot
curl http://localhost:8081/api/products
```

### Arrêt
```bash
# Jakarta EE
./run.sh jakarta-stop

# Spring Boot
./run.sh spring-stop
# ou Ctrl+C
```

### Logs
```bash
# Jakarta EE
./run.sh jakarta-logs

# Spring Boot
# Visible dans le terminal ou tail -f /tmp/spring-boot.log
```

### Nettoyage
```bash
./run.sh clean      # Nettoie les builds
./run.sh clean-all  # Nettoie tout + Docker
```

---

## 🐛 Troubleshooting Rapide

### Port déjà utilisé
```bash
# Voir ce qui utilise le port
lsof -i :8080  # Jakarta EE
lsof -i :8081  # Spring Boot

# Tuer le processus
kill -9 $(lsof -t -i:8081)
```

### WildFly ne démarre pas
```bash
docker compose logs -f
docker compose down && docker compose up -d --build
```

### Spring Boot ne compile pas
```bash
mvn clean install -U
java -version  # Vérifier Java >= 17
```

### L'API ne répond pas
- Jakarta EE : Attendre ~30 sec après démarrage
- Spring Boot : Vérifier les logs pour erreurs
- Les deux : Vérifier que le port n'est pas bloqué par le firewall

---

## 💡 Points Importants à Retenir

### 1. Architecture Indépendante du Framework
Le même design fonctionne avec Jakarta EE ET Spring Boot.
Seules les annotations changent, pas la structure.

### 2. Interface = Flexibilité
`ProductService` dépend de `IProductRepository`, pas de l'implémentation.
→ On peut changer de BDD sans toucher au Service !

### 3. Injection de Dépendances
Évite le couplage fort. Pas de `new` dans le code (sauf Model).

### 4. Principes SOLID en Action
- **SRP** : Product (données), Service (métier), Repository (persistence)
- **DIP** : Dépendance vers abstraction (interface)
- **OCP** : Peut ajouter JpaRepository sans modifier le code existant

### 5. REST Best Practices
- GET pour lecture
- POST pour création (201 + Location header)
- PUT pour mise à jour complète
- PATCH pour mise à jour partielle
- DELETE pour suppression (204 No Content)
- Codes HTTP appropriés (200, 201, 204, 404, 400)

---

## 🎓 Pour Approfondir

### BONUS : Ajouter JPA
```bash
# Démarrer PostgreSQL
./run.sh db-start

# Modifier Product.java (ajouter @Entity, @Id, etc.)
# Créer JpaProductRepository implements IProductRepository
# Configurer application.properties
```

**Observation** : Le Service et Controller NE CHANGENT PAS !
C'est la force de l'architecture en couches.

### Ressources Utiles
- [Jakarta EE Tutorial](https://eclipse-ee4j.github.io/jakartaee-tutorial/)
- [Spring Boot Docs](https://spring.io/projects/spring-boot)
- [SOLID Principles](https://www.digitalocean.com/community/conceptual-articles/s-o-l-i-d-the-first-five-principles-of-object-oriented-design)
- [REST API Best Practices](https://restfulapi.net/)

---

## ✅ Checklist Finale

**Avant de rendre le TP :**

- [ ] Jakarta EE compile et démarre
- [ ] Spring Boot compile et démarre
- [ ] Les deux APIs retournent les produits de test
- [ ] Au moins 5 endpoints testés sur chaque API
- [ ] Captures d'écran prises (10 minimum)
- [ ] ANALYSE.md complété avec vos observations
- [ ] README.md personnalisé (nom, difficultés, apprentissages)
- [ ] Archive créée : `TP1_NOM_Prenom.zip`
- [ ] Vérifié que l'archive contient tout

---

## 📧 Support

**En cas de problème :**
1. Consultez QUICKSTART.md
2. Vérifiez les logs
3. Essayez `./run.sh clean-all` puis recommencez
4. Consultez la section Troubleshooting

---

**Bon courage ! 🚀**

*Ce TP démontre qu'une bonne architecture résiste au changement de technologie.*
