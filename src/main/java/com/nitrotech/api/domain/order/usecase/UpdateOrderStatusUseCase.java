package com.nitrotech.api.domain.order.usecase;

import com.nitrotech.api.domain.audit.AuditAction;
import com.nitrotech.api.domain.audit.AuditResourceType;
import com.nitrotech.api.domain.audit.dto.AuditLogCommand;
import com.nitrotech.api.domain.audit.service.AuditLogService;
import com.nitrotech.api.domain.order.OrderStatus;
import com.nitrotech.api.domain.order.dto.OrderData;
import com.nitrotech.api.domain.order.exception.InvalidOrderStatusTransitionException;
import com.nitrotech.api.domain.order.exception.OrderNotFoundException;
import com.nitrotech.api.domain.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UpdateOrderStatusUseCase {

    private final OrderRepository orderRepository;
    private final AuditLogService auditLogService;

    public OrderData execute(Long id, String newStatus) {
        return execute(id, newStatus, null, null);
    }

    @Transactional
    public OrderData execute(Long id, String newStatus, String reason, String note) {
        OrderData order = orderRepository.findById(id)
                .orElseThrow(OrderNotFoundException::new);

        OrderStatus current = OrderStatus.fromValue(order.status());
        OrderStatus next = OrderStatus.fromValue(newStatus);
        if (current == null || next == null || !current.canTransitionTo(next)) {
            throw new InvalidOrderStatusTransitionException(order.status(), newStatus);
        }
        OrderData updated = orderRepository.updateStatus(id, next.value());
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("userId", order.userId());
        if (hasText(reason)) {
            metadata.put("reason", reason.trim());
        }
        if (hasText(note)) {
            metadata.put("note", note.trim());
        }
        auditLogService.record(AuditLogCommand.success(
                AuditAction.ORDER_STATUS_UPDATED,
                AuditResourceType.ORDER,
                id,
                Map.of("status", order.status()),
                Map.of("status", updated.status()),
                metadata
        ));
        return updated;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
