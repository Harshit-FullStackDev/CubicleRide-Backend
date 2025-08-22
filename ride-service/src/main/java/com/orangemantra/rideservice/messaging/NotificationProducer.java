package com.orangemantra.rideservice.messaging;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationProducer {

    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    @Value("${spring.kafka.topic.notifications:notifications}")
    private String notificationsTopic;

    public void send(String userId, String message) {
        kafkaTemplate.send(notificationsTopic, userId, new NotificationEvent(userId, message));
    }
}

