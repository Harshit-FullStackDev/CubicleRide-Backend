package com.orangemantra.employeeservice.service;

import com.orangemantra.employeeservice.model.Notification;
import com.orangemantra.employeeservice.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository repository;

    @InjectMocks
    private NotificationService service;

    @Test
    void saveNotification_persistsEntity() {
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        when(repository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.saveNotification("U1", "hello");

        verify(repository).save(captor.capture());
        Notification n = captor.getValue();
        assertEquals("U1", n.getUserId());
        assertEquals("hello", n.getMessage());
    }

    @Test
    void getNotifications_delegatesToRepository() {
        List<Notification> list = List.of(new Notification(), new Notification());
        when(repository.findByUserId("U2")).thenReturn(list);
        List<Notification> res = service.getNotifications("U2");
        assertEquals(2, res.size());
    }

    @Test
    void deleteNotification_callsRepository() {
        service.deleteNotification(5L);
        verify(repository).deleteById(5L);
    }
}

