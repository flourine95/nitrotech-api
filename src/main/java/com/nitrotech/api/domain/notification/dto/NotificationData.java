package com.nitrotech.api.domain.notification.dto;

import java.time.Instant;

public record NotificationData(
        Long id,
        String type,
        String title,
        String message,
        String href,
        Long recipientUserId,
        String requiredAuthority,
        Instant createdAt,
        boolean read
) {
}
