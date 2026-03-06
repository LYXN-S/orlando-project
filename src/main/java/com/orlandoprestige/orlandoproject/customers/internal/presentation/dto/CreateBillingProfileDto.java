package com.orlandoprestige.orlandoproject.customers.internal.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateBillingProfileDto(
        @NotNull(message = "Billing type is required")
        String billingType,

        @NotBlank(message = "Name is required")
        String name,

        String tin,

        @NotBlank(message = "Address is required")
        String address,

        @NotBlank(message = "Payment terms are required")
        String paymentTerms
) {
}
