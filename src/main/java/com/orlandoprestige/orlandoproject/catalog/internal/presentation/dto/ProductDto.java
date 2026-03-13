package com.orlandoprestige.orlandoproject.catalog.internal.presentation.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductDto(
        Long id,
        String name,
        String description,
        String sku,
        BigDecimal price,
        Integer stockQuantity,
        String category,
        List<ImageDto> images
) {
}

