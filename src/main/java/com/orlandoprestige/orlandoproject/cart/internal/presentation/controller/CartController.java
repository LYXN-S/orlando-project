package com.orlandoprestige.orlandoproject.cart.internal.presentation.controller;

import com.orlandoprestige.orlandoproject.auth.AuthenticatedUser;
import com.orlandoprestige.orlandoproject.cart.internal.domain.ShoppingCart;
import com.orlandoprestige.orlandoproject.cart.internal.presentation.dto.AddCartItemDto;
import com.orlandoprestige.orlandoproject.cart.internal.presentation.dto.CartDto;
import com.orlandoprestige.orlandoproject.cart.internal.presentation.dto.CartItemDto;
import com.orlandoprestige.orlandoproject.cart.internal.presentation.dto.UpdateCartItemDto;
import com.orlandoprestige.orlandoproject.cart.internal.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Shopping cart endpoints")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @Operation(summary = "Get current customer's cart")
    public ResponseEntity<CartDto> getCart(@AuthenticationPrincipal AuthenticatedUser user) {
        return cartService.getCart(user.userId())
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(new CartDto(null, user.userId(), List.of(), BigDecimal.ZERO)));
    }

    @PostMapping("/items")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @Operation(summary = "Add an item to the cart")
    public ResponseEntity<CartDto> addItem(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody AddCartItemDto dto) {
        ShoppingCart cart = cartService.addItem(user.userId(), dto.productId(), dto.quantity());
        return ResponseEntity.ok(toDto(cart));
    }

    @PutMapping("/items/{productId}")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @Operation(summary = "Update item quantity in cart (set to 0 to remove)")
    public ResponseEntity<CartDto> updateItem(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long productId,
            @Valid @RequestBody UpdateCartItemDto dto) {
        ShoppingCart cart = cartService.updateItemQuantity(user.userId(), productId, dto.quantity());
        return ResponseEntity.ok(toDto(cart));
    }

    @DeleteMapping("/items/{productId}")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @Operation(summary = "Remove an item from the cart")
    public ResponseEntity<Void> removeItem(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long productId) {
        cartService.removeItem(user.userId(), productId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @Operation(summary = "Clear all items from the cart")
    public ResponseEntity<Void> clearCart(@AuthenticationPrincipal AuthenticatedUser user) {
        cartService.clearCart(user.userId());
        return ResponseEntity.noContent().build();
    }

    private CartDto toDto(ShoppingCart cart) {
        List<CartItemDto> items = cart.getItems().stream()
                .map(item -> new CartItemDto(
                        item.getProductId(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                ))
                .toList();

        BigDecimal total = items.stream()
                .map(CartItemDto::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartDto(cart.getId(), cart.getCustomerId(), items, total);
    }
}

