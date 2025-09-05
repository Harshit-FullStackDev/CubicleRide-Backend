package com.orangemantra.employeeservice.service;

import com.orangemantra.employeeservice.model.Notification;
import com.orangemantra.employeeservice.repository.NotificationRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@AllArgsConstructor

public class NotificationService {

    private NotificationRepository notificationRepository;

    public void saveNotification(String userId, String message) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setMessage(message);
        notificationRepository.save(notification);
    }

    public List<Notification> getNotifications(String userId) {
        return notificationRepository.findByUserId(userId);
    }

    public void deleteNotification(Long id) {
        notificationRepository.deleteById(id);
    }

    public long getNotificationCount(String userId) {
        return notificationRepository.countByUserId(userId);
    }
}