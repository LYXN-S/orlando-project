package com.orlandoprestige.orlandoproject.orders.internal.presentation.dto;

import java.time.LocalDateTime;
import java.util.List;

public record POReviewDto(
        Long id,
        Long orderId,
        String status,
        Long customerId,
        LocalDateTime orderCreatedAt,
        Long reviewedBy,
        LocalDateTime reviewedAt,
        String reviewNote,
        List<OrderItemDto> items,
        List<POAllocationDto> allocations,
        String billingType,
        String billingName,
        String billingTin,
        String billingAddress,
        String billingTerms
) {
}
