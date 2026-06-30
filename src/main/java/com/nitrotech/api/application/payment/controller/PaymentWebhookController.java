package com.nitrotech.api.application.payment.controller;

import com.nitrotech.api.application.payment.request.RawWebhookRequest;
import com.nitrotech.api.domain.payment.usecase.HandlePaymentWebhookUseCase;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final HandlePaymentWebhookUseCase handlePaymentWebhookUseCase;

    @GetMapping("/api/webhooks/payments/{provider}")
    public ResponseEntity<?> receiveGet(
            @PathVariable String provider,
            @RequestParam Map<String, String> queryParams,
            @RequestHeader Map<String, String> headers
    ) {
        log.info("Incoming payment webhook for provider: {}", provider);

        RawWebhookRequest rawRequest = new RawWebhookRequest(headers, queryParams, "");
        Map<String, Object> result = handlePaymentWebhookUseCase.execute(provider, rawRequest);
        return ResponseEntity.ok(result);
    }

    @PostMapping({"/api/webhooks/payments/{provider}", "/api/webhooks/sepay"})
    public ResponseEntity<?> receive(
            @PathVariable(required = false) String provider,
            @RequestParam Map<String, String> queryParams,
            @RequestHeader Map<String, String> headers,
            HttpServletRequest request
    ) throws IOException {
        String resolvedProvider = provider != null ? provider : "sepay";
        log.info("Incoming payment webhook for provider: {}", resolvedProvider);

        String rawBody = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        RawWebhookRequest rawRequest = new RawWebhookRequest(headers, queryParams, rawBody);

        Map<String, Object> result = handlePaymentWebhookUseCase.execute(resolvedProvider, rawRequest);
        return ResponseEntity.ok(result);
    }
}
