package com.orlandoprestige.orlandoproject.auth.internal.presentation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto(
        @NotBlank @Email(message = "Invalid email format") String email,
        @NotBlank(message = "Password is required") String password
) {
}

