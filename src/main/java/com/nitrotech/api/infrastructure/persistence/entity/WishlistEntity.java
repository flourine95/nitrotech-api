package com.nitrotech.api.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "wishlists")
@Getter @Setter @NoArgsConstructor
@IdClass(WishlistEntity.WishlistId.class)
public class WishlistEntity {

    @Id
    private Long userId;

    @Id
    private Long productId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public record WishlistId(Long userId, Long productId) implements Serializable {}
}
