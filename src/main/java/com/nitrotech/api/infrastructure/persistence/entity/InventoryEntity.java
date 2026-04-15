package com.nitrotech.api.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventories")
@Getter @Setter @NoArgsConstructor
public class InventoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long variantId;

    @Column(nullable = false)
    private int quantity = 0;

    @Column(nullable = false)
    private int lowStockThreshold = 5;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}
