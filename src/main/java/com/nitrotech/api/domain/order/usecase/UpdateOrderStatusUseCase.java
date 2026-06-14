package com.nitrotech.api.domain.order.usecase;

import com.nitrotech.api.domain.audit.dto.AuditLogCommand;
import com.nitrotech.api.domain.audit.service.AuditLogService;
import com.nitrotech.api.domain.order.dto.OrderData;
import com.nitrotech.api.domain.order.repository.OrderRepository;
import com.nitrotech.api.shared.exception.DomainException;
import com.nitrotech.api.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UpdateOrderStatusUseCase {

    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
            "pending",    Set.of("confirmed", "cancelled"),
            "confirmed",  Set.of("processing", "cancelled"),
            "processing", Set.of("shipped"),
            "shipped",    Set.of("delivered"),
            "delivered",  Set.of("refunded")
    );

    private final OrderRepository orderRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public OrderData execute(Long id, String newStatus) {
        OrderData order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));

        Set<String> allowed = TRANSITIONS.getOrDefault(order.status(), Set.of());
        if (!allowed.contains(newStatus)) {
            throw new DomainException("INVALID_STATUS_TRANSITION",
                    "Cannot transition from " + order.status() + " to " + newStatus) {};
        }
        OrderData updated = orderRepository.updateStatus(id, newStatus);
        auditLogService.record(AuditLogCommand.success(
                "ORDER_STATUS_UPDATED",
                "ORDER",
                id,
                Map.of("status", order.status()),
                Map.of("status", updated.status()),
                Map.of("userId", order.userId())
        ));
        return updated;
    }
}
