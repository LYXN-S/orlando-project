package com.orlandoprestige.orlandoproject.orders.internal.event;

import java.math.BigDecimal;
import java.util.List;

/**
 * Published when a customer submits a new order.
 * Can be consumed by notification or audit listeners.
 */
public record OrderSubmittedEvent(
        Long orderId,
        Long customerId,
        List<OrderItemSnapshot> items
) {
    public record OrderItemSnapshot(Long productId, String productName, int quantity, BigDecimal unitPrice) {}
}

