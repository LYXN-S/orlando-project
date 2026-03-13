package com.orlandoprestige.orlandoproject.inventory.internal.service;

import com.orlandoprestige.orlandoproject.catalog.CatalogFacade;
import com.orlandoprestige.orlandoproject.inventory.internal.domain.InventoryItem;
import com.orlandoprestige.orlandoproject.inventory.internal.domain.InventoryMovement;
import com.orlandoprestige.orlandoproject.inventory.internal.domain.MovementType;
import com.orlandoprestige.orlandoproject.inventory.internal.domain.WarehouseCode;
import com.orlandoprestige.orlandoproject.inventory.internal.domain.WarehouseStock;
import com.orlandoprestige.orlandoproject.inventory.internal.event.StockChangedEvent;
import com.orlandoprestige.orlandoproject.inventory.internal.repository.InventoryItemRepository;
import com.orlandoprestige.orlandoproject.inventory.internal.repository.InventoryMovementRepository;
import com.orlandoprestige.orlandoproject.inventory.internal.repository.WarehouseStockRepository;
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
    private final WarehouseStockRepository warehouseStockRepository;
    private final CatalogFacade catalogFacade;
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
        return applyWarehouseAdjustment(item, WarehouseCode.OFFICE, adjustment, note, staffId, "MANUAL", null,
                adjustment > 0 ? MovementType.STOCK_IN : adjustment < 0 ? MovementType.STOCK_OUT : MovementType.ADJUSTMENT);
    }

    @Transactional
    public InventoryItem stockIn(Long productId, WarehouseCode warehouseCode, int quantity, String note, Long staffId) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Stock in quantity must be greater than zero.");
        }

        InventoryItem item = itemRepository.findByProductId(productId)
                .orElseThrow(() -> new EntityNotFoundException("Inventory item not found for product: " + productId));

        return applyWarehouseAdjustment(item, warehouseCode, quantity, note, staffId, "MANUAL", null, MovementType.STOCK_IN);
    }

    @Transactional
    public void deductForOrder(Long productId, int quantity, Long orderId) {
        InventoryItem item = itemRepository.findByProductId(productId)
                .orElseThrow(() -> new EntityNotFoundException("Inventory item not found for product: " + productId));
        applyWarehouseAdjustment(
                item,
                WarehouseCode.OFFICE,
                -quantity,
                "Order #" + orderId + " approved - stock deducted",
                null,
                "ORDER",
                orderId,
                MovementType.ORDER_DEDUCTION
        );
    }

    @Transactional
    public void reverseForOrder(Long productId, int quantity, Long orderId) {
        InventoryItem item = itemRepository.findByProductId(productId)
                .orElseThrow(() -> new EntityNotFoundException("Inventory item not found for product: " + productId));

        applyWarehouseAdjustment(
            item,
            WarehouseCode.OFFICE,
            quantity,
            "Order #" + orderId + " reversed - stock restored",
            null,
            "ORDER",
            orderId,
            MovementType.ORDER_REVERSAL
        );
    }

        @Transactional
        public void deductForWarehouseOrder(Long productId, WarehouseCode warehouseCode, int quantity, Long orderId, Long staffId) {
        InventoryItem item = itemRepository.findByProductId(productId)
            .orElseThrow(() -> new EntityNotFoundException("Inventory item not found for product: " + productId));

        applyWarehouseAdjustment(
            item,
            warehouseCode,
            -quantity,
            "Order #" + orderId + " approved - stock deducted from " + warehouseCode.name(),
            staffId,
            "ORDER",
            orderId,
            MovementType.ORDER_DEDUCTION
        );
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

        ensureWarehouseRow(saved, WarehouseCode.OFFICE, stock);

        // Create initial STOCK_IN movement
        InventoryMovement movement = new InventoryMovement();
        movement.setInventoryItemId(saved.getId());
        movement.setProductId(productId);
        movement.setWarehouseCode(WarehouseCode.OFFICE);
        movement.setMovementType(MovementType.STOCK_IN);
        movement.setQuantity(stock);
        movement.setReferenceType("SYSTEM");
        movement.setNote("Initial stock from catalog seed");
        movementRepository.save(movement);

        return saved;
    }

    @Transactional(readOnly = true)
    public List<InventoryMovement> getMovements(Long productId, WarehouseCode warehouseCode, MovementType type, LocalDateTime from, LocalDateTime to) {
        return movementRepository.findFiltered(productId, warehouseCode, type, from, to);
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

    @Transactional
    public void backfillWarehouseStockFromAggregate() {
        for (InventoryItem item : itemRepository.findAll()) {
            List<WarehouseStock> existing = warehouseStockRepository.findAllByProductId(item.getProductId());
            if (!existing.isEmpty()) {
                continue;
            }

            WarehouseStock office = new WarehouseStock();
            office.setProductId(item.getProductId());
            office.setProductName(item.getProductName());
            office.setSku(item.getSku());
            office.setWarehouseCode(WarehouseCode.OFFICE);
            office.setQuantity(item.getCurrentStock());
            warehouseStockRepository.save(office);
        }
    }

    @Transactional(readOnly = true)
    public List<WarehouseCode> getWarehouses() {
        return List.of(WarehouseCode.values());
    }

    @Transactional(readOnly = true)
    public List<WarehouseStock> getWarehouseStocks(Long productId, WarehouseCode warehouseCode) {
        if (productId != null && warehouseCode != null) {
            return warehouseStockRepository.findByProductIdAndWarehouseCode(productId, warehouseCode)
                    .map(List::of)
                    .orElseGet(List::of);
        }
        if (productId != null) {
            return warehouseStockRepository.findAllByProductId(productId);
        }
        if (warehouseCode != null) {
            return warehouseStockRepository.findAllByWarehouseCode(warehouseCode);
        }
        return warehouseStockRepository.findAll();
    }

    @Transactional
    public InventoryItem updateProductMetadata(Long inventoryItemId, String productName, String sku, String category, int reorderLevel) {
        InventoryItem item = itemRepository.findById(inventoryItemId)
                .orElseThrow(() -> new EntityNotFoundException("Inventory item not found: " + inventoryItemId));

        item.setProductName(productName);
        item.setSku(sku);
        item.setReorderLevel(reorderLevel);
        InventoryItem saved = itemRepository.save(item);

        catalogFacade.updateOperationalFields(item.getProductId(), productName, sku, category);
        return saved;
    }

    private InventoryItem applyWarehouseAdjustment(
            InventoryItem item,
            WarehouseCode warehouseCode,
            int adjustment,
            String note,
            Long staffId,
            String referenceType,
            Long referenceId,
            MovementType movementType
    ) {
        WarehouseStock warehouseStock = ensureWarehouseRow(item, warehouseCode, 0);

        int newWarehouseStock = warehouseStock.getQuantity() + adjustment;
        if (newWarehouseStock < 0) {
            throw new IllegalStateException("Insufficient stock in " + warehouseCode.name() + " for product " + item.getProductId());
        }

        int newAggregateStock = item.getCurrentStock() + adjustment;
        if (newAggregateStock < 0) {
            throw new IllegalStateException("Stock cannot go below zero. Current: " + item.getCurrentStock() + ", adjustment: " + adjustment);
        }

        warehouseStock.setQuantity(newWarehouseStock);
        warehouseStockRepository.save(warehouseStock);

        item.setCurrentStock(newAggregateStock);
        InventoryItem saved = itemRepository.save(item);

        InventoryMovement movement = new InventoryMovement();
        movement.setInventoryItemId(item.getId());
        movement.setProductId(item.getProductId());
        movement.setWarehouseCode(warehouseCode);
        movement.setMovementType(movementType);
        movement.setQuantity(adjustment);
        movement.setReferenceType(referenceType);
        movement.setReferenceId(referenceId);
        movement.setNote(note);
        movement.setPerformedBy(staffId);
        movementRepository.save(movement);

        eventPublisher.publishEvent(new StockChangedEvent(item.getProductId(), newAggregateStock));
        return saved;
    }

    private WarehouseStock ensureWarehouseRow(InventoryItem item, WarehouseCode warehouseCode, int defaultQuantity) {
        return warehouseStockRepository.findByProductIdAndWarehouseCode(item.getProductId(), warehouseCode)
                .orElseGet(() -> {
                    WarehouseStock ws = new WarehouseStock();
                    ws.setProductId(item.getProductId());
                    ws.setProductName(item.getProductName());
                    ws.setSku(item.getSku());
                    ws.setWarehouseCode(warehouseCode);
                    ws.setQuantity(defaultQuantity);
                    return warehouseStockRepository.save(ws);
                });
    }
}
