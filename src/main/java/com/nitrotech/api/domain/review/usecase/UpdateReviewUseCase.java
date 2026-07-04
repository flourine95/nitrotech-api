package com.nitrotech.api.domain.review.usecase;

import com.nitrotech.api.domain.review.dto.ReviewData;
import com.nitrotech.api.domain.review.exception.ReviewNotFoundException;
import com.nitrotech.api.domain.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UpdateReviewUseCase {

    private final ReviewRepository reviewRepository;

    public ReviewData execute(Long userId, Long reviewId, Integer rating, String comment, List<String> images) {
        ReviewData current = reviewRepository.findByIdAndUserId(reviewId, userId)
                .orElseThrow(() -> new ReviewNotFoundException());
        return reviewRepository.update(reviewId,
                rating == null ? current.rating() : rating,
                comment == null ? current.comment() : comment,
                images == null ? current.images() : images);
    }
}
