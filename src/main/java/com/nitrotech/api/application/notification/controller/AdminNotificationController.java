package com.nitrotech.api.application.notification.controller;

import com.nitrotech.api.domain.notification.dto.NotificationData;
import com.nitrotech.api.domain.notification.usecase.NotificationUseCase;
import com.nitrotech.api.infrastructure.notification.SseNotificationService;
import com.nitrotech.api.shared.response.ApiResult;
import com.nitrotech.api.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final SseNotificationService sseNotificationService;
    private final NotificationUseCase notificationUseCase;

    @GetMapping
    @PreAuthorize("hasAuthority('NOTIFICATION_READ')")
    public ResponseEntity<ApiResult<List<NotificationData>>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "30") int size
    ) {
        return ResponseEntity.ok(ApiResult.ok(
                notificationUseCase.list(principal.id(), principal.permissions(), size)
        ));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAuthority('NOTIFICATION_READ')")
    public SseEmitter streamNotifications(@AuthenticationPrincipal UserPrincipal principal) {
        return sseNotificationService.createEmitter(String.valueOf(principal.id()), principal.permissions());
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("hasAuthority('NOTIFICATION_READ')")
    public ResponseEntity<ApiResult<Void>> markAsRead(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        notificationUseCase.markAsRead(id, principal.id(), principal.permissions());
        return ResponseEntity.ok(ApiResult.ok("Notification marked as read"));
    }

    @PatchMapping("/read-all")
    @PreAuthorize("hasAuthority('NOTIFICATION_READ')")
    public ResponseEntity<ApiResult<Void>> markAllAsRead(@AuthenticationPrincipal UserPrincipal principal) {
        notificationUseCase.markAllAsRead(principal.id(), principal.permissions());
        return ResponseEntity.ok(ApiResult.ok("Notifications marked as read"));
    }
}
