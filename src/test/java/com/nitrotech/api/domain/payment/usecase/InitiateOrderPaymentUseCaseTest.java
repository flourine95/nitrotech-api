package com.nitrotech.api.domain.payment.usecase;

import com.nitrotech.api.domain.order.dto.OrderData;
import com.nitrotech.api.domain.order.exception.OrderNotFoundException;
import com.nitrotech.api.domain.order.usecase.GetOrderUseCase;
import com.nitrotech.api.domain.payment.dto.PaymentInitResult;
import com.nitrotech.api.infrastructure.payment.PaymentProviderRegistry;
import com.nitrotech.api.infrastructure.payment.sepay.SepayPaymentProvider;
import com.nitrotech.api.infrastructure.payment.vnpay.VnpayPaymentProvider;
import com.nitrotech.api.infrastructure.payment.vnpay.VnpayProperties;
import com.nitrotech.api.shared.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InitiateOrderPaymentUseCaseTest {

    private GetOrderUseCase getOrderUseCase;
    private InitiateOrderPaymentUseCase useCase;

    @BeforeEach
    void setUp() {
        getOrderUseCase = mock(GetOrderUseCase.class);

        SepayPaymentProvider sepayPaymentProvider = new SepayPaymentProvider();
        ReflectionTestUtils.setField(sepayPaymentProvider, "paymentCodePrefix", "NT");
        ReflectionTestUtils.setField(sepayPaymentProvider, "sepayAccountNumber", "123456789");
        ReflectionTestUtils.setField(sepayPaymentProvider, "sepayBankName", "MBBank");

        VnpayPaymentProvider vnpayPaymentProvider = new VnpayPaymentProvider(new VnpayProperties(
                "DEMO_TMN",
                "secret-key",
                "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",
                "http://localhost:3000/payment/vnpay/return",
                "http://localhost:8080/api/webhooks/payments/vnpay",
                "vn",
                "other",
                15
        ));

        PaymentProviderRegistry registry = new PaymentProviderRegistry(List.of(sepayPaymentProvider, vnpayPaymentProvider));
        useCase = new InitiateOrderPaymentUseCase(getOrderUseCase, registry);
    }

    @Test
    void initiatesVnpayPaymentForPendingOwnOrder() {
        when(getOrderUseCase.execute(123L, 10L)).thenReturn(order(123L, 10L, "pending", "vnpay"));

        PaymentInitResult result = useCase.execute(123L, 10L);

        assertThat(result.redirect()).isTrue();
        assertThat(result.paymentUrl()).contains("vnp_Amount=50000000");
        assertThat(result.paymentUrl()).contains("vnp_TmnCode=DEMO_TMN");
        assertThat(result.paymentUrl()).contains("vnp_ReturnUrl=http%3A%2F%2Flocalhost%3A3000%2Fpayment%2Fvnpay%2Freturn");
        assertThat(result.paymentUrl()).contains("vnp_TxnRef=123");
        assertThat(result.paymentUrl()).contains("vnp_SecureHash=");
    }

    @Test
    void initiatesSepayPaymentForPendingOwnOrder() {
        when(getOrderUseCase.execute(124L, 10L)).thenReturn(order(124L, 10L, "pending", "sepay"));

        PaymentInitResult result = useCase.execute(124L, 10L);

        assertThat(result.redirect()).isFalse();
        assertThat(result.paymentUrl())
                .isEqualTo("https://qr.sepay.vn/img?acc=123456789&bank=MBBank&amount=500000&des=NT124");
        verify(getOrderUseCase).execute(124L, 10L);
    }

    @Test
    void rejectsCodPaymentInitiation() {
        when(getOrderUseCase.execute(456L, 10L)).thenReturn(order(456L, 10L, "pending", "cod"));

        assertThatThrownBy(() -> useCase.execute(456L, 10L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("COD orders do not require payment initiation");
    }

    @Test
    void rejectsPaymentInitiationForNonPendingOrder() {
        when(getOrderUseCase.execute(790L, 10L)).thenReturn(order(790L, 10L, "confirmed", "vnpay"));

        assertThatThrownBy(() -> useCase.execute(790L, 10L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Payment can only be initiated for pending orders");
    }

    @Test
    void rejectsPaymentInitiationForOrderOwnedByAnotherUser() {
        when(getOrderUseCase.execute(789L, 99L)).thenThrow(new OrderNotFoundException());

        assertThatThrownBy(() -> useCase.execute(789L, 99L))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("Order not found");
    }

    private OrderData order(Long id, Long userId, String status, String paymentMethod) {
        return new OrderData(
                id,
                userId,
                "SO-" + id,
                null,
                status,
                paymentMethod,
                new BigDecimal("500000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("500000"),
                null,
                null,
                List.of(),
                Instant.now(),
                Instant.now(),
                null,
                null
        );
    }
}
