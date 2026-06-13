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

@Slf4j
@RestController
@RequiredArgsConstructor
public class ShippingWebhookController {

    private final HandleShippingWebhookUseCase handleShippingWebhookUseCase;

    @PostMapping("/api/webhooks/shipping/{provider}")
    public ResponseEntity<Map<String, Object>> receive(
            @PathVariable String provider,
            @RequestBody Map<String, Object> payload
    ) {
        log.info("Incoming shipping webhook for provider: {}", provider);
        return ResponseEntity.ok(handleShippingWebhookUseCase.execute(provider, payload));
    }

    @PostMapping(
            value = "/api/webhooks/shipping/{provider}",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public ResponseEntity<Map<String, Object>> receiveForm(
            @PathVariable String provider,
            @RequestParam MultiValueMap<String, String> payload
    ) {
        log.info("Incoming form shipping webhook for provider: {}", provider);
        Map<String, Object> normalized = new LinkedHashMap<>();
        payload.forEach((key, values) -> normalized.put(key, values.isEmpty() ? null : values.get(0)));
        return ResponseEntity.ok(handleShippingWebhookUseCase.execute(provider, normalized));
    }
}
