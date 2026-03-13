package com.orlandoprestige.orlandoproject.notifications.internal.service;

import com.orlandoprestige.orlandoproject.notifications.internal.domain.NotificationType;
import com.orlandoprestige.orlandoproject.orders.internal.event.OrderEvaluatedEvent;
import com.orlandoprestige.orlandoproject.orders.internal.event.OrderSubmittedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;

    /**
     * When a new order is submitted, notify all staff members.
     * Uses @ApplicationModuleListener for guaranteed delivery.
     */
    @ApplicationModuleListener
    void onOrderSubmitted(OrderSubmittedEvent event) {
        log.info("Received OrderSubmittedEvent for order #{}", event.orderId());

        int itemCount = event.items().size();
        String title = "New Order #" + event.orderId();
        String message = "A new order with " + itemCount + " item(s) has been submitted and is pending evaluation.";

        notificationService.createStaffBroadcast(
                NotificationType.ORDER_SUBMITTED,
                title,
                message,
                event.orderId(),
                "MANAGE_ORDERS"
        );
    }

    /**
     * When an order is evaluated (approved/rejected), notify the customer.
     * Uses @ApplicationModuleListener for guaranteed delivery.
     */
    @ApplicationModuleListener
    void onOrderEvaluated(OrderEvaluatedEvent event) {
        log.info("Received OrderEvaluatedEvent for order #{} - approved: {}", event.orderId(), event.approved());

        NotificationType type = event.approved() ? NotificationType.ORDER_APPROVED : NotificationType.ORDER_REJECTED;
        String title = event.approved()
                ? "Order #" + event.orderId() + " Approved"
                : "Order #" + event.orderId() + " Rejected";
        String message = event.approved()
                ? "Your order #" + event.orderId() + " has been approved and is being processed."
                : "Your order #" + event.orderId() + " has been rejected."
                  + (event.note() != null && !event.note().isBlank() ? " Reason: " + event.note() : "");

        notificationService.createNotification(
                event.customerId(),
                "ROLE_CUSTOMER",
                type,
                title,
                message,
                event.orderId()
        );
    }
}
