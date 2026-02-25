package com.formation.springproducts.exception;

public class CategoryNotEmptyException extends RuntimeException {

    public CategoryNotEmptyException(String message) {
        super(message);
    }

    public CategoryNotEmptyException(Long id, int productCount) {
        super(String.format(
            "Impossible de supprimer la catégorie ID %d : elle contient %d produit(s)",
            id, productCount));
    }
}
