package com.orlandoprestige.orlandoproject.orders.internal.presentation.controller;

import com.orlandoprestige.orlandoproject.auth.AuthenticatedUser;
import com.orlandoprestige.orlandoproject.orders.internal.domain.Order;
import com.orlandoprestige.orlandoproject.orders.internal.domain.OrderItem;
import com.orlandoprestige.orlandoproject.orders.internal.presentation.dto.EvaluateOrderDto;
import com.orlandoprestige.orlandoproject.orders.internal.presentation.dto.OrderDto;
import com.orlandoprestige.orlandoproject.orders.internal.presentation.dto.OrderItemDto;
import com.orlandoprestige.orlandoproject.orders.internal.presentation.dto.SubmitOrderDto;
import com.orlandoprestige.orlandoproject.orders.internal.service.OrderService;
import com.orlandoprestige.orlandoproject.orders.internal.service.PurchaseOrderReviewService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order management endpoints")
public class OrderController {

    private final OrderService orderService;
    private final PurchaseOrderReviewService purchaseOrderReviewService;

    @PostMapping
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @Operation(summary = "Submit an order with items and billing details")
    public ResponseEntity<OrderDto> submitOrder(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody SubmitOrderDto dto) {
        Order order = orderService.submitOrder(user.userId(), dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(order));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @Operation(summary = "Get current customer's orders")
    public ResponseEntity<List<OrderDto>> getMyOrders(@AuthenticationPrincipal AuthenticatedUser user) {
        List<OrderDto> orders = orderService.getOrdersByCustomer(user.userId())
                .stream().map(this::toDto).toList();
        return ResponseEntity.ok(orders);
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or @permissionChecker.has(authentication, 'MANAGE_ORDERS')")
    @Operation(summary = "Get all orders (Staff with permission)")
    public ResponseEntity<List<OrderDto>> getAllOrders() {
        List<OrderDto> orders = orderService.getAllOrders()
                .stream().map(this::toDto).toList();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @permissionChecker.has(authentication, 'MANAGE_ORDERS') or @orderAccessGuard.isOwner(#id, authentication)")
    @Operation(summary = "Get order by ID (Staff with permission or order owner)")
    public ResponseEntity<OrderDto> getOrderById(@PathVariable Long id) {
        return orderService.getOrderById(id)
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/evaluate")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @permissionChecker.has(authentication, 'MANAGE_ORDERS')")
    @Operation(summary = "Approve or reject an order (Staff with permission)")
    public ResponseEntity<OrderDto> evaluateOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody EvaluateOrderDto dto) {
        Order order = orderService.evaluateOrder(id, user.userId(), dto.approved(), dto.note());
        return ResponseEntity.ok(toDto(order));
    }

    @GetMapping("/{id}/sales-invoice/pdf")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @permissionChecker.has(authentication, 'MANAGE_ORDERS') or @orderAccessGuard.isOwner(#id, authentication)")
    @Operation(summary = "Open sales invoice PDF for approved order")
    public ResponseEntity<byte[]> getOrderSalesInvoicePdf(@PathVariable Long id) {
        byte[] pdf = purchaseOrderReviewService.generateSalesInvoicePdfForOrder(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"sales-invoice-order-" + id + ".pdf\"")
                .body(pdf);
    }

    private OrderDto toDto(Order order) {
        List<OrderItemDto> itemDtos = order.getItems().stream()
                .map(this::toItemDto)
                .toList();

        BigDecimal total = itemDtos.stream()
                .map(OrderItemDto::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new OrderDto(
                order.getId(),
                order.getCustomerId(),
                order.getStatus(),
                itemDtos,
                total,
                order.getCreatedAt(),
                order.getEvaluatedByStaffId(),
                order.getEvaluationNote(),
                order.getBillingType() != null ? order.getBillingType().name() : null,
                order.getBillingName(),
                order.getBillingTin(),
                order.getBillingAddress(),
                order.getBillingTerms()
        );
    }

    private OrderItemDto toItemDto(OrderItem item) {
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
    }
}

