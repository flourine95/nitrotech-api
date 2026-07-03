package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.domain.review.ReviewStatus;
import com.nitrotech.api.domain.review.dto.CreateReviewCommand;
import com.nitrotech.api.domain.review.dto.ReviewData;
import com.nitrotech.api.domain.review.dto.ReviewStatsData;
import com.nitrotech.api.domain.review.exception.ReviewNotFoundException;
import com.nitrotech.api.domain.review.repository.ReviewRepository;
import com.nitrotech.api.infrastructure.persistence.entity.ReviewEntity;
import com.nitrotech.api.infrastructure.persistence.entity.ReviewReportEntity;
import com.nitrotech.api.infrastructure.persistence.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReviewRepositoryImpl implements ReviewRepository {

    private final ReviewJpaRepository jpa;
    private final UserJpaRepository userJpa;
    private final ReviewReportJpaRepository reportJpa;

    @Override
    public ReviewData create(CreateReviewCommand command) {
        ReviewEntity entity = new ReviewEntity();
        entity.setProductId(command.productId());
        entity.setUserId(command.userId());
        entity.setOrderId(command.orderId());
        entity.setRating((short) command.rating());
        entity.setComment(command.comment());
        entity.setImages(command.images());
        return toData(jpa.save(entity));
    }

    @Override
    public Optional<ReviewData> findById(Long id) {
        return jpa.findActiveById(id).map(this::toData);
    }

    @Override
    public Optional<ReviewData> findByIdAndUserId(Long id, Long userId) {
        return jpa.findActiveByIdAndUserId(id, userId).map(this::toData);
    }

    @Override
    public Page<ReviewData> findByProductId(Long productId, String status, Pageable pageable) {
        return jpa.findByProductId(productId, status, pageable).map(this::toData);
    }

    @Override
    public Page<ReviewData> findAll(String status, Pageable pageable) {
        return jpa.findAllActive(status, pageable).map(this::toData);
    }

    @Override
    public Page<ReviewData> findPending(Pageable pageable) {
        return jpa.findPending(pageable).map(this::toData);
    }

    @Override
    public ReviewStatsData getStats(Long productId) {
        Object[] stats = jpa.getRatingDistribution(productId);
        return new ReviewStatsData(productId, doubleValue(stats[0]), longValue(stats[1]),
                longValue(stats[2]), longValue(stats[3]), longValue(stats[4]),
                longValue(stats[5]), longValue(stats[6]));
    }

    @Override
    public ReviewData updateStatus(Long id, ReviewStatus status) {
        ReviewEntity entity = jpa.findActiveById(id)
                .orElseThrow(() -> new ReviewNotFoundException());
        entity.setStatus(status.value());
        return toData(jpa.save(entity));
    }

    @Override
    public ReviewData update(Long id, int rating, String comment, List<String> images) {
        ReviewEntity entity = jpa.findActiveById(id)
                .orElseThrow(() -> new ReviewNotFoundException());
        entity.setRating((short) rating);
        entity.setComment(comment);
        entity.setImages(images);
        entity.setStatus(ReviewStatus.PENDING.value());
        return toData(jpa.save(entity));
    }

    @Override
    public boolean existsByUserIdAndProductIdAndOrderId(Long userId, Long productId, Long orderId) {
        return jpa.existsByUserIdAndProductIdAndOrderId(userId, productId, orderId);
    }

    @Override
    public boolean orderContainsProduct(Long orderId, Long productId) {
        return jpa.orderContainsProduct(orderId, productId);
    }

    @Override
    public boolean reportExists(Long reviewId, Long userId) {
        return reportJpa.existsByReviewIdAndUserId(reviewId, userId);
    }

    @Override
    public void report(Long reviewId, Long userId, String reason) {
        ReviewReportEntity entity = new ReviewReportEntity();
        entity.setReviewId(reviewId);
        entity.setUserId(userId);
        entity.setReason(reason);
        reportJpa.save(entity);
    }

    @Override
    public void softDelete(Long id) {
        jpa.findActiveById(id).ifPresent(e -> {
            e.setDeletedAt(Instant.now());
            jpa.save(e);
        });
    }

    private ReviewData toData(ReviewEntity e) {
        String userName = userJpa.findById(e.getUserId())
                .map(UserEntity::getName).orElse(null);
        return new ReviewData(e.getId(), e.getProductId(), e.getUserId(), userName,
                e.getOrderId(), e.getRating().intValue(), e.getComment(), e.getImages(),
                e.getStatus(), e.getCreatedAt(), e.getUpdatedAt());
    }

    private long longValue(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private double doubleValue(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0.0;
    }
}
