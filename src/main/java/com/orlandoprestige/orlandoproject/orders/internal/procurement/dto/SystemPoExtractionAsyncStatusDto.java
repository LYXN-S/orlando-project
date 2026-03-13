package com.orlandoprestige.orlandoproject.orders.internal.procurement.dto;

public record SystemPoExtractionAsyncStatusDto(
        String requestId,
        String status,
        String message,
        SystemPoResponseDto result
) {
    public static SystemPoExtractionAsyncStatusDto pending(String requestId) {
        return new SystemPoExtractionAsyncStatusDto(requestId, "PENDING", "Extraction is in progress", null);
    }

    public static SystemPoExtractionAsyncStatusDto success(String requestId, SystemPoResponseDto result) {
        return new SystemPoExtractionAsyncStatusDto(requestId, "SUCCESS", "Extraction completed", result);
    }

    public static SystemPoExtractionAsyncStatusDto failed(String requestId, String message) {
        return new SystemPoExtractionAsyncStatusDto(requestId, "FAILED", message, null);
    }
}
