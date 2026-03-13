package com.orlandoprestige.orlandoproject.catalog.internal.presentation.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateProductDto(
        @NotBlank(message = "Product name is required") String name,
        String description,
        @NotBlank(message = "SKU is required") String sku,
        @NotNull @DecimalMin(value = "0.01", message = "Price must be greater than 0") BigDecimal price,
        @Min(value = 0, message = "Stock quantity cannot be negative") int stockQuantity,
        @NotBlank(message = "Category is required") String category,
        String availabilityStatus
) {
}

