package com.orlandoprestige.orlandoproject.inventory.dto;

public record InventoryInfoDto(
        Long id,
        Long productId,
        String productName,
        String sku,
        int currentStock,
        int reorderLevel
) {
}
