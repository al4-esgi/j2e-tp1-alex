package com.formation.springproducts.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidSKUValidator.class)
public @interface ValidSKU {
    String message() default "SKU invalide. Format attendu: ABC123 (3 lettres majuscules + 3 chiffres)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
