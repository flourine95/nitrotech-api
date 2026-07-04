package com.nitrotech.api.domain.review;

public enum ReviewStatus {
    PENDING("pending"),
    APPROVED("approved"),
    REJECTED("rejected");

    private final String value;

    ReviewStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
