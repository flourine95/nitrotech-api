package com.nitrotech.api.domain.review.usecase;

import com.nitrotech.api.domain.order.OrderStatus;
import com.nitrotech.api.domain.order.exception.OrderNotFoundException;
import com.nitrotech.api.domain.order.repository.OrderRepository;
import com.nitrotech.api.domain.review.dto.CreateReviewCommand;
import com.nitrotech.api.domain.review.dto.ReviewData;
import com.nitrotech.api.domain.review.exception.OrderNotDeliveredException;
import com.nitrotech.api.domain.review.exception.ReviewAlreadyExistsException;
import com.nitrotech.api.domain.review.exception.ReviewNotAllowedException;
import com.nitrotech.api.domain.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateReviewUseCase {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;

    public ReviewData execute(CreateReviewCommand command) {
        var order = orderRepository.findByIdAndUserId(command.orderId(), command.userId())
                .orElseThrow(OrderNotFoundException::new);

        if (!OrderStatus.is(order.status(), OrderStatus.DELIVERED)) {
            throw new OrderNotDeliveredException();
        }

        if (!reviewRepository.orderContainsProduct(command.orderId(), command.productId())) {
            throw new ReviewNotAllowedException();
        }

        if (reviewRepository.existsByUserIdAndProductIdAndOrderId(
                command.userId(), command.productId(), command.orderId())) {
            throw new ReviewAlreadyExistsException();
        }

        return reviewRepository.create(command);
    }
}
