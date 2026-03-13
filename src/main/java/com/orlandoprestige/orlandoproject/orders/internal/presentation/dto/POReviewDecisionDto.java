package com.orlandoprestige.orlandoproject.orders.internal.presentation.dto;

import jakarta.validation.constraints.NotNull;

public record POReviewDecisionDto(
        @NotNull(message = "Approval decision is required") Boolean approved,
        String note
) {
}
