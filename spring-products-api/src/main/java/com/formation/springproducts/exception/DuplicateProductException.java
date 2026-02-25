package com.formation.springproducts.exception;

public class DuplicateProductException extends RuntimeException {

    public DuplicateProductException(String sku) {
        super("Un produit existe déjà avec le SKU: " + sku);
    }
}
