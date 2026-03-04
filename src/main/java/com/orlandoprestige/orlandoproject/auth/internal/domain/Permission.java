package com.orlandoprestige.orlandoproject.auth.internal.domain;

/**
 * Granular permissions that can be assigned to staff members.
 * Super admins implicitly have ALL permissions.
 */
public enum Permission {
    VIEW_DASHBOARD,
    MANAGE_PRODUCTS,
    MANAGE_ORDERS,
    MANAGE_INVENTORY,
    MANAGE_USERS,
    MANAGE_PERMISSIONS,
    CREATE_ADMIN
}
