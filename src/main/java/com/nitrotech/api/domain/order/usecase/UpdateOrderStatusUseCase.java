package com.nitrotech.api.domain.order.usecase;

import com.nitrotech.api.domain.order.dto.OrderData;
import com.nitrotech.api.domain.order.repository.OrderRepository;
import com.nitrotech.api.domain.shipping.usecase.CreateShipmentUseCase;
import com.nitrotech.api.shared.exception.DomainException;
import com.nitrotech.api.shared.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
public class UpdateOrderStatusUseCase {

    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
            "pending",    Set.of("confirmed", "cancelled"),
            "confirmed",  Set.of("processing", "cancelled"),
            "processing", Set.of("shipped"),
            "shipped",    Set.of("delivered"),
            "delivered",  Set.of("refunded")
    );

    private final OrderRepository orderRepository;
    private final CreateShipmentUseCase createShipmentUseCase;
    private final String defaultProvider;

    public UpdateOrderStatusUseCase(
            OrderRepository orderRepository,
            CreateShipmentUseCase createShipmentUseCase,
            @Value("${app.shipping.default-provider:ghtk}") String defaultProvider
    ) {
        this.orderRepository = orderRepository;
        this.createShipmentUseCase = createShipmentUseCase;
        this.defaultProvider = defaultProvider;
    }

    public OrderData execute(Long id, String newStatus) {
        OrderData order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));

        Set<String> allowed = TRANSITIONS.getOrDefault(order.status(), Set.of());
        if (!allowed.contains(newStatus)) {
            throw new DomainException("INVALID_STATUS_TRANSITION",
                    "Cannot transition from " + order.status() + " to " + newStatus) {};
        }
        OrderData updatedOrder = orderRepository.updateStatus(id, newStatus);

        if ("confirmed".equalsIgnoreCase(newStatus)) {
            createShipmentUseCase.execute(id, defaultProvider);
        }

        return updatedOrder;
    }
}
