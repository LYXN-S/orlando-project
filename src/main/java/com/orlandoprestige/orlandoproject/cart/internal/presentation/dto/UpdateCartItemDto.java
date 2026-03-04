package com.orlandoprestige.orlandoproject.cart.internal.presentation.dto;

import jakarta.validation.constraints.Min;

public record UpdateCartItemDto(
        @Min(value = 0, message = "Quantity cannot be negative (use 0 to remove)") int quantity
) {
}

