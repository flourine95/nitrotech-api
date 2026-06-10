package com.nitrotech.api.domain.shipping.usecase;

import com.nitrotech.api.domain.order.dto.OrderData;
import com.nitrotech.api.domain.order.repository.OrderRepository;
import com.nitrotech.api.domain.shipping.dto.ShipmentData;
import com.nitrotech.api.domain.shipping.dto.ShippingResult;
import com.nitrotech.api.domain.shipping.provider.ShippingProvider;
import com.nitrotech.api.domain.shipping.repository.ShipmentRepository;
import com.nitrotech.api.infrastructure.shipping.ShippingProviderRegistry;
import com.nitrotech.api.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateShipmentUseCase {

    private final OrderRepository orderRepository;
    private final ShipmentRepository shipmentRepository;
    private final ShippingProviderRegistry shippingProviderRegistry;

    @Transactional
    public ShipmentData execute(Long orderId, String providerName) {
        log.info("Creating shipment for orderId: {}, provider: {}", orderId, providerName);

        // Check if shipment already exists
        Optional<ShipmentData> existing = shipmentRepository.findByOrderId(orderId);
        if (existing.isPresent()) {
            log.warn("Shipment already exists for orderId: {}", orderId);
            return existing.get();
        }

        // Retrieve order details
        OrderData order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", 
                        "Order with ID " + orderId + " not found"));

        // Get shipping provider
        ShippingProvider provider = shippingProviderRegistry.getProvider(providerName);

        // Request shipment creation
        ShippingResult result = provider.createShipment(order);

        // Save shipment details
        ShipmentData shipment = ShipmentData.builder()
                .orderId(orderId)
                .provider(providerName.toLowerCase())
                .trackingCode(result.getTrackingCode())
                .status("ready_to_pick")
                .fee(result.getFee())
                .estimatedAt(result.getEstimatedAt())
                .build();

        ShipmentData savedShipment = shipmentRepository.save(shipment);

        // Create initial shipment status log
        shipmentRepository.addLog(
                savedShipment.getId(),
                "ready_to_pick",
                null,
                "Vận đơn được khởi tạo thành công qua đối tác " + provider.getProviderName().toUpperCase()
        );

        log.info("Shipment created successfully: {}", savedShipment);
        return savedShipment;
    }
}
