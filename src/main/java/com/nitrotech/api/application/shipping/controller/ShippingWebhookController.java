package com.nitrotech.api.application.shipping.controller;

import com.nitrotech.api.domain.shipping.usecase.HandleShippingWebhookUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import com.nitrotech.api.shared.exception.ForbiddenException;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ShippingWebhookController {

    private final HandleShippingWebhookUseCase handleShippingWebhookUseCase;

    @Value("${ghtk.webhook-hash:}")
    private String ghtkWebhookHash;

    @PostMapping("/api/webhooks/shipping/{provider}")
    public ResponseEntity<Map<String, Object>> receive(
            @PathVariable String provider,
            @RequestParam(required = false) String hash,
            @RequestBody Map<String, Object> payload
    ) {
        verifyGhtkHash(provider, hash);
        log.info("Incoming shipping webhook for provider: {}", provider);
        return ResponseEntity.ok(handleShippingWebhookUseCase.execute(provider, payload));
    }

    @PostMapping(
            value = "/api/webhooks/shipping/{provider}",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public ResponseEntity<Map<String, Object>> receiveForm(
            @PathVariable String provider,
            @RequestParam(required = false) String hash,
            @RequestParam MultiValueMap<String, String> payload
    ) {
        verifyGhtkHash(provider, hash);
        log.info("Incoming form shipping webhook for provider: {}", provider);
        Map<String, Object> normalized = new LinkedHashMap<>();
        payload.forEach((key, values) -> normalized.put(key, values.isEmpty() ? null : values.get(0)));
        return ResponseEntity.ok(handleShippingWebhookUseCase.execute(provider, normalized));
    }

    private void verifyGhtkHash(String provider, String hash) {
        if (!"ghtk".equalsIgnoreCase(provider)) {
            return;
        }
        if (ghtkWebhookHash == null || ghtkWebhookHash.isBlank() || hash == null
                || !MessageDigest.isEqual(
                        ghtkWebhookHash.getBytes(StandardCharsets.UTF_8),
                        hash.getBytes(StandardCharsets.UTF_8)
                )) {
            throw new ForbiddenException("INVALID_GHTK_WEBHOOK_HASH", "Invalid GHTK webhook hash");
        }
    }
}
