package com.nitrotech.api.domain.review.usecase;

import com.nitrotech.api.domain.review.exception.ReviewNotFoundException;
import com.nitrotech.api.domain.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeleteReviewUseCase {

    private final ReviewRepository reviewRepository;

    public void execute(Long userId, Long reviewId) {
        reviewRepository.findByIdAndUserId(reviewId, userId)
                .orElseThrow(() -> new ReviewNotFoundException());
        reviewRepository.softDelete(reviewId);
    }
}
