package com.nitrotech.api.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "reviews")
@Getter @Setter @NoArgsConstructor
public class ReviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private short rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> images;

    @Column(nullable = false)
    private String status = "pending";

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    private LocalDateTime deletedAt;
}
