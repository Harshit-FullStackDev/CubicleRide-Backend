package com.orangemantra.employeeservice.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String empId; // owner employee id

    private String make;
    private String model;
    private String color;
    private String registrationNumber;
    private Integer capacity; // total seats including driver

    private String proofImageName;

    // Potentially large Base64 data URL; store as LOB (LONGTEXT in MySQL)
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String proofImageUrl; // stored (base64 or local path) for demo only – consider external storage in production

    private String status; // PENDING, APPROVED, REJECTED
    private String rejectionReason;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime verifiedAt;
}
