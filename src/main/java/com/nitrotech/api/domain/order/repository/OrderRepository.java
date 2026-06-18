package com.nitrotech.api.domain.order.repository;

import com.nitrotech.api.domain.order.dto.OrderData;
import com.nitrotech.api.domain.order.dto.OrderFilter;
import com.nitrotech.api.domain.order.dto.OrderListItemData;
import com.nitrotech.api.domain.order.dto.PlaceOrderData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    OrderData place(PlaceOrderData data);
    Optional<OrderData> findByIdAndUserId(Long id, Long userId);
    Optional<OrderData> findById(Long id);
    Page<OrderListItemData> findList(OrderFilter filter, Pageable pageable);
    long countFacetsTotal(OrderFilter filter);
    List<Object[]> countStatuses(OrderFilter filter);
    List<Object[]> countPaymentMethods(OrderFilter filter);
    OrderData updateStatus(Long id, String status);
    boolean existsByIdAndUserId(Long id, Long userId);
    int expirePendingCreatedAtOrBefore(Instant cutoff, Instant expiredAt);
}
