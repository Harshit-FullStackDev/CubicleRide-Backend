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
    public List<Notification> getNotifications(@PathVariable("userId") String userId,
                                              org.springframework.security.core.Authentication authentication) {
        // Ensure user can only access their own notifications
        String authenticatedUserId = authentication.getName();
        if (!authenticatedUserId.equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: Cannot access other user's notifications");
        }
        return notificationService.getNotifications(userId);
    }

    @DeleteMapping("/{id}")
    public void deleteNotification(@PathVariable("id") Long id) {
        notificationService.deleteNotification(id);
    }

    @GetMapping("/{userId}/count")
    public long getNotificationCount(@PathVariable("userId") String userId, 
                                   org.springframework.security.core.Authentication authentication) {
        // Ensure user can only access their own notification count
        String authenticatedUserId = authentication.getName();
        if (!authenticatedUserId.equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: Cannot access other user's notifications");
        }
        return notificationService.getNotificationCount(userId);
    }

    @PostMapping("/{userId}/mark-all-read")
    public void markAllAsRead(@PathVariable("userId") String userId,
                            org.springframework.security.core.Authentication authentication) {
        // Ensure user can only mark their own notifications as read
        String authenticatedUserId = authentication.getName();
        if (!authenticatedUserId.equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: Cannot access other user's notifications");
        }
        notificationService.markAllAsRead(userId);
    }
}