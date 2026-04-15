package com.nitrotech.api.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "addresses")
@Getter @Setter @NoArgsConstructor
public class AddressEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String receiver;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String province;

    @Column(nullable = false)
    private String provinceCode;

    @Column(nullable = false)
    private String district;

    @Column(nullable = false)
    private String districtCode;

    @Column(nullable = false)
    private String ward;

    @Column(nullable = false)
    private String wardCode;

    @Column(nullable = false)
    private String street;

    @Column(nullable = false)
    private boolean defaultAddress = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}
