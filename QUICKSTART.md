# 🚀 Guide de Démarrage Rapide - TP1

Ce guide vous permet de démarrer rapidement les applications Jakarta EE et Spring Boot.

---

## ⚡ Démarrage Ultra-Rapide (2 minutes)

### Option A : Utiliser le script automatique

```bash
# Rendre le script exécutable
chmod +x run.sh

# Voir toutes les commandes disponibles
./run.sh help

# Démarrer Jakarta EE
./run.sh jakarta-start

# Démarrer Spring Boot (dans un autre terminal)
./run.sh spring-start
```

### Option B : Commandes manuelles

**Jakarta EE :**
```bash
cd jakarta-products-api
mvn clean package
docker compose up -d
```

**Spring Boot :**
```bash
cd spring-products-api
mvn spring-boot:run
```

---

## 🧪 Tests Rapides

### Tester Jakarta EE (port 8080)
```bash
curl http://localhost:8080/products-api/api/products
```

### Tester Spring Boot (port 8081)
```bash
curl http://localhost:8081/api/products
```

---

## 📝 Créer un Produit

### Jakarta EE
```bash
curl -X POST http://localhost:8080/products-api/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Mon Produit",
    "description": "Description du produit",
    "price": 99.99,
    "category": "Test",
    "stock": 10
  }'
```

### Spring Boot
```bash
curl -X POST http://localhost:8081/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Mon Produit",
    "description": "Description du produit",
    "price": 99.99,
    "category": "Test",
    "stock": 10
  }'
```

---

## 🔍 Tous les Endpoints Disponibles

| Endpoint | Méthode | Description |
|----------|---------|-------------|
| `/api/products` | GET | Liste tous les produits |
| `/api/products?category=X` | GET | Filtre par catégorie |
| `/api/products/{id}` | GET | Récupère un produit |
| `/api/products` | POST | Crée un produit |
| `/api/products/{id}` | PUT | Met à jour un produit |
| `/api/products/{id}/stock` | PATCH | Ajuste le stock |
| `/api/products/{id}` | DELETE | Supprime un produit |
| `/api/products/count` | GET | Compte les produits |

**URLs complètes :**
- Jakarta EE : `http://localhost:8080/products-api/api/products`
- Spring Boot : `http://localhost:8081/api/products`

---

## 🛑 Arrêter les Applications

### Jakarta EE
```bash
cd jakarta-products-api
docker compose down
```

### Spring Boot
```bash
# Ctrl+C dans le terminal
# Ou
./run.sh spring-stop
```

---

## 🐛 Problèmes Courants

### Port déjà utilisé

**Spring Boot (8081) :**
```bash
# Trouver le processus
lsof -i :8081

# Tuer le processus
kill -9 $(lsof -t -i:8081)
```

**Jakarta EE (8080) :**
```bash
docker compose down
docker ps  # Vérifier qu'aucun conteneur ne tourne
```

### L'API ne répond pas

**Jakarta EE :**
```bash
# Vérifier les logs
docker compose logs -f

# Attendre ~30 secondes après le démarrage
# WildFly prend du temps à déployer
```

**Spring Boot :**
```bash
# Vérifier que Maven a bien compilé
mvn clean package
```

### Erreur de compilation

```bash
# Nettoyer et recompiler
mvn clean install

# Vérifier Java
java -version  # Doit être >= 17

# Vérifier Maven
mvn -version   # Doit être >= 3.9
```

---

## 📊 Vérifier que Tout Fonctionne

### Test Complet Automatique
```bash
./run.sh test-all
```

### Test Manuel

1. **Jakarta EE** : Ouvrir http://localhost:8080/products-api/api/products
2. **Spring Boot** : Ouvrir http://localhost:8081/api/products

Vous devriez voir une liste JSON avec 5 produits de test.

---

## 🎯 Pour le Rendu du TP

1. **Tester tous les endpoints** (utilisez `API_TESTS.http`)
2. **Prendre des captures d'écran** (au moins 10 : 5 Jakarta + 5 Spring)
3. **Compléter ANALYSE.md** avec vos observations
4. **Vérifier que tout compile** :
   ```bash
   cd jakarta-products-api && mvn clean package && cd ..
   cd spring-products-api && mvn clean package && cd ..
   ```
5. **Créer l'archive** :
   ```bash
   zip -r TP1_NOM_Prenom.zip \
     jakarta-products-api/ \
     spring-products-api/ \
     ANALYSE.md \
     README.md \
     captures/
   ```

---

## 💡 Astuces

### Redémarrage Rapide Jakarta EE
```bash
./run.sh jakarta-restart
```

### Voir les Logs en Direct
```bash
# Jakarta EE
./run.sh jakarta-logs

# Spring Boot (si lancé en arrière-plan)
tail -f /tmp/spring-boot.log
```

### Utiliser jq pour Formater le JSON
```bash
# Installer jq
brew install jq  # macOS
sudo apt install jq  # Linux

# Utiliser avec curl
curl http://localhost:8081/api/products | jq
```

### Compter les Produits
```bash
curl http://localhost:8081/api/products/count
```

---

## 🎓 Comprendre l'Architecture

Chaque application suit cette architecture :

```
Controller/Resource (HTTP)
    ↓
Service (Logique métier)
    ↓
Repository Interface
    ↓
Repository Implémentation (En mémoire)
    ↓
Model (Product)
```

**Points clés :**
- Le Service ne connaît QUE l'interface Repository
- On peut changer l'implémentation sans toucher au Service
- C'est le principe d'Inversion de Dépendances (DIP)

---

## 📚 Aller Plus Loin

### Ajouter PostgreSQL (BONUS)
```bash
# Démarrer la base de données
./run.sh db-start

# Modifier application.properties
# Ajouter les annotations JPA sur Product
# Créer JpaProductRepository
```

### Ajouter des Tests Unitaires
```bash
# Dans pom.xml, ajouter JUnit et Mockito
# Créer src/test/java/...
# Tester le Service avec un mock Repository
```

### Activer DevTools (Spring Boot)
DevTools est déjà configuré ! 
Modifiez n'importe quel fichier Java et Spring Boot redémarre automatiquement.

---

## ✅ Checklist de Démarrage

- [ ] Java 17 installé
- [ ] Maven 3.9+ installé
- [ ] Docker installé et démarré
- [ ] Jakarta EE compile (mvn clean package)
- [ ] Jakarta EE démarre (docker compose up)
- [ ] Jakarta EE répond (curl http://localhost:8080/products-api/api/products)
- [ ] Spring Boot compile (mvn clean package)
- [ ] Spring Boot démarre (mvn spring-boot:run)
- [ ] Spring Boot répond (curl http://localhost:8081/api/products)
- [ ] Les deux retournent 5 produits

**Si toutes les cases sont cochées, vous êtes prêt pour le TP ! 🎉**

---

## 🆘 Support

En cas de problème :
1. Consultez les logs
2. Vérifiez les prérequis
3. Relisez les sections Troubleshooting
4. Nettoyez et recompilez : `./run.sh clean-all`

**Bon courage ! 💪**
