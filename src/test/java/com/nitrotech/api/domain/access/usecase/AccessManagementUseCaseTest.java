package com.nitrotech.api.domain.access.usecase;

import com.nitrotech.api.domain.access.dto.RoleData;
import com.nitrotech.api.domain.access.dto.UserAccessData;
import com.nitrotech.api.domain.access.repository.AccessManagementRepository;
import com.nitrotech.api.domain.audit.dto.AuditLogCommand;
import com.nitrotech.api.domain.audit.service.AuditLogService;
import com.nitrotech.api.domain.auth.service.AuthSessionInvalidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

class AccessManagementUseCaseTest {

    private AccessManagementRepository accessRepository;
    private AuditLogService auditLogService;
    private AuthSessionInvalidator authSessionInvalidator;
    private AccessManagementUseCase useCase;

    @BeforeEach
    void setUp() {
        accessRepository = mock(AccessManagementRepository.class);
        auditLogService = mock(AuditLogService.class);
        authSessionInvalidator = mock();
        useCase = new AccessManagementUseCase(accessRepository, auditLogService, authSessionInvalidator);
    }

    @Test
    void updatingUserRolesInvalidatesThatUsersSessions() {
        UserAccessData before = user("user@example.com", Set.of("customer"));
        UserAccessData after = user("user@example.com", Set.of("staff"));

        when(accessRepository.findUserById(2L)).thenReturn(Optional.of(before), Optional.of(after));
        when(accessRepository.findRoleIdsBySlugs(Set.of("staff"))).thenReturn(Set.of(10L));
        when(accessRepository.findPermissionSlugsByRoleSlugs(Set.of("staff")))
                .thenReturn(Set.of("ROLE_MANAGE", "USER_MANAGE_ROLE"));
        when(accessRepository.countUsersWithPermissionExceptUser(2L, "ROLE_MANAGE")).thenReturn(1);
        when(accessRepository.countUsersWithPermissionExceptUser(2L, "USER_MANAGE_ROLE")).thenReturn(1);

        useCase.updateUserRoles(1L, 2L, Set.of("staff"));

        verify(accessRepository).replaceUserRoles(2L, Set.of(10L));
        verify(authSessionInvalidator).invalidateByEmail("user@example.com");
        verify(auditLogService).record(any(AuditLogCommand.class));
    }

    @Test
    void updatingRolePermissionsInvalidatesSessionsForUsersWithThatRole() {
        RoleData before = role(Set.of("ROLE_MANAGE", "USER_MANAGE_ROLE"));
        RoleData after = role(Set.of("ROLE_MANAGE", "USER_MANAGE_ROLE", "ORDER_READ"));

        when(accessRepository.findRoleById(10L)).thenReturn(Optional.of(before), Optional.of(after));
        when(accessRepository.findPermissionIdsBySlugs(Set.of("ROLE_MANAGE", "USER_MANAGE_ROLE", "ORDER_READ")))
                .thenReturn(Set.of(1L, 2L, 3L));
        when(accessRepository.findUserIdsByRoleId(10L)).thenReturn(Set.of(2L));
        when(accessRepository.findEmailsByRoleId(10L)).thenReturn(java.util.List.of("user@example.com"));

        useCase.updateRolePermissions(1L, 10L, Set.of("ROLE_MANAGE", "USER_MANAGE_ROLE", "ORDER_READ"));

        verify(accessRepository).replaceRolePermissions(10L, Set.of(1L, 2L, 3L));
        verify(authSessionInvalidator).invalidateByEmails(java.util.List.of("user@example.com"));
        verify(auditLogService).record(any(AuditLogCommand.class));
    }

    @Test
    void changingRoleActiveStateInvalidatesSessionsForUsersWithThatRole() {
        RoleData before = role(Set.of("ROLE_MANAGE"));
        RoleData after = new RoleData(10L, "Staff", "staff", null, false, false, Set.of("ROLE_MANAGE"));

        when(accessRepository.findRoleById(10L)).thenReturn(Optional.of(before), Optional.of(after));
        when(accessRepository.findEmailsByRoleId(10L)).thenReturn(java.util.List.of("user@example.com"));

        useCase.updateRole(10L, "Staff", null, false);

        verify(accessRepository).updateRole(10L, "Staff", null, false);
        verify(authSessionInvalidator).invalidateByEmails(java.util.List.of("user@example.com"));
        verify(auditLogService).record(any(AuditLogCommand.class));
    }

    @Test
    void blocksAdminFromRemovingTheirOwnCriticalPermissionThroughRoleChange() {
        RoleData before = role(Set.of("ROLE_MANAGE", "USER_MANAGE_ROLE"));

        when(accessRepository.findRoleById(10L)).thenReturn(Optional.of(before));
        when(accessRepository.findPermissionIdsBySlugs(Set.of("USER_MANAGE_ROLE"))).thenReturn(Set.of(2L));
        when(accessRepository.findUserIdsByRoleId(10L)).thenReturn(Set.of(1L));
        when(accessRepository.countUsersWithPermissionExceptRole(10L, "ROLE_MANAGE")).thenReturn(1);

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                useCase.updateRolePermissions(1L, 10L, Set.of("USER_MANAGE_ROLE")))
                .hasMessage("You cannot remove your own ROLE_MANAGE permission");

        verify(accessRepository, never()).replaceRolePermissions(any(), any());
    }

    private static UserAccessData user(String email, Set<String> roles) {
        return new UserAccessData(2L, "User", email, "active", roles, Set.of());
    }

    private static RoleData role(Set<String> permissions) {
        return new RoleData(10L, "Staff", "staff", null, true, false, permissions);
    }
}
