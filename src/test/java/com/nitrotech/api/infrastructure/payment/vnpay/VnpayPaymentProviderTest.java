package com.nitrotech.api.infrastructure.payment.vnpay;

import com.nitrotech.api.application.payment.request.RawWebhookRequest;
import com.nitrotech.api.domain.payment.dto.PaymentInitResult;
import com.nitrotech.api.domain.payment.dto.PaymentOrderData;
import com.nitrotech.api.domain.payment.dto.VerifiedPaymentWebhook;
import com.nitrotech.api.shared.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VnpayPaymentProviderTest {

    private VnpayPaymentProvider provider;

    @BeforeEach
    void setUp() {
        provider = new VnpayPaymentProvider(new VnpayProperties(
                "DEMO_TMN",
                "secret-key",
                "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",
                "http://localhost:3000/payment/vnpay/return",
                "http://localhost:8080/api/webhooks/payments/vnpay",
                "vn",
                "other",
                15
        ));
    }

    @Test
    void initiatePaymentBuildsSignedSandboxUrl() {
        PaymentInitResult result = provider.initiatePayment(
                new PaymentOrderData(123L, new BigDecimal("500000"), "Payment for order SO-123")
        );

        assertThat(result.paymentUrl()).isNotBlank();
        assertThat(result.redirect()).isTrue();
        assertThat(result.paymentUrl()).contains("vnp_TmnCode=DEMO_TMN");
        assertThat(result.paymentUrl()).contains("vnp_Amount=50000000");
        assertThat(result.paymentUrl()).contains("vnp_TxnRef=123");
        assertThat(result.paymentUrl()).contains("vnp_ReturnUrl=http%3A%2F%2Flocalhost%3A3000%2Fpayment%2Fvnpay%2Freturn");
        assertThat(result.paymentUrl()).contains("vnp_SecureHash=");
    }

    @Test
    void initiatePaymentSignsSortedEncodedParamsWithoutSecureHashInInput() {
        PaymentInitResult result = provider.initiatePayment(
                new PaymentOrderData(123L, new BigDecimal("500000"), "Payment for order SO-123")
        );

        Map<String, String> queryParams = parseQuery(result.paymentUrl());

        assertThat(queryParams).containsKey("vnp_SecureHash");
        assertThat(queryParams).containsEntry("vnp_Amount", "50000000");
        assertThat(queryParams).containsEntry("vnp_TmnCode", "DEMO_TMN");
        assertThat(queryParams).containsEntry("vnp_ReturnUrl", "http://localhost:3000/payment/vnpay/return");
        assertThat(queryParams).containsEntry("vnp_TxnRef", "123");

        String expectedHashData = queryParams.entrySet().stream()
                .filter(entry -> !"vnp_SecureHash".equals(entry.getKey()))
                .filter(entry -> !"vnp_SecureHashType".equals(entry.getKey()))
                .filter(entry -> entry.getValue() != null && !entry.getValue().isBlank())
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII))
                .collect(Collectors.joining("&"));

        assertThat(expectedHashData).doesNotContain("vnp_SecureHash");
        assertThat(expectedHashData).doesNotContain("vnp_SecureHashType");

        String expectedHash = hmacSha512("secret-key", expectedHashData);
        assertThat(queryParams.get("vnp_SecureHash")).isEqualTo(expectedHash);
    }

    @Test
    void parseAndVerifyWebhookAcceptsValidQueryCallback() {
        Map<String, String> params = signedCallbackParams("00", "00", "50000000", "123", "456789");

        VerifiedPaymentWebhook verified = provider.parseAndVerifyWebhook(
                new RawWebhookRequest(Map.of(), params, "")
        );

        assertThat(verified.provider()).isEqualTo("vnpay");
        assertThat(verified.orderId()).isEqualTo(123L);
        assertThat(verified.amount()).isEqualByComparingTo("500000.00");
        assertThat(verified.status()).isEqualTo("paid");
        assertThat(verified.externalTransactionId()).isEqualTo("456789");
    }

    @Test
    void parseAndVerifyWebhookRejectsInvalidSecureHash() {
        Map<String, String> params = signedCallbackParams("00", "00", "50000000", "123", "456789");
        params.put("vnp_SecureHash", "invalid-hash");

        assertThatThrownBy(() -> provider.parseAndVerifyWebhook(new RawWebhookRequest(Map.of(), params, "")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid VNPAY secure hash");
    }

    @Test
    void parseAndVerifyWebhookMarksFailedWhenResponseCodeIsNotSuccessful() {
        Map<String, String> params = signedCallbackParams("24", "24", "50000000", "123", "456790");

        VerifiedPaymentWebhook verified = provider.parseAndVerifyWebhook(
                new RawWebhookRequest(Map.of(), params, "")
        );

        assertThat(verified.status()).isEqualTo("failed");
    }

    private Map<String, String> signedCallbackParams(
            String responseCode,
            String transactionStatus,
            String amount,
            String txnRef,
            String transactionNo
    ) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_Amount", amount);
        params.put("vnp_BankCode", "NCB");
        params.put("vnp_OrderInfo", "Payment for order SO-" + txnRef);
        params.put("vnp_PayDate", "20260701153045");
        params.put("vnp_ResponseCode", responseCode);
        params.put("vnp_TmnCode", "DEMO_TMN");
        params.put("vnp_TransactionNo", transactionNo);
        params.put("vnp_TransactionStatus", transactionStatus);
        params.put("vnp_TxnRef", txnRef);

        String hashData = params.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isBlank())
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII))
                .collect(Collectors.joining("&"));
        params.put("vnp_SecureHash", hmacSha512("secret-key", hashData));
        return params;
    }

    private Map<String, String> parseQuery(String url) {
        String query = url.substring(url.indexOf('?') + 1);
        return Arrays.stream(query.split("&"))
                .map(pair -> pair.split("=", 2))
                .collect(Collectors.toMap(
                        pair -> URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                        pair -> pair.length > 1 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "",
                        (left, right) -> right,
                        LinkedHashMap::new
                ));
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
