package com.nitrotech.api.domain.notification.service;

import com.nitrotech.api.domain.notification.dto.NotificationEvent;

public interface NotificationPublisher {
    void publish(NotificationEvent event);
}
