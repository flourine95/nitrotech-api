package com.nitrotech.api.domain.review.usecase;

import com.nitrotech.api.domain.order.dto.OrderData;
import com.nitrotech.api.domain.order.repository.OrderRepository;
import com.nitrotech.api.domain.review.dto.CreateReviewCommand;
import com.nitrotech.api.domain.review.exception.ReviewNotAllowedException;
import com.nitrotech.api.domain.review.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class CreateReviewUseCaseTest {

    private ReviewRepository reviewRepository;
    private OrderRepository orderRepository;
    private CreateReviewUseCase useCase;

    @BeforeEach
    void setUp() {
        reviewRepository = mock(ReviewRepository.class);
        orderRepository = mock(OrderRepository.class);
        useCase = new CreateReviewUseCase(reviewRepository, orderRepository);
    }

    @Test
    void rejectsReviewWhenProductIsNotInDeliveredOrder() {
        when(orderRepository.findByIdAndUserId(20L, 10L)).thenReturn(Optional.of(order()));
        when(reviewRepository.orderContainsProduct(20L, 99L)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(new CreateReviewCommand(
                10L, 99L, 20L, 5, "Good", List.of())))
                .isInstanceOf(ReviewNotAllowedException.class)
                .hasMessage("You can only review products from your delivered order");

        verify(reviewRepository, never()).create(any());
    }

    private OrderData order() {
        return new OrderData(
                20L,
                10L,
                "SO-20",
                null,
                "delivered",
                "cod",
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                Instant.now(),
                Instant.now(),
                null,
                null
        );
    }
}
