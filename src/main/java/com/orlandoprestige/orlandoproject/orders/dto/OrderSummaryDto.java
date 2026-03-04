package com.orlandoprestige.orlandoproject.orders.dto;

import com.orlandoprestige.orlandoproject.orders.internal.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Public DTO exposed by the Orders module.
 */
public record OrderSummaryDto(
        Long id,
        OrderStatus status,
        LocalDateTime createdAt,
        BigDecimal totalAmount
) {
}

