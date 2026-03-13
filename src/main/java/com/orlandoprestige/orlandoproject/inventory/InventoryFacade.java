package com.orlandoprestige.orlandoproject.inventory;

import com.orlandoprestige.orlandoproject.inventory.dto.InventoryInfoDto;
import com.orlandoprestige.orlandoproject.inventory.internal.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Public facade for the Inventory module.
 * Other modules must use this class to interact with inventory data.
 */
@Service
@RequiredArgsConstructor
public class InventoryFacade {

    private final InventoryService inventoryService;

    public Optional<InventoryInfoDto> findByProductId(Long productId) {
        return inventoryService.getByProductId(productId)
                .map(item -> new InventoryInfoDto(
                        item.getId(),
                        item.getProductId(),
                        item.getProductName(),
                        item.getSku(),
                        item.getCurrentStock(),
                        item.getReorderLevel()
                ));
    }

    public void adjustStock(Long productId, int adjustment, String note, Long staffId) {
        inventoryService.getByProductId(productId).ifPresent(item ->
                inventoryService.adjustStock(item.getId(), adjustment, note, staffId));
    }

    public void createOrUpdate(Long productId, String productName, String sku, int stock) {
        inventoryService.createOrUpdate(productId, productName, sku, stock);
    }
}
