package com.orlandoprestige.orlandoproject.inventory.internal.service;

import com.orlandoprestige.orlandoproject.inventory.internal.domain.InventoryItem;
import com.orlandoprestige.orlandoproject.inventory.internal.domain.InventoryMovement;
import com.orlandoprestige.orlandoproject.inventory.internal.domain.MovementType;
import com.orlandoprestige.orlandoproject.inventory.internal.event.StockChangedEvent;
import com.orlandoprestige.orlandoproject.inventory.internal.repository.InventoryItemRepository;
import com.orlandoprestige.orlandoproject.inventory.internal.repository.InventoryMovementRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryItemRepository itemRepository;
    private final InventoryMovementRepository movementRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<InventoryItem> getAll() {
        return itemRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<InventoryItem> getById(Long id) {
        return itemRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<InventoryItem> getByProductId(Long productId) {
        return itemRepository.findByProductId(productId);
    }

    @Transactional
    public InventoryItem adjustStock(Long inventoryItemId, int adjustment, String note, Long staffId) {
        InventoryItem item = itemRepository.findById(inventoryItemId)
                .orElseThrow(() -> new EntityNotFoundException("Inventory item not found: " + inventoryItemId));

        int newStock = item.getCurrentStock() + adjustment;
        if (newStock < 0) {
            throw new IllegalStateException("Stock cannot go below zero. Current: " + item.getCurrentStock() + ", adjustment: " + adjustment);
        }

        item.setCurrentStock(newStock);
        InventoryItem saved = itemRepository.save(item);

        // Create movement record
        InventoryMovement movement = new InventoryMovement();
        movement.setInventoryItemId(item.getId());
        movement.setProductId(item.getProductId());
        movement.setMovementType(adjustment > 0 ? MovementType.STOCK_IN : adjustment < 0 ? MovementType.STOCK_OUT : MovementType.ADJUSTMENT);
        movement.setQuantity(adjustment);
        movement.setReferenceType("MANUAL");
        movement.setNote(note);
        movement.setPerformedBy(staffId);
        movementRepository.save(movement);

        // Publish event so catalog stays in sync
        eventPublisher.publishEvent(new StockChangedEvent(item.getProductId(), newStock));

        log.info("Stock adjusted for product {} (inventory item {}): {} → {}", item.getProductId(), item.getId(), item.getCurrentStock() - adjustment, newStock);
        return saved;
    }

    @Transactional
    public void deductForOrder(Long productId, int quantity, Long orderId) {
        InventoryItem item = itemRepository.findByProductId(productId)
                .orElseThrow(() -> new EntityNotFoundException("Inventory item not found for product: " + productId));

        int newStock = item.getCurrentStock() - quantity;
        if (newStock < 0) {
            throw new IllegalStateException("Insufficient stock for product " + productId + ". Current: " + item.getCurrentStock() + ", requested: " + quantity);
        }

        item.setCurrentStock(newStock);
        itemRepository.save(item);

        InventoryMovement movement = new InventoryMovement();
        movement.setInventoryItemId(item.getId());
        movement.setProductId(productId);
        movement.setMovementType(MovementType.ORDER_DEDUCTION);
        movement.setQuantity(-quantity);
        movement.setReferenceType("ORDER");
        movement.setReferenceId(orderId);
        movement.setNote("Order #" + orderId + " approved - stock deducted");
        movementRepository.save(movement);

        eventPublisher.publishEvent(new StockChangedEvent(productId, newStock));
        log.info("Stock deducted for order #{}: product {} qty {} → new stock {}", orderId, productId, quantity, newStock);
    }

    @Transactional
    public void reverseForOrder(Long productId, int quantity, Long orderId) {
        InventoryItem item = itemRepository.findByProductId(productId)
                .orElseThrow(() -> new EntityNotFoundException("Inventory item not found for product: " + productId));

        int newStock = item.getCurrentStock() + quantity;
        item.setCurrentStock(newStock);
        itemRepository.save(item);

        InventoryMovement movement = new InventoryMovement();
        movement.setInventoryItemId(item.getId());
        movement.setProductId(productId);
        movement.setMovementType(MovementType.ORDER_REVERSAL);
        movement.setQuantity(quantity);
        movement.setReferenceType("ORDER");
        movement.setReferenceId(orderId);
        movement.setNote("Order #" + orderId + " reversed - stock restored");
        movementRepository.save(movement);

        eventPublisher.publishEvent(new StockChangedEvent(productId, newStock));
        log.info("Stock reversed for order #{}: product {} qty {} → new stock {}", orderId, productId, quantity, newStock);
    }

    @Transactional
    public InventoryItem createOrUpdate(Long productId, String productName, String sku, int stock) {
        Optional<InventoryItem> existing = itemRepository.findByProductId(productId);
        if (existing.isPresent()) {
            InventoryItem item = existing.get();
            item.setProductName(productName);
            item.setSku(sku);
            return itemRepository.save(item);
        }

        InventoryItem item = new InventoryItem();
        item.setProductId(productId);
        item.setProductName(productName);
        item.setSku(sku);
        item.setCurrentStock(stock);
        item.setReorderLevel(5);
        InventoryItem saved = itemRepository.save(item);

        // Create initial STOCK_IN movement
        InventoryMovement movement = new InventoryMovement();
        movement.setInventoryItemId(saved.getId());
        movement.setProductId(productId);
        movement.setMovementType(MovementType.STOCK_IN);
        movement.setQuantity(stock);
        movement.setReferenceType("SYSTEM");
        movement.setNote("Initial stock from catalog seed");
        movementRepository.save(movement);

        return saved;
    }

    @Transactional(readOnly = true)
    public List<InventoryMovement> getMovements(Long productId, MovementType type, LocalDateTime from, LocalDateTime to) {
        return movementRepository.findFiltered(productId, type, from, to);
    }

    @Transactional(readOnly = true)
    public List<InventoryMovement> getDailyMovements(LocalDate date) {
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.atTime(LocalTime.MAX);
        return movementRepository.findByDay(dayStart, dayEnd);
    }

    public boolean hasInventoryItems() {
        return itemRepository.count() > 0;
    }
}
