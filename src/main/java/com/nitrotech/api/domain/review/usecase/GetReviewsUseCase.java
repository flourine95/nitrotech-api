package com.nitrotech.api.domain.review.usecase;

import com.nitrotech.api.domain.review.dto.ReviewData;
import com.nitrotech.api.domain.review.dto.ReviewStatsData;
import com.nitrotech.api.domain.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetReviewsUseCase {

    private final ReviewRepository reviewRepository;

    public Page<ReviewData> executeByProduct(Long productId, String status, int page, int size) {
        return reviewRepository.findByProductId(productId, status, PageRequest.of(page, size));
    }

    public Page<ReviewData> executeAll(String status, int page, int size) {
        return reviewRepository.findAll(status, PageRequest.of(page, size));
    }

    public Page<ReviewData> executePending(int page, int size) {
        return reviewRepository.findPending(PageRequest.of(page, size));
    }

    public ReviewStatsData stats(Long productId) {
        return reviewRepository.getStats(productId);
    }
}
