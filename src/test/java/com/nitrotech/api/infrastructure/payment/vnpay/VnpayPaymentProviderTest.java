package com.nitrotech.api.infrastructure.payment.vnpay;

import com.nitrotech.api.domain.payment.dto.PaymentInitResult;
import com.nitrotech.api.domain.payment.dto.PaymentOrderData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

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
                .map(entry -> entry.getKey() + "=" + java.net.URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII))
                .collect(Collectors.joining("&"));

        assertThat(expectedHashData).doesNotContain("vnp_SecureHash");
        assertThat(expectedHashData).doesNotContain("vnp_SecureHashType");

        String expectedHash = hmacSha512("secret-key", expectedHashData);
        assertThat(queryParams.get("vnp_SecureHash")).isEqualTo(expectedHash);
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
