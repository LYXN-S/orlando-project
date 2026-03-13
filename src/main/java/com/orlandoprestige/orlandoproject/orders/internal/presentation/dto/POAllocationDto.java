package com.orlandoprestige.orlandoproject.orders.internal.presentation.dto;

public record POAllocationDto(
        Long id,
        Long orderItemId,
        Long productId,
        String warehouseCode,
        int allocatedQuantity
) {
}
