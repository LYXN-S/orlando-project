package com.orlandoprestige.orlandoproject.orders.internal.procurement.dto;

import java.time.LocalDateTime;

public record SystemPoAttachmentDto(
        Long id,
        String fileName,
        String mimeType,
        Long fileSize,
        LocalDateTime uploadedAt,
        String downloadUrl
) {
}
