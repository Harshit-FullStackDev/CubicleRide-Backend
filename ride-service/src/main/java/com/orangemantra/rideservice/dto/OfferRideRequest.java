package com.orangemantra.rideservice.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class OfferRideRequest {
    @NotBlank @Size(max = 100)
    private String origin;
    @NotBlank @Size(max = 100)
    private String destination;
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Date must be yyyy-MM-dd")
    private String date;
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Arrival time must be HH:mm")
    private String arrivalTime;
    @Size(max = 160)
    private String carDetails;
    @Min(1) @Max(8)
    private int totalSeats;
    @Pattern(regexp = "^$|^\\d+(?:\\.\\d{1,2})?$", message = "Fare must be decimal with up to 2 places")
    private String fare; // decimal string; null/blank => free
    private boolean instantBookingEnabled = true; // true: auto accept joins; false: owner reviews
}
