package com.nitrotech.api.domain.payment.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nitrotech.api.application.payment.request.RawWebhookRequest;
import com.nitrotech.api.domain.order.dto.OrderData;
import com.nitrotech.api.domain.order.repository.OrderRepository;
import com.nitrotech.api.domain.order.usecase.UpdateOrderStatusUseCase;
import com.nitrotech.api.domain.payment.dto.VerifiedPaymentWebhook;
import com.nitrotech.api.domain.payment.repository.PaymentTransactionRepository;
import com.nitrotech.api.infrastructure.payment.PaymentProviderRegistry;
import com.nitrotech.api.infrastructure.payment.sepay.SepayPaymentProvider;
import com.nitrotech.api.infrastructure.payment.sepay.dto.SepayWebhookPayload;
import com.nitrotech.api.shared.exception.BadRequestException;
import com.nitrotech.api.shared.exception.ForbiddenException;
import com.nitrotech.api.shared.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class HandlePaymentWebhookUseCaseTest {

    private OrderRepository orderRepository;
    private PaymentTransactionRepository paymentTransactionRepository;
    private UpdateOrderStatusUseCase updateOrderStatusUseCase;
    private SepayPaymentProvider sepayPaymentProvider;
    private PaymentProviderRegistry registry;
    private HandlePaymentWebhookUseCase useCase;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        orderRepository = mock(OrderRepository.class);
        paymentTransactionRepository = mock(PaymentTransactionRepository.class);
        updateOrderStatusUseCase = mock(UpdateOrderStatusUseCase.class);

        sepayPaymentProvider = new SepayPaymentProvider();
        ReflectionTestUtils.setField(sepayPaymentProvider, "paymentCodePrefix", "NT");
        ReflectionTestUtils.setField(sepayPaymentProvider, "webhookApiKey", "dev-sepay-key");
        ReflectionTestUtils.setField(sepayPaymentProvider, "sepayAccountNumber", "123456789");
        ReflectionTestUtils.setField(sepayPaymentProvider, "sepayBankName", "MBBank");

        registry = new PaymentProviderRegistry(List.of(sepayPaymentProvider));
        useCase = new HandlePaymentWebhookUseCase(registry, orderRepository, paymentTransactionRepository, updateOrderStatusUseCase);
    }

    @Test
    void marksPendingOrderConfirmedWhenIncomingAmountMatches() throws Exception {
        when(paymentTransactionRepository.existsByProviderAndProviderRef("sepay", "92704"))
                .thenReturn(false);
        when(orderRepository.findById(123L)).thenReturn(Optional.of(order("pending")));

        RawWebhookRequest rawRequest = rawRequest("NT123", "NT123 content", new BigDecimal("500000"), "in", 92704L, "FT24012345678");
        Map<String, Object> result = useCase.execute("sepay", rawRequest);

        assertThat(result.get("success")).isEqualTo(true);

        ArgumentCaptor<VerifiedPaymentWebhook> captor = ArgumentCaptor.forClass(VerifiedPaymentWebhook.class);
        verify(paymentTransactionRepository).save(captor.capture(), eq("paid"));
        assertThat(captor.getValue().orderId()).isEqualTo(123L);
        verify(updateOrderStatusUseCase).execute(123L, "confirmed");
    }

    @Test
    void storesPaymentButDoesNotConfirmWhenOrderIsNotPending() throws Exception {
        when(paymentTransactionRepository.existsByProviderAndProviderRef("sepay", "92704"))
                .thenReturn(false);
        when(orderRepository.findById(123L)).thenReturn(Optional.of(order("expired")));

        RawWebhookRequest rawRequest = rawRequest("NT123", "NT123 content", new BigDecimal("500000"), "in", 92704L, "FT24012345678");
        Map<String, Object> result = useCase.execute("sepay", rawRequest);

        assertThat(result.get("success")).isEqualTo(true);

        verify(paymentTransactionRepository).save(any(VerifiedPaymentWebhook.class), eq("paid"));
        verify(updateOrderStatusUseCase, never()).execute(anyLong(), anyString());
    }

    @Test
    void ignoresDuplicateProviderReference() throws Exception {
        when(paymentTransactionRepository.existsByProviderAndProviderRef("sepay", "92704"))
                .thenReturn(true);

        RawWebhookRequest rawRequest = rawRequest("NT123", "NT123 content", new BigDecimal("500000"), "in", 92704L, "FT24012345678");
        Map<String, Object> result = useCase.execute("sepay", rawRequest);

        assertThat(result.get("success")).isEqualTo(true);
        verify(paymentTransactionRepository, never()).save(any(), anyString());
        verify(updateOrderStatusUseCase, never()).execute(anyLong(), anyString());
    }

    @Test
    void ignoresWebhookWhenPaymentCodeIsMissing() throws Exception {
        when(paymentTransactionRepository.existsByProviderAndProviderRef("sepay", "92704"))
                .thenReturn(false);

        RawWebhookRequest rawRequest = rawRequest(null, "Khach chuyen khoan", new BigDecimal("500000"), "in", 92704L, "FT24012345678");

        Map<String, Object> result = useCase.execute("sepay", rawRequest);

        assertThat(result.get("success")).isEqualTo(false);
        assertThat(result.get("message").toString()).contains("Ignored: Cannot extract order ID");

        verify(orderRepository, never()).findById(anyLong());
        verify(paymentTransactionRepository, never()).save(any(), anyString());
        verify(updateOrderStatusUseCase, never()).execute(anyLong(), anyString());
    }

    @Test
    void ignoresWebhookWhenOrderCannotBeFound() throws Exception {
        when(paymentTransactionRepository.existsByProviderAndProviderRef("sepay", "92704"))
                .thenReturn(false);
        when(orderRepository.findById(123L)).thenReturn(Optional.empty());

        RawWebhookRequest rawRequest = rawRequest("NT123", "NT123 content", new BigDecimal("500000"), "in", 92704L, "FT24012345678");

        Map<String, Object> result = useCase.execute("sepay", rawRequest);

        assertThat(result.get("success")).isEqualTo(false);
        assertThat(result.get("message").toString()).contains("Ignored: Order with ID 123 not found");

        verify(paymentTransactionRepository, never()).save(any(), anyString());
        verify(updateOrderStatusUseCase, never()).execute(anyLong(), anyString());
    }

    @Test
    void recordsMismatchWhenAmountDoesNotMatchOrder() throws Exception {
        when(paymentTransactionRepository.existsByProviderAndProviderRef("sepay", "92704"))
                .thenReturn(false);
        when(orderRepository.findById(123L)).thenReturn(Optional.of(order("pending")));

        RawWebhookRequest rawRequest = rawRequest("NT123", "NT123 content", new BigDecimal("400000"), "in", 92704L, "FT24012345678");
        Map<String, Object> result = useCase.execute("sepay", rawRequest);

        assertThat(result.get("success")).isEqualTo(false);

        verify(paymentTransactionRepository).save(any(VerifiedPaymentWebhook.class), eq("mismatch"));
        verify(updateOrderStatusUseCase, never()).execute(anyLong(), anyString());
    }

    @Test
    void recordsMismatchWhenTransferIsOutgoing() throws Exception {
        when(paymentTransactionRepository.existsByProviderAndProviderRef("sepay", "92704"))
                .thenReturn(false);
        when(orderRepository.findById(123L)).thenReturn(Optional.of(order("pending")));

        RawWebhookRequest rawRequest = rawRequest("NT123", "NT123 chuyen tien", new BigDecimal("500000"), "out", 92704L, "FT24012345678");
        Map<String, Object> result = useCase.execute("sepay", rawRequest);

        assertThat(result.get("success")).isEqualTo(false);

        verify(paymentTransactionRepository).save(any(VerifiedPaymentWebhook.class), eq("mismatch"));
        verify(updateOrderStatusUseCase, never()).execute(anyLong(), anyString());
    }

    @Test
    void throwsExceptionWhenAuthorizationFails() throws Exception {
        RawWebhookRequest rawRequest = new RawWebhookRequest(
                Map.of("Authorization", "Apikey invalid-key"),
                Map.of(),
                objectMapper.writeValueAsString(new SepayWebhookPayload(92704L, "Vietcombank", "2024-07-02 11:08:33", "1017588888", "", "NT123", "NT123 chuyen", "in", "", new BigDecimal("500000"), BigDecimal.ZERO, "FT24012345678"))
        );

        assertThatThrownBy(() -> useCase.execute("sepay", rawRequest))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Invalid SePay API Key");
    }

    private RawWebhookRequest rawRequest(
            String code,
            BigDecimal amount
    ) throws Exception {
        return rawRequest(code, code + " chuyen tien", amount, "in", 92704L, "FT24012345678");
    }

    private RawWebhookRequest rawRequest(
            String code,
            String content,
            BigDecimal amount,
            String transferType,
            Long id,
            String referenceCode
    ) throws Exception {
        SepayWebhookPayload payload = new SepayWebhookPayload(
                id,
                "Vietcombank",
                "2024-07-02 11:08:33",
                "1017588888",
                "",
                code,
                content,
                transferType,
                "",
                amount,
                BigDecimal.ZERO,
                referenceCode
        );
        return new RawWebhookRequest(
                Map.of("Authorization", "Apikey dev-sepay-key"),
                Map.of(),
                objectMapper.writeValueAsString(payload)
        );
    }

    private OrderData order(String status) {
        return new OrderData(
                123L,
                1L,
                null,
                status,
                "sepay",
                new BigDecimal("500000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("500000"),
                null,
                null,
                List.of(),
                Instant.now(),
                Instant.now()
        );
    }
}
