package com.orangemantra.rideservice.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notif_emp", columnList = "empId"),
        @Index(name = "idx_notif_emp_read", columnList = "empId,readFlag")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String empId; // recipient
    @Column(length = 500)
    private String message;
    @Column(length = 40)
    private String type; // e.g. JOIN, RIDE_CANCELLED
    private Instant createdAt;
    @Builder.Default
    private boolean readFlag = false;
}
