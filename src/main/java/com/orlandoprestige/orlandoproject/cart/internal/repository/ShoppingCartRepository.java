package com.orlandoprestige.orlandoproject.cart.internal.repository;

import com.orlandoprestige.orlandoproject.cart.internal.domain.ShoppingCart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShoppingCartRepository extends JpaRepository<ShoppingCart, Long> {
    Optional<ShoppingCart> findByCustomerId(Long customerId);
}

