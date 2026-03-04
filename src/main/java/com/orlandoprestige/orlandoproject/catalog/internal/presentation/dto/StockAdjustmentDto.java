package com.orlandoprestige.orlandoproject.catalog.internal.presentation.dto;

import jakarta.validation.constraints.NotNull;

public record StockAdjustmentDto(
        @NotNull(message = "Adjustment value is required") Integer adjustment
) {
}
