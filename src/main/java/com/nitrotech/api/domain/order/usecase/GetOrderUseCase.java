package com.nitrotech.api.domain.order.usecase;

import com.nitrotech.api.domain.order.dto.OrderData;
import com.nitrotech.api.domain.order.repository.OrderRepository;
import com.nitrotech.api.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetOrderUseCase {

    private final OrderRepository orderRepository;

    public OrderData execute(Long id, Long userId) {
        return orderRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));
    }

    // Admin: lấy bất kỳ order
    public OrderData executeById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));
    }
}
