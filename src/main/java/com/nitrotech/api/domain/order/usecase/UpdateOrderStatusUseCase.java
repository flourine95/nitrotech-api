package com.nitrotech.api.domain.order.usecase;

import com.nitrotech.api.domain.order.dto.OrderData;
import com.nitrotech.api.domain.order.repository.OrderRepository;
import com.nitrotech.api.shared.exception.DomainException;
import com.nitrotech.api.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
public class UpdateOrderStatusUseCase {

    // Các transition hợp lệ
    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
            "pending",    Set.of("confirmed", "cancelled"),
            "confirmed",  Set.of("processing", "cancelled"),
            "processing", Set.of("shipped"),
            "shipped",    Set.of("delivered"),
            "delivered",  Set.of("refunded")
    );

    private final OrderRepository orderRepository;

    public UpdateOrderStatusUseCase(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public OrderData execute(Long id, String newStatus) {
        OrderData order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));

        Set<String> allowed = TRANSITIONS.getOrDefault(order.status(), Set.of());
        if (!allowed.contains(newStatus)) {
            throw new DomainException("INVALID_STATUS_TRANSITION",
                    "Cannot transition from " + order.status() + " to " + newStatus) {};
        }
        return orderRepository.updateStatus(id, newStatus);
    }
}
