package com.orlandoprestige.orlandoproject.orders.internal.presentation.dto;

public record EvaluateOrderDto(
        boolean approved,
        String note
) {
}

