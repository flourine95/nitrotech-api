package com.nitrotech.api.application.order.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateOrderStatusRequest(
        @NotBlank(message = "Status is required")
        @Pattern(regexp = "^(confirmed|processing|shipped|delivered|cancelled|refunded)$",
                message = "Invalid status")
        String status
) {}
