package com.orangemantra.rideservice.model;

import jakarta.persistence.*;
import lombok.*;

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
    private String route;
    private int totalSeats;
    private int availableSeats;
    private String arrivalTime;

    @ElementCollection
    private List<String> joinedEmpIds = new ArrayList<>();
}
