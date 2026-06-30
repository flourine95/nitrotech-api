package com.nitrotech.api.domain.payment.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nitrotech.api.application.payment.request.RawWebhookRequest;
import com.nitrotech.api.domain.order.dto.OrderData;
import com.nitrotech.api.domain.payment.provider.PaymentProviderResolver;
import com.nitrotech.api.domain.order.repository.OrderRepository;
import com.nitrotech.api.domain.order.usecase.UpdateOrderStatusUseCase;
import com.nitrotech.api.domain.payment.dto.VerifiedPaymentWebhook;
import com.nitrotech.api.domain.payment.repository.PaymentTransactionRepository;
import com.nitrotech.api.infrastructure.payment.sepay.SepayPaymentProvider;
import com.nitrotech.api.infrastructure.payment.sepay.dto.SepayWebhookPayload;
import com.nitrotech.api.infrastructure.payment.vnpay.VnpayPaymentProvider;
import com.nitrotech.api.infrastructure.payment.vnpay.VnpayProperties;
import com.nitrotech.api.shared.exception.BadRequestException;
import com.nitrotech.api.shared.exception.ForbiddenException;
import com.nitrotech.api.shared.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.time.Instant;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class HandlePaymentWebhookUseCaseTest {

    private OrderRepository orderRepository;
    private PaymentTransactionRepository paymentTransactionRepository;
    private UpdateOrderStatusUseCase updateOrderStatusUseCase;
    private SepayPaymentProvider sepayPaymentProvider;
    private VnpayPaymentProvider vnpayPaymentProvider;
    private PaymentProviderResolver registry;
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

        vnpayPaymentProvider = new VnpayPaymentProvider(new VnpayProperties(
                "DEMO_TMN",
                "secret-key",
                "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",
                "http://localhost:3000/payment/vnpay/return",
                "http://localhost:8080/api/webhooks/payments/vnpay",
                "vn",
                "other",
                15
        ));

        registry = new PaymentProviderResolver(List.of(sepayPaymentProvider, vnpayPaymentProvider));
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

    @Test
    void marksPendingOrderConfirmedWhenValidVnpayCallbackArrives() {
        when(paymentTransactionRepository.existsByProviderAndProviderRef("vnpay", "456789"))
                .thenReturn(false);
        when(orderRepository.findById(123L)).thenReturn(Optional.of(order("pending")));

        Map<String, Object> result = useCase.execute("vnpay", vnpayRawRequest("00", "00", "50000000", "123", "456789"));

        assertThat(result.get("success")).isEqualTo(true);
        verify(paymentTransactionRepository).save(any(VerifiedPaymentWebhook.class), eq("paid"));
        verify(updateOrderStatusUseCase).execute(123L, "confirmed");
    }

    @Test
    void rejectsVnpayCallbackWhenSecureHashIsInvalid() {
        RawWebhookRequest rawRequest = vnpayRawRequest("00", "00", "50000000", "123", "456789");
        rawRequest.queryParams().put("vnp_SecureHash", "invalid");

        assertThatThrownBy(() -> useCase.execute("vnpay", rawRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid VNPAY secure hash");
    }

    @Test
    void recordsFailedVnpayCallbackWithoutMarkingOrderPaid() {
        when(paymentTransactionRepository.existsByProviderAndProviderRef("vnpay", "456790"))
                .thenReturn(false);
        when(orderRepository.findById(123L)).thenReturn(Optional.of(order("pending")));

        Map<String, Object> result = useCase.execute("vnpay", vnpayRawRequest("24", "24", "50000000", "123", "456790"));

        assertThat(result.get("success")).isEqualTo(false);
        verify(paymentTransactionRepository).save(any(VerifiedPaymentWebhook.class), eq("failed"));
        verify(updateOrderStatusUseCase, never()).execute(anyLong(), anyString());
    }

    @Test
    void ignoresDuplicateVnpayCallbackByProviderReference() {
        when(paymentTransactionRepository.existsByProviderAndProviderRef("vnpay", "456791"))
                .thenReturn(true);

        Map<String, Object> result = useCase.execute("vnpay", vnpayRawRequest("00", "00", "50000000", "123", "456791"));

        assertThat(result.get("success")).isEqualTo(true);
        assertThat(result.get("message")).isEqualTo("Duplicate transaction ignored");
        verify(paymentTransactionRepository, never()).save(any(), anyString());
        verify(updateOrderStatusUseCase, never()).execute(anyLong(), anyString());
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
                "SO-123",
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
                Instant.now(),
                null,
                null
        );
    }

    private RawWebhookRequest vnpayRawRequest(
            String responseCode,
            String transactionStatus,
            String amount,
            String txnRef,
            String transactionNo
    ) {
        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("vnp_Amount", amount);
        queryParams.put("vnp_BankCode", "NCB");
        queryParams.put("vnp_OrderInfo", "Payment for order SO-" + txnRef);
        queryParams.put("vnp_PayDate", "20260701153045");
        queryParams.put("vnp_ResponseCode", responseCode);
        queryParams.put("vnp_TmnCode", "DEMO_TMN");
        queryParams.put("vnp_TransactionNo", transactionNo);
        queryParams.put("vnp_TransactionStatus", transactionStatus);
        queryParams.put("vnp_TxnRef", txnRef);

        String hashData = queryParams.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isBlank())
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII))
                .collect(Collectors.joining("&"));

        queryParams.put("vnp_SecureHash", hmacSha512("secret-key", hashData));
        return new RawWebhookRequest(Map.of(), queryParams, "");
    }

    private String hmacSha512(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            return java.util.HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
