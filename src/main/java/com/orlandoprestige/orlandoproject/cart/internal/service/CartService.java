package com.orlandoprestige.orlandoproject.cart.internal.service;

import com.orlandoprestige.orlandoproject.cart.internal.domain.CartItem;
import com.orlandoprestige.orlandoproject.cart.internal.domain.ShoppingCart;
import com.orlandoprestige.orlandoproject.cart.internal.repository.ShoppingCartRepository;
import com.orlandoprestige.orlandoproject.catalog.CatalogFacade;
import com.orlandoprestige.orlandoproject.catalog.dto.ProductInfoDto;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final ShoppingCartRepository cartRepository;
    private final CatalogFacade catalogFacade;

    @Transactional
    public ShoppingCart getOrCreateCart(Long customerId) {
        return cartRepository.findByCustomerId(customerId)
                .orElseGet(() -> {
                    ShoppingCart cart = new ShoppingCart();
                    cart.setCustomerId(customerId);
                    return cartRepository.save(cart);
                });
    }

    @Transactional(readOnly = true)
    public Optional<ShoppingCart> getCart(Long customerId) {
        return cartRepository.findByCustomerId(customerId);
    }

    @Transactional
    public ShoppingCart addItem(Long customerId, Long productId, int quantity) {
        ProductInfoDto product = catalogFacade.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));

        if (product.stockQuantity() < quantity) {
            throw new IllegalStateException("Insufficient stock for product: " + product.name());
        }

        ShoppingCart cart = getOrCreateCart(customerId);

        // If item already exists, increment quantity
        cart.getItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .ifPresentOrElse(
                        item -> item.setQuantity(item.getQuantity() + quantity),
                        () -> {
                            CartItem newItem = new CartItem();
                            newItem.setProductId(productId);
                            newItem.setQuantity(quantity);
                            newItem.setUnitPrice(product.price());
                            newItem.setCart(cart);
                            cart.getItems().add(newItem);
                        }
                );

        return cartRepository.save(cart);
    }

    @Transactional
    public ShoppingCart updateItemQuantity(Long customerId, Long productId, int quantity) {
        ShoppingCart cart = cartRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Cart not found for customer: " + customerId));

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Item not found in cart: " + productId));

        if (quantity <= 0) {
            cart.getItems().remove(item);
        } else {
            ProductInfoDto product = catalogFacade.findById(productId)
                    .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));
            if (product.stockQuantity() < quantity) {
                throw new IllegalStateException("Insufficient stock for product: " + product.name());
            }
            item.setQuantity(quantity);
        }

        return cartRepository.save(cart);
    }

    @Transactional
    public void removeItem(Long customerId, Long productId) {
        ShoppingCart cart = cartRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Cart not found for customer: " + customerId));

        cart.getItems().removeIf(item -> item.getProductId().equals(productId));
        cartRepository.save(cart);
    }

    @Transactional
    public void clearCart(Long customerId) {
        cartRepository.findByCustomerId(customerId).ifPresent(cart -> {
            cart.getItems().clear();
            cartRepository.save(cart);
        });
    }
}

