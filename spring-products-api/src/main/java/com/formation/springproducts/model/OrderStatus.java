package com.formation.springproducts.model;

/**
 * Statut d'une commande.
 *
 * Cycle de vie :
 * PENDING → CONFIRMED → SHIPPED → DELIVERED
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    SHIPPED,
    DELIVERED
}
