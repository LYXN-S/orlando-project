package com.orlandoprestige.orlandoproject.auth;

import java.util.List;

/**
 * Represents the authenticated principal extracted from the JWT token.
 * Stored in the SecurityContext so controllers can access userId without DB lookup.
 */
public record AuthenticatedUser(Long userId, String email, String role, List<String> permissions) {
}

