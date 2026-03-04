package com.orlandoprestige.orlandoproject.auth.internal.presentation.dto;

import java.util.Set;

public record UpdatePermissionsRequestDto(
        Set<String> permissions
) {
}
