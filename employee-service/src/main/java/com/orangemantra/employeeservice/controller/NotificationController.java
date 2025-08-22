package com.orangemantra.employeeservice.controller;

import com.orangemantra.employeeservice.model.Notification;
import com.orangemantra.employeeservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final org.springframework.kafka.core.KafkaTemplate<String, com.orangemantra.employeeservice.messaging.NotificationEvent> kafkaTemplate;
    @org.springframework.beans.factory.annotation.Value("${spring.kafka.topic.notifications:notifications}")
    private String notificationsTopic;

    @PostMapping
    public void createNotification(@RequestBody Notification notification) {
        kafkaTemplate.send(notificationsTopic, notification.getUserId(), new com.orangemantra.employeeservice.messaging.NotificationEvent(notification.getUserId(), notification.getMessage()));
    }

    @GetMapping("/{userId}")
    public List<Notification> getNotifications(@PathVariable String userId) {
        return notificationService.getNotifications(userId);
    }

    @DeleteMapping("/{id}")
    public void deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
    }
}