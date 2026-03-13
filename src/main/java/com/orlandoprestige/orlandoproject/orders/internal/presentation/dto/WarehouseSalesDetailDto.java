package com.orlandoprestige.orlandoproject.orders.internal.presentation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WarehouseSalesDetailDto(
        Long poId,
        Long orderId,
        LocalDateTime reviewedAt,
        String warehouseCode,
        Long productId,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal amount
) {
}
