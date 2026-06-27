package com.nitrotech.api.domain.shipping.service;

import com.nitrotech.api.domain.order.OrderStatus;
import com.nitrotech.api.domain.order.dto.OrderData;
import com.nitrotech.api.domain.order.repository.OrderRepository;
import com.nitrotech.api.domain.order.usecase.UpdateOrderStatusUseCase;
import com.nitrotech.api.domain.shipping.ShipmentStatus;
import com.nitrotech.api.domain.shipping.dto.ShipmentData;
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
        OrderStatus orderStatus = OrderStatus.fromValue(status);
        return orderStatus == OrderStatus.CANCELLED
                || orderStatus == OrderStatus.REFUNDED
                || orderStatus == OrderStatus.EXPIRED;
    }

    private void advanceToDelivered(OrderData order, String note) {
        switch (OrderStatus.fromValue(order.status())) {
            case CONFIRMED -> {
                updateOrderStatusUseCase.execute(order.id(), OrderStatus.PROCESSING.value(), "shipping_webhook", note);
                updateOrderStatusUseCase.execute(order.id(), OrderStatus.SHIPPED.value(), "shipping_webhook", note);
                updateOrderStatusUseCase.execute(order.id(), OrderStatus.DELIVERED.value(), "shipping_webhook", note);
            }
            case PROCESSING -> {
                updateOrderStatusUseCase.execute(order.id(), OrderStatus.SHIPPED.value(), "shipping_webhook", note);
                updateOrderStatusUseCase.execute(order.id(), OrderStatus.DELIVERED.value(), "shipping_webhook", note);
            }
            case SHIPPED ->
                    updateOrderStatusUseCase.execute(order.id(), OrderStatus.DELIVERED.value(), "shipping_webhook", note);
            default -> {
                // A shipment cannot be created for any other active order state.
            }
        }
    }
}
