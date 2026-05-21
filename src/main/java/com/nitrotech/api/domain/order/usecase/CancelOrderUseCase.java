package com.nitrotech.api.domain.order.usecase;

import com.nitrotech.api.domain.order.dto.OrderData;
import com.nitrotech.api.domain.order.repository.OrderRepository;
import com.nitrotech.api.shared.exception.DomainException;
import com.nitrotech.api.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class CancelOrderUseCase {

    private static final Set<String> CANCELLABLE = Set.of("pending", "confirmed");

    private final OrderRepository orderRepository;

    public OrderData execute(Long id, Long userId) {
        OrderData order = orderRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));

        if (!CANCELLABLE.contains(order.status())) {
            throw new DomainException("ORDER_CANNOT_CANCEL",
                    "Order cannot be cancelled in status: " + order.status()) {};
        }
        return orderRepository.updateStatus(id, "cancelled");
    }
}
