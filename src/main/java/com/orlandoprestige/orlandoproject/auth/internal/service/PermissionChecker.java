package com.orlandoprestige.orlandoproject.auth.internal.service;

import com.orlandoprestige.orlandoproject.auth.AuthenticatedUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * SpEL-accessible bean for permission checks in @PreAuthorize expressions.
 * Usage: @PreAuthorize("@permissionChecker.has(authentication, 'MANAGE_PRODUCTS')")
 */
@Component("permissionChecker")
public class PermissionChecker {

    public boolean has(Authentication authentication, String permission) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return false;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof AuthenticatedUser user)) {
            return false;
        }

        // Super admin always has all permissions
        if ("ROLE_SUPER_ADMIN".equals(user.role())) {
            return true;
        }

        return user.permissions() != null && user.permissions().contains(permission);
    }
}
