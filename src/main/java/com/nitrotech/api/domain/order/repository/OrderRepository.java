package com.nitrotech.api.domain.order.repository;

import com.nitrotech.api.domain.order.dto.OrderData;
import com.nitrotech.api.domain.order.dto.OrderListQuery;
import com.nitrotech.api.domain.order.dto.PlaceOrderData;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.Optional;

public interface OrderRepository {
    OrderData place(PlaceOrderData data);
    Optional<OrderData> findByIdAndUserId(Long id, Long userId);
    Optional<OrderData> findById(Long id);
    Page<OrderData> findAll(OrderListQuery query);
    OrderData updateStatus(Long id, String status);
    boolean existsByIdAndUserId(Long id, Long userId);
    int expirePendingCreatedAtOrBefore(Instant cutoff, Instant expiredAt);
}
