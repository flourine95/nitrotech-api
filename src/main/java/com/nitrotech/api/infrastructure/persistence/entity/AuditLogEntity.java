package com.nitrotech.api.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "audit_logs")
@Getter @Setter @NoArgsConstructor
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "correlation_id", nullable = false)
    private String correlationId;

    @Column(name = "actor_type", nullable = false)
    private String actorType;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "actor_email")
    private String actorEmail;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "actor_roles", columnDefinition = "jsonb", nullable = false)
    private List<String> actorRoles = List.of();

    @Column(nullable = false)
    private String action;

    @Column(name = "resource_type", nullable = false)
    private String resourceType;

    @Column(name = "resource_id")
    private String resourceId;

    @Column(nullable = false)
    private String outcome;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_data", columnDefinition = "jsonb")
    private Map<String, Object> beforeData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_data", columnDefinition = "jsonb")
    private Map<String, Object> afterData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
