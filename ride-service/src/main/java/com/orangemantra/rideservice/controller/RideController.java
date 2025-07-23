package com.orangemantra.rideservice.controller;

import com.orangemantra.rideservice.dto.JoinRequest;
import com.orangemantra.rideservice.model.Ride;
import com.orangemantra.rideservice.service.RideService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ride")
@RequiredArgsConstructor
public class RideController {

    private final RideService rideService;

    @PostMapping("/offer")
    public Ride offerRide(@RequestBody Ride ride) {
        String empId = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        ride.setOwnerEmpId(empId);
        return rideService.offerRide(ride);
    }

    @PostMapping("/join/{id}")
    public String joinRide(@PathVariable Long id, @RequestBody JoinRequest req) {
        return rideService.joinRide(id, req);
    }

    @GetMapping("/route/{route}")
    public List<Ride> getRides(@PathVariable String route) {
        return rideService.getRidesByRoute(route);
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
