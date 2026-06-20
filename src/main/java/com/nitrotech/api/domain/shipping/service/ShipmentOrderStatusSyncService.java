package com.nitrotech.api.domain.shipping.service;

import com.nitrotech.api.domain.order.dto.OrderData;
import com.nitrotech.api.domain.order.repository.OrderRepository;
import com.nitrotech.api.domain.order.usecase.UpdateOrderStatusUseCase;
import com.nitrotech.api.domain.shipping.dto.ShipmentData;
import com.nitrotech.api.domain.shipping.dto.ShipmentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ShipmentOrderStatusSyncService {

    private final OrderRepository orderRepository;
    private final UpdateOrderStatusUseCase updateOrderStatusUseCase;

    public void syncDeliveredShipment(ShipmentData shipment, String note) {
        orderRepository.findById(shipment.getOrderId()).ifPresent(order -> {
            if (shipment.getStatus() != ShipmentStatus.DELIVERED || isTerminalOrderStatus(order.status())) {
                return;
            }
            advanceToDelivered(order, note);
        });
    }

    private boolean isTerminalOrderStatus(String status) {
        return "cancelled".equals(status) || "refunded".equals(status) || "expired".equals(status);
    }

    private void advanceToDelivered(OrderData order, String note) {
        switch (order.status()) {
            case "confirmed" -> {
                updateOrderStatusUseCase.execute(order.id(), "processing", "shipping_webhook", note);
                updateOrderStatusUseCase.execute(order.id(), "shipped", "shipping_webhook", note);
                updateOrderStatusUseCase.execute(order.id(), "delivered", "shipping_webhook", note);
            }
            case "processing" -> {
                updateOrderStatusUseCase.execute(order.id(), "shipped", "shipping_webhook", note);
                updateOrderStatusUseCase.execute(order.id(), "delivered", "shipping_webhook", note);
            }
            case "shipped" -> updateOrderStatusUseCase.execute(order.id(), "delivered", "shipping_webhook", note);
            default -> {
                // A shipment cannot be created for any other active order state.
            }
        }
    }
}
