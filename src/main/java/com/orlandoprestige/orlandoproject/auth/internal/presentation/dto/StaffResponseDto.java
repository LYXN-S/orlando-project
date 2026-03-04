package com.orlandoprestige.orlandoproject.auth.internal.presentation.dto;

import java.util.Set;

public record StaffResponseDto(
        Long id,
        String firstName,
        String lastName,
        String email,
        boolean superAdmin,
        Set<String> permissions
) {
}
