package com.formation.springproducts.handler;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import com.formation.springproducts.exception.CategoryNotEmptyException;
import com.formation.springproducts.exception.CategoryNotFoundException;
import com.formation.springproducts.exception.DuplicateProductException;
import com.formation.springproducts.exception.ErrorResponse;
import com.formation.springproducts.exception.FieldError;
import com.formation.springproducts.exception.InsufficientStockException;
import com.formation.springproducts.exception.ProductNotFoundException;

import jakarta.validation.ConstraintViolationException;

/**
 * GlobalExceptionHandler — Gestionnaire d'exceptions global (TP3 — Partie 5)
 *
 * Centralise la gestion de TOUTES les exceptions de l'application.
 * Grâce à @ControllerAdvice, ce handler intercepte les exceptions levées
 * par n'importe quel @RestController AVANT qu'elles ne remontent au client.
 *
 * Avantages :
 * - Pas de try/catch répétitifs dans les controllers
 * - Réponses d'erreur cohérentes et structurées
 * - Codes HTTP appropriés selon le type d'erreur
 * - Messages clairs en français
 *
 * Hiérarchie des handlers (du plus spécifique au plus générique) :
 * 1. MethodArgumentNotValidException → 400 (validation @Valid sur @RequestBody)
 * 2. ConstraintViolationException    → 400 (validation Bean sur paramètres/service)
 * 3. IllegalArgumentException        → 400 (validation métier manuelle)
 * 4. ProductNotFoundException        → 404
 * 5. CategoryNotFoundException       → 404
 * 6. DuplicateProductException       → 409
 * 7. CategoryNotEmptyException       → 409
 * 8. InsufficientStockException      → 422
 * 9. Exception (fallback)            → 500
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // =========================================================================
    // 400 — Bad Request : erreurs de validation
    // =========================================================================

    /**
     * Gère les échecs de validation déclenchés par @Valid sur un @RequestBody.
     *
     * Spring lève MethodArgumentNotValidException quand les contraintes Bean Validation
     * (@NotBlank, @Size, @Min, @ValidSKU…) échouent sur un paramètre @RequestBody.
     * On extrait la liste des erreurs de champs pour retourner un JSON structuré.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            WebRequest request) {

        List<FieldError> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new FieldError(
                        error.getField(),
                        error.getDefaultMessage(),
                        error.getRejectedValue()))
                .collect(Collectors.toList());

        ErrorResponse errorResponse = new ErrorResponse(
                400,
                "Bad Request",
                "Validation échouée : " + errors.size() + " erreur(s) détectée(s)",
                request.getDescription(false));
        errorResponse.setErrors(errors);

        log.warn("Validation failed: {} field error(s)", errors.size());
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Gère les violations de contraintes Bean Validation au niveau service/repository.
     *
     * Levée quand @Validated est utilisé sur un service ou quand Hibernate valide
     * une entité avant de la persister (si la validation Hibernate est activée).
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            WebRequest request) {

        List<FieldError> errors = ex.getConstraintViolations()
                .stream()
                .map(violation -> new FieldError(
                        violation.getPropertyPath().toString(),
                        violation.getMessage(),
                        violation.getInvalidValue()))
                .collect(Collectors.toList());

        ErrorResponse errorResponse = new ErrorResponse(
                400,
                "Bad Request",
                "Contrainte de validation violée",
                request.getDescription(false));
        errorResponse.setErrors(errors);

        log.warn("Constraint violation: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Gère les IllegalArgumentException levées par les services lors de
     * validations métier manuelles (données invalides, paramètres incorrects…).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            WebRequest request) {

        ErrorResponse error = new ErrorResponse(
                400,
                "Bad Request",
                ex.getMessage(),
                request.getDescription(false));

        log.warn("Illegal argument: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    // =========================================================================
    // 404 — Not Found
    // =========================================================================

    /**
     * Gère les ProductNotFoundException → 404 avec message clair.
     */
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProductNotFound(
            ProductNotFoundException ex,
            WebRequest request) {

        ErrorResponse error = new ErrorResponse(
                404,
                "Not Found",
                ex.getMessage(),
                request.getDescription(false));

        log.info("Product not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Gère les CategoryNotFoundException → 404 avec message clair.
     */
    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCategoryNotFound(
            CategoryNotFoundException ex,
            WebRequest request) {

        ErrorResponse error = new ErrorResponse(
                404,
                "Not Found",
                ex.getMessage(),
                request.getDescription(false));

        log.info("Category not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // =========================================================================
    // 409 — Conflict
    // =========================================================================

    /**
     * Gère les DuplicateProductException → 409 (SKU déjà utilisé).
     */
    @ExceptionHandler(DuplicateProductException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateProduct(
            DuplicateProductException ex,
            WebRequest request) {

        ErrorResponse error = new ErrorResponse(
                409,
                "Conflict",
                ex.getMessage(),
                request.getDescription(false));

        log.warn("Duplicate product: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Gère les CategoryNotEmptyException → 409 (suppression impossible car produits liés).
     */
    @ExceptionHandler(CategoryNotEmptyException.class)
    public ResponseEntity<ErrorResponse> handleCategoryNotEmpty(
            CategoryNotEmptyException ex,
            WebRequest request) {

        ErrorResponse error = new ErrorResponse(
                409,
                "Conflict",
                ex.getMessage(),
                request.getDescription(false));

        log.warn("Category not empty: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // =========================================================================
    // 422 — Unprocessable Entity : erreurs métier (stock insuffisant…)
    // =========================================================================

    /**
     * Gère les InsufficientStockException → 422 (stock insuffisant).
     *
     * 422 est plus précis que 400 pour les erreurs métier "compréhensibles"
     * (la requête est bien formée, mais ne peut pas être traitée).
     */
    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientStock(
            InsufficientStockException ex,
            WebRequest request) {

        ErrorResponse error = new ErrorResponse(
                422,
                "Unprocessable Entity",
                ex.getMessage(),
                request.getDescription(false));

        log.warn("Insufficient stock: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }

    // =========================================================================
    // 500 — Internal Server Error : fallback générique
    // =========================================================================

    /**
     * Handler de dernier recours pour toute exception non interceptée.
     *
     * IMPORTANT :
     * - On logue TOUJOURS l'exception complète côté serveur (pour le debugging).
     * - On N'EXPOSE JAMAIS le message technique au client (sécurité).
     * - Message générique uniquement → évite de fuiter des infos d'implémentation.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex,
            WebRequest request) {

        // Log complet côté serveur (indispensable en production)
        log.error("Unexpected error on {}: {}", request.getDescription(false), ex.getMessage(), ex);

        ErrorResponse error = new ErrorResponse(
                500,
                "Internal Server Error",
                "Une erreur inattendue s'est produite. Veuillez réessayer plus tard.",
                request.getDescription(false));

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
