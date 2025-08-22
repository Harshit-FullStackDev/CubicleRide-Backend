package com.orangemantra.employeeservice.controller;

import com.orangemantra.employeeservice.model.Notification;
import com.orangemantra.employeeservice.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    private NotificationController controller;

    @BeforeEach
    void setup() {
        controller = new NotificationController(notificationService);
    }

    @Test
    void createNotification_delegates() {
        Notification n = new Notification();
        n.setUserId("U1");
        n.setMessage("hello");
        controller.createNotification(n);
        verify(notificationService).saveNotification("U1", "hello");
    }

    @Test
    void getNotifications_delegates() {
        List<Notification> list = List.of(new Notification());
        when(notificationService.getNotifications("U1")).thenReturn(list);
        assertEquals(list, controller.getNotifications("U1"));
    }

    @Test
    void deleteNotification_delegates() {
        controller.deleteNotification(5L);
        verify(notificationService).deleteNotification(5L);
    }
}

