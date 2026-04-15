package com.nitrotech.api.domain.review.usecase;

import com.nitrotech.api.domain.order.repository.OrderRepository;
import com.nitrotech.api.domain.review.dto.CreateReviewCommand;
import com.nitrotech.api.domain.review.dto.ReviewData;
import com.nitrotech.api.domain.review.repository.ReviewRepository;
import com.nitrotech.api.shared.exception.ConflictException;
import com.nitrotech.api.shared.exception.DomainException;
import com.nitrotech.api.shared.exception.NotFoundException;
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
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));

        // Chỉ review được khi order đã delivered
        if (!"delivered".equals(order.status())) {
            throw new DomainException("ORDER_NOT_DELIVERED",
                    "You can only review after order is delivered") {};
        }

        // Mỗi order chỉ review 1 lần cho 1 product
        if (reviewRepository.existsByUserIdAndProductIdAndOrderId(
                command.userId(), command.productId(), command.orderId())) {
            throw new ConflictException("REVIEW_ALREADY_EXISTS",
                    "You have already reviewed this product for this order");
        }

        return reviewRepository.create(command);
    }
}
