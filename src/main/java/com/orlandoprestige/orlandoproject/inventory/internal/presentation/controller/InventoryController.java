package com.orlandoprestige.orlandoproject.inventory.internal.presentation.controller;

import com.orlandoprestige.orlandoproject.auth.AuthenticatedUser;
import com.orlandoprestige.orlandoproject.inventory.internal.domain.InventoryItem;
import com.orlandoprestige.orlandoproject.inventory.internal.domain.InventoryMovement;
import com.orlandoprestige.orlandoproject.inventory.internal.domain.MovementType;
import com.orlandoprestige.orlandoproject.inventory.internal.domain.WarehouseCode;
import com.orlandoprestige.orlandoproject.inventory.internal.domain.WarehouseStock;
import com.orlandoprestige.orlandoproject.inventory.internal.presentation.dto.*;
import com.orlandoprestige.orlandoproject.inventory.internal.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "Inventory management endpoints")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or @permissionChecker.has(authentication, 'MANAGE_INVENTORY')")
    @Operation(summary = "List all inventory items")
    public ResponseEntity<List<InventoryItemDto>> getAll() {
        List<InventoryItemDto> items = inventoryService.getAll().stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(items);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @permissionChecker.has(authentication, 'MANAGE_INVENTORY')")
    @Operation(summary = "Get a single inventory item")
    public ResponseEntity<InventoryItemDto> getById(@PathVariable Long id) {
        return inventoryService.getById(id)
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/adjust")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @permissionChecker.has(authentication, 'MANAGE_INVENTORY')")
    @Operation(summary = "Manually adjust stock for an inventory item")
    public ResponseEntity<InventoryItemDto> adjustStock(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody StockAdjustmentRequestDto dto) {
        InventoryItem updated = inventoryService.adjustStock(id, dto.adjustment(), dto.note(), user.userId());
        return ResponseEntity.ok(toDto(updated));
    }

        @GetMapping("/warehouses")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "List fixed warehouse references")
        public ResponseEntity<List<WarehouseDto>> getWarehouses() {
                List<WarehouseDto> warehouses = inventoryService.getWarehouses().stream()
                                .map(w -> new WarehouseDto(w.name(), w.displayName()))
                                .toList();
                return ResponseEntity.ok(warehouses);
        }

        @GetMapping("/warehouses/stocks")
        @PreAuthorize("hasRole('SUPER_ADMIN') or @permissionChecker.has(authentication, 'MANAGE_INVENTORY')")
        @Operation(summary = "List per-warehouse stock")
        public ResponseEntity<List<WarehouseStockDto>> getWarehouseStocks(
                        @RequestParam(required = false) Long productId,
                        @RequestParam(required = false) String warehouseCode) {
                WarehouseCode code = warehouseCode != null ? WarehouseCode.from(warehouseCode) : null;
                List<WarehouseStockDto> stocks = inventoryService.getWarehouseStocks(productId, code)
                                .stream()
                                .map(this::toWarehouseStockDto)
                                .toList();
                return ResponseEntity.ok(stocks);
        }

        @PostMapping("/stock-in")
        @PreAuthorize("hasRole('SUPER_ADMIN') or @permissionChecker.has(authentication, 'MANAGE_INVENTORY')")
        @Operation(summary = "Add stock to a specific warehouse")
        public ResponseEntity<InventoryItemDto> stockIn(
                        @AuthenticationPrincipal AuthenticatedUser user,
                        @Valid @RequestBody StockInRequestDto dto) {
                InventoryItem updated = inventoryService.stockIn(
                                dto.productId(),
                                WarehouseCode.from(dto.warehouseCode()),
                                dto.quantity(),
                                dto.note(),
                                user.userId()
                );
                return ResponseEntity.ok(toDto(updated));
        }

    @GetMapping("/movements")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @permissionChecker.has(authentication, 'MANAGE_INVENTORY')")
    @Operation(summary = "List inventory movements with optional filters")
    public ResponseEntity<List<InventoryMovementDto>> getMovements(
            @RequestParam(required = false) Long productId,
                        @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        MovementType movementType = type != null ? MovementType.valueOf(type) : null;
                WarehouseCode movementWarehouse = warehouseCode != null ? WarehouseCode.from(warehouseCode) : null;
        LocalDateTime fromDt = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDt = to != null ? to.atTime(LocalTime.MAX) : null;

                List<InventoryMovementDto> movements = inventoryService.getMovements(productId, movementWarehouse, movementType, fromDt, toDt)
                .stream()
                .map(this::toMovementDto)
                .toList();
        return ResponseEntity.ok(movements);
    }

    @GetMapping("/movements/daily")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @permissionChecker.has(authentication, 'MANAGE_INVENTORY')")
    @Operation(summary = "Get daily in/out summary per product")
    public ResponseEntity<List<DailySummaryDto>> getDailySummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDate targetDate = date != null ? date : LocalDate.now();
        List<InventoryMovement> movements = inventoryService.getDailyMovements(targetDate);

        // Group by product and calculate totals
        Map<Long, List<InventoryMovement>> byProduct = movements.stream()
                .collect(Collectors.groupingBy(InventoryMovement::getProductId));

        List<DailySummaryDto> summaries = new ArrayList<>();
        for (Map.Entry<Long, List<InventoryMovement>> entry : byProduct.entrySet()) {
            List<InventoryMovement> productMovements = entry.getValue();
            String productName = productMovements.getFirst().getNote() != null
                    ? getProductName(productMovements.getFirst())
                    : "Product #" + entry.getKey();

            int totalIn = productMovements.stream()
                    .filter(m -> m.getQuantity() > 0)
                    .mapToInt(InventoryMovement::getQuantity)
                    .sum();
            int totalOut = productMovements.stream()
                    .filter(m -> m.getQuantity() < 0)
                    .mapToInt(m -> Math.abs(m.getQuantity()))
                    .sum();

            // Get product name from inventory item
            String name = inventoryService.getByProductId(entry.getKey())
                    .map(InventoryItem::getProductName)
                    .orElse("Product #" + entry.getKey());
            String sku = inventoryService.getByProductId(entry.getKey())
                    .map(InventoryItem::getSku)
                    .orElse("");

            summaries.add(new DailySummaryDto(entry.getKey(), name, sku, totalIn, totalOut, totalIn - totalOut));
        }

        return ResponseEntity.ok(summaries);
    }

    private String getProductName(InventoryMovement movement) {
        return inventoryService.getByProductId(movement.getProductId())
                .map(InventoryItem::getProductName)
                .orElse("Unknown");
    }

    private InventoryItemDto toDto(InventoryItem item) {
        String status;
        if (item.getCurrentStock() == 0) {
            status = "OUT_OF_STOCK";
        } else if (item.getCurrentStock() <= item.getReorderLevel()) {
            status = "LOW_STOCK";
        } else {
            status = "IN_STOCK";
        }

        return new InventoryItemDto(
                item.getId(),
                item.getProductId(),
                item.getProductName(),
                item.getSku(),
                item.getCurrentStock(),
                item.getReorderLevel(),
                status,
                item.getLastUpdated()
        );
    }

    private InventoryMovementDto toMovementDto(InventoryMovement m) {
        String productName = inventoryService.getByProductId(m.getProductId())
                .map(InventoryItem::getProductName)
                .orElse("Unknown");

        return new InventoryMovementDto(
                m.getId(),
                m.getInventoryItemId(),
                m.getProductId(),
                productName,
                                m.getWarehouseCode() != null ? m.getWarehouseCode().name() : null,
                m.getMovementType().name(),
                m.getQuantity(),
                m.getReferenceType(),
                m.getReferenceId(),
                m.getNote(),
                m.getPerformedBy(),
                m.getCreatedAt()
        );
    }

        private WarehouseStockDto toWarehouseStockDto(WarehouseStock stock) {
                return new WarehouseStockDto(
                                stock.getId(),
                                stock.getProductId(),
                                stock.getProductName(),
                                stock.getSku(),
                                stock.getWarehouseCode().name(),
                                stock.getWarehouseCode().displayName(),
                                stock.getQuantity(),
                                stock.getLastUpdated()
                );
        }
}
