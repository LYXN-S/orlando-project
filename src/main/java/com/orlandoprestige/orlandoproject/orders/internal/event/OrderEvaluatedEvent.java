package com.orlandoprestige.orlandoproject.orders.internal.event;

import java.util.List;

/**
 * Published when a staff member evaluates (approves/rejects) an order.
 * Includes item snapshots so the inventory module can deduct stock without coupling to Order internals.
 */
public record OrderEvaluatedEvent(
        Long orderId,
        Long customerId,
        Long staffId,
        boolean approved,
        String note,
        List<OrderItemSnapshot> items
) {
    public record OrderItemSnapshot(Long productId, String productName, int quantity) {}
}
