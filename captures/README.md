# Captures d'écran - TP1

Ce dossier contient les captures d'écran des tests réalisés pour le TP1.

## 📸 Captures requises

### 1. Environnement (environnement.png)
- Versions de Java, Maven et Docker
- Commandes : `java -version`, `mvn -version`, `docker --version`

### 2. Tests Jakarta EE (jakarta-tests-*.png)
Minimum 5 endpoints testés :
- `GET /api/products` - Liste tous les produits
- `GET /api/products/{id}` - Récupération d'un produit
- `POST /api/products` - Création d'un produit
- `PUT /api/products/{id}` - Mise à jour
- `DELETE /api/products/{id}` - Suppression

### 3. Tests Spring Boot (spring-tests-*.png)
Minimum 5 endpoints testés :
- `GET /api/products` - Liste tous les produits
- `GET /api/products?category=X` - Filtrage par catégorie
- `POST /api/products` - Création
- `PATCH /api/products/{id}/stock` - Ajustement du stock
- `GET /api/products/count` - Comptage

### 4. Erreurs (errors-*.png) - Optionnel
- Test 404 (produit inexistant)
- Test 400 (validation échouée)

## 🛠️ Comment prendre les captures

### Avec Thunder Client (VS Code)
1. Installer l'extension Thunder Client
2. Importer les requêtes depuis `API_TESTS.http`
3. Exécuter chaque requête
4. Capturer l'écran avec la requête et la réponse visibles

### Avec REST Client (VS Code)
1. Installer l'extension REST Client
2. Ouvrir `API_TESTS.http`
3. Cliquer sur "Send Request"
4. Capturer la réponse

### Avec curl
1. Exécuter les commandes depuis le README.md
2. Capturer le terminal avec commande et résultat

## 📋 Nomenclature suggérée

```
environnement.png
jakarta-get-all.png
jakarta-get-by-id.png
jakarta-post-create.png
jakarta-put-update.png
jakarta-delete.png
spring-get-all.png
spring-get-by-category.png
spring-post-create.png
spring-patch-stock.png
spring-get-count.png
error-404.png
error-400-validation.png
```

## ✅ Checklist avant le rendu

- [ ] Capture des versions (environnement.png)
- [ ] Au moins 5 captures Jakarta EE
- [ ] Au moins 5 captures Spring Boot
- [ ] Images claires et lisibles
- [ ] Requête et réponse visibles sur chaque capture
- [ ] Code HTTP visible (200, 201, 404, etc.)

## 💡 Conseils

- Utilisez un format PNG pour une meilleure qualité
- Assurez-vous que le texte est lisible
- Incluez l'URL complète dans la capture
- Montrez le corps de la requête pour POST/PUT/PATCH
- Montrez le corps de la réponse JSON
