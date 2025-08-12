package com.orangemantra.rideservice.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.orangemantra.rideservice.dto.JoinRequest;
import com.orangemantra.rideservice.dto.OfferRideRequest;
import com.orangemantra.rideservice.dto.RideResponseDTO;
import com.orangemantra.rideservice.model.Ride;
import com.orangemantra.rideservice.service.NotificationService;
import com.orangemantra.rideservice.service.RideService;

import lombok.RequiredArgsConstructor;

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
    public ResponseEntity<Void> joinRide(@PathVariable Long id, @RequestBody JoinRequest req) {
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
    rideService.expirePastRidesInternal();
    return rideService.getAllRidesWithEmployeeDetails();
    }

    @GetMapping("/active")
    public List<RideResponseDTO> activeRides() {
        rideService.expirePastRidesInternal();
        return rideService.getActiveRidesWithEmployeeDetails();
    }

    @GetMapping("/my-rides")
    public List<RideResponseDTO> myRides() {
        String empId = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
    rideService.expirePastRidesInternal();
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
    rideService.expirePastRidesInternal();
    return rideService.getJoinedRidesWithEmployeeDetails(empId);
    }

    @PostMapping("/leave/{rideId}")
    public ResponseEntity<Void> leaveRide(@PathVariable Long rideId, @RequestBody JoinRequest req) {
        rideService.leaveRide(rideId, req.getEmpId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/history/published")
    public List<RideResponseDTO> publishedHistory() {
        String empId = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        return rideService.getPublishedRideHistory(empId);
    }

    @GetMapping("/history/joined/{empId}")
    public List<RideResponseDTO> joinedHistory(@PathVariable String empId) {
        return rideService.getJoinedRideHistory(empId);
    }
}
