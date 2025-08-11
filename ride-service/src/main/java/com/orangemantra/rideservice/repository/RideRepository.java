package com.orangemantra.rideservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.orangemantra.rideservice.model.Ride;

public interface RideRepository extends JpaRepository<Ride, Long> {
    List<Ride> findByOriginAndDestination(String origin, String destination);
    List<Ride> findByOwnerEmpId(String ownerEmpId);
    List<Ride> findByJoinedEmpIdsContaining(String empId);
    List<Ride> findByOwnerEmpIdAndStatus(String ownerEmpId, String status);
    List<Ride> findByJoinedEmpIdsContainingAndStatus(String empId, String status);
    List<Ride> findByStatus(String status);
    List<Ride> findByDateBeforeAndStatus(java.time.LocalDate date, String status);
}
