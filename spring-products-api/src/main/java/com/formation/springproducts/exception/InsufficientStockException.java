package com.formation.springproducts.exception;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String productName, int requested, int available) {
        super(String.format(
            "Stock insuffisant pour %s. Demandé: %d, Disponible: %d",
            productName, requested, available));
    }
}
