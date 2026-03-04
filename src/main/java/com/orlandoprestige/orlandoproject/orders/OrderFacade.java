package com.orlandoprestige.orlandoproject.orders;

import com.orlandoprestige.orlandoproject.orders.dto.OrderSummaryDto;
import com.orlandoprestige.orlandoproject.orders.internal.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Public facade for the Orders module.
 */
@Service
@RequiredArgsConstructor
public class OrderFacade {

    private final OrderService orderService;

    public List<OrderSummaryDto> getOrdersByCustomerId(Long customerId) {
        return orderService.getOrdersByCustomer(customerId).stream()
                .map(order -> {
                    BigDecimal total = order.getItems().stream()
                            .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new OrderSummaryDto(order.getId(), order.getStatus(), order.getCreatedAt(), total);
                })
                .toList();
    }
}

