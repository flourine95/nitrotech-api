package com.nitrotech.api.infrastructure.payment.vnpay;

import com.nitrotech.api.application.payment.request.RawWebhookRequest;
import com.nitrotech.api.domain.payment.dto.PaymentInitResult;
import com.nitrotech.api.domain.payment.dto.PaymentOrderData;
import com.nitrotech.api.domain.payment.dto.VerifiedPaymentWebhook;
import com.nitrotech.api.domain.payment.provider.PaymentProvider;
import com.nitrotech.api.shared.exception.BadRequestException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@EnableConfigurationProperties(VnpayProperties.class)
public class VnpayPaymentProvider implements PaymentProvider {

    private static final DateTimeFormatter VNPAY_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final VnpayProperties properties;

    public VnpayPaymentProvider(VnpayProperties properties) {
        this.properties = properties;
    }

    @Override
    public String getProviderName() {
        return "vnpay";
    }

    @Override
    public PaymentInitResult initiatePayment(PaymentOrderData order) {
        requireConfig(properties.tmnCode(), "vnpay.tmn-code");
        requireConfig(properties.hashSecret(), "vnpay.hash-secret");
        requireConfig(properties.payUrl(), "vnpay.pay-url");
        requireConfig(properties.returnUrl(), "vnpay.return-url");

        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        ZonedDateTime expiresAt = now.plusMinutes(properties.expireMinutes() == null ? 15 : properties.expireMinutes());

        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_Amount", toVnpayAmount(order.amount()));
        params.put("vnp_Command", "pay");
        params.put("vnp_CreateDate", VNPAY_TIME_FORMAT.format(now));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_ExpireDate", VNPAY_TIME_FORMAT.format(expiresAt));
        params.put("vnp_IpAddr", "127.0.0.1");
        params.put("vnp_Locale", defaultIfBlank(properties.locale(), "vn"));
        params.put("vnp_OrderInfo", sanitizeOrderInfo(order.description()));
        params.put("vnp_OrderType", defaultIfBlank(properties.orderType(), "other"));
        params.put("vnp_ReturnUrl", properties.returnUrl().trim());
        params.put("vnp_TmnCode", properties.tmnCode().trim());
        params.put("vnp_TxnRef", String.valueOf(order.orderId()));
        params.put("vnp_Version", "2.1.0");

        Map<String, String> signableParams = prepareSignableParams(params);
        String hashData = buildEncodedParamString(signableParams);
        String secureHash = hmacSha512(properties.hashSecret().trim(), hashData);
        String queryString = buildEncodedParamString(signableParams);
        String paymentUrl = properties.payUrl().trim() + "?" + queryString + "&vnp_SecureHash=" + secureHash;

        return new PaymentInitResult(paymentUrl, true);
    }

    @Override
    public VerifiedPaymentWebhook parseAndVerifyWebhook(RawWebhookRequest rawRequest) {
        requireConfig(properties.hashSecret(), "vnpay.hash-secret");

        Map<String, String> payload = extractPayload(rawRequest);
        String secureHash = required(payload, "vnp_SecureHash");

        Map<String, String> signableParams = prepareSignableParams(payload);
        String expectedHash = hmacSha512(properties.hashSecret().trim(), buildEncodedParamString(signableParams));
        if (!expectedHash.equalsIgnoreCase(secureHash.trim())) {
            throw new BadRequestException("INVALID_VNPAY_SIGNATURE", "Invalid VNPAY secure hash");
        }

        Long orderId = parseOrderId(required(payload, "vnp_TxnRef"));
        String responseCode = payload.getOrDefault("vnp_ResponseCode", "");
        String transactionStatus = payload.getOrDefault("vnp_TransactionStatus", "");
        String status = ("00".equals(responseCode) && "00".equals(transactionStatus)) ? "paid" : "failed";

        String providerRef = payload.getOrDefault("vnp_TransactionNo", payload.get("vnp_TxnRef"));

        return new VerifiedPaymentWebhook(
                getProviderName(),
                providerRef,
                orderId,
                parseAmount(payload.get("vnp_Amount")),
                status,
                parsePayDate(payload.get("vnp_PayDate")),
                payload.get("vnp_OrderInfo"),
                new LinkedHashMap<>(payload)
        );
    }

    private Map<String, String> extractPayload(RawWebhookRequest rawRequest) {
        if (rawRequest.queryParams() != null && !rawRequest.queryParams().isEmpty()) {
            return new LinkedHashMap<>(rawRequest.queryParams());
        }
        if (rawRequest.rawBody() == null || rawRequest.rawBody().isBlank()) {
            throw new BadRequestException("INVALID_VNPAY_CALLBACK", "VNPAY callback payload is empty");
        }

        Map<String, String> parsed = new LinkedHashMap<>();
        String[] parts = rawRequest.rawBody().split("&");
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            String[] kv = part.split("=", 2);
            String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            String value = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            parsed.put(key, value);
        }
        return parsed;
    }

    private Map<String, String> prepareSignableParams(Map<String, String> params) {
        return params.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isBlank())
                .filter(entry -> !"vnp_SecureHash".equals(entry.getKey()))
                .filter(entry -> !"vnp_SecureHashType".equals(entry.getKey()))
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (left, right) -> right,
                        LinkedHashMap::new
                ));
    }

    private String buildEncodedParamString(Map<String, String> params) {
        return params.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + encode(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.US_ASCII);
    }

    private String hmacSha512(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            return java.util.HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign VNPAY payload", ex);
        }
    }

    private String toVnpayAmount(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .toPlainString();
    }

    private Long parseOrderId(String txnRef) {
        try {
            return Long.valueOf(txnRef);
        } catch (NumberFormatException ex) {
            throw new BadRequestException("ORDER_ID_NOT_FOUND", "Cannot parse order ID from VNPAY transaction reference");
        }
    }

    private BigDecimal parseAmount(String rawAmount) {
        if (rawAmount == null || rawAmount.isBlank()) {
            throw new BadRequestException("INVALID_VNPAY_AMOUNT", "VNPAY amount is missing");
        }
        try {
            return new BigDecimal(rawAmount).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            throw new BadRequestException("INVALID_VNPAY_AMOUNT", "VNPAY amount is invalid");
        }
    }

    private Instant parsePayDate(String rawPayDate) {
        if (rawPayDate == null || rawPayDate.isBlank()) {
            return Instant.now();
        }
        try {
            LocalDateTime localDateTime = LocalDateTime.parse(rawPayDate.trim(), VNPAY_TIME_FORMAT);
            return localDateTime.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
        } catch (RuntimeException ex) {
            return Instant.now();
        }
    }

    private String sanitizeOrderInfo(String description) {
        String value = defaultIfBlank(description, "NitroTech payment");
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9 _.,:-]", "")
                .trim();
        return normalized.isBlank() ? "NitroTech payment" : normalized;
    }

    private String required(Map<String, String> payload, String key) {
        String value = payload.get(key);
        if (value == null || value.isBlank()) {
            throw new BadRequestException("INVALID_VNPAY_CALLBACK", "Missing VNPAY field: " + key);
        }
        return value;
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private void requireConfig(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("PAYMENT_PROVIDER_MISCONFIGURED",
                    "Missing VNPAY configuration: " + field);
        }
    }
}
