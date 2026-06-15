package com.nitrotech.api.domain.shipping.usecase;

import com.nitrotech.api.domain.order.dto.OrderData;
import com.nitrotech.api.domain.order.repository.OrderRepository;
import com.nitrotech.api.domain.shipping.dto.OrderShipmentData;
import com.nitrotech.api.domain.shipping.dto.ShipmentData;
import com.nitrotech.api.domain.shipping.dto.ShipmentLogData;
import com.nitrotech.api.domain.shipping.dto.ShipmentStatus;
import com.nitrotech.api.domain.shipping.repository.ShipmentRepository;
import com.nitrotech.api.shared.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class GetOrderShipmentUseCaseTest {

    private OrderRepository orderRepository;
    private ShipmentRepository shipmentRepository;
    private GetOrderShipmentUseCase useCase;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        shipmentRepository = mock(ShipmentRepository.class);
        useCase = new GetOrderShipmentUseCase(orderRepository, shipmentRepository);
    }

    @Test
    void returnsEmptyShipmentWhenOrderHasNoShipment() {
        when(orderRepository.findById(123L)).thenReturn(Optional.of(mock(OrderData.class)));
        when(shipmentRepository.findByOrderId(123L)).thenReturn(Optional.empty());

        OrderShipmentData result = useCase.execute(123L);

        assertThat(result.shipment()).isNull();
        assertThat(result.logs()).isEmpty();
    }

    @Test
    void returnsShipmentWithLogs() {
        ShipmentData shipment = ShipmentData.builder().id(10L).orderId(123L).build();
        ShipmentLogData log = new ShipmentLogData(
                1L, 10L, ShipmentStatus.READY_TO_PICK, "ready_to_pick", "ADMIN",
                null, "Created", Instant.now());
        when(orderRepository.findById(123L)).thenReturn(Optional.of(mock(OrderData.class)));
        when(shipmentRepository.findByOrderId(123L)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.findLogsByShipmentId(10L)).thenReturn(List.of(log));

        OrderShipmentData result = useCase.execute(123L);

        assertThat(result.shipment()).isEqualTo(shipment);
        assertThat(result.logs()).containsExactly(log);
    }

    @Test
    void throwsWhenOrderDoesNotExist() {
        when(orderRepository.findById(123L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(123L))
                .isInstanceOf(NotFoundException.class);

        verify(shipmentRepository, never()).findByOrderId(anyLong());
    }
}
