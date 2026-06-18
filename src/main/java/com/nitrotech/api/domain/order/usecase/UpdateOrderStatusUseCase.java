package com.nitrotech.api.domain.order.usecase;

import com.nitrotech.api.domain.audit.dto.AuditLogCommand;
import com.nitrotech.api.domain.audit.dto.AuditAction;
import com.nitrotech.api.domain.audit.dto.AuditResourceType;
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
import java.util.LinkedHashMap;

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

    public OrderData execute(Long id, String newStatus) {
        return execute(id, newStatus, null, null);
    }

    @Transactional
    public OrderData execute(Long id, String newStatus, String reason, String note) {
        OrderData order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));

        Set<String> allowed = TRANSITIONS.getOrDefault(order.status(), Set.of());
        if (!allowed.contains(newStatus)) {
            throw new DomainException("INVALID_STATUS_TRANSITION",
                    "Cannot transition from " + order.status() + " to " + newStatus) {};
        }
        OrderData updated = orderRepository.updateStatus(id, newStatus);
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
