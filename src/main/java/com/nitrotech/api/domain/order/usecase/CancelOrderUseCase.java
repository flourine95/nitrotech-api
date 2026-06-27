package com.nitrotech.api.domain.order.usecase;

import com.nitrotech.api.domain.audit.dto.AuditLogCommand;
import com.nitrotech.api.domain.audit.AuditAction;
import com.nitrotech.api.domain.audit.AuditResourceType;
import com.nitrotech.api.domain.audit.service.AuditLogService;
import com.nitrotech.api.domain.inventory.repository.InventoryRepository;
import com.nitrotech.api.domain.order.OrderStatus;
import com.nitrotech.api.domain.order.dto.OrderData;
import com.nitrotech.api.domain.order.exception.OrderCannotCancelException;
import com.nitrotech.api.domain.order.exception.OrderNotFoundException;
import com.nitrotech.api.domain.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CancelOrderUseCase {

    private final OrderRepository orderRepository;
    private final AuditLogService auditLogService;
    private final InventoryRepository inventoryRepository;

    @Transactional
    public OrderData execute(Long id, Long userId) {
        OrderData order = orderRepository.findByIdAndUserId(id, userId)
                .orElseThrow(OrderNotFoundException::new);

        OrderStatus status = OrderStatus.fromValue(order.status());
        if (status != OrderStatus.PENDING && status != OrderStatus.CONFIRMED) {
            throw new OrderCannotCancelException(order.status());
        }
        OrderData updated = orderRepository.updateStatus(id, OrderStatus.CANCELLED.value());
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
