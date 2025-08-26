package com.orangemantra.rideservice.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RideResponseDTO {
    private Long id;
    private String ownerEmpId;
    private String ownerName; // Added ownerName
    private String ownerPhone; // Conditionally exposed based on booking/approval
    private String origin;
    private String destination;
    private Double originLat;
    private Double originLng;
    private Double destinationLat;
    private Double destinationLng;
    private String date;
    private String arrivalTime;
    private String carDetails;
    private int totalSeats;
    private int availableSeats;
    private String fare; // string representation (per seat)
    private String status;
    private List<JoinedEmployeeDTO> joinedEmployees;
    private boolean instantBookingEnabled;
    private List<JoinedEmployeeDTO> pendingEmployees; // when instantBookingEnabled=false
    private Integer routeDistanceMeters;
    private Integer routeDurationSeconds;
    private String routeGeometry;
    private String driverNote;
}
