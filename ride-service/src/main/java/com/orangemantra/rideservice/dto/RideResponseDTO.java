package com.orangemantra.rideservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RideResponseDTO {
    private Long id;
    private String ownerEmpId;
    private String ownerName; // Added ownerName
    private String origin;
    private String destination;
    private String date;
    private String arrivalTime;
    private String carDetails;
    private int totalSeats;
    private int availableSeats;
    private String status;
    private List<JoinedEmployeeDTO> joinedEmployees;
}
