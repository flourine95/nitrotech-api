package com.nitrotech.api.domain.order.usecase;

import com.nitrotech.api.domain.order.dto.OrderFilter;
import com.nitrotech.api.domain.order.dto.OrderListItemData;
import com.nitrotech.api.domain.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetOrdersUseCase {

    private final OrderRepository orderRepository;

    public Page<OrderListItemData> execute(OrderFilter filter, Pageable pageable) {
        return orderRepository.findList(filter, pageable);
    }
}
