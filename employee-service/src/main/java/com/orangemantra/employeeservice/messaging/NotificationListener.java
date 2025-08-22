package com.orangemantra.employeeservice.messaging;

import com.orangemantra.employeeservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationListener {
    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    private final NotificationService notificationService;

    @KafkaListener(topics = "${spring.kafka.topic.notifications:notifications}", containerFactory = "notificationKafkaListenerContainerFactory")
    public void onNotification(@Payload NotificationEvent event) {
        try {
            if (event == null || event.getUserId() == null || event.getUserId().isBlank()) return;
            notificationService.saveNotification(event.getUserId(), event.getMessage());
        } catch (Exception e) {
            log.error("Failed to persist notification event: {}", e.getMessage());
        }
    }
}

