package com.orangemantra.rideservice.controller;

import com.orangemantra.rideservice.dto.JoinRequest;
import com.orangemantra.rideservice.dto.OfferRideRequest;
import com.orangemantra.rideservice.dto.RideResponseDTO;
import com.orangemantra.rideservice.model.Ride;
import com.orangemantra.rideservice.service.NotificationService;
import com.orangemantra.rideservice.service.RideService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/ride")
@RequiredArgsConstructor
public class RideController {

    private final RideService rideService;
    private final NotificationService notificationService;

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
    public ResponseEntity<?> joinRide(@PathVariable Long id, @RequestBody JoinRequest req) {
        rideService.joinRide(id, req.getEmpId());
        notificationService.notifyRideOwnerOnJoin(id, req.getEmpId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/search")
    public List<Ride> getRidesByOriginDestination(
            @RequestParam String origin,
            @RequestParam String destination) {
        return rideService.getRidesByOriginAndDestination(origin, destination);
    }

    @GetMapping("/all")
    public List<RideResponseDTO> allRides() {
        return rideService.getAllRidesWithEmployeeDetails();
    }

    @GetMapping("/my-rides")
    public List<RideResponseDTO> myRides() {
        String empId = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        return rideService.getRidesWithEmployeeDetailsByOwner(empId);
    }
    @PutMapping("/edit/{id}")
    public Ride updateRide(@PathVariable Long id, @RequestBody Ride updatedRide) {
        return rideService.updateRide(id, updatedRide);
    }
    @GetMapping("edit/{id}")
    public Ride getRideById(@PathVariable Long id) {
        return rideService.getRideById(id);

    }
    @DeleteMapping("/{id}")
    public void deleteRide(@PathVariable Long id) {
        rideService.deleteRide(id);
    }

    @GetMapping("/joined/{empId}")
    public List<RideResponseDTO> getJoinedRides(@PathVariable String empId) {
        return rideService.getJoinedRidesWithEmployeeDetails(empId);
    }

    @PostMapping("/leave/{rideId}")
    public ResponseEntity<?> leaveRide(@PathVariable Long rideId, @RequestBody JoinRequest req) {
        rideService.leaveRide(rideId, req.getEmpId());
        return ResponseEntity.ok().build();
    }
}
