package com.orangemantra.rideservice.repository;

import com.orangemantra.rideservice.model.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByEmpIdOrderByCreatedAtDesc(String empId);
    List<Notification> findByEmpIdOrderByCreatedAtDesc(String empId, Pageable pageable);
    long countByEmpIdAndReadFlagFalse(String empId);

    @Transactional @Modifying
    @Query("UPDATE Notification n SET n.readFlag = true WHERE n.empId = :empId AND n.readFlag = false")
    int markAllRead(@Param("empId") String empId);
}
