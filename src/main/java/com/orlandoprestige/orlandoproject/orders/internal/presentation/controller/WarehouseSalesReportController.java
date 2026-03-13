package com.orlandoprestige.orlandoproject.orders.internal.presentation.controller;

import com.orlandoprestige.orlandoproject.inventory.internal.domain.WarehouseCode;
import com.orlandoprestige.orlandoproject.orders.internal.presentation.dto.WarehouseSalesDetailDto;
import com.orlandoprestige.orlandoproject.orders.internal.presentation.dto.WarehouseSalesSummaryDto;
import com.orlandoprestige.orlandoproject.orders.internal.service.PurchaseOrderReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports/sales/warehouses")
@RequiredArgsConstructor
@Tag(name = "Warehouse Sales Reports", description = "Sales metrics grouped by warehouse")
public class WarehouseSalesReportController {

    private final PurchaseOrderReviewService poService;

    @GetMapping("/summary")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @permissionChecker.has(authentication, 'VIEW_DASHBOARD')")
    @Operation(summary = "Warehouse sales summary")
    public ResponseEntity<List<WarehouseSalesSummaryDto>> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String warehouseCode) {
        WarehouseCode warehouse = warehouseCode != null ? WarehouseCode.from(warehouseCode) : null;
        return ResponseEntity.ok(poService.getWarehouseSalesSummary(from, to, warehouse));
    }

    @GetMapping("/details")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @permissionChecker.has(authentication, 'VIEW_DASHBOARD')")
    @Operation(summary = "Warehouse sales detail rows")
    public ResponseEntity<List<WarehouseSalesDetailDto>> details(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam(required = false) Long productId) {
        WarehouseCode warehouse = warehouseCode != null ? WarehouseCode.from(warehouseCode) : null;
        return ResponseEntity.ok(poService.getWarehouseSalesDetails(from, to, warehouse, productId));
    }
}
