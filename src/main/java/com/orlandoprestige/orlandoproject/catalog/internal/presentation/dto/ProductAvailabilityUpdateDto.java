package com.orlandoprestige.orlandoproject.catalog.internal.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record ProductAvailabilityUpdateDto(
        @NotBlank(message = "Availability status is required") String availabilityStatus,
        String note
) {
}
