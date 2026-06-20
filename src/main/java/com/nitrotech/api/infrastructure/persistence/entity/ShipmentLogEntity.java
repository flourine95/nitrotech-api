package com.nitrotech.api.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "shipment_logs")
@Getter @Setter @NoArgsConstructor
public class ShipmentLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    private ShipmentEntity shipment;

    @Column(nullable = false)
    private String status;

    @Column(name = "raw_status")
    private String rawStatus;

    @Column(nullable = false)
    private String source = "SYSTEM";

    @Column
    private String location;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "occurred_at")
    private Instant occurredAt;

    @Column(name = "reason_code")
    private String reasonCode;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
