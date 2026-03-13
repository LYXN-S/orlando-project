package com.orlandoprestige.orlandoproject.inventory.internal.presentation.dto;

import java.time.LocalDateTime;

public record InventoryItemDto(
        Long id,
        Long productId,
        String productName,
        String sku,
        String category,
        int currentStock,
        int reorderLevel,
        String status,
        LocalDateTime lastUpdated
) {
}
