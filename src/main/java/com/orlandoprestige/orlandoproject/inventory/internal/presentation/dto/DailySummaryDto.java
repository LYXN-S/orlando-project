package com.orlandoprestige.orlandoproject.inventory.internal.presentation.dto;

public record DailySummaryDto(
        Long productId,
        String productName,
        String sku,
        int totalIn,
        int totalOut,
        int netChange
) {
}
