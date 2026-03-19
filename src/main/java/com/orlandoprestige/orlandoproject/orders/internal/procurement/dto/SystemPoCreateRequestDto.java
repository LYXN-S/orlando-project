package com.orlandoprestige.orlandoproject.orders.internal.procurement.dto;

import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public record SystemPoCreateRequestDto(
        String customerName,
        String supplierName,
        String poNumber,
        LocalDate poDate,
        LocalDate deliveryDate,
        String currency,
        String tin,
        String businessAddress,
        String deliveryAddress,
        Boolean sameAsBusinessAddress,
        BigDecimal subtotal,
        BigDecimal tax,
        BigDecimal total,
        String notes,
        @Valid List<SystemPoItemDto> items
) {
    public String resolvedCustomerName() {
        if (customerName != null && !customerName.isBlank()) {
            return customerName;
        }
        if (supplierName != null && !supplierName.isBlank()) {
            return supplierName;
        }
        throw new IllegalArgumentException("Customer name is required");
    }

    public List<SystemPoItemDto> safeItems() {
        return items != null ? items : new ArrayList<>();
    }
}
