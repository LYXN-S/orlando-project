package com.orlandoprestige.orlandoproject.inventory.internal.event;

/**
 * Published when inventory stock changes, so the Catalog module can sync Product.stockQuantity.
 */
public record StockChangedEvent(
        Long productId,
        int newStock
) {
}
