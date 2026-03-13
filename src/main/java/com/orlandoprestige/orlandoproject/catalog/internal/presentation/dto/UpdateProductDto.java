package com.orlandoprestige.orlandoproject.catalog.internal.presentation.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record UpdateProductDto(
        @NotBlank(message = "Product name is required") String name,
        String description,
        @NotNull @DecimalMin(value = "0.01", message = "Price must be greater than 0") BigDecimal price,
        @NotBlank(message = "Category is required") String category,
        String availabilityStatus
) {
}

