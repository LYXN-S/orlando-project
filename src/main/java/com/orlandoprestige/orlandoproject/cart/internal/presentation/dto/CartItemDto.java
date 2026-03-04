package com.orlandoprestige.orlandoproject.cart.internal.presentation.dto;

import java.math.BigDecimal;

public record CartItemDto(
        Long productId,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {
}

