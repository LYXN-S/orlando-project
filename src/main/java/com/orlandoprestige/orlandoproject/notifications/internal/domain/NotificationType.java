package com.orlandoprestige.orlandoproject.notifications.internal.domain;

public enum NotificationType {
    ORDER_SUBMITTED,        // Staff: new order placed
    ORDER_APPROVED,         // Customer: your order was approved
    ORDER_REJECTED,         // Customer: your order was rejected
    LOW_STOCK               // Staff: product stock is low
}
