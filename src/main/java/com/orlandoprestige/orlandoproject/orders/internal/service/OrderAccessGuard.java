package com.orlandoprestige.orlandoproject.orders.internal.service;

import com.orlandoprestige.orlandoproject.auth.AuthenticatedUser;
import com.orlandoprestige.orlandoproject.orders.internal.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Spring Security SpEL helper bean used in @PreAuthorize expressions.
 * Allows checking whether the authenticated user owns a given order.
 */
@Component("orderAccessGuard")
@RequiredArgsConstructor
public class OrderAccessGuard {

    private final OrderRepository orderRepository;

    public boolean isOwner(Long orderId, Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            return false;
        }
        return orderRepository.findById(orderId)
                .map(order -> order.getCustomerId().equals(user.userId()))
                .orElse(false);
    }
}

