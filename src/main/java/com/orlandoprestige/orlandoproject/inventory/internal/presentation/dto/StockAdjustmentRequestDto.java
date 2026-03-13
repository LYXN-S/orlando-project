package com.orlandoprestige.orlandoproject.inventory.internal.presentation.dto;

import jakarta.validation.constraints.NotNull;

public record StockAdjustmentRequestDto(
        @NotNull(message = "Adjustment value is required") Integer adjustment,
        String note
) {
}
