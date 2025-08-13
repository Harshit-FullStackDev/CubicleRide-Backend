package com.orangemantra.employeeservice.repository;

import com.orangemantra.employeeservice.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    Optional<Vehicle> findByEmpId(String empId);
}
