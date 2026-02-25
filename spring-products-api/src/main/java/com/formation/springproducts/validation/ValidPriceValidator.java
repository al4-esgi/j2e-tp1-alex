package com.formation.springproducts.validation;

import java.math.BigDecimal;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidPriceValidator implements ConstraintValidator<ValidPrice, BigDecimal> {

    @Override
    public void initialize(ValidPrice constraintAnnotation) {
        // Pas d'initialisation nécessaire
    }

    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        // null est considéré valide — utiliser @NotNull séparément si le champ est obligatoire
        if (value == null) {
            return true;
        }
        // scale() retourne le nombre de chiffres après la virgule
        // stripTrailingZeros() normalise 99.990 → 99.99 avant de vérifier
        return value.stripTrailingZeros().scale() <= 2;
    }
}
