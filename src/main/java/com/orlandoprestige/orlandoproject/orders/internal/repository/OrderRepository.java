package com.orlandoprestige.orlandoproject.orders.internal.repository;

import com.orlandoprestige.orlandoproject.orders.internal.domain.Order;
import com.orlandoprestige.orlandoproject.orders.internal.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findAllByCustomerId(Long customerId);
    List<Order> findAllByStatus(OrderStatus status);
    List<Order> findAllByOrderByCreatedAtDesc();
}

