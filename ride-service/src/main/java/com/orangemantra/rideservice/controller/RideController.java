package com.orangemantra.rideservice.controller;

import com.orangemantra.rideservice.dto.JoinRequest;
import com.orangemantra.rideservice.dto.OfferRideRequest;
import com.orangemantra.rideservice.model.Ride;
import com.orangemantra.rideservice.service.RideService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/ride")
@RequiredArgsConstructor
public class RideController {

    private final RideService rideService;

    @PostMapping("/offer")
    public Ride offerRide(@RequestBody OfferRideRequest request) {
        String empId = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();

        Ride ride = Ride.builder()
                .origin(request.getOrigin())
                .destination(request.getDestination())
                .date(LocalDate.parse(request.getDate()))
                .arrivalTime(request.getArrivalTime())
                .carDetails(request.getCarDetails())
                .totalSeats(request.getTotalSeats())
                .availableSeats(request.getTotalSeats())
                .ownerEmpId(empId)
                .build();

        return rideService.offerRide(ride);
    }


    @PostMapping("/join/{id}")
    public String joinRide(@PathVariable Long id, @RequestBody JoinRequest req) {
        return rideService.joinRide(id, req);
    }

    @GetMapping("/search")
    public List<Ride> getRidesByOriginDestination(
            @RequestParam String origin,
            @RequestParam String destination) {
        return rideService.getRidesByOriginAndDestination(origin, destination);
    }

    @GetMapping("/all")
    public List<Ride> allRides() {
        return rideService.getAllRides();
    }

    @GetMapping("/my-rides")
    public List<Ride> myRides() {
        String empId = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        return rideService.getRidesByOwner(empId);
    }
}
