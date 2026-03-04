package com.orlandoprestige.orlandoproject.cart.internal.presentation.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartDto(
        Long cartId,
        Long customerId,
        List<CartItemDto> items,
        BigDecimal totalPrice
) {
}

