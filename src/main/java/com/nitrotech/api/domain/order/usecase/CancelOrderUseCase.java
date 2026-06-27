package com.nitrotech.api.domain.order.usecase;

import com.nitrotech.api.domain.audit.dto.AuditLogCommand;
import com.nitrotech.api.domain.audit.dto.AuditAction;
import com.nitrotech.api.domain.audit.dto.AuditResourceType;
import com.nitrotech.api.domain.audit.service.AuditLogService;
import com.nitrotech.api.domain.inventory.repository.InventoryRepository;
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
public class CancelOrderUseCase {

    private static final Set<String> CANCELLABLE = Set.of("pending", "confirmed");

    private final OrderRepository orderRepository;
    private final AuditLogService auditLogService;
    private final InventoryRepository inventoryRepository;

    @Transactional
    public OrderData execute(Long id, Long userId) {
        OrderData order = orderRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));

        if (!CANCELLABLE.contains(order.status())) {
            throw new DomainException("ORDER_CANNOT_CANCEL",
                    "Order cannot be cancelled in status: " + order.status()) {};
        }
        OrderData updated = orderRepository.updateStatus(id, "cancelled");
        order.items().forEach(item -> inventoryRepository.adjust(item.variantId(), item.quantity()));
        auditLogService.record(AuditLogCommand.success(
                AuditAction.ORDER_CANCELLED,
                AuditResourceType.ORDER,
                id,
                Map.of("status", order.status()),
                Map.of("status", updated.status()),
                Map.of("userId", userId)
        ));
        return updated;
    }
}
