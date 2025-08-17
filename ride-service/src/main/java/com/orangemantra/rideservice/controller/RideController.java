package com.orangemantra.rideservice.controller;

import java.time.LocalDate;
import java.util.List;
import java.math.BigDecimal;

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
import jakarta.validation.Valid;
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

    @GetMapping("/health")
    public java.util.Map<String,Object> health(){
        return java.util.Map.of("status","UP","service","ride-service","timestamp",System.currentTimeMillis());
    }

    @PostMapping("/offer")
    public Ride offerRide(@Valid @RequestBody OfferRideRequest request) {
        String empId = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();

        Ride ride = Ride.builder()
                .origin(request.getOrigin())
                .destination(request.getDestination())
                .date(LocalDate.parse(request.getDate()))
                .arrivalTime(request.getArrivalTime())
                .carDetails(request.getCarDetails())
                .totalSeats(request.getTotalSeats())
                .availableSeats(request.getTotalSeats())
                .instantBookingEnabled(request.isInstantBookingEnabled())
                .ownerEmpId(empId)
                .build();

        if (request.getFare() != null && !request.getFare().isBlank()) {
            try {
                BigDecimal fare = new BigDecimal(request.getFare().trim());
                if (fare.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("Fare cannot be negative");
                ride.setFare(fare);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid fare value");
            }
        }

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
    public List<RideResponseDTO> allRides(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "50") int size) {
        rideService.expirePastRidesInternal();
        List<RideResponseDTO> all = rideService.getAllRidesWithEmployeeDetails();
        int from = Math.min(page * size, all.size());
        int to = Math.min(from + size, all.size());
        return all.subList(from, to);
    }

    @GetMapping("/active")
    public List<RideResponseDTO> activeRides(@RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "50") int size) {
        rideService.expirePastRidesInternal();
        List<RideResponseDTO> list = rideService.getActiveRidesWithEmployeeDetails();
        int from = Math.min(page * size, list.size());
        int to = Math.min(from + size, list.size());
        return list.subList(from, to);
    }

    @GetMapping("/my-rides")
    public List<RideResponseDTO> myRides(@RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "50") int size) {
        String empId = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        rideService.expirePastRidesInternal();
        List<RideResponseDTO> list = rideService.getRidesWithEmployeeDetailsByOwner(empId);
        int from = Math.min(page * size, list.size());
        int to = Math.min(from + size, list.size());
        return list.subList(from, to);
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
    public List<RideResponseDTO> getJoinedRides(@PathVariable String empId,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "50") int size) {
        rideService.expirePastRidesInternal();
        List<RideResponseDTO> list = rideService.getJoinedRidesWithEmployeeDetails(empId);
        int from = Math.min(page * size, list.size());
        int to = Math.min(from + size, list.size());
        return list.subList(from, to);
    }

    @PostMapping("/leave/{rideId}")
    public ResponseEntity<Void> leaveRide(@PathVariable Long rideId, @RequestBody JoinRequest req) {
        rideService.leaveRide(rideId, req.getEmpId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/approve/{rideId}")
    public ResponseEntity<Void> approve(@PathVariable Long rideId, @RequestBody JoinRequest req) {
        String ownerEmpId = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        rideService.approveJoin(rideId, ownerEmpId, req.getEmpId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/decline/{rideId}")
    public ResponseEntity<Void> decline(@PathVariable Long rideId, @RequestBody JoinRequest req) {
        String ownerEmpId = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        rideService.declineJoin(rideId, ownerEmpId, req.getEmpId());
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
