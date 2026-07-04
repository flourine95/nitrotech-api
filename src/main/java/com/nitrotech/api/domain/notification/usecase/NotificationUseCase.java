package com.nitrotech.api.domain.notification.usecase;

import com.nitrotech.api.domain.notification.dto.NotificationData;
import com.nitrotech.api.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NotificationUseCase {

    private final NotificationRepository notificationRepository;

    public List<NotificationData> list(Long userId, Set<String> authorities, int size) {
        return notificationRepository.findVisibleForUser(userId, authorities, size);
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId, Set<String> authorities) {
        notificationRepository.markAsRead(notificationId, userId, authorities);
    }

    @Transactional
    public void markAllAsRead(Long userId, Set<String> authorities) {
        notificationRepository.markAllAsRead(userId, authorities);
    }
}
