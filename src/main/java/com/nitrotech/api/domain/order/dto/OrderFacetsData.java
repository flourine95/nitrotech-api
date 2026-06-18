package com.nitrotech.api.domain.order.dto;

import java.util.List;

public record OrderFacetsData(
        long total,
        List<OrderFacetItemData> statuses,
        List<OrderFacetItemData> paymentMethods
) {}
