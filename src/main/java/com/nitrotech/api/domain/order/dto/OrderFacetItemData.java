package com.nitrotech.api.domain.order.dto;

public record OrderFacetItemData(
        String value,
        String label,
        long count
) {}
