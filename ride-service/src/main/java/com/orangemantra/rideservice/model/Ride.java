package com.orangemantra.rideservice.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ownerEmpId;

    private String origin;
    private String destination;

    private LocalDate date;
    private String arrivalTime;

    private String carDetails;

    private int totalSeats;
    private int availableSeats;

    // Fare per seat (currency). Nullable => free ride. Non-negative.
    private BigDecimal fare;

    @ElementCollection
    @Builder.Default
    private List<String> joinedEmpIds = new ArrayList<>();

    // If true passengers are auto-added. If false owner must approve each request.
    @Builder.Default
    private boolean instantBookingEnabled = true;

    // Pending employee IDs waiting for owner approval (used when instantBookingEnabled = false)
    @ElementCollection
    @Builder.Default
    private List<String> pendingEmpIds = new ArrayList<>();

    private String status; // Active, Expired, Cancelled
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
