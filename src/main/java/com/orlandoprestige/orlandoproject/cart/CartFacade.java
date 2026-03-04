package com.orlandoprestige.orlandoproject.cart;

import com.orlandoprestige.orlandoproject.cart.dto.CartItemSummaryDto;
import com.orlandoprestige.orlandoproject.cart.dto.CartSummaryDto;
import com.orlandoprestige.orlandoproject.cart.internal.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Public facade for the Cart module.
 * The Orders module uses this to read and clear cart data at checkout.
 */
@Service
@RequiredArgsConstructor
public class CartFacade {

    private final CartService cartService;

    public Optional<CartSummaryDto> getCartByCustomerId(Long customerId) {
        return cartService.getCart(customerId)
                .map(cart -> {
                    var items = cart.getItems().stream()
                            .map(item -> new CartItemSummaryDto(
                                    item.getProductId(),
                                    item.getQuantity(),
                                    item.getUnitPrice()
                            ))
                            .toList();
                    return new CartSummaryDto(cart.getId(), cart.getCustomerId(), items);
                });
    }

    public void clearCart(Long customerId) {
        cartService.clearCart(customerId);
    }
}

