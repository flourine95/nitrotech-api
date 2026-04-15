package com.nitrotech.api.application.inventory.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SetInventoryRequest(
        @NotNull
        @Min(value = 0, message = "Quantity cannot be negative")
        Integer quantity,

        @Min(value = 0, message = "Threshold cannot be negative")
        Integer lowStockThreshold
) {}
