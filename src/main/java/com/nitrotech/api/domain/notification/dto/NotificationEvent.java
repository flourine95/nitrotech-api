package com.nitrotech.api.domain.notification.dto;

import java.io.Serializable;

public record NotificationEvent(
        String id,
        String type,
        String title,
        String message,
        String href,
        String createdAt,
        String recipientUserId,
        String requiredAuthority
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
