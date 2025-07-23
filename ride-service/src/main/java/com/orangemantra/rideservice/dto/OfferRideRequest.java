package com.orangemantra.rideservice.dto;

import lombok.Data;

@Data
public class OfferRideRequest {
    private String origin;
    private String destination;
    private String date;
    private String arrivalTime;  
    private String carDetails;
    private int totalSeats;
}
