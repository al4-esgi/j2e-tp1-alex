#!/bin/bash

SLOW_URL="http://localhost:8081/api/products/slow"
FAST_URL="http://localhost:8081/api/products/fast"

echo ""
echo "╔══════════════════════════════════════════════════════════╗"
echo "║          DÉMONSTRATION PROBLÈME N+1 — TP2 JPA            ║"
echo "╚══════════════════════════════════════════════════════════╝"

# ── /slow ──────────────────────────────────────────────────────
echo ""
echo "┌──────────────────────────────────────────────────────────┐"
echo "│  GET /api/products/slow  →  SANS JOIN FETCH (N+1)        │"
echo "└──────────────────────────────────────────────────────────┘"

TS_SLOW=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
sleep 1
curl -s "$SLOW_URL" > /dev/null
sleep 1

SLOW_LOGS=$(docker logs spring-products-api --since "$TS_SLOW" 2>&1 | grep "Hibernate:" | wc -l | tr -d ' ')

echo ""
docker logs spring-products-api --since "$TS_SLOW" 2>&1 | grep "Hibernate:" | while read -r line; do
  echo "  ──── Requête SQL ────"
done

echo ""
echo "  → TOTAL REQUÊTES SQL : $SLOW_LOGS (1 SELECT products + $((SLOW_LOGS - 1)) SELECT séparés)"

# ── /fast ──────────────────────────────────────────────────────
echo ""
echo "┌──────────────────────────────────────────────────────────┐"
echo "│  GET /api/products/fast  →  AVEC JOIN FETCH (optimisé)   │"
echo "└──────────────────────────────────────────────────────────┘"

TS_FAST=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
sleep 1
curl -s "$FAST_URL" > /dev/null
sleep 1

FAST_LOGS=$(docker logs spring-products-api --since "$TS_FAST" 2>&1 | grep "Hibernate:" | wc -l | tr -d ' ')

echo ""
echo "  ──── Requête SQL ────"
docker logs spring-products-api --since "$TS_FAST" 2>&1 | grep -E "select|from|join|left join" | head -5 | while read -r line; do
  echo "    $line"
done

echo ""
echo "  → TOTAL REQUÊTES SQL : $FAST_LOGS (1 seul SELECT avec JOIN FETCH)"

# ── Bilan ──────────────────────────────────────────────────────
echo ""
echo "╔══════════════════════════════════════════════════════════╗"
echo "║  BILAN                                                   ║"
printf  "║  /slow  →  %2s requêtes SQL  (N+1 : 1 + N par entite)    ║\n" "$SLOW_LOGS"
printf  "║  /fast  →  %2s requete  SQL  (JOIN FETCH tout en 1)       ║\n" "$FAST_LOGS"
echo "╚══════════════════════════════════════════════════════════╝"
echo ""
