#!/bin/bash

# ============================================
# Script de gestion des applications TP1
# Jakarta EE et Spring Boot
# ============================================

set -e

# Couleurs pour l'affichage
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Fonction d'affichage
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

# Fonction d'aide
show_help() {
    cat << EOF
Usage: ./run.sh [COMMAND]

Commandes disponibles:

  help              Affiche cette aide

  Jakarta EE:
    jakarta-build     Compile l'application Jakarta EE
    jakarta-start     Démarre Jakarta EE avec Docker
    jakarta-stop      Arrête Jakarta EE
    jakarta-logs      Affiche les logs Jakarta EE
    jakarta-restart   Redémarre Jakarta EE
    jakarta-test      Teste l'API Jakarta EE

  Spring Boot:
    spring-start      Démarre Spring Boot
    spring-stop       Arrête Spring Boot
    spring-test       Teste l'API Spring Boot

  Base de données:
    db-start          Démarre PostgreSQL (pour BONUS JPA)
    db-stop           Arrête PostgreSQL
    db-logs           Affiche les logs PostgreSQL

  Tests:
    test-all          Teste les deux APIs

  Nettoyage:
    clean             Nettoie tous les builds
    clean-all         Nettoie tout (builds + Docker)

Exemples:
  ./run.sh jakarta-start    # Démarre Jakarta EE
  ./run.sh spring-start     # Démarre Spring Boot
  ./run.sh test-all         # Teste les deux APIs

EOF
}

# Vérifier les prérequis
check_prerequisites() {
    local missing=0

    if ! command -v java &> /dev/null; then
        print_error "Java n'est pas installé"
        missing=1
    fi

    if ! command -v mvn &> /dev/null; then
        print_error "Maven n'est pas installé"
        missing=1
    fi

    if ! command -v docker &> /dev/null; then
        print_error "Docker n'est pas installé"
        missing=1
    fi

    if [ $missing -eq 1 ]; then
        print_error "Veuillez installer les prérequis manquants"
        exit 1
    fi
}

# Jakarta EE - Build
jakarta_build() {
    print_header "Compilation Jakarta EE"
    cd jakarta-products-api
    mvn clean package
    print_success "Compilation terminée"
    cd ..
}

# Jakarta EE - Start
jakarta_start() {
    print_header "Démarrage Jakarta EE avec WildFly"

    if [ ! -f "jakarta-products-api/target/products-api.war" ]; then
        print_warning "WAR non trouvé. Compilation en cours..."
        jakarta_build
    fi

    cd jakarta-products-api
    docker compose up -d
    cd ..

    print_success "WildFly démarré sur http://localhost:8080"
    print_warning "Attendez ~20-30 secondes pour le déploiement complet"
    echo ""
    echo "API disponible sur: http://localhost:8080/products-api/api/products"
    echo "Logs: ./run.sh jakarta-logs"
}

# Jakarta EE - Stop
jakarta_stop() {
    print_header "Arrêt Jakarta EE"
    cd jakarta-products-api
    docker compose down
    cd ..
    print_success "Jakarta EE arrêté"
}

# Jakarta EE - Logs
jakarta_logs() {
    print_header "Logs Jakarta EE"
    cd jakarta-products-api
    docker compose logs -f
    cd ..
}

# Jakarta EE - Restart
jakarta_restart() {
    print_header "Redémarrage Jakarta EE"
    jakarta_stop
    sleep 2
    jakarta_build
    jakarta_start
}

# Jakarta EE - Test
jakarta_test() {
    print_header "Test API Jakarta EE"

    local BASE_URL="http://localhost:8080/products-api/api/products"

    echo "Test GET /products..."
    if curl -s -f "${BASE_URL}" > /dev/null; then
        print_success "GET /products OK"
        echo ""
        echo "Produits disponibles:"
        curl -s "${BASE_URL}" | jq -r '.[] | "  - \(.name) (\(.category))"' 2>/dev/null || curl -s "${BASE_URL}"
    else
        print_error "GET /products FAILED"
        print_warning "Assurez-vous que Jakarta EE est démarré: ./run.sh jakarta-start"
    fi
}

# Spring Boot - Start
spring_start() {
    print_header "Démarrage Spring Boot"

    cd spring-products-api

    # Vérifier si un processus Spring Boot tourne déjà
    if lsof -ti:8081 > /dev/null 2>&1; then
        print_error "Le port 8081 est déjà utilisé"
        print_warning "Arrêtez l'application avec: ./run.sh spring-stop"
        cd ..
        exit 1
    fi

    print_warning "Démarrage en cours... (Ctrl+C pour arrêter)"
    mvn spring-boot:run
    cd ..
}

# Spring Boot - Start en arrière-plan
spring_start_bg() {
    print_header "Démarrage Spring Boot (arrière-plan)"

    cd spring-products-api

    # Vérifier si un processus Spring Boot tourne déjà
    if lsof -ti:8081 > /dev/null 2>&1; then
        print_error "Le port 8081 est déjà utilisé"
        cd ..
        exit 1
    fi

    mvn spring-boot:run > /tmp/spring-boot.log 2>&1 &
    echo $! > /tmp/spring-boot.pid

    cd ..

    print_success "Spring Boot démarré en arrière-plan"
    echo "API disponible sur: http://localhost:8081/api/products"
    echo "Logs: tail -f /tmp/spring-boot.log"
    echo "Arrêter: ./run.sh spring-stop"
}

# Spring Boot - Stop
spring_stop() {
    print_header "Arrêt Spring Boot"

    if [ -f /tmp/spring-boot.pid ]; then
        PID=$(cat /tmp/spring-boot.pid)
        if ps -p $PID > /dev/null 2>&1; then
            kill $PID
            rm /tmp/spring-boot.pid
            print_success "Spring Boot arrêté (PID: $PID)"
        else
            print_warning "Processus non trouvé"
            rm /tmp/spring-boot.pid
        fi
    else
        # Chercher le processus sur le port 8081
        PID=$(lsof -ti:8081 2>/dev/null)
        if [ ! -z "$PID" ]; then
            kill $PID
            print_success "Spring Boot arrêté (PID: $PID)"
        else
            print_warning "Aucun processus Spring Boot trouvé"
        fi
    fi
}

# Spring Boot - Test
spring_test() {
    print_header "Test API Spring Boot"

    local BASE_URL="http://localhost:8081/api/products"

    echo "Test GET /products..."
    if curl -s -f "${BASE_URL}" > /dev/null; then
        print_success "GET /products OK"
        echo ""
        echo "Produits disponibles:"
        curl -s "${BASE_URL}" | jq -r '.[] | "  - \(.name) (\(.category))"' 2>/dev/null || curl -s "${BASE_URL}"
    else
        print_error "GET /products FAILED"
        print_warning "Assurez-vous que Spring Boot est démarré: ./run.sh spring-start"
    fi
}

# Database - Start
db_start() {
    print_header "Démarrage PostgreSQL"
    docker compose -f docker-compose-db.yml up -d
    print_success "PostgreSQL démarré sur localhost:5432"
    echo ""
    echo "Informations de connexion:"
    echo "  Database: productsdb"
    echo "  User: products_user"
    echo "  Password: products_pass"
    echo "  Port: 5432"
}

# Database - Stop
db_stop() {
    print_header "Arrêt PostgreSQL"
    docker compose -f docker-compose-db.yml down
    print_success "PostgreSQL arrêté"
}

# Database - Logs
db_logs() {
    print_header "Logs PostgreSQL"
    docker compose -f docker-compose-db.yml logs -f
}

# Test All
test_all() {
    print_header "Test des deux APIs"

    echo ""
    echo "=== Jakarta EE (port 8080) ==="
    jakarta_test

    echo ""
    echo ""
    echo "=== Spring Boot (port 8081) ==="
    spring_test
}

# Clean
clean() {
    print_header "Nettoyage des builds"

    if [ -d "jakarta-products-api/target" ]; then
        rm -rf jakarta-products-api/target
        print_success "Jakarta EE target/ supprimé"
    fi

    if [ -d "spring-products-api/target" ]; then
        rm -rf spring-products-api/target
        print_success "Spring Boot target/ supprimé"
    fi

    print_success "Nettoyage terminé"
}

# Clean All
clean_all() {
    print_header "Nettoyage complet"

    clean

    print_warning "Arrêt des conteneurs Docker..."
    jakarta_stop 2>/dev/null || true
    db_stop 2>/dev/null || true

    print_success "Nettoyage complet terminé"
}

# Main
main() {
    if [ $# -eq 0 ]; then
        show_help
        exit 0
    fi

    case "$1" in
        help|--help|-h)
            show_help
            ;;
        jakarta-build)
            check_prerequisites
            jakarta_build
            ;;
        jakarta-start)
            check_prerequisites
            jakarta_start
            ;;
        jakarta-stop)
            jakarta_stop
            ;;
        jakarta-logs)
            jakarta_logs
            ;;
        jakarta-restart)
            check_prerequisites
            jakarta_restart
            ;;
        jakarta-test)
            jakarta_test
            ;;
        spring-start)
            check_prerequisites
            spring_start
            ;;
        spring-start-bg)
            check_prerequisites
            spring_start_bg
            ;;
        spring-stop)
            spring_stop
            ;;
        spring-test)
            spring_test
            ;;
        db-start)
            check_prerequisites
            db_start
            ;;
        db-stop)
            db_stop
            ;;
        db-logs)
            db_logs
            ;;
        test-all)
            test_all
            ;;
        clean)
            clean
            ;;
        clean-all)
            clean_all
            ;;
        *)
            print_error "Commande inconnue: $1"
            echo ""
            show_help
            exit 1
            ;;
    esac
}

# Exécuter
main "$@"
