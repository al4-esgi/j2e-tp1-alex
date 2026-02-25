package com.formation.springproducts.dto;

import java.math.BigDecimal;

/**
 * DTO CategoryStats — Projection JPQL (Partie 6.3)
 *
 * Utilisé avec la syntaxe JPQL "SELECT NEW" pour éviter de retourner
 * des Object[] non typés depuis les requêtes d'agrégation.
 *
 * Requête JPQL associée :
 * SELECT NEW com.formation.springproducts.dto.CategoryStats(
 *     p.category.name, COUNT(p), AVG(p.price))
 * FROM Product p
 * WHERE p.category IS NOT NULL
 * GROUP BY p.category.name
 */
public class CategoryStats {

    private String categoryName;
    private Long productCount;
    private BigDecimal averagePrice;

    /**
     * Constructeur utilisé par JPQL dans les requêtes SELECT NEW.
     * Les types et l'ordre des paramètres doivent correspondre exactement
     * à ce qui est projeté dans la requête.
     *
     * @param categoryName nom de la catégorie (p.category.name)
     * @param productCount nombre de produits dans la catégorie (COUNT(p))
     * @param averagePrice prix moyen des produits (AVG(p.price))
     */
    public CategoryStats(String categoryName, Long productCount, BigDecimal averagePrice) {
        this.categoryName = categoryName;
        this.productCount = productCount;
        this.averagePrice = averagePrice != null ? averagePrice.setScale(2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    /**
     * Constructeur appelé par JPQL quand AVG() retourne un Double.
     * Hibernate 6 retourne AVG(p.price) en tant que Double même si p.price est BigDecimal.
     */
    public CategoryStats(String categoryName, Long productCount, Double averagePrice) {
        this.categoryName = categoryName;
        this.productCount = productCount;
        this.averagePrice = averagePrice != null ? BigDecimal.valueOf(averagePrice).setScale(2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    // -------------------------------------------------------------------------
    // Getters / Setters
    // -------------------------------------------------------------------------

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Long getProductCount() {
        return productCount;
    }

    public void setProductCount(Long productCount) {
        this.productCount = productCount;
    }

    public BigDecimal getAveragePrice() {
        return averagePrice;
    }

    public void setAveragePrice(BigDecimal averagePrice) {
        this.averagePrice = averagePrice;
    }

    @Override
    public String toString() {
        return "CategoryStats{" + "categoryName='" + categoryName + '\'' + ", productCount=" + productCount + ", averagePrice=" + averagePrice + '}';
    }
}
