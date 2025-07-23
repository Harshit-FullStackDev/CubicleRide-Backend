package com.orangemantra.rideservice.service;

import com.orangemantra.rideservice.dto.JoinRequest;
import com.orangemantra.rideservice.model.Ride;
import com.orangemantra.rideservice.repository.RideRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RideService {

    private final RideRepository rideRepository;

    public Ride offerRide(Ride ride) {
        ride.setAvailableSeats(ride.getTotalSeats());
        return rideRepository.save(ride);
    }

    public List<Ride> getRidesByOwner(String ownerEmpId) {
        return rideRepository.findByOwnerEmpId(ownerEmpId);
    }

    public List<Ride> getRidesByOriginAndDestination(String origin, String destination) {
        return rideRepository.findByOriginAndDestination(origin, destination);
    }

    public String joinRide(Long rideId, JoinRequest req) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));

        if (ride.getAvailableSeats() <= 0) {
            throw new RuntimeException("No seats available");
        }

        ride.getJoinedEmpIds().add(req.getEmpId());
        ride.setAvailableSeats(ride.getAvailableSeats() - 1);
        rideRepository.save(ride);

        return "Joined ride successfully";
    }

    public List<Ride> getAllRides() {
        return rideRepository.findAll();
    }
}
