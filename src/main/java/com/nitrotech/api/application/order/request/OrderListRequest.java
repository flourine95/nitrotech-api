package com.nitrotech.api.application.order.request;

import com.nitrotech.api.domain.order.dto.OrderFilter;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
public class OrderListRequest {

    @Size(max = 100, message = "Search query must not exceed 100 characters")
    private String search;

    private String status;
    private String paymentMethod;
    private String createdFrom;
    private String createdTo;
    private BigDecimal amountMin;
    private BigDecimal amountMax;

    public OrderFilter toFilter(Long userId) {
        return new OrderFilter(
                userId,
                blankToNull(search),
                blankToNull(status),
                blankToNull(paymentMethod),
                parseTimestamp(createdFrom),
                parseTimestamp(createdTo),
                amountMin,
                amountMax
        );
    }

    public OrderFilter toCustomerFilter(Long userId) {
        return new OrderFilter(userId, null, blankToNull(status), null, null, null, null, null);
    }

    private Instant parseTimestamp(String value) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            return null;
        }
        return Instant.parse(normalized);
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
