package com.orlandoprestige.orlandoproject.orders.internal.presentation.dto;

import com.orlandoprestige.orlandoproject.orders.internal.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderDto(
        Long id,
        Long customerId,
        OrderStatus status,
        List<OrderItemDto> items,
        BigDecimal totalAmount,
        LocalDateTime createdAt,
        Long evaluatedByStaffId,
        String evaluationNote
) {
}

