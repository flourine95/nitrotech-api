package com.nitrotech.api.domain.order.usecase;

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
    private final Clock clock;

    @Transactional
    public int execute() {
        Instant now = clock.instant();
        return orderRepository.expirePendingCreatedAtOrBefore(now.minus(PAYMENT_TIMEOUT), now);
    }
}
