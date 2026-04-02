package com.nitrotech.api.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "promotions")
@Getter @Setter @NoArgsConstructor
public class PromotionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(unique = true)
    private String code;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal discountValue;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(nullable = false)
    private boolean stackable = false;

    @Column(nullable = false)
    private int priority = 0;

    private Integer usageLimit;

    @Column(nullable = false)
    private int usagePerUser = 1;

    @Column(nullable = false)
    private LocalDateTime startAt;

    @Column(nullable = false)
    private LocalDateTime endAt;

    @Column(nullable = false)
    private String status = "draft";

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}
