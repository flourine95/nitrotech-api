package com.nitrotech.api.domain.order.repository;

import com.nitrotech.api.domain.order.dto.OrderData;
import com.nitrotech.api.domain.order.dto.OrderListQuery;
import com.nitrotech.api.domain.order.dto.PlaceOrderData;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    OrderData place(PlaceOrderData data);
    Optional<OrderData> findByIdAndUserId(Long id, Long userId);
    Optional<OrderData> findById(Long id);
    List<OrderData> findAll(OrderListQuery query);
    long countAll(OrderListQuery query);
    OrderData updateStatus(Long id, String status);
    boolean existsByIdAndUserId(Long id, Long userId);
}
