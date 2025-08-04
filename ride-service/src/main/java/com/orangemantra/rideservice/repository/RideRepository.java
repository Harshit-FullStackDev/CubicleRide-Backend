package com.orangemantra.rideservice.repository;

import com.orangemantra.rideservice.model.Ride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RideRepository extends JpaRepository<Ride, Long> {
    List<Ride> findByOriginAndDestination(String origin, String destination);
    List<Ride> findByOwnerEmpId(String ownerEmpId);
    List<Ride> findByJoinedEmpIdsContaining(String empId);
}
