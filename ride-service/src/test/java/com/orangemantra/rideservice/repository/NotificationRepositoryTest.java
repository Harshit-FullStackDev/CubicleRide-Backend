package com.orangemantra.rideservice.repository;

import com.orangemantra.rideservice.model.Notification;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class NotificationRepositoryTest {
    @Autowired
    private NotificationRepository repo;

    @Test
    void saveAndCountUnread() {
        Notification n = Notification.builder().empId("e1").message("Hello").type("TEST").createdAt(Instant.now()).readFlag(false).build();
        repo.save(n);
        assertEquals(1, repo.countByEmpIdAndReadFlagFalse("e1"));
        repo.markAllRead("e1");
        assertEquals(0, repo.countByEmpIdAndReadFlagFalse("e1"));
    }
}
