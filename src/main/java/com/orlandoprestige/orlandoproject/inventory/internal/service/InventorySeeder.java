package com.orlandoprestige.orlandoproject.inventory.internal.service;

import com.orlandoprestige.orlandoproject.catalog.CatalogFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Seeds inventory_items table from existing products on first startup.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InventorySeeder {

    private final InventoryService inventoryService;
    private final CatalogFacade catalogFacade;

    @EventListener(ApplicationReadyEvent.class)
    public void seedInventory() {
        if (inventoryService.hasInventoryItems()) {
            log.info("Inventory items already exist — skipping seed.");
            return;
        }

        log.info("Seeding inventory from existing products...");
        catalogFacade.findAll().forEach(product -> {
            inventoryService.createOrUpdate(
                    product.id(),
                    product.name(),
                    product.sku(),
                    product.stockQuantity()
            );
            log.info("Seeded inventory for product: {} (stock: {})", product.name(), product.stockQuantity());
        });
        log.info("Inventory seeding complete.");
    }
}
