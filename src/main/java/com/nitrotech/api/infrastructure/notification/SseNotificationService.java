package com.nitrotech.api.infrastructure.notification;

import com.nitrotech.api.domain.notification.dto.NotificationEvent;
import jakarta.annotation.PreDestroy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SseNotificationService {

    static final long SSE_TIMEOUT_MS = Long.MAX_VALUE;

    private final Map<String, List<ClientEmitter>> userEmitters = new ConcurrentHashMap<>();

    public SseEmitter createEmitter(String userId, Set<String> permissions) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        ClientEmitter client = new ClientEmitter(emitter, Set.copyOf(permissions));
        
        List<ClientEmitter> emitters = userEmitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>());
        emitters.add(client);

        Runnable cleanup = () -> userEmitters.computeIfPresent(userId, (key, list) -> {
            list.remove(client);
            return list.isEmpty() ? null : list;
        });

        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());

        return emitter;
    }

    public void sendToUser(String userId, NotificationEvent event) {
        List<ClientEmitter> emitters = userEmitters.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (ClientEmitter client : emitters) {
            send(client, event);
        }
    }

    public void broadcast(NotificationEvent event) {
        if (event.requiredAuthority() == null || event.requiredAuthority().isBlank()) {
            return;
        }
        userEmitters.forEach((userId, emitters) -> {
            for (ClientEmitter client : emitters) {
                if (client.permissions().contains(event.requiredAuthority())) {
                    send(client, event);
                }
            }
        });
    }

    @Scheduled(fixedRate = 15000)
    public void sendHeartbeat() {
        if (userEmitters.isEmpty()) {
            return;
        }
        
        userEmitters.forEach((userId, emitters) -> {
            for (ClientEmitter client : emitters) {
                try {
                    client.emitter().send(SseEmitter.event().comment("heartbeat-ping"));
                } catch (IOException e) {
                    client.emitter().completeWithError(e);
                }
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        userEmitters.values().forEach(emitters -> {
            for (ClientEmitter client : emitters) {
                try {
                    client.emitter().complete();
                } catch (Exception ignored) {
                }
            }
        });
        userEmitters.clear();
    }

    private void send(ClientEmitter client, NotificationEvent event) {
        try {
            client.emitter().send(SseEmitter.event()
                    .id(event.id())
                    .name("notification")
                    .data(event));
        } catch (IOException e) {
            client.emitter().completeWithError(e);
        }
    }

    private record ClientEmitter(SseEmitter emitter, Set<String> permissions) {
    }
}
