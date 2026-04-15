package com.nitrotech.api.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "promotion_usages")
@Getter @Setter @NoArgsConstructor
public class PromotionUsageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long promotionId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long orderId;

    private String usedCode;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal discountAmount;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
