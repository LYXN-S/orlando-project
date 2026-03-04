package com.orlandoprestige.orlandoproject.cart.dto;

import java.util.List;

/**
 * Public DTO exposed by the Cart module.
 * Used by the Orders module to read the full cart summary at checkout.
 */
public record CartSummaryDto(
        Long cartId,
        Long customerId,
        List<CartItemSummaryDto> items
) {
}

