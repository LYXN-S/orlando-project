package com.orlandoprestige.orlandoproject.cart.dto;

import java.math.BigDecimal;

/**
 * Public DTO exposed by the Cart module.
 * Used by the Orders module to read cart contents at checkout.
 */
public record CartItemSummaryDto(
        Long productId,
        int quantity,
        BigDecimal unitPrice
) {
}

