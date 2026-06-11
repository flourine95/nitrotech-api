package com.nitrotech.api.application.payment.request;

import java.util.Map;

public record RawWebhookRequest(
        Map<String, String> headers,
        Map<String, String> queryParams,
        String rawBody
) {}
