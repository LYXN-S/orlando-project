package com.orlandoprestige.orlandoproject.catalog.internal.presentation.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record UpdateProductDto(
        @NotBlank(message = "Product name is required") String name,
        String description,
        @NotNull @DecimalMin(value = "0.01", message = "Price must be greater than 0") BigDecimal price,
        @Min(value = 0, message = "Stock quantity cannot be negative") int stockQuantity,
        @NotBlank(message = "Category is required") String category
) {
}

