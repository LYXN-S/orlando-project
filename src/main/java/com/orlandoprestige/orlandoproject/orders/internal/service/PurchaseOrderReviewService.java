package com.orlandoprestige.orlandoproject.orders.internal.service;

import com.orlandoprestige.orlandoproject.inventory.internal.domain.WarehouseCode;
import com.orlandoprestige.orlandoproject.inventory.internal.domain.WarehouseStock;
import com.orlandoprestige.orlandoproject.inventory.internal.service.InventoryService;
import com.orlandoprestige.orlandoproject.orders.internal.domain.*;
import com.orlandoprestige.orlandoproject.orders.internal.presentation.dto.WarehouseSalesDetailDto;
import com.orlandoprestige.orlandoproject.orders.internal.presentation.dto.WarehouseSalesSummaryDto;
import com.orlandoprestige.orlandoproject.orders.internal.repository.OrderRepository;
import com.orlandoprestige.orlandoproject.orders.internal.repository.POAllocationLineRepository;
import com.orlandoprestige.orlandoproject.orders.internal.repository.PurchaseOrderReviewRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderReviewService {

    private final PurchaseOrderReviewRepository poRepository;
    private final POAllocationLineRepository allocationRepository;
    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;

    @Transactional
    public PurchaseOrderReview createForOrder(Long orderId) {
        return poRepository.findByOrderId(orderId).orElseGet(() -> {
            PurchaseOrderReview po = new PurchaseOrderReview();
            po.setOrderId(orderId);
            po.setStatus(PurchaseOrderStatus.PENDING_REVIEW);
            return poRepository.save(po);
        });
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrderReview> getAll(PurchaseOrderStatus status) {
        if (status == null) {
            return poRepository.findAllByOrderByCreatedAtDesc();
        }
        return poRepository.findAllByStatusOrderByCreatedAtDesc(status);
    }

    @Transactional(readOnly = true)
    public PurchaseOrderReview getById(Long poId) {
        return poRepository.findById(poId)
                .orElseThrow(() -> new EntityNotFoundException("PO review not found: " + poId));
    }

    @Transactional
    public PurchaseOrderReview upsertAllocations(Long poId, List<POAllocationLine> inputAllocations) {
        PurchaseOrderReview po = getById(poId);
        if (po.getStatus() != PurchaseOrderStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Allocations can only be edited while PO is pending review.");
        }

        allocationRepository.deleteAllByPoReviewId(poId);
        List<POAllocationLine> toSave = new ArrayList<>();
        for (POAllocationLine line : inputAllocations) {
            line.setId(null);
            line.setPoReview(po);
            toSave.add(line);
        }

        allocationRepository.saveAll(toSave);
        return poRepository.findById(poId).orElseThrow(() -> new EntityNotFoundException("PO review not found: " + poId));
    }

    @Transactional
    public PurchaseOrderReview decide(Long poId, Long staffId, boolean approved, String note) {
        PurchaseOrderReview po = getById(poId);
        Order order = orderRepository.findById(po.getOrderId())
                .orElseThrow(() -> new EntityNotFoundException("Order not found for PO: " + po.getOrderId()));

        if (po.getStatus() != PurchaseOrderStatus.PENDING_REVIEW) {
            throw new IllegalStateException("PO review is already finalized.");
        }
        if (order.getStatus() != OrderStatus.PENDING_EVALUATION) {
            throw new IllegalStateException("Order is no longer pending evaluation.");
        }

        if (approved) {
            List<POAllocationLine> allocations = allocationRepository.findAllByPoReviewId(poId);
            validateAllocations(order, allocations);

            for (POAllocationLine allocation : allocations) {
                inventoryService.deductForWarehouseOrder(
                        allocation.getProductId(),
                        allocation.getWarehouseCode(),
                        allocation.getAllocatedQuantity(),
                        order.getId(),
                        staffId
                );
            }

            order.setStatus(OrderStatus.APPROVED);
            po.setStatus(PurchaseOrderStatus.APPROVED);
        } else {
            order.setStatus(OrderStatus.REJECTED);
            po.setStatus(PurchaseOrderStatus.REJECTED);
        }

        order.setEvaluatedByStaffId(staffId);
        order.setEvaluationNote(note);

        po.setReviewedBy(staffId);
        po.setReviewedAt(LocalDateTime.now());
        po.setReviewNote(note);

        orderRepository.save(order);
        return poRepository.save(po);
    }

    @Transactional(readOnly = true)
    public List<WarehouseSalesSummaryDto> getWarehouseSalesSummary(LocalDate from, LocalDate to, WarehouseCode warehouseCode) {
        List<WarehouseSalesDetailDto> details = getWarehouseSalesDetails(from, to, warehouseCode, null);

        Map<String, List<WarehouseSalesDetailDto>> byWarehouse = new LinkedHashMap<>();
        for (WarehouseSalesDetailDto detail : details) {
            byWarehouse.computeIfAbsent(detail.warehouseCode(), key -> new ArrayList<>()).add(detail);
        }

        List<WarehouseSalesSummaryDto> output = new ArrayList<>();
        for (Map.Entry<String, List<WarehouseSalesDetailDto>> entry : byWarehouse.entrySet()) {
            int totalQty = entry.getValue().stream().mapToInt(WarehouseSalesDetailDto::quantity).sum();
            BigDecimal gross = entry.getValue().stream().map(WarehouseSalesDetailDto::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
            int orderCount = (int) entry.getValue().stream().map(WarehouseSalesDetailDto::orderId).distinct().count();
            output.add(new WarehouseSalesSummaryDto(entry.getKey(), totalQty, gross, orderCount));
        }

        return output;
    }

    @Transactional(readOnly = true)
    public List<WarehouseSalesDetailDto> getWarehouseSalesDetails(LocalDate from, LocalDate to, WarehouseCode warehouseCode, Long productId) {
        LocalDate fromDate = from != null ? from : LocalDate.now().minusDays(30);
        LocalDate toDate = to != null ? to : LocalDate.now();

        List<PurchaseOrderReview> approvedPos = poRepository.findAllByStatusAndReviewedAtBetween(
                PurchaseOrderStatus.APPROVED,
                fromDate.atStartOfDay(),
                toDate.atTime(LocalTime.MAX)
        );

        List<WarehouseSalesDetailDto> output = new ArrayList<>();
        for (PurchaseOrderReview po : approvedPos) {
            Order order = orderRepository.findById(po.getOrderId()).orElse(null);
            if (order == null) {
                continue;
            }

            Map<Long, OrderItem> orderItemsById = new HashMap<>();
            for (OrderItem item : order.getItems()) {
                orderItemsById.put(item.getId(), item);
            }

            List<POAllocationLine> allocations = allocationRepository.findAllByPoReviewId(po.getId());
            for (POAllocationLine allocation : allocations) {
                if (warehouseCode != null && allocation.getWarehouseCode() != warehouseCode) {
                    continue;
                }
                if (productId != null && !allocation.getProductId().equals(productId)) {
                    continue;
                }

                OrderItem item = orderItemsById.get(allocation.getOrderItemId());
                if (item == null) {
                    continue;
                }

                BigDecimal amount = item.getUnitPrice().multiply(BigDecimal.valueOf(allocation.getAllocatedQuantity()));
                WarehouseCode allocationWarehouse = allocation.getWarehouseCode() != null
                    ? allocation.getWarehouseCode()
                    : WarehouseCode.OFFICE;
                output.add(new WarehouseSalesDetailDto(
                        po.getId(),
                        order.getId(),
                        po.getReviewedAt(),
                    allocationWarehouse.name(),
                        allocation.getProductId(),
                        item.getProductName(),
                        allocation.getAllocatedQuantity(),
                        item.getUnitPrice(),
                        amount
                ));
            }
        }

        output.sort(Comparator.comparing(WarehouseSalesDetailDto::reviewedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return output;
    }

    @Transactional
    public void autoAllocateOffice(Long orderId) {
        PurchaseOrderReview po = poRepository.findByOrderId(orderId)
                .orElseThrow(() -> new EntityNotFoundException("PO review not found for order: " + orderId));
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));

        List<POAllocationLine> officeAllocations = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            POAllocationLine line = new POAllocationLine();
            line.setOrderItemId(item.getId());
            line.setProductId(item.getProductId());
            line.setWarehouseCode(WarehouseCode.OFFICE);
            line.setAllocatedQuantity(item.getQuantity());
            officeAllocations.add(line);
        }

        upsertAllocations(po.getId(), officeAllocations);
    }

    private void validateAllocations(Order order, List<POAllocationLine> allocations) {
        if (allocations == null || allocations.isEmpty()) {
            throw new IllegalStateException("Cannot approve without warehouse allocations.");
        }

        Map<Long, Integer> requiredByOrderItem = new HashMap<>();
        for (OrderItem item : order.getItems()) {
            requiredByOrderItem.put(item.getId(), item.getQuantity());
        }

        Map<Long, Integer> allocatedByOrderItem = new HashMap<>();
        for (POAllocationLine allocation : allocations) {
            allocatedByOrderItem.merge(allocation.getOrderItemId(), allocation.getAllocatedQuantity(), Integer::sum);

            List<WarehouseStock> stocks = inventoryService.getWarehouseStocks(allocation.getProductId(), allocation.getWarehouseCode());
            int available = stocks.isEmpty() ? 0 : stocks.getFirst().getQuantity();
            if (available < allocation.getAllocatedQuantity()) {
                throw new IllegalStateException("Insufficient stock in " + allocation.getWarehouseCode().name() + " for product " + allocation.getProductId());
            }
        }

        for (Map.Entry<Long, Integer> required : requiredByOrderItem.entrySet()) {
            int allocated = allocatedByOrderItem.getOrDefault(required.getKey(), 0);
            if (allocated != required.getValue()) {
                throw new IllegalStateException("Allocation mismatch for order item " + required.getKey() + ": required " + required.getValue() + ", allocated " + allocated);
            }
        }
    }
}
