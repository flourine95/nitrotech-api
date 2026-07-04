package com.nitrotech.api.infrastructure.notification;

import com.nitrotech.api.domain.notification.dto.NotificationData;
import com.nitrotech.api.domain.notification.dto.NotificationEvent;
import com.nitrotech.api.domain.notification.repository.NotificationRepository;
import com.nitrotech.api.domain.notification.service.NotificationPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisNotificationPublisher implements NotificationPublisher {

    private final StringRedisTemplate redisTemplate;
    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;

    public static final String TOPIC_NAME = "nitrotech:notifications";

    @Transactional
    @Override
    public void publish(NotificationEvent event) {
        NotificationData saved = notificationRepository.save(event);
        NotificationEvent savedEvent = new NotificationEvent(
                String.valueOf(saved.id()),
                saved.type(),
                saved.title(),
                saved.message(),
                saved.href(),
                saved.createdAt() == null ? null : saved.createdAt().toString(),
                saved.recipientUserId() == null ? null : String.valueOf(saved.recipientUserId()),
                saved.requiredAuthority()
        );
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publishToRedis(savedEvent);
            }
        });
    }

    private void publishToRedis(NotificationEvent event) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(TOPIC_NAME, jsonMessage);
        } catch (RuntimeException e) {
            log.warn("Failed to publish notification event {}", event.id(), e);
        }
    }
}
