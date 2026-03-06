package com.orlandoprestige.orlandoproject.orders.internal.service;

import com.orlandoprestige.orlandoproject.cart.CartFacade;
import com.orlandoprestige.orlandoproject.catalog.CatalogFacade;
import com.orlandoprestige.orlandoproject.catalog.dto.ProductInfoDto;
import com.orlandoprestige.orlandoproject.customers.internal.domain.BillingProfile;
import com.orlandoprestige.orlandoproject.customers.internal.domain.BillingType;
import com.orlandoprestige.orlandoproject.customers.internal.service.BillingProfileService;
import com.orlandoprestige.orlandoproject.orders.internal.domain.Order;
import com.orlandoprestige.orlandoproject.orders.internal.domain.OrderItem;
import com.orlandoprestige.orlandoproject.orders.internal.domain.OrderStatus;
import com.orlandoprestige.orlandoproject.orders.internal.event.OrderEvaluatedEvent;
import com.orlandoprestige.orlandoproject.orders.internal.event.OrderSubmittedEvent;
import com.orlandoprestige.orlandoproject.orders.internal.presentation.dto.SubmitOrderDto;
import com.orlandoprestige.orlandoproject.orders.internal.presentation.dto.SubmitOrderItemDto;
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
    private final BillingProfileService billingProfileService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Order submitOrder(Long customerId, SubmitOrderDto dto) {
        if (dto.items() == null || dto.items().isEmpty()) {
            throw new IllegalStateException("Order must have at least one item.");
        }

        Order order = new Order();
        order.setCustomerId(customerId);
        order.setStatus(OrderStatus.PENDING_EVALUATION);

        // Set billing details
        BillingType billingType = BillingType.valueOf(dto.billingType());
        order.setBillingType(billingType);
        order.setBillingName(dto.billingName());
        order.setBillingTin(dto.billingTin());
        order.setBillingAddress(dto.billingAddress());
        order.setBillingTerms(dto.billingTerms());

        // Build order items from submitted items
        List<OrderItem> orderItems = dto.items().stream().map(submittedItem -> {
            ProductInfoDto product = catalogFacade.findById(submittedItem.productId())
                    .orElseThrow(() -> new EntityNotFoundException("Product not found: " + submittedItem.productId()));

            if (product.stockQuantity() < submittedItem.quantity()) {
                throw new IllegalStateException("Insufficient stock for product: " + product.name());
            }

            OrderItem item = new OrderItem();
            item.setProductId(submittedItem.productId());
            item.setProductName(product.name());
            item.setQuantity(submittedItem.quantity());
            item.setUnitPrice(product.price());
            item.setOrder(order);
            return item;
        }).toList();

        order.getItems().addAll(orderItems);
        Order saved = orderRepository.save(order);

        // Clear server-side cart if it exists
        try {
            cartFacade.clearCart(customerId);
        } catch (Exception ignored) {
            // Cart may not exist server-side — that's fine
        }

        // Auto-save billing profile if customer has none
        if (!billingProfileService.existsByCustomerId(customerId)) {
            BillingProfile profile = new BillingProfile();
            profile.setCustomerId(customerId);
            profile.setBillingType(billingType);
            profile.setName(dto.billingName());
            profile.setTin(dto.billingTin());
            profile.setAddress(dto.billingAddress());
            profile.setPaymentTerms(dto.billingTerms());
            billingProfileService.create(profile);
        }

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
    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
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

