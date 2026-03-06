package com.orlandoprestige.orlandoproject.orders.internal.presentation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SubmitOrderDto(
        @NotEmpty(message = "Order must have at least one item")
        @Valid
        List<SubmitOrderItemDto> items,

        Long billingProfileId,

        @NotBlank(message = "Billing type is required")
        String billingType,

        @NotBlank(message = "Billing name is required")
        String billingName,

        String billingTin,

        @NotBlank(message = "Billing address is required")
        String billingAddress,

        @NotBlank(message = "Billing terms are required")
        String billingTerms
) {
}
