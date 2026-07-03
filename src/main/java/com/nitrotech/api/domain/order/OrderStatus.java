package com.nitrotech.api.domain.order;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum OrderStatus {
    PENDING("pending"),
    CONFIRMED("confirmed"),
    PROCESSING("processing"),
    SHIPPED("shipped"),
    DELIVERED("delivered"),
    CANCELLED("cancelled"),
    EXPIRED("expired"),
    REFUNDED("refunded");

    private final String value;

    OrderStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    public boolean canTransitionTo(OrderStatus next) {
        return switch (this) {
            case PENDING -> next == CONFIRMED || next == CANCELLED;
            case CONFIRMED -> next == PROCESSING || next == CANCELLED;
            case PROCESSING -> next == SHIPPED;
            case SHIPPED -> next == DELIVERED;
            case DELIVERED -> next == REFUNDED;
            case CANCELLED, EXPIRED, REFUNDED -> false;
        };
    }

    public boolean matches(String value) {
        return this.value.equalsIgnoreCase(value == null ? "" : value.trim());
    }

    public static boolean is(String value, OrderStatus status) {
        return status != null && status.matches(value);
    }

    @JsonCreator
    public static OrderStatus fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
                .filter(status -> status.value.equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElse(null);
    }
}
