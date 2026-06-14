package com.nitrotech.api.domain.shipping.usecase;

import com.nitrotech.api.domain.order.repository.OrderRepository;
import com.nitrotech.api.domain.shipping.dto.OrderShipmentData;
import com.nitrotech.api.domain.shipping.dto.ShipmentData;
import com.nitrotech.api.domain.shipping.repository.ShipmentRepository;
import com.nitrotech.api.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetOrderShipmentUseCase {

    private final OrderRepository orderRepository;
    private final ShipmentRepository shipmentRepository;

    public OrderShipmentData execute(Long orderId) {
        orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));

        return shipmentRepository.findByOrderId(orderId)
                .map(this::withLogs)
                .orElseGet(() -> new OrderShipmentData(null, List.of()));
    }

    private OrderShipmentData withLogs(ShipmentData shipment) {
        return new OrderShipmentData(
                shipment,
                shipmentRepository.findLogsByShipmentId(shipment.getId())
        );
    }
}
