package com.nitrotech.api.domain.order.usecase;

import com.nitrotech.api.domain.order.dto.OrderData;
import com.nitrotech.api.domain.order.dto.OrderListQuery;
import com.nitrotech.api.domain.order.repository.OrderRepository;
import com.nitrotech.api.shared.response.ApiResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetOrdersUseCase {

    private final OrderRepository orderRepository;

    public ApiResult<List<OrderData>> execute(OrderListQuery query) {
        List<OrderData> data = orderRepository.findAll(query);
        long total = orderRepository.countAll(query);
        return ApiResult.paginated(data, query.page(), query.size(), total);
    }
}
