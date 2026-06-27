package com.nitrotech.api.domain.order.usecase;

import com.nitrotech.api.domain.inventory.repository.InventoryRepository;
import com.nitrotech.api.domain.order.dto.OrderData;
import com.nitrotech.api.domain.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ExpirePendingOrdersUseCase {

    private static final Duration PAYMENT_TIMEOUT = Duration.ofMinutes(15);

    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;
    private final Clock clock;

    @Transactional
    public int execute() {
        Instant now = clock.instant();
        var orders = orderRepository.findPendingCreatedAtOrBefore(now.minus(PAYMENT_TIMEOUT));
        orders.forEach(this::expire);
        return orders.size();
    }

    private void expire(OrderData order) {
        orderRepository.updateStatus(order.id(), "expired");
        order.items().forEach(item -> inventoryRepository.adjust(item.variantId(), item.quantity()));
    }
}
