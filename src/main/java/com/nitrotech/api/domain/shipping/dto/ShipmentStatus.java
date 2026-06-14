package com.nitrotech.api.domain.shipping.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum ShipmentStatus {
    READY_TO_PICK("ready_to_pick"),
    PICKED("picked"),
    STORING("storing"),
    TRANSPORTING("transporting"),
    SORTING("sorting"),
    DELIVERING("delivering"),
    DELIVERED("delivered"),
    RETURNING("returning"),
    RETURNED("returned"),
    PICKUP_FAILED("pickup_failed"),
    DELIVERY_FAILED("delivery_failed"),
    CANCEL("cancel"),
    COMPENSATING("compensating"),
    MONEY_COLLECT_DELIVERING("money_collect_delivering"),
    WAITING_TO_RETURN("waiting_to_return"),
    RETURN("return"),
    RETURN_TRANSPORTING("return_transporting"),
    RETURN_SORTING("return_sorting"),
    UNKNOWN("unknown");

    private final String value;

    ShipmentStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static ShipmentStatus fromValue(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        return Arrays.stream(values())
                .filter(status -> status.value.equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElse(UNKNOWN);
    }
}
