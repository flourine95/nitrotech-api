package com.nitrotech.api.domain.order.usecase;

import com.nitrotech.api.domain.order.dto.OrderFacetItemData;
import com.nitrotech.api.domain.order.dto.OrderFacetsData;
import com.nitrotech.api.domain.order.dto.OrderFilter;
import com.nitrotech.api.domain.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GetOrderFacetsUseCase {

    private static final List<OrderFacetItemData> STATUSES = List.of(
            new OrderFacetItemData("pending", "Chờ xác nhận", 0),
            new OrderFacetItemData("confirmed", "Đã xác nhận", 0),
            new OrderFacetItemData("processing", "Đang xử lý", 0),
            new OrderFacetItemData("shipped", "Đang giao", 0),
            new OrderFacetItemData("delivered", "Hoàn thành", 0),
            new OrderFacetItemData("cancelled", "Đã hủy", 0),
            new OrderFacetItemData("refunded", "Đã hoàn tiền", 0),
            new OrderFacetItemData("expired", "Đã hết hạn", 0)
    );

    private static final List<OrderFacetItemData> PAYMENT_METHODS = List.of(
            new OrderFacetItemData("cod", "COD", 0),
            new OrderFacetItemData("vnpay", "VNPay", 0),
            new OrderFacetItemData("momo", "MoMo", 0),
            new OrderFacetItemData("sepay", "SePay", 0)
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
