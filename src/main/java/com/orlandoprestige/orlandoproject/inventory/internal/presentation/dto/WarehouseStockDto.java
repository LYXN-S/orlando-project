package com.orlandoprestige.orlandoproject.inventory.internal.presentation.dto;

import java.time.LocalDateTime;

public record WarehouseStockDto(
        Long id,
        Long productId,
        String productName,
        String sku,
        String warehouseCode,
        String warehouseName,
        int quantity,
        LocalDateTime lastUpdated
) {
}
