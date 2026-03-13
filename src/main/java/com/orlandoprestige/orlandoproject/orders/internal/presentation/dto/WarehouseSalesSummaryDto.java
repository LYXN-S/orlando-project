package com.orlandoprestige.orlandoproject.orders.internal.presentation.dto;

import java.math.BigDecimal;

public record WarehouseSalesSummaryDto(
        String warehouseCode,
        int totalQuantity,
        BigDecimal grossSales,
        int orderCount
) {
}
