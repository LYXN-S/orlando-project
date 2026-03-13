package com.orlandoprestige.orlandoproject.inventory.internal.presentation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StockInRequestDto(
        @NotNull(message = "Product ID is required") Long productId,
        @NotBlank(message = "Warehouse is required") String warehouseCode,
        @NotNull(message = "Quantity is required") @Min(value = 1, message = "Quantity must be at least 1") Integer quantity,
        String note
) {
}
