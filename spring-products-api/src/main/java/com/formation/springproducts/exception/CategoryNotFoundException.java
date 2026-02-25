package com.formation.springproducts.exception;

public class CategoryNotFoundException extends RuntimeException {

    public CategoryNotFoundException(Long id) {
        super("Catégorie non trouvée avec l'ID: " + id);
    }

    public CategoryNotFoundException(String message) {
        super(message);
    }
}
