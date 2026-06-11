package com.nitrotech.api.application.shipping.controller;

import com.nitrotech.api.domain.shipping.usecase.HandleShippingWebhookUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
