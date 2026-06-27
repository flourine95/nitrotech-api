package com.nitrotech.api.domain.review.usecase;

import com.nitrotech.api.domain.review.exception.ReviewAlreadyExistsException;

import com.nitrotech.api.domain.order.OrderStatus;
import com.nitrotech.api.domain.order.exception.OrderNotFoundException;
import com.nitrotech.api.domain.order.repository.OrderRepository;
import com.nitrotech.api.domain.review.dto.CreateReviewCommand;
import com.nitrotech.api.domain.review.dto.ReviewData;
import com.nitrotech.api.domain.review.exception.OrderNotDeliveredException;
import com.nitrotech.api.domain.review.repository.ReviewRepository;
import org.springframework.stereotype.Service;

@Service
public class CreateReviewUseCase {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;

    public CreateReviewUseCase(ReviewRepository reviewRepository, OrderRepository orderRepository) {
        this.reviewRepository = reviewRepository;
        this.orderRepository = orderRepository;
    }

    public ReviewData execute(CreateReviewCommand command) {
        // Kiểm tra order tồn tại và thuộc về user
        var order = orderRepository.findByIdAndUserId(command.orderId(), command.userId())
                .orElseThrow(OrderNotFoundException::new);

        // Chỉ review được khi order đã delivered
        if (!OrderStatus.DELIVERED.value().equals(order.status())) {
            throw new OrderNotDeliveredException();
        }

        // Mỗi order chỉ review 1 lần cho 1 product
        if (reviewRepository.existsByUserIdAndProductIdAndOrderId(
                command.userId(), command.productId(), command.orderId())) {
            throw new ReviewAlreadyExistsException();
        }

        return reviewRepository.create(command);
    }
}
