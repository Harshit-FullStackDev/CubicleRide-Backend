package com.orangemantra.rideservice.repository;

import com.orangemantra.rideservice.model.Ride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RideRepository extends JpaRepository<Ride, Long> {
    List<Ride> findByRoute(String route);
    List<Ride> findByOwnerEmpId(String ownerEmpId);
}
