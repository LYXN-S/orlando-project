package com.orlandoprestige.orlandoproject.orders.internal.procurement.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record SystemPoItemDto(
        Integer lineNumber,
        @NotBlank(message = "Item description is required") String description,
        String sku,
        @Min(value = 1, message = "Quantity must be at least 1") int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
}
