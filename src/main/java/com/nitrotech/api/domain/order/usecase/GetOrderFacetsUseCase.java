package com.nitrotech.api.domain.order.usecase;

import com.nitrotech.api.domain.order.OrderStatus;
import com.nitrotech.api.domain.order.dto.OrderFacetItemData;
import com.nitrotech.api.domain.order.dto.OrderFacetsData;
import com.nitrotech.api.domain.order.dto.OrderFilter;
import com.nitrotech.api.domain.order.repository.OrderRepository;
import com.nitrotech.api.domain.payment.PaymentMethod;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GetOrderFacetsUseCase {

    private static final List<OrderFacetItemData> STATUSES = List.of(
            new OrderFacetItemData(OrderStatus.PENDING.value(), "Chờ xác nhận", 0),
            new OrderFacetItemData(OrderStatus.CONFIRMED.value(), "Đã xác nhận", 0),
            new OrderFacetItemData(OrderStatus.PROCESSING.value(), "Đang xử lý", 0),
            new OrderFacetItemData(OrderStatus.SHIPPED.value(), "Đang giao", 0),
            new OrderFacetItemData(OrderStatus.DELIVERED.value(), "Hoàn thành", 0),
            new OrderFacetItemData(OrderStatus.CANCELLED.value(), "Đã hủy", 0),
            new OrderFacetItemData(OrderStatus.REFUNDED.value(), "Đã hoàn tiền", 0),
            new OrderFacetItemData(OrderStatus.EXPIRED.value(), "Đã hết hạn", 0)
    );

    private static final List<OrderFacetItemData> PAYMENT_METHODS = List.of(
            new OrderFacetItemData(PaymentMethod.COD.value(), "COD", 0),
            new OrderFacetItemData("vnpay", "VNPay", 0),
            new OrderFacetItemData("momo", "MoMo", 0),
            new OrderFacetItemData(PaymentMethod.SEPAY.value(), "SePay", 0)
    );

    private final OrderRepository orderRepository;

    public OrderFacetsData execute(OrderFilter filter) {
        return new OrderFacetsData(
                orderRepository.countFacetsTotal(filter),
                withCounts(STATUSES, orderRepository.countStatuses(filter)),
                withCounts(PAYMENT_METHODS, orderRepository.countPaymentMethods(filter))
        );
    }

    private List<OrderFacetItemData> withCounts(List<OrderFacetItemData> catalog, List<Object[]> rows) {
        Map<String, Long> counts = toMap(rows);
        return catalog.stream()
                .map(item -> new OrderFacetItemData(
                        item.value(),
                        item.label(),
                        counts.getOrDefault(item.value(), 0L)
                ))
                .toList();
    }

    private Map<String, Long> toMap(List<Object[]> rows) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            if (row.length < 2 || row[0] == null || row[1] == null) {
                continue;
            }
            result.put((String) row[0], ((Number) row[1]).longValue());
        }
        return result;
    }
}
