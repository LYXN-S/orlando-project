package com.orlandoprestige.orlandoproject.orders.internal.procurement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record SystemPoResponseDto(
        Long id,
        String sourceType,
        String status,
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
        Long createdBy,
        Long confirmedBy,
        LocalDateTime confirmedAt,
        LocalDateTime createdAt,
        List<SystemPoItemDto> items,
        List<SystemPoAttachmentDto> attachments,
        SystemPoExtractionRunDto latestExtraction,
        SystemSalesInvoiceDto salesInvoice
) {
}
