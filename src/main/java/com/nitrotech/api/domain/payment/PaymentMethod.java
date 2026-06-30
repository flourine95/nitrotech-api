package com.nitrotech.api.domain.payment;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum PaymentMethod {
    COD("cod"),
    VNPAY("vnpay"),
    SEPAY("sepay");

    private final String value;

    PaymentMethod(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static PaymentMethod fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
                .filter(method -> method.value.equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElse(null);
    }
}
