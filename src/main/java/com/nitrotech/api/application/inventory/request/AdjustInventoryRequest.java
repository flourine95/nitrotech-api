package com.nitrotech.api.application.inventory.request;

import jakarta.validation.constraints.NotNull;

public record AdjustInventoryRequest(
        @NotNull(message = "Delta is required")
        Integer delta  // dương = nhập kho, âm = xuất kho
) {}
