package com.nitrotech.api.domain.shipping.usecase;

import com.nitrotech.api.domain.audit.dto.AuditLogCommand;
import com.nitrotech.api.domain.audit.dto.AuditAction;
import com.nitrotech.api.domain.audit.dto.AuditResourceType;
import com.nitrotech.api.domain.audit.service.AuditLogService;
import com.nitrotech.api.domain.order.dto.OrderData;
import com.nitrotech.api.domain.order.repository.OrderRepository;
import com.nitrotech.api.domain.shipping.dto.ShipmentData;
import com.nitrotech.api.domain.shipping.dto.ShipmentLogSource;
import com.nitrotech.api.domain.shipping.dto.ShipmentStatus;
import com.nitrotech.api.domain.shipping.dto.ShippingResult;
import com.nitrotech.api.domain.shipping.provider.ShippingProvider;
import com.nitrotech.api.domain.shipping.repository.ShipmentRepository;
import com.nitrotech.api.infrastructure.shipping.ShippingProviderRegistry;
import com.nitrotech.api.shared.exception.DomainException;
import com.nitrotech.api.shared.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class CreateShipmentUseCase {

    private final OrderRepository orderRepository;
    private final ShipmentRepository shipmentRepository;
    private final ShippingProviderRegistry shippingProviderRegistry;
    private final AuditLogService auditLogService;
    private final String defaultProvider;

    public CreateShipmentUseCase(
            OrderRepository orderRepository,
            ShipmentRepository shipmentRepository,
            ShippingProviderRegistry shippingProviderRegistry,
            AuditLogService auditLogService,
            @Value("${app.shipping.default-provider:ghtk}") String defaultProvider
    ) {
        this.orderRepository = orderRepository;
        this.shipmentRepository = shipmentRepository;
        this.shippingProviderRegistry = shippingProviderRegistry;
        this.auditLogService = auditLogService;
        this.defaultProvider = defaultProvider;
    }

    @Transactional
    public ShipmentData execute(Long orderId, String providerName) {
        String resolvedProvider = (providerName == null || providerName.isBlank()) ? defaultProvider : providerName;
        log.info("Creating shipment for orderId: {}, provider: {}", orderId, resolvedProvider);

        // Check if shipment already exists
        Optional<ShipmentData> existing = shipmentRepository.findByOrderId(orderId);
        if (existing.isPresent()) {
            log.warn("Shipment already exists for orderId: {}", orderId);
            throw new DomainException("SHIPMENT_ALREADY_EXISTS",
                    "Shipment already exists for order " + orderId) {};
        }

        // Retrieve order details
        OrderData order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", 
                        "Order with ID " + orderId + " not found"));

        // Check order status
        String status = order.status();
        if (!"confirmed".equalsIgnoreCase(status) && !"processing".equalsIgnoreCase(status)) {
            throw new DomainException("INVALID_ORDER_STATUS",
                    "Cannot create shipment for order " + orderId + " in " + status + " status") {};
        }

        // Get shipping provider
        ShippingProvider provider = shippingProviderRegistry.getProvider(resolvedProvider);

        // Request shipment creation
        ShippingResult result = provider.createShipment(order);

        // Save shipment details
        ShipmentData shipment = ShipmentData.builder()
                .orderId(orderId)
                .provider(resolvedProvider.toLowerCase())
                .trackingCode(result.getTrackingCode())
                .status(ShipmentStatus.READY_TO_PICK)
                .fee(result.getFee())
                .estimatedAt(result.getEstimatedAt())
                .build();

        ShipmentData savedShipment = shipmentRepository.save(shipment);

        // Create initial shipment status log
        shipmentRepository.addLog(
                savedShipment.getId(),
                ShipmentStatus.READY_TO_PICK,
                ShipmentStatus.READY_TO_PICK.value(),
                ShipmentLogSource.ADMIN,
                null,
                "Vận đơn được khởi tạo thành công qua đối tác " + provider.getProviderName().toUpperCase()
        );
        auditLogService.record(AuditLogCommand.success(
                AuditAction.SHIPMENT_CREATED,
                AuditResourceType.SHIPMENT,
                savedShipment.getId(),
                null,
                Map.of(
                        "orderId", savedShipment.getOrderId(),
                        "provider", savedShipment.getProvider(),
                        "trackingCode", savedShipment.getTrackingCode(),
                        "status", savedShipment.getStatus().value()
                ),
                Map.of("orderId", orderId)
        ));

        log.info("Shipment created successfully: {}", savedShipment);
        return savedShipment;
    }
}
