package com.orlandoprestige.orlandoproject.auth.internal.presentation.dto;

import java.util.List;

public record UserProfileResponseDto(
        Long userId,
        String email,
        String firstName,
        String lastName,
        String role,
        List<String> permissions
) {
}
