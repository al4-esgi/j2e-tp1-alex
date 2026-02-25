package com.formation.springproducts.validation;

import com.formation.springproducts.model.Order;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidDateRangeValidator implements ConstraintValidator<ValidDateRange, Order> {

    @Override
    public void initialize(ValidDateRange constraintAnnotation) {
        // Pas d'initialisation nécessaire
    }

    @Override
    public boolean isValid(Order order, ConstraintValidatorContext context) {
        // Ne valide que si les deux dates sont présentes
        if (order == null || order.getOrderDate() == null || order.getDeliveryDate() == null) {
            return true;
        }
        // deliveryDate doit être >= orderDate
        return !order.getDeliveryDate().isBefore(order.getOrderDate());
    }
}
