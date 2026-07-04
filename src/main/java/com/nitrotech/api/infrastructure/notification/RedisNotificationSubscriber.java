package com.nitrotech.api.infrastructure.notification;

import com.nitrotech.api.domain.notification.dto.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisNotificationSubscriber implements MessageListener {

    private final SseNotificationService sseNotificationService;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String jsonMessage = new String(message.getBody(), StandardCharsets.UTF_8);
            NotificationEvent event = objectMapper.readValue(jsonMessage, NotificationEvent.class);
            if (event.recipientUserId() != null && !event.recipientUserId().isBlank()) {
                sseNotificationService.sendToUser(event.recipientUserId(), event);
                return;
            }
            if (event.requiredAuthority() == null || event.requiredAuthority().isBlank()) {
                log.warn("Skipping broadcast notification without requiredAuthority: {}", event.id());
                return;
            }
            sseNotificationService.broadcast(event);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Failed to deserialize notification event", e);
        }
    }
}
