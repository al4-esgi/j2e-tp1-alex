#!/bin/bash

# ============================================
# Script de préparation du rendu TP1
# Crée l'archive TP1_NOM_Prenom.zip
# ============================================

set -e

# Couleurs
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_header() {
    echo -e "${BLUE}============================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}============================================${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

# Demander le nom et prénom
echo ""
print_header "Préparation du Rendu TP1"
echo ""

read -p "Entrez votre NOM : " nom
read -p "Entrez votre Prénom : " prenom

if [ -z "$nom" ] || [ -z "$prenom" ]; then
    print_error "Nom et prénom requis"
    exit 1
fi

# Nom de l'archive
ARCHIVE_NAME="TP1_${nom}_${prenom}.zip"

echo ""
print_header "Vérifications"

# Vérifier que les projets compilent
echo ""
echo "1. Vérification Jakarta EE..."
if [ -d "jakarta-products-api" ]; then
    cd jakarta-products-api
    if mvn clean package -q > /dev/null 2>&1; then
        print_success "Jakarta EE compile correctement"
    else
        print_error "Jakarta EE ne compile pas"
        echo "Essayez: cd jakarta-products-api && mvn clean package"
        exit 1
    fi
    cd ..
else
    print_error "Dossier jakarta-products-api non trouvé"
    exit 1
fi

echo ""
echo "2. Vérification Spring Boot..."
if [ -d "spring-products-api" ]; then
    cd spring-products-api
    if mvn clean package -q > /dev/null 2>&1; then
        print_success "Spring Boot compile correctement"
    else
        print_error "Spring Boot ne compile pas"
        echo "Essayez: cd spring-products-api && mvn clean package"
        exit 1
    fi
    cd ..
else
    print_error "Dossier spring-products-api non trouvé"
    exit 1
fi

echo ""
echo "3. Vérification des fichiers requis..."

REQUIRED_FILES=(
    "README.md"
    "ANALYSE.md"
    "jakarta-products-api/pom.xml"
    "spring-products-api/pom.xml"
)

missing=0
for file in "${REQUIRED_FILES[@]}"; do
    if [ -f "$file" ]; then
        print_success "$file présent"
    else
        print_error "$file manquant"
        missing=1
    fi
done

if [ $missing -eq 1 ]; then
    print_error "Fichiers manquants. Veuillez les créer avant de continuer."
    exit 1
fi

echo ""
echo "4. Vérification des captures d'écran..."
if [ -d "captures" ]; then
    nb_captures=$(find captures -type f \( -name "*.png" -o -name "*.jpg" -o -name "*.jpeg" \) | wc -l | tr -d ' ')
    if [ $nb_captures -ge 10 ]; then
        print_success "$nb_captures captures trouvées (minimum 10 requis)"
    else
        print_warning "$nb_captures captures trouvées (10 recommandées)"
        read -p "Continuer quand même ? (o/n) " confirm
        if [ "$confirm" != "o" ]; then
            exit 1
        fi
    fi
else
    print_warning "Dossier captures/ non trouvé"
    read -p "Continuer sans captures ? (o/n) " confirm
    if [ "$confirm" != "o" ]; then
        exit 1
    fi
fi

# Créer l'archive
echo ""
print_header "Création de l'archive"

# Supprimer l'ancienne archive si elle existe
if [ -f "$ARCHIVE_NAME" ]; then
    rm "$ARCHIVE_NAME"
    print_warning "Ancienne archive supprimée"
fi

echo ""
echo "Création de $ARCHIVE_NAME..."

# Créer un dossier temporaire
TEMP_DIR="TP1_${nom}_${prenom}"
rm -rf "$TEMP_DIR" 2>/dev/null || true
mkdir -p "$TEMP_DIR"

# Copier Jakarta EE
echo "Copie de jakarta-products-api..."
mkdir -p "$TEMP_DIR/jakarta-products-api"
rsync -a --exclude='target' \
         --exclude='.idea' \
         --exclude='*.iml' \
         --exclude='.DS_Store' \
         jakarta-products-api/ "$TEMP_DIR/jakarta-products-api/"

# Copier Spring Boot
echo "Copie de spring-products-api..."
mkdir -p "$TEMP_DIR/spring-products-api"
rsync -a --exclude='target' \
         --exclude='.idea' \
         --exclude='*.iml' \
         --exclude='.DS_Store' \
         spring-products-api/ "$TEMP_DIR/spring-products-api/"

# Copier les fichiers de documentation
echo "Copie des fichiers de documentation..."
cp README.md "$TEMP_DIR/" 2>/dev/null || print_warning "README.md non copié"
cp ANALYSE.md "$TEMP_DIR/" 2>/dev/null || print_warning "ANALYSE.md non copié"
cp API_TESTS.http "$TEMP_DIR/" 2>/dev/null || true
cp QUICKSTART.md "$TEMP_DIR/" 2>/dev/null || true

# Copier les captures
if [ -d "captures" ]; then
    echo "Copie des captures d'écran..."
    mkdir -p "$TEMP_DIR/captures"
    cp -r captures/* "$TEMP_DIR/captures/" 2>/dev/null || true
fi

# Créer l'archive
echo "Compression..."
zip -r "$ARCHIVE_NAME" "$TEMP_DIR" > /dev/null 2>&1

# Nettoyer
rm -rf "$TEMP_DIR"

# Vérifier la taille
ARCHIVE_SIZE=$(du -h "$ARCHIVE_NAME" | cut -f1)

echo ""
print_success "Archive créée : $ARCHIVE_NAME ($ARCHIVE_SIZE)"

# Contenu de l'archive
echo ""
print_header "Contenu de l'archive"
unzip -l "$ARCHIVE_NAME" | head -n 30

echo ""
echo "..."
echo ""
echo "Nombre total de fichiers : $(unzip -l "$ARCHIVE_NAME" | tail -1 | awk '{print $2}')"

# Checklist finale
echo ""
print_header "Checklist Finale"
echo ""
echo "Avant de soumettre, vérifiez :"
echo ""
echo "  [ ] Jakarta EE compile (vérifié ✓)"
echo "  [ ] Spring Boot compile (vérifié ✓)"
echo "  [ ] README.md personnalisé avec votre nom"
echo "  [ ] ANALYSE.md complété avec vos observations"
echo "  [ ] Au moins 10 captures d'écran"
echo "  [ ] Captures d'écran lisibles et annotées"
echo "  [ ] Tests réalisés sur les deux APIs"
echo ""

print_success "Rendu prêt à être soumis !"
echo ""
echo "Fichier : $ARCHIVE_NAME"
echo ""

# Option pour extraire et vérifier
read -p "Voulez-vous extraire l'archive pour vérification ? (o/n) " verify
if [ "$verify" = "o" ]; then
    VERIFY_DIR="verify_${nom}_${prenom}"
    rm -rf "$VERIFY_DIR" 2>/dev/null || true
    mkdir "$VERIFY_DIR"
    unzip -q "$ARCHIVE_NAME" -d "$VERIFY_DIR"
    echo ""
    print_success "Archive extraite dans $VERIFY_DIR/"
    echo ""
    echo "Vérifiez le contenu puis supprimez le dossier:"
    echo "  rm -rf $VERIFY_DIR"
fi

echo ""
print_success "Terminé ! Bon courage pour la suite ! 🚀"
