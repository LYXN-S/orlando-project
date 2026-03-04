package com.orlandoprestige.orlandoproject.auth.internal.presentation.dto;

import java.util.List;

public record LoginResponseDto(
        String token,
        String role,
        Long userId,
        List<String> permissions
) {
}

