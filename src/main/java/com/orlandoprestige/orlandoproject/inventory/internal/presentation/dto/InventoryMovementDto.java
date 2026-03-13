package com.orlandoprestige.orlandoproject.inventory.internal.presentation.dto;

import java.time.LocalDateTime;

public record InventoryMovementDto(
        Long id,
        Long inventoryItemId,
        Long productId,
        String productName,
        String movementType,
        int quantity,
        String referenceType,
        Long referenceId,
        String note,
        Long performedBy,
        LocalDateTime createdAt
) {
}
