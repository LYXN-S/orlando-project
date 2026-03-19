package com.orlandoprestige.orlandoproject.orders.internal.procurement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public record ParsedPurchaseOrderDto(
        String customerName,
        String poNumber,
        LocalDate poDate,
        LocalDate deliveryDate,
        String currency,
    String businessAddress,
    String deliveryAddress,
        BigDecimal subtotal,
        BigDecimal tax,
        BigDecimal total,
        List<SystemPoItemDto> items,
        double confidence,
        String warnings,
        String rawStructuredOutput
) {
    public List<SystemPoItemDto> safeItems() {
        return items != null ? items : new ArrayList<>();
    }
}
