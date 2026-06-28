package com.nitrotech.api.domain.audit.service;

import com.nitrotech.api.domain.audit.AuditActorType;
import com.nitrotech.api.domain.audit.dto.AuditLogCommand;
import com.nitrotech.api.domain.audit.dto.AuditLogData;
import com.nitrotech.api.domain.audit.repository.AuditLogRepository;
import com.nitrotech.api.shared.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private static final Set<String> BACK_OFFICE_ROLES = Set.of("admin", "super_admin", "staff");
    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "token", "apiKey", "api_key", "secret", "authorization", "cookie",
            "accessToken", "refreshToken", "bearer", "session", "set-cookie", "credential", "privateKey"
    );

    private final AuditLogRepository auditLogRepository;

    // Audit entries are committed with the business transaction; rolled-back work should not leave success audit rows.
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(AuditLogCommand command) {
        AuditActor actor = resolveActor(command);
        RequestSnapshot request = requestSnapshot();

        auditLogRepository.save(new AuditLogData(
                request.correlationId(),
                actor.type(),
                actor.id(),
                actor.email(),
                actor.roles(),
                command.action().name(),
                command.resourceType().name(),
                command.resourceId(),
                command.outcome().name(),
                sanitize(command.beforeData()),
                sanitize(command.afterData()),
                sanitize(command.metadata()),
                request.ipAddress(),
                request.userAgent()
        ));
    }

    private AuditActor resolveActor(AuditLogCommand command) {
        if (command.actorType() != null) {
            return new AuditActor(
                    command.actorType().name(),
                    command.actorId(),
                    command.actorEmail(),
                    List.of()
            );
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            List<String> roles = principal.roles().stream().sorted().toList();
            AuditActorType actorType = roles.stream().anyMatch(BACK_OFFICE_ROLES::contains)
                    ? AuditActorType.ADMIN
                    : AuditActorType.USER;
            return new AuditActor(actorType.name(), principal.id(), principal.email(), roles);
        }

        return new AuditActor(AuditActorType.SYSTEM.name(), null, null, List.of());
    }

    private RequestSnapshot requestSnapshot() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return new RequestSnapshot(UUID.randomUUID().toString(), null, null);
        }

        HttpServletRequest request = attributes.getRequest();
        String correlationId = firstNonBlank(request.getHeader("X-Request-Id"), request.getHeader("X-Correlation-Id"));
        return new RequestSnapshot(
                correlationId != null ? correlationId : UUID.randomUUID().toString(),
                clientIp(request),
                request.getHeader("User-Agent")
        );
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private Map<String, Object> sanitize(Map<String, Object> data) {
        if (data == null) {
            return null;
        }
        Map<String, Object> sanitized = new LinkedHashMap<>();
        data.forEach((key, value) -> sanitized.put(key, sanitizeValue(key, value)));
        return sanitized;
    }

    private Object sanitizeValue(String key, Object value) {
        if (isSensitive(key)) {
            return "[REDACTED]";
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sanitized = new LinkedHashMap<>();
            map.forEach((nestedKey, nestedValue) ->
                    sanitized.put(String.valueOf(nestedKey), sanitizeValue(String.valueOf(nestedKey), nestedValue)));
            return sanitized;
        }
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(item -> sanitizeNestedItem(item))
                    .toList();
        }
        return value;
    }

    private Object sanitizeNestedItem(Object item) {
        if (item instanceof Map<?, ?> map) {
            Map<String, Object> sanitized = new LinkedHashMap<>();
            map.forEach((key, value) ->
                    sanitized.put(String.valueOf(key), sanitizeValue(String.valueOf(key), value)));
            return sanitized;
        }
        if (item instanceof List<?> list) {
            return list.stream()
                    .map(this::sanitizeNestedItem)
                    .toList();
        }
        return item;
    }

    private boolean isSensitive(String key) {
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        return SENSITIVE_KEYS.stream().anyMatch(normalized::contains);
    }

    private record AuditActor(String type, Long id, String email, List<String> roles) {
    }

    private record RequestSnapshot(String correlationId, String ipAddress, String userAgent) {
    }
}
