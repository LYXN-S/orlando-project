package com.orlandoprestige.orlandoproject.customers.dto;

public record CustomerCredentialsDto(
        Long userId,
        String username,
        String passwordHash) {
}
