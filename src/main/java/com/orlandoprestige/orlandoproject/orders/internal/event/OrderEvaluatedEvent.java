package com.orlandoprestige.orlandoproject.orders.internal.event;

/**
 * Published when a staff member evaluates (approves/rejects) an order.
 */
public record OrderEvaluatedEvent(
        Long orderId,
        Long customerId,
        Long staffId,
        boolean approved,
        String note
) {
}
