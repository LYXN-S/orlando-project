package com.orlandoprestige.orlandoproject.users.dto;

public record UserCredentialsDto(
        Long userId,
        String username,
        String passwordHash) {
}
