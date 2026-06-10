package com.nitrotech.api.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "wishlists")
@Getter @Setter @NoArgsConstructor
@IdClass(WishlistEntity.WishlistId.class)
public class WishlistEntity {

    @Id
    private Long userId;

    @Id
    private Long productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "productId", insertable = false, updatable = false)
    private ProductEntity product;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public record WishlistId(Long userId, Long productId) implements Serializable {}
}
