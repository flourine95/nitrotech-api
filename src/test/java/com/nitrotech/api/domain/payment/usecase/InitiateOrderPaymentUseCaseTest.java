package com.nitrotech.api.domain.payment.usecase;

import com.nitrotech.api.domain.order.dto.OrderData;
import com.nitrotech.api.domain.order.dto.OrderItemData;
import com.nitrotech.api.domain.order.dto.ShippingAddressSnapshot;
import com.nitrotech.api.domain.order.exception.OrderNotFoundException;
import com.nitrotech.api.domain.order.repository.OrderRepository;
import com.nitrotech.api.domain.payment.dto.PaymentInitResult;
import com.nitrotech.api.domain.payment.dto.PaymentOrderData;
import com.nitrotech.api.domain.payment.provider.PaymentProvider;
import com.nitrotech.api.domain.payment.provider.PaymentProviderResolver;
import com.nitrotech.api.shared.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InitiateOrderPaymentUseCaseTest {

    private OrderRepository orderRepository;
    private PaymentProviderResolver paymentProviderResolver;
    private PaymentProvider paymentProvider;
    private InitiateOrderPaymentUseCase useCase;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        paymentProviderResolver = mock(PaymentProviderResolver.class);
        paymentProvider = mock(PaymentProvider.class);
        useCase = new InitiateOrderPaymentUseCase(orderRepository, paymentProviderResolver);
    }

    @Test
    void initiatesVnpayPaymentForPendingOwnOrder() {
        OrderData order = order(100L, 10L, "pending", "vnpay", "SO-100", new BigDecimal("520000"));
        when(orderRepository.findByIdAndUserId(100L, 10L)).thenReturn(Optional.of(order));
        when(paymentProviderResolver.getProvider("vnpay")).thenReturn(paymentProvider);
        when(paymentProvider.initiatePayment(any(PaymentOrderData.class)))
                .thenReturn(new PaymentInitResult("https://sandbox.vnpayment.vn/pay?token=abc", true));

        PaymentInitResult result = useCase.execute(100L, 10L);

        assertThat(result.redirect()).isTrue();
        assertThat(result.paymentUrl()).isEqualTo("https://sandbox.vnpayment.vn/pay?token=abc");

        ArgumentCaptor<PaymentOrderData> captor = ArgumentCaptor.forClass(PaymentOrderData.class);
        verify(paymentProvider).initiatePayment(captor.capture());
        assertThat(captor.getValue().orderId()).isEqualTo(100L);
        assertThat(captor.getValue().amount()).isEqualByComparingTo("520000");
        assertThat(captor.getValue().description()).isEqualTo("Payment for order SO-100");
    }

    @Test
    void initiatesPaymentForAdminUsingOrderReadAll() {
        OrderData order = order(101L, 20L, "pending", "vnpay", "SO-101", new BigDecimal("620000"));
        when(orderRepository.findById(101L)).thenReturn(Optional.of(order));
        when(paymentProviderResolver.getProvider("vnpay")).thenReturn(paymentProvider);
        when(paymentProvider.initiatePayment(any(PaymentOrderData.class)))
                .thenReturn(new PaymentInitResult("https://sandbox.vnpayment.vn/pay?token=admin", true));

        PaymentInitResult result = useCase.execute(101L, 999L, true);

        assertThat(result.redirect()).isTrue();
        assertThat(result.paymentUrl()).contains("sandbox.vnpayment.vn");
        verify(orderRepository).findById(101L);
        verify(orderRepository, never()).findByIdAndUserId(anyLong(), anyLong());
    }

    @Test
    void rejectsCodPaymentInitiation() {
        when(orderRepository.findByIdAndUserId(200L, 10L))
                .thenReturn(Optional.of(order(200L, 10L, "pending", "cod", "SO-200", new BigDecimal("300000"))));

        assertThatThrownBy(() -> useCase.execute(200L, 10L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("do not require online payment initiation");

        verify(paymentProviderResolver, never()).getProvider(anyString());
    }

    @Test
    void rejectsPaymentInitiationForOrderOwnedByAnotherUser() {
        when(orderRepository.findByIdAndUserId(300L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(300L, 10L))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("Order with ID 300 not found");
    }

    @Test
    void rejectsPaymentInitiationForNonPendingOrder() {
        when(orderRepository.findByIdAndUserId(400L, 10L))
                .thenReturn(Optional.of(order(400L, 10L, "confirmed", "vnpay", "SO-400", new BigDecimal("480000"))));

        assertThatThrownBy(() -> useCase.execute(400L, 10L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only pending orders can initiate payment");

        verify(paymentProviderResolver, never()).getProvider(anyString());
    }

    @Test
    void rejectsUnsupportedProvider() {
        when(orderRepository.findByIdAndUserId(500L, 10L))
                .thenReturn(Optional.of(order(500L, 10L, "pending", "sepay", "SO-500", new BigDecimal("280000"))));
        when(paymentProviderResolver.getProvider("sepay"))
                .thenThrow(new BadRequestException("PAYMENT_METHOD_UNSUPPORTED",
                        "Payment method is not supported yet: sepay"));

        assertThatThrownBy(() -> useCase.execute(500L, 10L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Payment method is not supported yet: sepay");
    }

    private OrderData order(
            Long id,
            Long userId,
            String status,
            String paymentMethod,
            String orderCode,
            BigDecimal finalAmount
    ) {
        ShippingAddressSnapshot address = new ShippingAddressSnapshot(
                "Nguyen Phi Long",
                "0900000000",
                "Ho Chi Minh",
                "79",
                "Quan 1",
                "760",
                "Ben Nghe",
                "26734",
                "1 Nguyen Hue"
        );

        return new OrderData(
                id,
                userId,
                orderCode,
                address,
                status,
                paymentMethod,
                finalAmount,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                finalAmount,
                null,
                null,
                List.<OrderItemData>of(),
                Instant.now(),
                Instant.now(),
                null,
                null
        );
    }
}
