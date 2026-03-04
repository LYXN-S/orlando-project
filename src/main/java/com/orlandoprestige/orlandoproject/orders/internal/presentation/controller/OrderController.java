package com.orlandoprestige.orlandoproject.orders.internal.presentation.controller;

import com.orlandoprestige.orlandoproject.auth.AuthenticatedUser;
import com.orlandoprestige.orlandoproject.orders.internal.domain.Order;
import com.orlandoprestige.orlandoproject.orders.internal.domain.OrderItem;
import com.orlandoprestige.orlandoproject.orders.internal.presentation.dto.EvaluateOrderDto;
import com.orlandoprestige.orlandoproject.orders.internal.presentation.dto.OrderDto;
import com.orlandoprestige.orlandoproject.orders.internal.presentation.dto.OrderItemDto;
import com.orlandoprestige.orlandoproject.orders.internal.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    @PostMapping
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @Operation(summary = "Submit an order from the current cart")
    public ResponseEntity<OrderDto> submitOrder(@AuthenticationPrincipal AuthenticatedUser user) {
        Order order = orderService.submitOrder(user.userId());
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
    @Operation(summary = "Get all pending orders (Staff with permission)")
    public ResponseEntity<List<OrderDto>> getPendingOrders() {
        List<OrderDto> orders = orderService.getAllPendingOrders()
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
                order.getEvaluationNote()
        );
    }

    private OrderItemDto toItemDto(OrderItem item) {
        BigDecimal subtotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        return new OrderItemDto(
                item.getProductId(),
                item.getProductName(),
                item.getQuantity(),
                item.getUnitPrice(),
                subtotal
        );
    }
}

