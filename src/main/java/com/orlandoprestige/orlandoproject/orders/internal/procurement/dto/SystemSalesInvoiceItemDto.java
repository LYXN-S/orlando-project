package com.orlandoprestige.orlandoproject.orders.internal.procurement.dto;

import java.math.BigDecimal;

public record SystemSalesInvoiceItemDto(
        Integer lineNumber,
        String itemLabel,
        String description,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal amount
) {
}
