package com.formation.springproducts.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidPriceValidator.class)
public @interface ValidPrice {
    String message() default "Prix invalide : maximum 2 décimales autorisées (ex: 99.99)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
