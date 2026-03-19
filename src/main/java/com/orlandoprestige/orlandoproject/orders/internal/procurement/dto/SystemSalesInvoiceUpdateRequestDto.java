package com.orlandoprestige.orlandoproject.orders.internal.procurement.dto;

import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public record SystemSalesInvoiceUpdateRequestDto(
        LocalDate invoiceDate,
        String registeredName,
        String tin,
        String businessAddress,
        String terms,
        String poNumber,
        BigDecimal total,
        BigDecimal totalSales,
        BigDecimal lessVat,
        BigDecimal amountNetOfVat,
        BigDecimal lessWhTax,
        BigDecimal totalAfterWhTax,
        BigDecimal addVat,
        BigDecimal totalAmountDue,
        BigDecimal vatableSales,
        BigDecimal vat,
        BigDecimal zeroRatedSales,
        BigDecimal vatExSales,
        String approvedBy,
        @Valid List<SystemSalesInvoiceItemDto> items
) {
    public List<SystemSalesInvoiceItemDto> safeItems() {
        return items != null ? items : new ArrayList<>();
    }
}
