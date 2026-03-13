package com.orlandoprestige.orlandoproject.orders.internal.procurement.dto;

import java.time.LocalDateTime;

public record SystemPoExtractionRunDto(
        Long id,
        String status,
        String modelName,
        Double confidence,
        String warnings,
        LocalDateTime createdAt
) {
}
