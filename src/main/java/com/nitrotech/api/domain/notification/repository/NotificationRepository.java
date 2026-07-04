package com.nitrotech.api.domain.notification.repository;

import com.nitrotech.api.domain.notification.dto.NotificationData;
import com.nitrotech.api.domain.notification.dto.NotificationEvent;

import java.util.List;
import java.util.Set;

public interface NotificationRepository {
    NotificationData save(NotificationEvent event);

    List<NotificationData> findVisibleForUser(Long userId, Set<String> authorities, int size);

    boolean markAsRead(Long notificationId, Long userId, Set<String> authorities);

    void markAllAsRead(Long userId, Set<String> authorities);
}
