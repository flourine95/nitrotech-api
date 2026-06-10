package com.nitrotech.api.infrastructure.scheduler;

import com.nitrotech.api.domain.order.usecase.ExpirePendingOrdersUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderExpiryScheduler {

    private final ExpirePendingOrdersUseCase expirePendingOrdersUseCase;

    @Scheduled(fixedDelayString = "${orders.expiry-scan-delay-ms:60000}")
    public void expirePendingOrders() {
        expirePendingOrdersUseCase.execute();
    }
}
