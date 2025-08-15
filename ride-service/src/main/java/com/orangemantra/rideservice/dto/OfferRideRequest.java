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
    private String fare; // decimal string; null/blank => free
    private boolean instantBookingEnabled = true; // true: auto accept joins; false: owner reviews
}
