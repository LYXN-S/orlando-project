package com.orlandoprestige.orlandoproject.orders.internal.presentation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record POAllocationRequestDto(
        @NotNull(message = "Order item ID is required") Long orderItemId,
        @NotNull(message = "Product ID is required") Long productId,
        @NotBlank(message = "Warehouse code is required") String warehouseCode,
        @NotNull(message = "Allocated quantity is required") @Min(value = 1, message = "Allocated quantity must be at least 1") Integer allocatedQuantity
) {
}
