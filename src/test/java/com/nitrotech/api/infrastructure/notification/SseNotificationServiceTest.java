package com.nitrotech.api.infrastructure.notification;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.lang.reflect.Field;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SseNotificationServiceTest {

    @Test
    void createEmitterUsesLongLivedTimeout() throws Exception {
        SseNotificationService service = new SseNotificationService();

        SseEmitter emitter = service.createEmitter("1", Set.of("NOTIFICATION_READ"));

        Field timeout = emitter.getClass().getSuperclass().getDeclaredField("timeout");
        timeout.setAccessible(true);
        assertThat(timeout.get(emitter)).isEqualTo(SseNotificationService.SSE_TIMEOUT_MS);
    }
}
