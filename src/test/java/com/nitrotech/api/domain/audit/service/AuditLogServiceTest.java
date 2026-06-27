package com.nitrotech.api.domain.audit.service;

import com.nitrotech.api.domain.audit.dto.AuditLogCommand;
import com.nitrotech.api.domain.audit.dto.AuditLogData;
import com.nitrotech.api.domain.audit.AuditAction;
import com.nitrotech.api.domain.audit.AuditResourceType;
import com.nitrotech.api.domain.audit.repository.AuditLogRepository;
import com.nitrotech.api.shared.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AuditLogServiceTest {

    private AuditLogRepository repository;
    private AuditLogService service;

    @BeforeEach
    void setUp() {
        repository = mock(AuditLogRepository.class);
        service = new AuditLogService(repository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recordsCurrentUserAndRedactsSensitiveKeys() {
        UserPrincipal principal = new UserPrincipal(
                7L,
                "admin@example.com",
                "Admin",
                Set.of("admin"),
                Set.of("ROLE_MANAGE")
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        service.record(AuditLogCommand.success(
                AuditAction.ROLE_PERMISSION_UPDATED,
                AuditResourceType.ROLE,
                1L,
                Map.of("permissionSlugs", Set.of("ROLE_READ")),
                Map.of(
                        "permissionSlugs", Set.of("ROLE_READ", "ROLE_MANAGE"),
                        "apiToken", "secret",
                        "headers", Map.of("authorization", "Bearer abc", "traceId", "trace-1"),
                        "events", List.of(Map.of("refreshToken", "refresh-secret", "device", "browser"))
                ),
                null
        ));

        ArgumentCaptor<AuditLogData> captor = ArgumentCaptor.forClass(AuditLogData.class);
        verify(repository).save(captor.capture());
        AuditLogData data = captor.getValue();

        assertThat(data.actorType()).isEqualTo("ADMIN");
        assertThat(data.actorId()).isEqualTo(7L);
        assertThat(data.actorEmail()).isEqualTo("admin@example.com");
        assertThat(data.actorRoles()).containsExactly("admin");
        assertThat(data.action()).isEqualTo("ROLE_PERMISSION_UPDATED");
        assertThat(data.afterData()).containsEntry("apiToken", "[REDACTED]");
        assertThat(data.afterData()).containsEntry("headers", Map.of("authorization", "[REDACTED]", "traceId", "trace-1"));
        assertThat((List<?>) data.afterData().get("events"))
                .singleElement()
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsEntry("refreshToken", "[REDACTED]")
                .containsEntry("device", "browser");
    }

    @Test
    void recordsCustomerPrincipalAsUserActor() {
        UserPrincipal principal = new UserPrincipal(
                8L,
                "customer@example.com",
                "Customer",
                Set.of("customer"),
                Set.of("ORDER_CANCEL_OWN")
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        service.record(AuditLogCommand.success(
                AuditAction.ORDER_CANCELLED,
                AuditResourceType.ORDER,
                10L,
                null,
                Map.of("status", "cancelled"),
                null
        ));

        ArgumentCaptor<AuditLogData> captor = ArgumentCaptor.forClass(AuditLogData.class);
        verify(repository).save(captor.capture());

        assertThat(captor.getValue().actorType()).isEqualTo("USER");
    }
}
