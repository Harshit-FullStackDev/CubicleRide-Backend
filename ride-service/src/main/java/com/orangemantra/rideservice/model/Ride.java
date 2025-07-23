package com.orangemantra.rideservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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

    @ElementCollection
    private List<String> joinedEmpIds = new ArrayList<>();
}
