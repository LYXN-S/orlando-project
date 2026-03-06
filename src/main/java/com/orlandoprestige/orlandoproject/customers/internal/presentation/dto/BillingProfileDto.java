package com.orlandoprestige.orlandoproject.customers.internal.presentation.dto;

import com.orlandoprestige.orlandoproject.customers.internal.domain.BillingType;

import java.time.LocalDateTime;

public record BillingProfileDto(
        Long id,
        Long customerId,
        BillingType billingType,
        String name,
        String tin,
        String address,
        String paymentTerms,
        boolean isDefault,
        LocalDateTime createdAt
) {
}
