package com.nitrotech.api.application.review.controller;

import com.nitrotech.api.application.review.request.CreateReviewRequest;
import com.nitrotech.api.application.review.request.ReportReviewRequest;
import com.nitrotech.api.application.review.request.UpdateReviewRequest;
import com.nitrotech.api.domain.review.dto.CreateReviewCommand;
import com.nitrotech.api.domain.review.dto.ReviewData;
import com.nitrotech.api.domain.review.dto.ReviewStatsData;
import com.nitrotech.api.domain.review.usecase.CreateReviewUseCase;
import com.nitrotech.api.domain.review.usecase.DeleteReviewUseCase;
import com.nitrotech.api.domain.review.usecase.GetReviewsUseCase;
import com.nitrotech.api.domain.review.usecase.ModerateReviewUseCase;
import com.nitrotech.api.domain.review.usecase.ReportReviewUseCase;
import com.nitrotech.api.domain.review.usecase.UpdateReviewUseCase;
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
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReviewController {

    private final CreateReviewUseCase createReviewUseCase;
    private final GetReviewsUseCase getReviewsUseCase;
    private final ModerateReviewUseCase moderateReviewUseCase;
    private final UpdateReviewUseCase updateReviewUseCase;
    private final DeleteReviewUseCase deleteReviewUseCase;
    private final ReportReviewUseCase reportReviewUseCase;

    @GetMapping("/products/{productId}/reviews")
    public ResponseEntity<ApiResult<List<ReviewData>>> listByProduct(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "approved") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResult.paged(getReviewsUseCase.executeByProduct(productId, status, page, size)));
    }

    @GetMapping("/products/{productId}/reviews/stats")
    public ResponseEntity<ApiResult<ReviewStatsData>> stats(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResult.ok(getReviewsUseCase.stats(productId)));
    }

    @PostMapping("/reviews")
    @PreAuthorize("hasAuthority('REVIEW_WRITE')")
    public ResponseEntity<ApiResult<ReviewData>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateReviewRequest req
    ) {
        ReviewData data = createReviewUseCase.execute(new CreateReviewCommand(
                principal.id(), req.productId(), req.orderId(),
                req.rating(), req.comment(), req.images()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.created(data));
    }

    @PatchMapping("/reviews/{id}")
    @PreAuthorize("hasAuthority('REVIEW_WRITE')")
    public ResponseEntity<ApiResult<ReviewData>> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateReviewRequest req
    ) {
        return ResponseEntity.ok(ApiResult.ok(updateReviewUseCase.execute(
                principal.id(), id, req.rating(), req.comment(), req.images())));
    }

    @DeleteMapping("/reviews/{id}")
    @PreAuthorize("hasAuthority('REVIEW_WRITE')")
    public ResponseEntity<ApiResult<Void>> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        deleteReviewUseCase.execute(principal.id(), id);
        return ResponseEntity.ok(ApiResult.ok("Deleted successfully"));
    }

    @PostMapping("/reviews/{id}/report")
    @PreAuthorize("hasAuthority('REVIEW_WRITE')")
    public ResponseEntity<ApiResult<Void>> report(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody ReportReviewRequest req
    ) {
        reportReviewUseCase.execute(principal.id(), id, req.reason());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.ok("Reported successfully"));
    }

    @GetMapping("/admin/reviews")
    @PreAuthorize("hasAuthority('REVIEW_MANAGE')")
    public ResponseEntity<ApiResult<List<ReviewData>>> all(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResult.paged(getReviewsUseCase.executeAll(status, page, size)));
    }

    @GetMapping("/admin/reviews/pending")
    @PreAuthorize("hasAuthority('REVIEW_MANAGE')")
    public ResponseEntity<ApiResult<List<ReviewData>>> pending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResult.paged(getReviewsUseCase.executePending(page, size)));
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
