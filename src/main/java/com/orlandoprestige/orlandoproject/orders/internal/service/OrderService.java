package com.orlandoprestige.orlandoproject.orders.internal.service;

import com.orlandoprestige.orlandoproject.cart.CartFacade;
import com.orlandoprestige.orlandoproject.cart.dto.CartSummaryDto;
import com.orlandoprestige.orlandoproject.catalog.CatalogFacade;
import com.orlandoprestige.orlandoproject.orders.internal.domain.Order;
import com.orlandoprestige.orlandoproject.orders.internal.domain.OrderItem;
import com.orlandoprestige.orlandoproject.orders.internal.domain.OrderStatus;
import com.orlandoprestige.orlandoproject.orders.internal.event.OrderEvaluatedEvent;
import com.orlandoprestige.orlandoproject.orders.internal.event.OrderSubmittedEvent;
import com.orlandoprestige.orlandoproject.orders.internal.repository.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartFacade cartFacade;
    private final CatalogFacade catalogFacade;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Order submitOrder(Long customerId) {
        CartSummaryDto cart = cartFacade.getCartByCustomerId(customerId)
                .orElseThrow(() -> new IllegalStateException("Cart is empty. Cannot submit an order."));

        if (cart.items().isEmpty()) {
            throw new IllegalStateException("Cart is empty. Cannot submit an order.");
        }

        Order order = new Order();
        order.setCustomerId(customerId);
        order.setStatus(OrderStatus.PENDING_EVALUATION);

        List<OrderItem> orderItems = cart.items().stream().map(cartItem -> {
            String productName = catalogFacade.findById(cartItem.productId())
                    .map(p -> p.name())
                    .orElse("Unknown Product");

            OrderItem item = new OrderItem();
            item.setProductId(cartItem.productId());
            item.setProductName(productName);
            item.setQuantity(cartItem.quantity());
            item.setUnitPrice(cartItem.unitPrice());
            item.setOrder(order);
            return item;
        }).toList();

        order.getItems().addAll(orderItems);
        Order saved = orderRepository.save(order);

        // Clear cart after order is placed
        cartFacade.clearCart(customerId);

        // Publish event for potential listeners (notifications, audit, etc.)
        List<OrderSubmittedEvent.OrderItemSnapshot> snapshots = saved.getItems().stream()
                .map(i -> new OrderSubmittedEvent.OrderItemSnapshot(
                        i.getProductId(), i.getProductName(), i.getQuantity(), i.getUnitPrice()))
                .toList();
        eventPublisher.publishEvent(new OrderSubmittedEvent(saved.getId(), customerId, snapshots));

        return saved;
    }

    @Transactional
    public Order evaluateOrder(Long orderId, Long staffId, boolean approved, String note) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));

        if (order.getStatus() != OrderStatus.PENDING_EVALUATION) {
            throw new IllegalStateException("Order is not in PENDING_EVALUATION state.");
        }

        order.setStatus(approved ? OrderStatus.APPROVED : OrderStatus.REJECTED);
        order.setEvaluatedByStaffId(staffId);
        order.setEvaluationNote(note);

        // Decrement stock only on approval
        if (approved) {
            order.getItems().forEach(item ->
                    catalogFacade.decrementStock(item.getProductId(), item.getQuantity()));
        }

        Order saved = orderRepository.save(order);

        // Publish evaluation event for notification listeners
        eventPublisher.publishEvent(new OrderEvaluatedEvent(
                saved.getId(), saved.getCustomerId(), staffId, approved, note));

        return saved;
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByCustomer(Long customerId) {
        return orderRepository.findAllByCustomerId(customerId);
    }

    @Transactional(readOnly = true)
    public List<Order> getAllPendingOrders() {
        return orderRepository.findAllByStatus(OrderStatus.PENDING_EVALUATION);
    }

    @Transactional(readOnly = true)
    public Optional<Order> getOrderById(Long orderId) {
        return orderRepository.findById(orderId);
    }
}

