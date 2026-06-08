package com.nitrotech.api.application.payment.controller;

import com.nitrotech.api.application.payment.request.SepayWebhookRequest;
import com.nitrotech.api.application.payment.response.SepayWebhookResponse;
import com.nitrotech.api.domain.payment.usecase.HandleSepayWebhookUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks/sepay")
@RequiredArgsConstructor
public class SepayWebhookController {

    private final HandleSepayWebhookUseCase handleSepayWebhookUseCase;

    @Value("${sepay.webhook-api-key:}")
    private String webhookApiKey;

    @PostMapping
    public ResponseEntity<SepayWebhookResponse> receive(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody SepayWebhookRequest request
    ) {
        if (!isAuthorized(authorization)) {
            return ResponseEntity.status(401).body(new SepayWebhookResponse(false));
        }

        handleSepayWebhookUseCase.execute(request);
        return ResponseEntity.ok(new SepayWebhookResponse(true));
    }

    private boolean isAuthorized(String authorization) {
        if (webhookApiKey == null || webhookApiKey.isBlank()) {
            return true;
        }
        return ("Apikey " + webhookApiKey).equals(authorization);
    }
}
