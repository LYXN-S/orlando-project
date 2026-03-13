package com.orlandoprestige.orlandoproject.catalog.internal.service;

import com.orlandoprestige.orlandoproject.inventory.internal.event.StockChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Listens for StockChangedEvent from the Inventory module
 * and syncs Product.stockQuantity to keep catalog reads fast.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CatalogStockSyncListener {

    private final ProductService productService;

    @ApplicationModuleListener
    void on(StockChangedEvent event) {
        productService.findById(event.productId()).ifPresent(product -> {
            product.setStockQuantity(event.newStock());
            productService.save(product);
            log.info("Synced Product {} stockQuantity to {}", event.productId(), event.newStock());
        });
    }
}
