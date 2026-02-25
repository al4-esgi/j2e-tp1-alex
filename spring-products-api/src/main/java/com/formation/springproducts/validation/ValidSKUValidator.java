package com.formation.springproducts.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidSKUValidator implements ConstraintValidator<ValidSKU, String> {

    private static final String SKU_PATTERN = "^[A-Z]{3}\\d{3}$";

    @Override
    public void initialize(ValidSKU constraintAnnotation) {
        // Pas d'initialisation nécessaire
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // null est considéré valide — utiliser @NotNull séparément si le champ est obligatoire
        if (value == null) {
            return true;
        }
        return value.matches(SKU_PATTERN);
    }
}
