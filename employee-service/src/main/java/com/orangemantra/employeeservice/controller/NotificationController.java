package com.orangemantra.employeeservice.controller;

import com.orangemantra.employeeservice.model.Notification;
import com.orangemantra.employeeservice.service.NotificationService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private NotificationService notificationService;

    @PostMapping
    public void createNotification(@RequestBody Notification notification) {
        notificationService.saveNotification(notification.getUserId(), notification.getMessage());
    }

    @GetMapping("/{userId}")
    public List<Notification> getNotifications(@PathVariable String userId) {
        return notificationService.getNotifications(userId);
    }
}