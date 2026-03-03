package com.orlandoprestige.orlandoproject.shared.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Response DTO for error responses.
 * Provides consistent error format across all API endpoints.
 */
@Schema(description = "Standard error response format for all API errors")
public record ErrorResponse(
        @Schema(description = "Human-readable error message", example = "Invalid credentials")
        String message,

        @Schema(description = "Timestamp when the error occurred", example = "2024-01-15T10:30:00")
        String timestamp,

        @Schema(description = "HTTP status code", example = "400")
        int status,

        @Schema(description = "Map of field names to validation error messages (for validation errors)")
        Map<String, String> fieldErrors
) {
    /**
     * Creates an error response without field errors.
     */
    public static ErrorResponse of(String message, int status) {
        return new ErrorResponse(message, LocalDateTime.now().toString(), status, null);
    }

    /**
     * Creates an error response with field errors (for validation failures).
     */
    public static ErrorResponse withFieldErrors(String message, int status, Map<String, String> fieldErrors) {
        return new ErrorResponse(message, LocalDateTime.now().toString(), status, fieldErrors);
    }
}
