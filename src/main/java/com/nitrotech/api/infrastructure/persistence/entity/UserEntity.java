package com.nitrotech.api.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class UserEntity {

    public enum Status {inactive, active, banned, suspended}

    public enum Provider {local, google, facebook}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    private String password;
    private String phone;
    private String avatar;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.inactive;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Provider provider = Provider.local;

    private String providerId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    private LocalDateTime deletedAt;
}
