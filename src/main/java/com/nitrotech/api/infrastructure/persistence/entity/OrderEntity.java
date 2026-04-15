package com.nitrotech.api.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "orders")
@Getter @Setter @NoArgsConstructor
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> shippingAddress;

    @Column(nullable = false)
    private String status = "pending";

    @Column(nullable = false)
    private String paymentMethod = "cod";

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal finalAmount;

    private String promotionCode;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    private LocalDateTime deletedAt;
}
