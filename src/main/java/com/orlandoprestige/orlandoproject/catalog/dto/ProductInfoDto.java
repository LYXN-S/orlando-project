package com.orlandoprestige.orlandoproject.catalog.dto;

import java.math.BigDecimal;

/**
 * Public DTO exposed by the Catalog module.
 * Used by Cart and Orders modules to reference product data without crossing module boundaries.
 */
public record ProductInfoDto(
        Long id,
        String name,
        String sku,
        BigDecimal price,
        int stockQuantity,
        String category
) {
}

