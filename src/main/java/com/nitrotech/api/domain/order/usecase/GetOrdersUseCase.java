package com.nitrotech.api.domain.order.usecase;

import com.nitrotech.api.domain.order.dto.OrderData;
import com.nitrotech.api.domain.order.dto.OrderListQuery;
import com.nitrotech.api.domain.order.repository.OrderRepository;
import com.nitrotech.api.shared.response.ApiResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetOrdersUseCase {

    private final OrderRepository orderRepository;

    public GetOrdersUseCase(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public ApiResponse<List<OrderData>> execute(OrderListQuery query) {
        List<OrderData> data = orderRepository.findAll(query);
        long total = orderRepository.countAll(query);
        return ApiResponse.paginated(data, query.page(), query.size(), total);
    }
}
