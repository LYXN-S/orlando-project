package com.orlandoprestige.orlandoproject.orders.internal.presentation.controller;

import com.orlandoprestige.orlandoproject.auth.AuthenticatedUser;
import com.orlandoprestige.orlandoproject.inventory.internal.domain.WarehouseCode;
import com.orlandoprestige.orlandoproject.orders.internal.domain.*;
import com.orlandoprestige.orlandoproject.orders.internal.presentation.dto.*;
import com.orlandoprestige.orlandoproject.orders.internal.repository.POAllocationLineRepository;
import com.orlandoprestige.orlandoproject.orders.internal.repository.OrderRepository;
import com.orlandoprestige.orlandoproject.orders.internal.service.PurchaseOrderReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/po-reviews")
@RequiredArgsConstructor
@Tag(name = "PO Reviews", description = "Customer order PO review workflow")
public class PurchaseOrderReviewController {

    private final PurchaseOrderReviewService poService;
    private final OrderRepository orderRepository;
    private final POAllocationLineRepository allocationRepository;

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or @permissionChecker.has(authentication, 'MANAGE_ORDERS')")
    @Operation(summary = "List PO reviews")
    public ResponseEntity<List<POReviewDto>> list(@RequestParam(required = false) String status) {
        PurchaseOrderStatus poStatus = null;
        if (status != null && !status.isBlank()) {
            poStatus = PurchaseOrderStatus.valueOf(status);
        }
        List<POReviewDto> output = poService.getAll(poStatus).stream().map(this::toDto).toList();
        return ResponseEntity.ok(output);
    }

    @GetMapping("/{poId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @permissionChecker.has(authentication, 'MANAGE_ORDERS')")
    @Operation(summary = "Get PO review detail")
    public ResponseEntity<POReviewDto> detail(@PathVariable Long poId) {
        return ResponseEntity.ok(toDto(poService.getById(poId)));
    }

    @PutMapping("/{poId}/allocations")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @permissionChecker.has(authentication, 'MANAGE_ORDERS')")
    @Operation(summary = "Upsert PO warehouse allocations")
    public ResponseEntity<POReviewDto> upsertAllocations(
            @PathVariable Long poId,
            @Valid @RequestBody List<POAllocationRequestDto> allocations) {

        List<POAllocationLine> lines = allocations.stream().map(input -> {
            POAllocationLine line = new POAllocationLine();
            line.setOrderItemId(input.orderItemId());
            line.setProductId(input.productId());
            line.setWarehouseCode(WarehouseCode.from(input.warehouseCode()));
            line.setAllocatedQuantity(input.allocatedQuantity());
            return line;
        }).toList();

        PurchaseOrderReview updated = poService.upsertAllocations(poId, lines);
        return ResponseEntity.ok(toDto(updated));
    }

    @PatchMapping("/{poId}/decision")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @permissionChecker.has(authentication, 'MANAGE_ORDERS')")
    @Operation(summary = "Approve or reject PO review")
    public ResponseEntity<POReviewDto> decide(
            @PathVariable Long poId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody POReviewDecisionDto decision) {
        PurchaseOrderReview updated = poService.decide(poId, user.userId(), decision.approved(), decision.note());
        return ResponseEntity.ok(toDto(updated));
    }

    @GetMapping("/reports/sales/warehouses/summary")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @permissionChecker.has(authentication, 'VIEW_DASHBOARD')")
    @Operation(summary = "Warehouse sales summary")
    public ResponseEntity<List<WarehouseSalesSummaryDto>> warehouseSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String warehouseCode) {
        WarehouseCode warehouse = warehouseCode != null ? WarehouseCode.from(warehouseCode) : null;
        return ResponseEntity.ok(poService.getWarehouseSalesSummary(from, to, warehouse));
    }

    @GetMapping("/reports/sales/warehouses/details")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @permissionChecker.has(authentication, 'VIEW_DASHBOARD')")
    @Operation(summary = "Warehouse sales detailed rows")
    public ResponseEntity<List<WarehouseSalesDetailDto>> warehouseDetails(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) Long productId) {
        WarehouseCode warehouse = warehouseCode != null ? WarehouseCode.from(warehouseCode) : null;
        return ResponseEntity.ok(poService.getWarehouseSalesDetails(from, to, warehouse, productId));
    }

    private POReviewDto toDto(PurchaseOrderReview po) {
        Order order = orderRepository.findById(po.getOrderId())
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + po.getOrderId()));

        List<OrderItemDto> items = order.getItems().stream().map(item -> {
            BigDecimal subtotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            return new OrderItemDto(
                    item.getId(),
                    item.getProductId(),
                    item.getProductName(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    subtotal,
                    null
            );
        }).toList();

        List<POAllocationDto> allocations = allocationRepository.findAllByPoReviewId(po.getId()).stream()
                .map(line -> new POAllocationDto(
                        line.getId(),
                        line.getOrderItemId(),
                        line.getProductId(),
                line.getWarehouseCode() != null ? line.getWarehouseCode().name() : WarehouseCode.OFFICE.name(),
                        line.getAllocatedQuantity()
                )).toList();

        PurchaseOrderStatus status = po.getStatus() != null ? po.getStatus() : PurchaseOrderStatus.PENDING_REVIEW;

        return new POReviewDto(
                po.getId(),
                po.getOrderId(),
            status.name(),
                order.getCustomerId(),
                order.getCreatedAt(),
                po.getReviewedBy(),
                po.getReviewedAt(),
                po.getReviewNote(),
                items,
                allocations,
                order.getBillingType() != null ? order.getBillingType().name() : null,
                order.getBillingName(),
                order.getBillingTin(),
                order.getBillingAddress(),
                order.getBillingTerms()
        );
    }
}
