package com.nitrotech.api.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "products")
@Getter @Setter @NoArgsConstructor
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long categoryId;

    private Long brandId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String thumbnail;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> specs;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    private LocalDateTime deletedAt;
}
