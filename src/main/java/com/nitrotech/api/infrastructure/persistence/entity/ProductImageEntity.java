package com.nitrotech.api.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "product_images")
@Getter @Setter @NoArgsConstructor
public class ProductImageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private int sortOrder = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
