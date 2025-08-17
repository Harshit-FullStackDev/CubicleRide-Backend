package com.orangemantra.adminservice.model;

import lombok.Data;

import java.util.List;

@Data
public class RideDTO {
    private Long id;
    private String ownerEmpId;
    private String origin;
    private String destination;
    private String date;
    private String arrivalTime;
    private String carDetails;
    private int totalSeats;
    private int availableSeats;
    private String fare;
    private String status;
    private boolean instantBookingEnabled;
    private List<String> joinedEmpIds; // simple list for admin overview
}
