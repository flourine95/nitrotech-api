package com.nitrotech.api.domain.payment.usecase;

import com.nitrotech.api.application.payment.request.SepayWebhookRequest;
import com.nitrotech.api.domain.order.dto.OrderData;
import com.nitrotech.api.domain.order.repository.OrderRepository;
import com.nitrotech.api.infrastructure.persistence.entity.PaymentTransactionEntity;
import com.nitrotech.api.infrastructure.persistence.repository.PaymentTransactionJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class HandleSepayWebhookUseCaseTest {

    private OrderRepository orderRepository;
    private PaymentTransactionJpaRepository paymentTransactionJpa;
    private HandleSepayWebhookUseCase useCase;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        paymentTransactionJpa = mock(PaymentTransactionJpaRepository.class);
        useCase = new HandleSepayWebhookUseCase(orderRepository, paymentTransactionJpa);
        ReflectionTestUtils.setField(useCase, "paymentCodePrefix", "NT");
    }

    @Test
    void marksPendingOrderConfirmedWhenIncomingAmountMatches() {
        when(paymentTransactionJpa.findByProviderAndProviderRef("sepay", "92704"))
                .thenReturn(Optional.empty());
        when(orderRepository.findById(123L)).thenReturn(Optional.of(order("pending")));

        useCase.execute(request("NT123", new BigDecimal("500000")));

        ArgumentCaptor<PaymentTransactionEntity> captor = ArgumentCaptor.forClass(PaymentTransactionEntity.class);
        verify(paymentTransactionJpa).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("paid");
        assertThat(captor.getValue().getOrderId()).isEqualTo(123L);
        verify(orderRepository).updateStatus(123L, "confirmed");
    }

    @Test
    void storesPaymentButDoesNotConfirmWhenOrderIsNotPending() {
        when(paymentTransactionJpa.findByProviderAndProviderRef("sepay", "92704"))
                .thenReturn(Optional.empty());
        when(orderRepository.findById(123L)).thenReturn(Optional.of(order("expired")));

        useCase.execute(request("NT123", new BigDecimal("500000")));

        ArgumentCaptor<PaymentTransactionEntity> captor = ArgumentCaptor.forClass(PaymentTransactionEntity.class);
        verify(paymentTransactionJpa).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("paid");
        assertThat(captor.getValue().getPaidAt()).isNotNull();
        verify(orderRepository, never()).updateStatus(anyLong(), anyString());
    }

    @Test
    void ignoresDuplicateProviderReference() {
        when(paymentTransactionJpa.findByProviderAndProviderRef("sepay", "92704"))
                .thenReturn(Optional.of(new PaymentTransactionEntity()));

        useCase.execute(request("NT123", new BigDecimal("500000")));

        verify(paymentTransactionJpa, never()).save(any());
        verify(orderRepository, never()).updateStatus(anyLong(), anyString());
    }

    @Test
    void ignoresWebhookWhenPaymentCodeIsMissing() {
        when(paymentTransactionJpa.findByProviderAndProviderRef("sepay", "92704"))
                .thenReturn(Optional.empty());

        useCase.execute(request(null, "Khach chuyen khoan", new BigDecimal("500000"), "in", 92704L, "FT24012345678"));

        verify(orderRepository, never()).findById(anyLong());
        verify(paymentTransactionJpa, never()).save(any());
        verify(orderRepository, never()).updateStatus(anyLong(), anyString());
    }

    @Test
    void ignoresWebhookWhenOrderCannotBeFound() {
        when(paymentTransactionJpa.findByProviderAndProviderRef("sepay", "92704"))
                .thenReturn(Optional.empty());
        when(orderRepository.findById(123L)).thenReturn(Optional.empty());

        useCase.execute(request("NT123", new BigDecimal("500000")));

        verify(paymentTransactionJpa, never()).save(any());
        verify(orderRepository, never()).updateStatus(anyLong(), anyString());
    }

    @Test
    void recordsMismatchWhenAmountDoesNotMatchOrder() {
        when(paymentTransactionJpa.findByProviderAndProviderRef("sepay", "92704"))
                .thenReturn(Optional.empty());
        when(orderRepository.findById(123L)).thenReturn(Optional.of(order("pending")));

        useCase.execute(request("NT123", new BigDecimal("400000")));

        ArgumentCaptor<PaymentTransactionEntity> captor = ArgumentCaptor.forClass(PaymentTransactionEntity.class);
        verify(paymentTransactionJpa).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("mismatch");
        assertThat(captor.getValue().getPaidAt()).isNull();
        verify(orderRepository, never()).updateStatus(anyLong(), anyString());
    }

    @Test
    void recordsMismatchWhenTransferIsOutgoing() {
        when(paymentTransactionJpa.findByProviderAndProviderRef("sepay", "92704"))
                .thenReturn(Optional.empty());
        when(orderRepository.findById(123L)).thenReturn(Optional.of(order("pending")));

        useCase.execute(request("NT123", "NT123 chuyen tien", new BigDecimal("500000"), "out", 92704L, "FT24012345678"));

        ArgumentCaptor<PaymentTransactionEntity> captor = ArgumentCaptor.forClass(PaymentTransactionEntity.class);
        verify(paymentTransactionJpa).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("mismatch");
        verify(orderRepository, never()).updateStatus(anyLong(), anyString());
    }

    @Test
    void extractsPaymentCodeFromContentWhenCodeFieldIsBlank() {
        when(paymentTransactionJpa.findByProviderAndProviderRef("sepay", "92704"))
                .thenReturn(Optional.empty());
        when(orderRepository.findById(123L)).thenReturn(Optional.of(order("pending")));

        useCase.execute(request("", "Thanh toan don hang NT123", new BigDecimal("500000"), "in", 92704L, "FT24012345678"));

        verify(orderRepository).updateStatus(123L, "confirmed");
    }

    @Test
    void usesReferenceCodeAsProviderReferenceWhenWebhookIdIsMissing() {
        when(paymentTransactionJpa.findByProviderAndProviderRef("sepay", "FT24012345678"))
                .thenReturn(Optional.empty());
        when(orderRepository.findById(123L)).thenReturn(Optional.of(order("pending")));

        useCase.execute(request("NT123", "NT123 chuyen tien", new BigDecimal("500000"), "in", null, "FT24012345678"));

        ArgumentCaptor<PaymentTransactionEntity> captor = ArgumentCaptor.forClass(PaymentTransactionEntity.class);
        verify(paymentTransactionJpa).save(captor.capture());
        assertThat(captor.getValue().getProviderRef()).isEqualTo("FT24012345678");
        verify(orderRepository).updateStatus(123L, "confirmed");
    }

    private SepayWebhookRequest request(String code, BigDecimal amount) {
        return request(code, code + " chuyen tien", amount, "in", 92704L, "FT24012345678");
    }

    private SepayWebhookRequest request(
            String code,
            String content,
            BigDecimal amount,
            String transferType,
            Long id,
            String referenceCode
    ) {
        return new SepayWebhookRequest(
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
