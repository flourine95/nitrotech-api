package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.domain.review.exception.ReviewNotFoundException;

import com.nitrotech.api.domain.review.dto.CreateReviewCommand;
import com.nitrotech.api.domain.review.dto.ReviewData;
import com.nitrotech.api.domain.review.repository.ReviewRepository;
import com.nitrotech.api.infrastructure.persistence.entity.ReviewEntity;
import com.nitrotech.api.infrastructure.persistence.entity.UserEntity;
import com.nitrotech.api.shared.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public class ReviewRepositoryImpl implements ReviewRepository {

    private final ReviewJpaRepository jpa;
    private final UserJpaRepository userJpa;

    public ReviewRepositoryImpl(ReviewJpaRepository jpa, UserJpaRepository userJpa) {
        this.jpa = jpa;
        this.userJpa = userJpa;
    }

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
    public Page<ReviewData> findByProductId(Long productId, String status, Pageable pageable) {
        return jpa.findByProductId(productId, status, pageable).map(this::toData);
    }

    @Override
    public Page<ReviewData> findPending(Pageable pageable) {
        return jpa.findPending(pageable).map(this::toData);
    }

    @Override
    public ReviewData updateStatus(Long id, String status) {
        ReviewEntity entity = jpa.findActiveById(id)
                .orElseThrow(() -> new ReviewNotFoundException());
        entity.setStatus(status);
        return toData(jpa.save(entity));
    }

    @Override
    public boolean existsByUserIdAndProductIdAndOrderId(Long userId, Long productId, Long orderId) {
        return jpa.existsByUserIdAndProductIdAndOrderId(userId, productId, orderId);
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
}
