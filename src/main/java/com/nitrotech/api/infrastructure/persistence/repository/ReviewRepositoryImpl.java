package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.domain.review.dto.CreateReviewCommand;
import com.nitrotech.api.domain.review.dto.ReviewData;
import com.nitrotech.api.domain.review.repository.ReviewRepository;
import com.nitrotech.api.infrastructure.persistence.entity.ReviewEntity;
import com.nitrotech.api.infrastructure.persistence.entity.UserEntity;
import com.nitrotech.api.shared.exception.NotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
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
    public List<ReviewData> findByProductId(Long productId, String status, int page, int size) {
        return jpa.findByProductId(productId, status, PageRequest.of(page, size))
                .getContent().stream().map(this::toData).toList();
    }

    @Override
    public long countByProductId(Long productId, String status) {
        return jpa.findByProductId(productId, status, PageRequest.of(0, Integer.MAX_VALUE))
                .getTotalElements();
    }

    @Override
    public List<ReviewData> findPending(int page, int size) {
        return jpa.findPending(PageRequest.of(page, size)).getContent().stream().map(this::toData).toList();
    }

    @Override
    public long countPending() {
        return jpa.findPending(PageRequest.of(0, Integer.MAX_VALUE)).getTotalElements();
    }

    @Override
    public ReviewData updateStatus(Long id, String status) {
        ReviewEntity entity = jpa.findActiveById(id)
                .orElseThrow(() -> new NotFoundException("REVIEW_NOT_FOUND", "Review not found"));
        entity.setStatus(status);
        entity.setUpdatedAt(LocalDateTime.now());
        return toData(jpa.save(entity));
    }

    @Override
    public boolean existsByUserIdAndProductIdAndOrderId(Long userId, Long productId, Long orderId) {
        return jpa.existsByUserIdAndProductIdAndOrderId(userId, productId, orderId);
    }

    @Override
    public void softDelete(Long id) {
        jpa.findActiveById(id).ifPresent(e -> {
            e.setDeletedAt(LocalDateTime.now());
            jpa.save(e);
        });
    }

    private ReviewData toData(ReviewEntity e) {
        String userName = userJpa.findById(e.getUserId())
                .map(UserEntity::getName).orElse(null);
        return new ReviewData(e.getId(), e.getProductId(), e.getUserId(), userName,
                e.getOrderId(), e.getRating(), e.getComment(), e.getImages(),
                e.getStatus(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
