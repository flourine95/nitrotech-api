package com.nitrotech.api.application.shipping.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SimulateShipmentEventRequest(
        @NotBlank(message = "Status is required")
        @Size(max = 50, message = "Status must not exceed 50 characters")
        String status,

        @Size(max = 255, message = "Location must not exceed 255 characters")
        String location,

        @Size(max = 500, message = "Note must not exceed 500 characters")
        String note
) {}
