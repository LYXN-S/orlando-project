package com.orlandoprestige.orlandoproject.inventory.internal.service;

import com.orlandoprestige.orlandoproject.orders.internal.event.OrderEvaluatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

/**
 * Listens for order events and updates inventory accordingly.
 * Uses @ApplicationModuleListener for guaranteed delivery via Spring Modulith Event Publication Registry.
 * If stock deduction fails, Modulith automatically retries the event later.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryEventListener {

    private final InventoryService inventoryService;

    /**
     * When an order is approved, deduct stock for each item.
     * If this fails, Modulith automatically retries the event.
     */
    @ApplicationModuleListener
    void on(OrderEvaluatedEvent event) {
        if (!event.approved()) {
            log.info("Order #{} rejected — no inventory changes needed", event.orderId());
            return;
        }

        log.info("Order #{} approved — deducting inventory for {} item(s)", event.orderId(), event.items().size());

        for (OrderEvaluatedEvent.OrderItemSnapshot item : event.items()) {
            inventoryService.deductForOrder(item.productId(), item.quantity(), event.orderId());
        }

        log.info("Inventory deduction complete for order #{}", event.orderId());
    }
}
