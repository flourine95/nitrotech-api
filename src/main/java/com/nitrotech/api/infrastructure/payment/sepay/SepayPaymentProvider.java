package com.nitrotech.api.infrastructure.payment.sepay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nitrotech.api.application.payment.request.RawWebhookRequest;
import com.nitrotech.api.domain.payment.dto.PaymentInitResult;
import com.nitrotech.api.domain.payment.dto.PaymentOrderData;
import com.nitrotech.api.domain.payment.dto.VerifiedPaymentWebhook;
import com.nitrotech.api.domain.payment.provider.PaymentProvider;
import com.nitrotech.api.infrastructure.payment.sepay.dto.SepayWebhookPayload;
import com.nitrotech.api.shared.exception.BadRequestException;
import com.nitrotech.api.shared.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class SepayPaymentProvider implements PaymentProvider {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${sepay.webhook-api-key:}")
    private String webhookApiKey;

    @Value("${sepay.payment-code-prefix:NT}")
    private String paymentCodePrefix;

    @Value("${sepay.account-number:123456789}")
    private String sepayAccountNumber;

    @Value("${sepay.bank-name:MBBank}")
    private String sepayBankName;

    @Override
    public String getProviderName() {
        return "sepay";
    }

    @Override
    public PaymentInitResult initiatePayment(PaymentOrderData order) {
        String qrUrl = String.format("https://qr.sepay.vn/img?acc=%s&bank=%s&amount=%s&des=%s%s",
                sepayAccountNumber, sepayBankName, order.amount().toPlainString(), paymentCodePrefix, order.orderId());
        return new PaymentInitResult(qrUrl, false);
    }

    @Override
    public VerifiedPaymentWebhook parseAndVerifyWebhook(RawWebhookRequest rawRequest) {
        // 1. Verify Authorization API Key
        String authorization = rawRequest.headers().get("authorization");
        if (authorization == null) {
            authorization = rawRequest.headers().get("Authorization");
        }

        if (webhookApiKey != null && !webhookApiKey.isBlank()) {
            if (!("Apikey " + webhookApiKey).equals(authorization)) {
                throw new ForbiddenException("UNAUTHORIZED_WEBHOOK", "Invalid SePay API Key");
            }
        }

        // 2. Parse payload JSON
        SepayWebhookPayload payload;
        try {
            payload = objectMapper.readValue(rawRequest.rawBody(), SepayWebhookPayload.class);
        } catch (Exception e) {
            throw new BadRequestException("INVALID_WEBHOOK_BODY", "Failed to parse SePay webhook payload: " + e.getMessage());
        }

        if (payload == null) {
            throw new BadRequestException("INVALID_WEBHOOK_BODY", "SePay payload is empty");
        }

        // 3. Extract order ID
        Long orderId = extractOrderId(payload);
        if (orderId == null) {
            throw new BadRequestException("ORDER_ID_NOT_FOUND", "Cannot extract order ID from SePay transaction code or content");
        }

        String providerRef = payload.id() != null ? payload.id().toString() : payload.referenceCode();
        String status = "in".equalsIgnoreCase(payload.transferType()) ? "paid" : "failed";

        return new VerifiedPaymentWebhook(
                getProviderName(),
                providerRef,
                orderId,
                payload.transferAmount(),
                status,
                parseTransactionDate(payload.transactionDate()),
                payload.content(),
                mapToRawData(payload)
        );
    }

    private Long extractOrderId(SepayWebhookPayload payload) {
        String raw = payload.code() != null && !payload.code().isBlank()
                ? payload.code()
                : payload.content();
        if (raw == null) {
            return null;
        }

        Matcher matcher = Pattern.compile("\\b" + Pattern.quote(paymentCodePrefix) + "(\\d+)\\b", Pattern.CASE_INSENSITIVE)
                .matcher(raw);
        if (!matcher.find()) {
            return null;
        }
        return Long.valueOf(matcher.group(1));
    }

    private Instant parseTransactionDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return Instant.now();
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime ldt = LocalDateTime.parse(dateStr.trim(), formatter);
            return ldt.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
        } catch (Exception e) {
            return Instant.now();
        }
    }

    private Map<String, Object> mapToRawData(SepayWebhookPayload payload) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", payload.id());
        data.put("gateway", payload.gateway());
        data.put("transactionDate", payload.transactionDate());
        data.put("accountNumber", payload.accountNumber());
        data.put("subAccount", payload.subAccount());
        data.put("code", payload.code());
        data.put("content", payload.content());
        data.put("transferType", payload.transferType());
        data.put("description", payload.description());
        data.put("accumulated", payload.accumulated());
        data.put("referenceCode", payload.referenceCode());
        return data;
    }
}
