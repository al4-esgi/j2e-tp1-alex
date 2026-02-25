# 🎯 CHEATSHEET - TP1 Commandes Essentielles

**Aide-mémoire pour Jakarta EE et Spring Boot**

---

## 🚀 Démarrage Rapide

### Jakarta EE
```bash
cd jakarta-products-api
mvn clean package
docker compose up -d
```

### Spring Boot
```bash
cd spring-products-api
mvn spring-boot:run
```

### Avec script automatique
```bash
./run.sh jakarta-start
./run.sh spring-start
```

---

## 🔗 URLs des APIs

| Application | URL de base |
|-------------|-------------|
| Jakarta EE | `http://localhost:8080/products-api/api/products` |
| Spring Boot | `http://localhost:8081/api/products` |

---

## 📡 Tests curl - Jakarta EE (port 8080)

```bash
# GET - Liste tous les produits
curl http://localhost:8080/products-api/api/products

# GET - Produit par ID
curl http://localhost:8080/products-api/api/products/{id}

# GET - Filtre par catégorie
curl "http://localhost:8080/products-api/api/products?category=Informatique"

# POST - Créer un produit
curl -X POST http://localhost:8080/products-api/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","description":"Desc","price":99.99,"category":"Test","stock":10}'

# PUT - Mettre à jour
curl -X PUT http://localhost:8080/products-api/api/products/{id} \
  -H "Content-Type: application/json" \
  -d '{"name":"Updated","description":"New desc","price":149.99,"category":"Test","stock":15}'

# PATCH - Ajuster le stock
curl -X PATCH http://localhost:8080/products-api/api/products/{id}/stock \
  -H "Content-Type: application/json" \
  -d '{"quantity":-3}'

# DELETE - Supprimer
curl -X DELETE http://localhost:8080/products-api/api/products/{id}

# GET - Compter
curl http://localhost:8080/products-api/api/products/count
```

---

## 📡 Tests curl - Spring Boot (port 8081)

```bash
# GET - Liste tous les produits
curl http://localhost:8081/api/products

# GET - Produit par ID
curl http://localhost:8081/api/products/{id}

# GET - Filtre par catégorie
curl "http://localhost:8081/api/products?category=Informatique"

# POST - Créer un produit
curl -X POST http://localhost:8081/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","description":"Desc","price":99.99,"category":"Test","stock":10}'

# PUT - Mettre à jour
curl -X PUT http://localhost:8081/api/products/{id} \
  -H "Content-Type: application/json" \
  -d '{"name":"Updated","description":"New desc","price":149.99,"category":"Test","stock":15}'

# PATCH - Ajuster le stock
curl -X PATCH http://localhost:8081/api/products/{id}/stock \
  -H "Content-Type: application/json" \
  -d '{"quantity":5}'

# DELETE - Supprimer
curl -X DELETE http://localhost:8081/api/products/{id}

# GET - Compter
curl http://localhost:8081/api/products/count
```

---

## 🛠️ Commandes Maven

```bash
# Compiler
mvn clean package

# Compiler sans tests
mvn clean package -DskipTests

# Nettoyer
mvn clean

# Lancer Spring Boot
mvn spring-boot:run

# Vérifier les dépendances
mvn dependency:tree

# Mise à jour des dépendances
mvn clean install -U
```

---

## 🐳 Commandes Docker

```bash
# Jakarta EE - Démarrer
docker compose up -d

# Jakarta EE - Arrêter
docker compose down

# Jakarta EE - Logs
docker compose logs -f

# Jakarta EE - Rebuild
docker compose up -d --build

# PostgreSQL - Démarrer (BONUS)
docker compose -f docker-compose-db.yml up -d

# PostgreSQL - Arrêter
docker compose -f docker-compose-db.yml down

# Voir les conteneurs actifs
docker ps

# Supprimer tous les conteneurs
docker compose down -v
```

---

## 🔍 Commandes de Débogage

```bash
# Voir les processus sur un port
lsof -i :8080  # Jakarta EE
lsof -i :8081  # Spring Boot
lsof -i :5432  # PostgreSQL

# Tuer un processus sur un port
kill -9 $(lsof -t -i:8081)

# Vérifier Java
java -version

# Vérifier Maven
mvn -version

# Vérifier Docker
docker --version
docker ps
```

---

## 📋 Script run.sh - Commandes

```bash
# Aide
./run.sh help

# Jakarta EE
./run.sh jakarta-build      # Compiler
./run.sh jakarta-start      # Démarrer
./run.sh jakarta-stop       # Arrêter
./run.sh jakarta-logs       # Logs
./run.sh jakarta-restart    # Redémarrer
./run.sh jakarta-test       # Tester l'API

# Spring Boot
./run.sh spring-start       # Démarrer
./run.sh spring-stop        # Arrêter
./run.sh spring-test        # Tester l'API

# Base de données
./run.sh db-start          # Démarrer PostgreSQL
./run.sh db-stop           # Arrêter PostgreSQL
./run.sh db-logs           # Logs PostgreSQL

# Tests
./run.sh test-all          # Tester les 2 APIs

# Nettoyage
./run.sh clean             # Nettoyer builds
./run.sh clean-all         # Nettoyer tout
```

---

## 📦 Préparer le Rendu

```bash
# Script automatique
chmod +x prepare-rendu.sh
./prepare-rendu.sh

# Manuel
zip -r TP1_NOM_Prenom.zip \
  jakarta-products-api/ \
  spring-products-api/ \
  ANALYSE.md \
  README.md \
  captures/ \
  -x "*/target/*" "*/.idea/*" "*/.DS_Store"
```

---

## 🔑 Annotations Importantes

### Jakarta EE
```java
@ApplicationScoped    // Bean CDI (singleton application)
@Inject              // Injection de dépendances
@Path("/products")   // Chemin REST
@GET, @POST, @PUT    // Méthodes HTTP
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@PathParam("id")     // Paramètre d'URL
@QueryParam("cat")   // Paramètre de requête
```

### Spring Boot
```java
@SpringBootApplication  // Application principale
@Service               // Service métier
@Repository            // Repository de données
@RestController        // Contrôleur REST
@RequestMapping("/api")
@GetMapping, @PostMapping, @PutMapping, @PatchMapping, @DeleteMapping
@PathVariable          // Paramètre d'URL
@RequestParam          // Paramètre de requête
@RequestBody           // Corps de la requête
```

---

## 🏗️ Structure des Couches

```
Presentation  → ProductResource / ProductController
Application   → ProductService
Infrastructure → IProductRepository (interface)
               → InMemoryProductRepository (impl)
Domain        → Product
```

**Dépendances :**
- Presentation dépend de Application
- Application dépend de Infrastructure (interface)
- Infrastructure dépend de Domain

---

## ✅ Codes HTTP

| Code | Signification | Usage |
|------|---------------|-------|
| 200 | OK | GET, PUT réussis |
| 201 | Created | POST réussi |
| 204 | No Content | DELETE réussi |
| 400 | Bad Request | Validation échouée |
| 404 | Not Found | Ressource inexistante |
| 500 | Server Error | Erreur serveur |

---

## 🔄 Format JSON - Produit

```json
{
  "id": "uuid-generated",
  "name": "Nom du produit",
  "description": "Description optionnelle",
  "price": 99.99,
  "category": "Catégorie",
  "stock": 10,
  "createdAt": "2024-01-28T16:00:00"
}
```

---

## 🧪 Tests Essentiels à Réaliser

### Jakarta EE (5 minimum)
- [ ] GET /products (liste)
- [ ] GET /products/{id} (récupération)
- [ ] POST /products (création)
- [ ] PUT /products/{id} (mise à jour)
- [ ] DELETE /products/{id} (suppression)

### Spring Boot (5 minimum)
- [ ] GET /products (liste)
- [ ] GET /products?category=X (filtre)
- [ ] POST /products (création)
- [ ] PATCH /products/{id}/stock (stock)
- [ ] GET /products/count (comptage)

---

## 🎓 Principes SOLID

| Principe | Signification | Application |
|----------|---------------|-------------|
| **S**RP | Single Responsibility | 1 classe = 1 responsabilité |
| **O**CP | Open/Closed | Ouvert extension, fermé modification |
| **L**SP | Liskov Substitution | Implémentations interchangeables |
| **I**SP | Interface Segregation | Interfaces petites et ciblées |
| **D**IP | Dependency Inversion | Dépendre d'abstractions |

---

## 📸 Captures Requises

```
captures/
├── environnement.png           # java -version, mvn -version
├── jakarta-get-all.png
├── jakarta-get-by-id.png
├── jakarta-post.png
├── jakarta-put.png
├── jakarta-delete.png
├── spring-get-all.png
├── spring-filter.png
├── spring-post.png
├── spring-patch-stock.png
└── spring-count.png
```

---

## 💡 Tips

### Formater le JSON avec jq
```bash
curl http://localhost:8081/api/products | jq
```

### Voir uniquement les noms de produits
```bash
curl -s http://localhost:8081/api/products | jq -r '.[].name'
```

### Enregistrer la réponse dans un fichier
```bash
curl http://localhost:8081/api/products > products.json
```

### Mesurer le temps de réponse
```bash
curl -w "\nTemps: %{time_total}s\n" http://localhost:8081/api/products
```

---

## 🚨 Troubleshooting Express

| Problème | Solution |
|----------|----------|
| Port 8080 occupé | `docker compose down` |
| Port 8081 occupé | `kill -9 $(lsof -t -i:8081)` |
| WAR non créé | `mvn clean package` |
| WildFly ne démarre pas | Attendre 30 sec, voir logs |
| Spring Boot erreur | Vérifier Java 17+ |
| Injection ne fonctionne pas | Vérifier annotations |

---

## 📚 Fichiers Importants

| Fichier | Description |
|---------|-------------|
| `README.md` | Guide principal complet |
| `QUICKSTART.md` | Démarrage en 2 minutes |
| `ANALYSE.md` | Comparaison Jakarta vs Spring |
| `API_TESTS.http` | Collection de tests HTTP |
| `CHEATSHEET.md` | Ce fichier |
| `run.sh` | Script d'automatisation |
| `prepare-rendu.sh` | Préparation du ZIP |

---

**🎯 Tout ce dont tu as besoin en une page !**
