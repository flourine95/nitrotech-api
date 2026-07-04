package com.nitrotech.api.domain.review.usecase;

import com.nitrotech.api.domain.review.exception.ReviewReportAlreadyExistsException;
import com.nitrotech.api.domain.review.exception.ReviewNotFoundException;
import com.nitrotech.api.domain.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportReviewUseCase {

    private final ReviewRepository reviewRepository;

    public void execute(Long userId, Long reviewId, String reason) {
        reviewRepository.findById(reviewId).orElseThrow(() -> new ReviewNotFoundException());
        if (reviewRepository.reportExists(reviewId, userId)) {
            throw new ReviewReportAlreadyExistsException();
        }
        reviewRepository.report(reviewId, userId, reason);
    }
}
