package com.nitrotech.api.application.review.controller;

import com.nitrotech.api.application.review.request.CreateReviewRequest;
import com.nitrotech.api.domain.review.dto.CreateReviewCommand;
import com.nitrotech.api.domain.review.dto.ReviewData;
import com.nitrotech.api.domain.review.usecase.CreateReviewUseCase;
import com.nitrotech.api.domain.review.usecase.GetReviewsUseCase;
import com.nitrotech.api.domain.review.usecase.ModerateReviewUseCase;
import com.nitrotech.api.shared.response.ApiResult;
import com.nitrotech.api.shared.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api") // Gom chung tiền tố tại đây
@RequiredArgsConstructor
public class ReviewController {

    private final CreateReviewUseCase createReviewUseCase;
    private final GetReviewsUseCase getReviewsUseCase;
    private final ModerateReviewUseCase moderateReviewUseCase;

    @GetMapping("/products/{productId}/reviews")
    public ResponseEntity<ApiResult<List<ReviewData>>> listByProduct(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "approved") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(getReviewsUseCase.executeByProduct(productId, status, page, size));
    }

    @PostMapping("/reviews")
    public ResponseEntity<ApiResult<ReviewData>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateReviewRequest req
    ) {
        ReviewData data = createReviewUseCase.execute(new CreateReviewCommand(
                principal.id(), req.productId(), req.orderId(),
                req.rating(), req.comment(), req.images()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.created(data));
    }

    @GetMapping("/admin/reviews/pending")
    @PreAuthorize("hasAuthority('REVIEW_MANAGE')")
    public ResponseEntity<ApiResult<List<ReviewData>>> pending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(getReviewsUseCase.executePending(page, size));
    }

    @PatchMapping("/admin/reviews/{id}/approve")
    @PreAuthorize("hasAuthority('REVIEW_MANAGE')")
    public ResponseEntity<ApiResult<ReviewData>> approve(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResult.ok(moderateReviewUseCase.approve(id)));
    }

    @PatchMapping("/admin/reviews/{id}/reject")
    @PreAuthorize("hasAuthority('REVIEW_MANAGE')")
    public ResponseEntity<ApiResult<ReviewData>> reject(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResult.ok(moderateReviewUseCase.reject(id)));
    }
}
