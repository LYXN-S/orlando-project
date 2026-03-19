package com.orlandoprestige.orlandoproject.orders.internal.procurement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record SystemSalesInvoiceDto(
        Long id,
        Long purchaseOrderId,
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
        String preparedBy,
        Long preparedByUserId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<SystemSalesInvoiceItemDto> items
) {
}
